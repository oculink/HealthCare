package com.fyp.healthcare

import android.content.Context
import com.google.android.gms.location.SleepClassifyEvent
import com.google.firebase.firestore.SetOptions
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Data for the Activity Monitoring screen.
 *
 * =====================================================================
 * FYP — mmWave RADAR
 *  Everything the radar will supply is `null` right now. When the mmWave
 *  sensor + its signal-processing pipeline are connected, fill these in
 *  (most likely by caching the latest values pushed from a
 *  BroadcastReceiver / foreground Service / server sync, then reading
 *  them back here).
 *
 *      isRadarConnected() -> radar link / heartbeat is alive
 *      lastSyncLabel()    -> when the radar last reported in
 *      presenceDetected() -> someone is currently in the radar's field
 *      fallCount()        -> times the radar detected a fall from the bed
 *      movementCount()    -> significant movements (excludes breathing / micro-motion)
 *      idleMinutes()      -> minutes the person has lain still on the bed
 *      hourlyActivity()   -> 24 movement buckets for the bar chart
 *
 *  The daily step goal is a user setting, NOT radar data, so it's real.
 *  Steps and sleep() are ALSO real now: steps from the phone step counter
 *  ([StepTracker]) and sleep from the phone's Sleep API ([SleepTracker]),
 *  both flagged in the UI as phone estimates until the radar takes over.
 * =====================================================================
 */
class ActivityDataManager(context: Context) {

    private val ctx = context.applicationContext
    private val prefs = ctx.getSharedPreferences(scopedPrefsName("activity"), Context.MODE_PRIVATE)


    fun stepGoal(): Int = prefs.getInt(KEY_GOAL, DEFAULT_STEP_GOAL)

    fun setStepGoal(goal: Int) {
        prefs.edit().putInt(KEY_GOAL, goal.coerceIn(1_000, 50_000)).apply()
        syncToCloud()
    }

    /** Mirror the step goal onto users/{uid}. */
    fun syncToCloud() {
        Cloud.userDoc?.set(mapOf("stepGoal" to stepGoal()), SetOptions.merge())
    }

    /** Overwrite local settings from a cloud copy (hydration). No re-push. */
    fun hydrateLocal(goal: Int?, syncedSteps: Int? = null, syncedDay: String? = null) {
        val e = prefs.edit()
        if (goal != null) e.putInt(KEY_GOAL, goal.coerceIn(1_000, 50_000))
        if (syncedDay != null) {
            e.putString(K_STEPS_SYNCED_DAY, syncedDay)
            e.putInt(K_STEPS_SYNCED, (syncedSteps ?: 0).coerceAtLeast(0))
        }
        e.apply()
    }

    /**
     * Overwrite the local sleep record from the cloud `sleepByDevice` map (hydration — the
     * caregiver view, and self-correction when several of the account's phones each logged a
     * segment). [pickSleep] chooses the longest recent session.
     */
    fun hydrateSleep(sleepByDevice: Any?) {
        val rec = pickSleep(sleepByDevice) ?: return
        prefs.edit()
            .putLong(K_SLEEP_START, rec.start)
            .putLong(K_SLEEP_END, rec.end)
            .putString(K_SLEEP_QUALITY, rec.quality)
            .putString(K_SLEEP_SOURCE, rec.source)
            .putInt(K_SLEEP_AWK, rec.awakenings ?: -1)
            .apply()
    }

    // ---------- today's step count (phone hardware step counter, patient's own phone) ----------
    // Written by [StepTracker] in self mode; see [StepTracker] for the daily-baseline math.
    //
    // MULTI-DEVICE: each phone on the account writes ONLY its own entry in the `stepsByDevice`
    // map on users/{uid} — { deviceId: { c: count, d: "yyyy-MM-dd" } } — via a merge write
    // (offline-safe, never clobbers another device's key). Today's number, everywhere (this
    // phone, the account's other phones, a linked caregiver), is the MAX across those entries
    // for today (see [stepsFromByDevice]); summing would double-count a day both phones were
    // carried. `K_STEPS*` = this phone's own live count; `K_STEPS_SYNCED*` = the cross-device
    // max pulled down by CloudHydrator / PatientMonitor.

