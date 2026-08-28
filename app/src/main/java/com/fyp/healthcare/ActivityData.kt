package com.fyp.healthcare

import android.content.Context

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
 *      steps()            -> step count from gait micro-doppler
 *      fallCount()        -> times the radar detected a fall from the bed
 *      movementCount()    -> significant movements (excludes breathing / micro-motion)
 *      idleMinutes()      -> minutes the person has lain still on the bed
 *      hourlyActivity()   -> 24 movement buckets for the bar chart
 *      sleep()            -> last night's session from the bedside radar
 *
 *  The daily step goal is a user setting, NOT radar data, so it's real.
 * =====================================================================
 */
class ActivityDataManager(context: Context) {

    private val prefs = context.getSharedPreferences("activity", Context.MODE_PRIVATE)

    // ---------- real settings (not radar) ----------

    fun stepGoal(): Int = prefs.getInt(KEY_GOAL, DEFAULT_STEP_GOAL)

    fun setStepGoal(goal: Int) =
        prefs.edit().putInt(KEY_GOAL, goal.coerceIn(1_000, 50_000)).apply()

    // ---------- from the mmWave radar (null until connected) ----------

    fun isRadarConnected(): Boolean = false

    fun lastSyncLabel(): String? = null          // e.g. "2 min ago"

    fun presenceDetected(): Boolean? = null

    fun steps(): Int? = null

    /** Times the radar detected a fall from the bed today. */
    fun fallCount(): Int? = null

    /** Significant movements the radar picked up — excludes breathing / micro-motion. */
    fun movementCount(): Int? = null

    /** Minutes the person has lain still on the bed under the radar. */
    fun idleMinutes(): Int? = null

    /** 24 values, index = hour of day (0..23). Null when there's no data. */
    fun hourlyActivity(): List<Int>? = null

    fun sleep(): SleepSummary? = null

    private companion object {
        const val KEY_GOAL = "step_goal"
        const val DEFAULT_STEP_GOAL = 8_000
    }
}

data class SleepSummary(
    val totalMinutes: Int,
    val bedTime: String,    // 24h "HH:mm"
    val wakeTime: String,   // 24h "HH:mm"
    val quality: String,    // "Good" | "Fair" | "Poor"
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
