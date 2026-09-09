package com.fyp.healthcare

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Optional online refresh for [AllergyCatalog]. Pulls a hosted JSON list of allergens so
 * the picker can grow without shipping a new app build, and caches it on disk so it keeps
 * working offline afterwards. Everything here is best-effort: any failure (offline, 404,
 * bad JSON) leaves [AllergyCatalog.BUILT_IN] as the list, and the picker never blocks on it.
 *
 * Hosting the JSON
 * [REMOTE_URL] points at a plain file in this repo, served raw by GitHub. To update the
 * list later, edit `docs/allergy_catalog.json`, commit and push - every installed client
 * picks it up within [TTL_MS]. No server, no Firestore rules.
 *
 * Expected shape - a JSON array of objects:
 *   [
 *     { "name": "Kiwi",   "category": "Food",          "note": "cross-reacts with latex" },
 *     { "name": "Quinoa", "category": "Food" }
 *   ]
 * `category` is matched leniently by [AllergyCatalog.Category.of]; `note` is optional.
 *
 * The AllergenOnline database (allergenonline.org) has no API or CSV/JSON feed - only an
 * annual PDF - so this file is the maintainer's own curated extract, not a live mirror.
 */
object AllergyCatalogRemote {

    private const val REMOTE_URL =
        "https://raw.githubusercontent.com/oculink/HealthCare/main/docs/allergy_catalog.json"

    private const val CACHE_FILE = "allergy_catalog_cache.json"
    private const val TTL_MS = 12L * 60 * 60 * 1000

    private fun cacheFile(context: Context) = File(context.filesDir, CACHE_FILE)

    /** Parsed entries from the last successful fetch, or empty if we've never fetched. */
    fun cachedEntries(context: Context): List<AllergyCatalog.Entry> {
        val f = cacheFile(context)
        if (!f.exists()) return emptyList()
        return runCatching { parse(f.readText()) }.getOrDefault(emptyList())
    }

    /**
     * Fetch [REMOTE_URL] and update the on-disk cache, unless the cache is younger than
     * [TTL_MS] (pass [force] to override). Safe to call on every picker open. Returns true
     * when the cache changed.
     */
    suspend fun refresh(context: Context, force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val f = cacheFile(context)
        val fresh = f.exists() && System.currentTimeMillis() - f.lastModified() < TTL_MS
        if (fresh && !force) return@withContext false

        runCatching {
            val conn = (URL(REMOTE_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 12_000
                setRequestProperty("User-Agent", "CareApp/0.9 (FYP; Android)")
                setRequestProperty("Accept", "application/json")
            }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            conn.disconnect()
            if (code !in 200..299) return@runCatching false

            val parsed = parse(body)
            if (parsed.isEmpty()) return@runCatching false

            val changed = !f.exists() || f.readText() != body
            f.writeText(body)
            f.setLastModified(System.currentTimeMillis())
            changed
        }.getOrDefault(false)
    }

    private fun parse(json: String): List<AllergyCatalog.Entry> {
        val arr = JSONArray(json)
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val name = o.optString("name").trim()
            if (name.isEmpty()) return@mapNotNull null
            AllergyCatalog.Entry(
                name = name,
                category = AllergyCatalog.Category.of(o.optString("category")),
                note = o.optString("note").trim(),
            )
        }
    }
}