    /** Today's steps = max(this phone's own count, the synced cross-device max). Null if neither. */
    fun steps(): Int? {
        val today = dateKey()
        val own = prefs.getString(K_STEPS_DAY, null)
            ?.takeIf { it == today }?.let { prefs.getInt(K_STEPS, 0) }
        val synced = prefs.getString(K_STEPS_SYNCED_DAY, null)
            ?.takeIf { it == today }?.let { prefs.getInt(K_STEPS_SYNCED, 0) }
        return if (own == null && synced == null) null else maxOf(own ?: 0, synced ?: 0)
    }

    /** Save this phone's step count locally + (throttled) push its own `stepsByDevice` entry. */
    fun recordSteps(count: Int) {
        val c = count.coerceAtLeast(0)
        val today = dateKey()
        prefs.edit().putString(K_STEPS_DAY, today).putInt(K_STEPS, c).apply()

        val lastDay = prefs.getString(K_STEPS_CLOUD_DAY, null)
        val lastCloud = prefs.getInt(K_STEPS_CLOUD, -1)
        if (lastDay == today && c != 0 && c - lastCloud < 25) return
        prefs.edit().putString(K_STEPS_CLOUD_DAY, today).putInt(K_STEPS_CLOUD, c).apply()

        Cloud.userDoc?.set(
            mapOf("stepsByDevice" to mapOf(deviceId() to mapOf("c" to c, "d" to today))),
            SetOptions.merge(),
        )
    }

    /** Stable random id for THIS install/device — kept in `cloud_sync` so it survives sign-out. */
    private fun deviceId(): String {
        val p = ctx.getSharedPreferences("cloud_sync", Context.MODE_PRIVATE)
        return p.getString(K_DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
            p.edit().putString(K_DEVICE_ID, it).apply()
        }
    }


    fun isRadarConnected(): Boolean = false

    fun lastSyncLabel(): String? = null

    fun presenceDetected(): Boolean? = null

    /** Times the radar detected a fall from the bed today. */
    fun fallCount(): Int? = null

    /** Significant movements the radar picked up — excludes breathing / micro-motion. */
    fun movementCount(): Int? = null

    /** Minutes the person has lain still on the bed under the radar. */
    fun idleMinutes(): Int? = null

    /** 24 values, index = hour of day (0..23). Null when there's no data. */
    fun hourlyActivity(): List<Int>? = null

    // ---------- last night's sleep (phone Sleep API, patient's own phone) ----------
    // Written by [SleepReceiver] from a SleepSegmentEvent in self mode; a linked caregiver
    // (and the account's other phones) read it back via [hydrateSleep]. Phone estimate —
    // the UI says so.
    //
    // MULTI-DEVICE: like steps, each phone writes ONLY its own entry in a `sleepByDevice`
    // map on users/{uid} — { deviceId: { s, e, q, src, awk } } — via merge (no clobber).
    // The displayed session is the LONGEST recent one ([pickSleep]): a phone left on a desk
    // overnight can log a shorter false "sleep"; the phone by the bed logs the real one.
    // Local `K_SLEEP_*` = whatever this phone last recorded, self-corrected on each hydrate.

    /** Last night's sleep, or null if nothing recent (hidden once >30h stale). */
    fun sleep(): SleepSummary? {
        val start = prefs.getLong(K_SLEEP_START, 0L)
        val end = prefs.getLong(K_SLEEP_END, 0L)
        if (start <= 0L || end <= start) return null
        if (System.currentTimeMillis() - end > 30L * 60 * 60 * 1000) return null
        return SleepSummary(
            totalMinutes = ((end - start) / 60_000L).toInt(),
            bedTime = hhmm(start),
            wakeTime = hhmm(end),
            quality = prefs.getString(K_SLEEP_QUALITY, "Fair") ?: "Fair",
            source = prefs.getString(K_SLEEP_SOURCE, "phone") ?: "phone",
            awakenings = prefs.getInt(K_SLEEP_AWK, -1).takeIf { it >= 0 },
        )
    }

