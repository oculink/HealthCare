package com.fyp.healthcare

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Optional online refresh for [ConditionCatalog] - same design as [AllergyCatalogRemote].
 * Pulls a hosted JSON list of conditions so the picker can grow without an app update, and
 * caches it on disk so it keeps working offline. Best-effort: any failure leaves
 * [ConditionCatalog.BUILT_IN] as the list, and the picker never blocks on it.
 *
 * Hosting the JSON
 * [REMOTE_URL] points at `docs/condition_catalog.json` in this repo, served raw by GitHub.
 * To add conditions later, edit that file, commit and push - installed clients pick it up
 * within [TTL_MS].
 *
 * Expected shape - a JSON array of objects:
 *   [
 *     { "name": "Long COVID", "category": "Other", "note": "symptoms weeks after infection" },
 *     { "name": "Costochondritis", "category": "Bones, joints & muscles" }
 *   ]
 * `category` is matched leniently by [ConditionCatalog.Category.of]; `note` is optional.
 *
 * The NHS Health A-Z (nhs.uk/conditions) has no free feed, so this is the maintainer's own
 * curated extract mirroring the NHS names and summaries, not a live mirror.
 */
object ConditionCatalogRemote {

    private const val REMOTE_URL =
        "https://raw.githubusercontent.com/oculink/HealthCare/main/docs/condition_catalog.json"

    private const val CACHE_FILE = "condition_catalog_cache.json"
    private const val TTL_MS = 12L * 60 * 60 * 1000

    private fun cacheFile(context: Context) = File(context.filesDir, CACHE_FILE)

    fun cachedEntries(context: Context): List<ConditionCatalog.Entry> {
        val f = cacheFile(context)
        if (!f.exists()) return emptyList()
        return runCatching { parse(f.readText()) }.getOrDefault(emptyList())
    }

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

    private fun parse(json: String): List<ConditionCatalog.Entry> {
        val arr = JSONArray(json)
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val name = o.optString("name").trim()
            if (name.isEmpty()) return@mapNotNull null
            ConditionCatalog.Entry(
                name = name,
                category = ConditionCatalog.Category.of(o.optString("category")),
                note = o.optString("note").trim(),
            )
        }
    }
}
