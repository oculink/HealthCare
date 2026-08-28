package com.fyp.healthcare

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import org.json.JSONArray
import org.json.JSONObject

/**
 * HEALTH DATA STORAGE
 * - Local SharedPreferences holds the LATEST reading of each type (fast, offline-safe reads).
 * - Local "history" keeps every recorded set with its timestamp, so the Health Trends screen
 *   can compute Min / Avg / Max and draw the line chart without a network round-trip.
 * - Every recording is ALSO written to Firestore: users/{uid}/readings/{auto} — one doc per
 *   recorded set, timestamped. That subcollection is the cloud history / caregiver view.
 */

class HealthDataManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("health_data", Context.MODE_PRIVATE)

    // Weight is NOT recorded here — it's part of the health profile (set at onboarding,
    // editable in Settings). The Record Data screen only takes point-in-time vitals.
    fun saveReadings(
        bloodPressure: String,
        bloodSugar: String,
        heartRate: String,
        temperature: String,
        oxygen: String,
    ) {
        prefs.edit()
            .putString("blood_pressure", bloodPressure)
            .putString("blood_sugar", bloodSugar)
            .putString("heart_rate", heartRate)
            .putString("temperature", temperature)
            .putString("oxygen", oxygen)
            .apply()

        appendHistory(bloodPressure, bloodSugar, heartRate, temperature, oxygen)
        pushReading(bloodPressure, bloodSugar, heartRate, temperature, oxygen)
    }

    fun getBloodPressure(): String? = prefs.getString("blood_pressure", null)
    fun getBloodSugar(): String? = prefs.getString("blood_sugar", null)
    fun getHeartRate(): String? = prefs.getString("heart_rate", null)
    fun getTemperature(): String? = prefs.getString("temperature", null)
    fun getOxygen(): String? = prefs.getString("oxygen", null)

    // ---- local history ----

    /** One recorded set of readings. Blank string means "not entered". */
    data class Reading(
        val timestamp: Long,
        val bloodPressure: String,
        val bloodSugar: String,
        val heartRate: String,
        val temperature: String,
        val oxygen: String,
    ) {
        val heartRateBpm: Int? get() = heartRate.trim().toIntOrNull()
        val bloodSugarValue: Float? get() = bloodSugar.trim().toFloatOrNull()
        val oxygenValue: Float? get() = oxygen.trim().toFloatOrNull()
        /** Systolic reading from a "120/80" string. */
        val systolic: Int? get() = bloodPressure.split("/").getOrNull(0)?.trim()?.toIntOrNull()
    }

    private fun appendHistory(
        bloodPressure: String, bloodSugar: String, heartRate: String,
        temperature: String, oxygen: String,
    ) {
        val arr = rawHistory()
        arr.put(
            JSONObject().apply {
                put("ts", System.currentTimeMillis())
                put("bp", bloodPressure)
                put("bs", bloodSugar)
                put("hr", heartRate)
                put("temp", temperature)
                put("ox", oxygen)
            }
        )
        // keep the list bounded — a couple of years of daily readings is plenty
        val trimmed = if (arr.length() > MAX_HISTORY) {
            JSONArray().also { out ->
                for (i in arr.length() - MAX_HISTORY until arr.length()) out.put(arr.get(i))
            }
        } else arr
        prefs.edit().putString(KEY_HISTORY, trimmed.toString()).apply()
    }

    private fun rawHistory(): JSONArray =
        runCatching { JSONArray(prefs.getString(KEY_HISTORY, "[]")) }.getOrDefault(JSONArray())

    /** Every recorded set, oldest first. */
    fun history(): List<Reading> {
        val arr = rawHistory()
        val out = ArrayList<Reading>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(
                Reading(
                    timestamp = o.optLong("ts"),
                    bloodPressure = o.optString("bp"),
                    bloodSugar = o.optString("bs"),
                    heartRate = o.optString("hr"),
                    temperature = o.optString("temp"),
                    oxygen = o.optString("ox"),
                )
            )
        }
        return out.sortedBy { it.timestamp }
    }

    // ---- cloud ----

    private fun pushReading(
        bloodPressure: String, bloodSugar: String, heartRate: String,
        temperature: String, oxygen: String,
        countInCommunity: Boolean = true,
    ) {
        Cloud.userDoc?.collection("readings")?.add(
            mapOf(
                "bloodPressure" to bloodPressure,
                "bloodSugar" to bloodSugar,
                "heartRate" to heartRate,
                "temperature" to temperature,
                "oxygen" to oxygen,
                // serverTimestamp is authoritative once synced; clientTime is always present
                // (even offline / before the server round-trip) so trends can sort/aggregate.
                "recordedAt" to FieldValue.serverTimestamp(),
                "clientTime" to System.currentTimeMillis(),
            )
        )
        if (countInCommunity) {
            Cloud.bumpCommunityStats(communityValues(bloodPressure, bloodSugar, heartRate, oxygen))
        }
    }

    /** Pull the in-range numeric vitals out of a recorded set for the community aggregate. */
    private fun communityValues(
        bloodPressure: String, bloodSugar: String, heartRate: String, oxygen: String,
    ): Map<String, Double> = buildMap {
        heartRate.trim().toIntOrNull()?.takeIf { it in 20..250 }?.let { put("hr", it.toDouble()) }
        bloodPressure.split("/").getOrNull(0)?.trim()?.toIntOrNull()
            ?.takeIf { it in 70..250 }?.let { put("sys", it.toDouble()) }
        bloodSugar.trim().toDoubleOrNull()?.takeIf { it in 20.0..600.0 }?.let { put("sugar", it) }
        oxygen.trim().toDoubleOrNull()?.takeIf { it in 50.0..100.0 }?.let { put("oxy", it) }
    }

    /**
     * Pull the full reading history from Firestore (users/{uid}/readings) and map it to the
     * same [Reading] shape the Health Trends screen aggregates.
     *
     * [fromServer] = false reads the on-device Firestore cache only (instant, offline-safe);
     * true forces a network fetch. Throws if signed out or the fetch fails — the caller
     * decides whether to fall back to the cache or to local [history].
     */
    suspend fun cloudHistory(fromServer: Boolean): List<Reading> {
        val col = Cloud.userDoc?.collection("readings") ?: return emptyList()
        val snap = col.get(if (fromServer) Source.SERVER else Source.CACHE).awaitResult()
        return snap.documents.map { d ->
            Reading(
                timestamp = d.getTimestamp("recordedAt")?.toDate()?.time
                    ?: d.getLong("clientTime")
                    ?: 0L,
                bloodPressure = d.getString("bloodPressure").orEmpty(),
                bloodSugar = d.getString("bloodSugar").orEmpty(),
                heartRate = d.getString("heartRate").orEmpty(),
                temperature = d.getString("temperature").orEmpty(),
                oxygen = d.getString("oxygen").orEmpty(),
            )
        }.sortedBy { it.timestamp }
    }

    /** Backlog: if anything was recorded before cloud sync, push the current latest as one reading. */
    fun syncLatestToCloud() {
        val any = listOf(
            getBloodPressure(), getBloodSugar(), getHeartRate(),
            getTemperature(), getOxygen(),
        ).any { it != null }
        if (!any) return
        // Backlog only mirrors the latest snapshot — don't fold it into the community
        // aggregate (those readings were already counted when first recorded, or are a
        // one-off snapshot that shouldn't skew the average).
        pushReading(
            getBloodPressure().orEmpty(), getBloodSugar().orEmpty(), getHeartRate().orEmpty(),
            getTemperature().orEmpty(), getOxygen().orEmpty(),
            countInCommunity = false,
        )
    }

    private companion object {
        const val KEY_HISTORY = "reading_history_v1"
        const val MAX_HISTORY = 800
    }
}