    /** Persist a completed sleep segment + mirror it to users/{uid}. Called from [SleepReceiver]. */
    fun recordSleepSegment(startMillis: Long, endMillis: Long, source: String = "phone") {
        val durationMin = ((endMillis - startMillis) / 60_000L).toInt()
        if (durationMin < MIN_SLEEP_MINUTES) return

        val quality = sleepQuality(durationMin)
        prefs.edit()
            .putLong(K_SLEEP_START, startMillis)
            .putLong(K_SLEEP_END, endMillis)
            .putString(K_SLEEP_QUALITY, quality)
            .putString(K_SLEEP_SOURCE, source)
            .putInt(K_SLEEP_AWK, -1)
            .remove(K_SLEEP_CLASSIFY)
            .apply()

        Cloud.userDoc?.set(
            mapOf(
                "sleepByDevice" to mapOf(
                    deviceId() to mapOf(
                        "s" to startMillis, "e" to endMillis,
                        "q" to quality, "src" to source, "awk" to -1,
                    ),
                ),
            ),
            SetOptions.merge(),
        )
        Session.bumpDataVersion()
    }

    /** Longest sleep session ended within the last 30h, across the account's `sleepByDevice` entries. */
    private fun pickSleep(sleepByDevice: Any?): SleepRecord? {
        val map = sleepByDevice as? Map<*, *> ?: return null
        val now = System.currentTimeMillis()
        var best: SleepRecord? = null
        for (v in map.values) {
            val e = v as? Map<*, *> ?: continue
            val s = (e["s"] as? Number)?.toLong() ?: continue
            val en = (e["e"] as? Number)?.toLong() ?: continue
            if (en <= s || now - en > 30L * 60 * 60 * 1000) continue
            if (best != null && en - s <= best.end - best.start) continue
            best = SleepRecord(
                start = s, end = en,
                quality = (e["q"] as? String) ?: "Fair",
                source = (e["src"] as? String) ?: "phone",
                awakenings = (e["awk"] as? Number)?.toInt()?.takeIf { it >= 0 },
            )
        }
        return best
    }

    /**
     * Buffer overnight SleepClassifyEvents (bounded). Not interpreted yet — reserved for a
     * future sleep-efficiency / awakenings figure once the confidence semantics are pinned down.
     */
    fun appendSleepClassify(events: List<SleepClassifyEvent>) {
        if (events.isEmpty()) return
        val arr = runCatching { JSONArray(prefs.getString(K_SLEEP_CLASSIFY, "[]")) }.getOrDefault(JSONArray())
        events.forEach { e ->
            arr.put(JSONArray().put(e.timestampMillis).put(e.confidence).put(e.motion).put(e.light))
        }
        val trimmed = if (arr.length() > MAX_CLASSIFY) {
            JSONArray().also { out -> for (i in arr.length() - MAX_CLASSIFY until arr.length()) out.put(arr.get(i)) }
        } else arr
        prefs.edit().putString(K_SLEEP_CLASSIFY, trimmed.toString()).apply()
    }

    private fun hhmm(millis: Long): String =
        SimpleDateFormat("HH:mm", Locale.US).format(Date(millis))

