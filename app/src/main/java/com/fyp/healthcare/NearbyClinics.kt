package com.fyp.healthcare

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Nearby Clinics, data layer.
 *
 * Finds hospitals / clinics / pharmacies / doctors around a coordinate by querying the
 * free, key-less Overpass API over OpenStreetMap data. No Google Cloud project, no
 * billing account, no API key. The only etiquette rules are a real User-Agent and not
 * hammering the endpoint (we query on screen-open and on an explicit "search this area").
 *
 * Overpass is community-run and can occasionally be slow or down, so the last successful
 * result is cached to SharedPreferences and returned as a fallback - a demo is never empty.
 */
object NearbyClinics {

    data class Place(
        val id: Long,
        val name: String,
        val kind: String,
        val lat: Double,
        val lon: Double,
        val phone: String?,
        val hours: String?,
        val address: String?,
    )

    private val ENDPOINTS = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter",
        "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
    )
    private const val PREFS = "nearby_clinics"
    private const val KINDS = "^(clinic|hospital|pharmacy|doctors)$"

    // Hits all mirrors at once plus a quick short-radius "nearby" probe; results are
    // merged and streamed to `onPartial` as each request lands. Detached launches
    // keep running (until their socket times out) without blocking the caller.
    private val raceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private const val RACE_BUDGET_MS = 22_000L
    private const val LINGER_MS = 4_000L
    private const val PROBE_RADIUS_M = 1_800

    private class Reply(val full: Boolean, val places: List<Place>?)

    private fun query(lat: Double, lon: Double, radiusM: Int) = """
        [out:json][timeout:20];
        (
          node["amenity"~"$KINDS"](around:$radiusM,$lat,$lon);
          way["amenity"~"$KINDS"](around:$radiusM,$lat,$lon);
        );
        out center 120;
    """.trimIndent()

    /**
     * Search around a point (metres). `onPartial` is invoked (on the caller's context)
     * with the running union of results every time a request lands, so the UI can show
     * the nearest hits within a couple of seconds and grow the list as more arrive.
     * Falls back to the last cached result if every request fails.
     */
    suspend fun search(
        context: Context,
        lat: Double,
        lon: Double,
        radiusM: Int = 3000,
        onPartial: (List<Place>) -> Unit = {},
    ): Result {
        val merged = LinkedHashMap<Long, Place>()
        fun mergeAndEmit(list: List<Place>) {
            val snapshot = synchronized(merged) {
                list.forEach { merged[it.id] = it }
                merged.values.toList()
            }
            onPartial(snapshot)
        }

        val total = ENDPOINTS.size + 1
        val replies = Channel<Reply>(total)

        raceScope.launch {
            val r = runCatching { fetch(ENDPOINTS[0], lat, lon, minOf(radiusM, PROBE_RADIUS_M)) }.getOrNull()
            replies.trySend(Reply(full = false, places = r))
        }
        ENDPOINTS.forEach { endpoint ->
            raceScope.launch {
                val r = runCatching { fetch(endpoint, lat, lon, radiusM) }
                    .onFailure { Log.w("NearbyClinics", "endpoint failed: $endpoint — ${it.message}") }
                    .getOrNull()
                replies.trySend(Reply(full = true, places = r))
            }
        }

        var anySuccess = false
        var seen = 0
        withTimeoutOrNull(RACE_BUDGET_MS) {
            var haveFull = false
            while (seen < total) {
                val rep = replies.receive()
                seen++
                if (rep.places != null) {
                    anySuccess = true
                    mergeAndEmit(rep.places)
                    if (rep.full) haveFull = true
                }
                if (haveFull) {
                    withTimeoutOrNull(LINGER_MS) {
                        while (seen < total) {
                            val more = replies.receive()
                            seen++
                            if (more.places != null) { anySuccess = true; mergeAndEmit(more.places) }
                        }
                    }
                    break
                }
            }
        }
        replies.close()

        return withContext(Dispatchers.IO) {
            if (anySuccess) {
                val all = synchronized(merged) { merged.values.toList() }
                if (all.isNotEmpty()) cache(context, all)
                Result(all, fromCache = false, error = null)
            } else {
                Result(cached(context), fromCache = true, error = "clinics service unavailable")
            }
        }
    }

    data class Result(val places: List<Place>, val fromCache: Boolean, val error: String?)

    data class Suggestion(val label: String, val lat: Double, val lon: Double)

    /**
     * Address autocomplete via Photon (photon.komoot.io) - a free, key-less geocoder built
     * for type-ahead. Returns a handful of ranked matches for a partial address string.
     */
    suspend fun suggestAddresses(text: String): List<Suggestion> = withContext(Dispatchers.IO) {
        if (text.trim().length < 3) return@withContext emptyList()
        runCatching {
            val url = URL(
                "https://photon.komoot.io/api/?limit=6&q=" + URLEncoder.encode(text, "UTF-8")
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 12_000
                setRequestProperty("User-Agent", "CareApp/0.9 (FYP; Android)")
            }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()
            if (code !in 200..299) return@runCatching emptyList<Suggestion>()
            val feats = JSONObject(body).optJSONArray("features") ?: return@runCatching emptyList()
            (0 until feats.length()).mapNotNull { i ->
                val f = feats.optJSONObject(i) ?: return@mapNotNull null
                val coords = f.optJSONObject("geometry")?.optJSONArray("coordinates")
                    ?: return@mapNotNull null
                val lon = coords.optDouble(0, Double.NaN)
                val lat = coords.optDouble(1, Double.NaN)
                if (lat.isNaN() || lon.isNaN()) return@mapNotNull null
                val p = f.optJSONObject("properties") ?: JSONObject()
                val street = listOf(p.optString("housenumber"), p.optString("street"))
                    .filter { it.isNotBlank() }.joinToString(" ")
                val label = listOf(
                    p.optString("name"),
                    street,
                    p.optString("city").ifBlank { p.optString("county") },
                    p.optString("state"),
                    p.optString("country"),
                ).filter { it.isNotBlank() }.distinct().joinToString(", ")
                if (label.isBlank()) null else Suggestion(label, lat, lon)
            }
        }.getOrDefault(emptyList())
    }

    /** Reverse-geocode a coordinate to a short human address via Nominatim (free, key-less). */
    suspend fun reverseGeocode(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(
                "https://nominatim.openstreetmap.org/reverse" +
                    "?format=json&zoom=18&addressdetails=0&lat=$lat&lon=$lon"
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 15_000
                setRequestProperty("User-Agent", "CareApp/0.9 (FYP; Android)")
                setRequestProperty("Accept-Language", "en")
            }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()
            if (code !in 200..299) return@runCatching null
            val full = JSONObject(text).optString("display_name").ifBlank { null }
            full?.split(",")?.map { it.trim() }?.take(4)?.joinToString(", ")
        }.getOrNull()
    }

    private fun fetch(endpoint: String, lat: Double, lon: Double, radiusM: Int): List<Place> {
        val body = "data=" + URLEncoder.encode(query(lat, lon, radiusM), "UTF-8")
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("User-Agent", "CareApp/0.9 (FYP; Android; contact via app store listing)")
        }
        conn.outputStream.use { it.write(body.toByteArray()) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        conn.disconnect()
        if (code !in 200..299) error("Overpass HTTP $code")
        return parse(text)
    }

    private fun parse(json: String): List<Place> {
        val els = JSONObject(json).optJSONArray("elements") ?: return emptyList()
        val out = ArrayList<Place>(els.length())
        for (i in 0 until els.length()) {
            val e = els.optJSONObject(i) ?: continue
            val tags = e.optJSONObject("tags") ?: continue
            val center = e.optJSONObject("center")
            val lat = when {
                e.has("lat") -> e.optDouble("lat")
                center != null -> center.optDouble("lat", Double.NaN)
                else -> Double.NaN
            }
            val lon = when {
                e.has("lon") -> e.optDouble("lon")
                center != null -> center.optDouble("lon", Double.NaN)
                else -> Double.NaN
            }
            if (lat.isNaN() || lon.isNaN()) continue
            val amenity = tags.optString("amenity").ifBlank { "clinic" }
            out += Place(
                id = e.optLong("id", i.toLong()),
                name = tags.optString("name")
                    .ifBlank { amenity.replaceFirstChar { c -> c.uppercase() } },
                kind = amenity,
                lat = lat,
                lon = lon,
                phone = tags.optString("phone")
                    .ifBlank { tags.optString("contact:phone") }
                    .ifBlank { null },
                hours = tags.optString("opening_hours").ifBlank { null },
                address = listOf(
                    tags.optString("addr:housenumber"),
                    tags.optString("addr:street"),
                    tags.optString("addr:city"),
                ).filter { it.isNotBlank() }.joinToString(" ").ifBlank { null },
            )
        }
        return out
    }

    /**
     * Best-effort "open right now?" from an OSM `opening_hours` string.
     * Handles the common shapes (`24/7`, `Mo-Fr 09:00-18:30; Sa 10:00-16:00`,
     * `Mo-Su 10:00-21:00`, comma day lists, multiple time ranges). Returns null when
     * there are no hours or the string is too exotic to read.
     */
    fun openNow(hours: String?): Boolean? {
        val h = hours?.trim().orEmpty()
        if (h.isEmpty()) return null
        if (h.equals("24/7", true) || h.contains("24/7")) return true

        val cal = Calendar.getInstance()
        val todayIdx = cal.get(Calendar.DAY_OF_WEEK) - 1
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val names = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
        val timeRange = Regex("""(\d{1,2}):(\d{2})\s*-\s*(\d{1,2}):(\d{2})""")

        var matchedTodayRule = false
        for (raw in h.split(";")) {
            val rule = raw.trim()
            if (rule.isEmpty()) continue
            val ranges = timeRange.findAll(rule).toList()
            if (ranges.isEmpty()) continue

            val daySpec = Regex("""^([A-Za-z][A-Za-z,\- ]*?)(?=\d|PH|SH|off|closed|$)""")
                .find(rule)?.groupValues?.get(1)?.trim().orEmpty()
            if (daySpec.isNotEmpty() && !dayMatches(daySpec, todayIdx, names)) continue

            matchedTodayRule = true
            for (m in ranges) {
                val (h1, m1, h2, m2) = m.destructured
                val start = h1.toInt() * 60 + m1.toInt()
                var end = h2.toInt() * 60 + m2.toInt()
                if (end <= start) end += 24 * 60
                if (nowMin in start until end) return true
            }
        }
        return if (matchedTodayRule) false else null
    }

    private fun dayMatches(spec: String, todayIdx: Int, names: List<String>): Boolean {
        val idx = { s: String -> names.indexOfFirst { it.equals(s.trim().take(2), true) } }
        for (part in spec.split(",")) {
            val p = part.trim()
            if (p.contains("-")) {
                val (a, b) = p.split("-", limit = 2).map { idx(it) }
                if (a < 0 || b < 0) continue
                var d = a
                while (true) {
                    if (d == todayIdx) return true
                    if (d == b) break
                    d = (d + 1) % 7
                }
            } else {
                if (idx(p) == todayIdx) return true
            }
        }
        return false
    }

    /** Great-circle distance in metres. */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }


    private fun cache(context: Context, places: List<Place>) {
        val arr = JSONArray()
        places.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id); put("name", p.name); put("kind", p.kind)
                put("lat", p.lat); put("lon", p.lon)
                put("phone", p.phone ?: JSONObject.NULL)
                put("hours", p.hours ?: JSONObject.NULL)
                put("address", p.address ?: JSONObject.NULL)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("last", arr.toString()).apply()
    }

    private fun cached(context: Context): List<Place> {
        val s = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("last", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(s)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Place(
                    id = o.optLong("id"),
                    name = o.optString("name"),
                    kind = o.optString("kind"),
                    lat = o.optDouble("lat"),
                    lon = o.optDouble("lon"),
                    phone = o.optString("phone").ifBlank { null }.takeIf { it != "null" },
                    hours = o.optString("hours").ifBlank { null }.takeIf { it != "null" },
                    address = o.optString("address").ifBlank { null }.takeIf { it != "null" },
                )
            }
        }.getOrElse { emptyList() }
    }
}