    private companion object {
        const val KEY_GOAL = "step_goal"
        const val DEFAULT_STEP_GOAL = 8_000
        const val K_STEPS = "steps_today"
        const val K_STEPS_DAY = "steps_day"
        const val K_STEPS_CLOUD = "steps_cloud"
        const val K_STEPS_CLOUD_DAY = "steps_cloud_day"
        const val K_STEPS_SYNCED = "steps_synced"
        const val K_STEPS_SYNCED_DAY = "steps_synced_day"
        const val K_DEVICE_ID = "device_id"

        const val K_SLEEP_START = "sleep_start"
        const val K_SLEEP_END = "sleep_end"
        const val K_SLEEP_QUALITY = "sleep_quality"
        const val K_SLEEP_SOURCE = "sleep_source"
        const val K_SLEEP_AWK = "sleep_awakenings"
        const val K_SLEEP_CLASSIFY = "sleep_classify_buf"
        const val MIN_SLEEP_MINUTES = 45
        const val MAX_CLASSIFY = 240
    }
}

/** Parsed sleep session from the `sleepByDevice` map — internal; [SleepSummary] is UI-facing. */
data class SleepRecord(
    val start: Long,
    val end: Long,
    val quality: String,
    val source: String,
    val awakenings: Int?,
)

/** Coarse sleep-quality label from total sleep minutes (elderly target: 7–8 h). */
fun sleepQuality(totalMinutes: Int): String = when {
    totalMinutes >= 7 * 60 -> "Good"
    totalMinutes >= 5 * 60 + 30 -> "Fair"
    else -> "Poor"
}

/**
 * Today's step count as the MAX across all of the account's devices, from the `stepsByDevice`
 * map on `users/{uid}` — `{ deviceId: { c: count, d: "yyyy-MM-dd" } }`. Each phone writes only
 * its own entry, so whichever recorded the most today wins (summing would double-count a day
 * both phones were carried together). Returns null when no device has reported today.
 */
fun stepsFromByDevice(raw: Any?, today: String = dateKey()): Int? {
    val map = raw as? Map<*, *> ?: return null
    var max: Int? = null
    for (v in map.values) {
        val e = v as? Map<*, *> ?: continue
        if ((e["d"] as? String) != today) continue
        val c = (e["c"] as? Number)?.toInt() ?: continue
        max = maxOf(max ?: 0, c)
    }
    return max
}

/**
 * Rough estimate of calories burned walking [steps] steps today, personalised to the
 * user's profile:
 *
 *   steps → distance   (stride length derived from height + sex)
 *   distance → speed    (assumed casual cadence)
 *   speed → MET         (walking-intensity table)
 *   kcal = MET × weightKg × hours, with a small taper for age > 30
 *
 * Every input is optional — population defaults fill any gap. This is a fitness-tracker
 * style estimate, not a clinical figure.
 */
fun estimateWalkingCalories(
    steps: Int,
    weightKg: Double?,
    heightCm: Double?,
    ageYears: Int?,
    sex: String?,
): Int {
    if (steps <= 0) return 0
    val female = sex.equals("Female", ignoreCase = true)

    val strideM = when {
        heightCm != null && heightCm > 60.0 -> heightCm / 100.0 * (if (female) 0.413 else 0.415)
        else -> if (female) 0.67 else 0.72
    }
    val distanceKm = steps * strideM / 1000.0

    val speedKmh = (110.0 * strideM * 60.0) / 1000.0
    val met = when {
        speedKmh < 3.2 -> 2.8
        speedKmh < 4.8 -> 3.3
        speedKmh < 6.4 -> 4.3
        else -> 5.0
    }
    val hours = distanceKm / speedKmh.coerceAtLeast(0.1)

    val weight = weightKg?.takeIf { it in 20.0..350.0 } ?: if (female) 62.0 else 75.0
    val ageTaper = if (ageYears != null && ageYears > 30)
        (1.0 - (ageYears - 30) * 0.002).coerceAtLeast(0.85) else 1.0

    return (met * weight * hours * ageTaper).roundToInt().coerceAtLeast(0)
}

data class SleepSummary(
    val totalMinutes: Int,
    val bedTime: String,
    val wakeTime: String,
    val quality: String,
    val source: String = "radar",
    val awakenings: Int? = null,
)

/** 82 -> "1h 22m", 45 -> "45m", 0 -> "0m" */
fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}
