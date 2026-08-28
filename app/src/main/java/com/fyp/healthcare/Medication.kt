package com.fyp.healthcare

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * One medication reminder.
 *
 * [days] uses 0 = Sunday .. 6 = Saturday (matches Calendar.DAY_OF_WEEK - 1).
 * [log] records what the user did on a given calendar day:
 *   "yyyy-MM-dd" -> "taken" | "missed".
 * A day that isn't in the log is still pending (and shows as "Missed" on its own
 * once enough time has passed — see [stateNow]).
 */
data class Medication(
    val id: Long,
    val name: String,
    val dosageMg: Int,
    val amount: Int,            // tablets / capsules per dose
    val time: String,          // 24h "HH:mm"
    val days: Set<Int>,
    val log: Map<String, String> = emptyMap(),
) {
    val hour: Int get() = time.substringBefore(":").trim().toIntOrNull() ?: 0
    val minute: Int get() = time.substringAfter(":").trim().toIntOrNull() ?: 0
    val minutesOfDay: Int get() = hour * 60 + minute

    val dosageText: String
        get() = "${dosageMg}mg · $amount " + if (amount == 1) "tablet" else "tablets"

    fun isScheduledOn(cal: Calendar): Boolean = (cal.get(Calendar.DAY_OF_WEEK) - 1) in days
    fun actionOn(date: String): String? = log[date]
}

enum class DoseState { TAKEN, MISSED, SOON, UPCOMING, OFF }

private const val SOON_BEFORE_MIN = 60   // "Soon" starts 60 min before the dose time
private const val LATE_GRACE_MIN = 120   // still actionable up to 120 min after; then auto-"Missed"

/** Status of *today's* dose for this med. */
fun Medication.stateNow(now: Calendar = Calendar.getInstance()): DoseState {
    when (actionOn(dateKey(now))) {
        "taken" -> return DoseState.TAKEN
        "missed" -> return DoseState.MISSED
    }
    if (!isScheduledOn(now)) return DoseState.OFF

    val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val diff = minutesOfDay - nowMin           // > 0 means the dose is still in the future
    return when {
        diff > SOON_BEFORE_MIN -> DoseState.UPCOMING
        diff >= -LATE_GRACE_MIN -> DoseState.SOON
        else -> DoseState.MISSED
    }
}

private val DAY_ABBR = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

fun daysLabel(days: Set<Int>): String = when {
    days.isEmpty() -> "—"
    days.size == 7 -> "Every day"
    days == setOf(1, 2, 3, 4, 5) -> "Weekdays"
    days == setOf(0, 6) -> "Weekends"
    else -> days.sorted().joinToString(", ") { DAY_ABBR[it] }
}

fun dateKey(cal: Calendar = Calendar.getInstance()): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

/** "20:00" -> "8:00 PM" */
fun formatTime12(time24: String): String {
    val p = time24.split(":")
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, p.getOrNull(0)?.trim()?.toIntOrNull() ?: 0)
        set(Calendar.MINUTE, p.getOrNull(1)?.trim()?.toIntOrNull() ?: 0)
    }
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
}

class MedicationManager(context: Context) {

    // =====================================================================
    // TODO (FUTURE - SQL DATABASE):
    //  Each medication AND every entry in `log` (taken/missed per day) also
    //  syncs to the server, so the family caregiver account can see the
    //  patient's schedule and full adherence history.
    //  For now everything is stored locally as JSON in SharedPreferences.
    // =====================================================================

    private val prefs: SharedPreferences =
        context.getSharedPreferences("medications", Context.MODE_PRIVATE)

    /** All meds, always sorted by time-of-day (then name). */
    fun getAll(): List<Medication> =
        load().sortedWith(compareBy({ it.minutesOfDay }, { it.name.lowercase() }))

    fun get(id: Long): Medication? = load().firstOrNull { it.id == id }

    fun add(name: String, dosageMg: Int, amount: Int, time: String, days: Set<Int>): Medication {
        val med = Medication(newId(), name.trim(), dosageMg, amount, time, days)
        save(load() + med)
        return med
    }

    fun update(med: Medication) {
        save(load().map { if (it.id == med.id) med.copy(name = med.name.trim()) else it })
    }

    fun delete(id: Long) = save(load().filterNot { it.id == id })

    /** action = "taken" | "missed" | null (null = clear the entry, i.e. "undo"). */
    fun logStatus(id: Long, action: String?, date: String = dateKey()) {
        save(load().map { m ->
            if (m.id != id) m
            else m.copy(log = m.log.toMutableMap().apply {
                if (action == null) remove(date) else put(date, action)
            })
        })
    }

    private fun newId(): Long {
        val used = load().map { it.id }.toHashSet()
        var id = System.currentTimeMillis()
        while (id in used) id++
        return id
    }

    // ---- storage ----

    private fun load(): List<Medication> {
        prefs.getString(KEY, null)?.let { return parse(it) }

        // first run on this version: migrate the old pipe-delimited format (if any),
        // then always write KEY so this branch doesn't run again
        val migrated = migrateLegacy()
        save(migrated)
        prefs.edit().remove(LEGACY_KEY).apply()
        return migrated
    }

    private fun save(meds: List<Medication>) {
        val arr = JSONArray()
        meds.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id)
                put("name", m.name)
                put("dosageMg", m.dosageMg)
                put("amount", m.amount)
                put("time", m.time)
                put("days", JSONArray(m.days.sorted()))
                put("log", JSONObject().apply { m.log.forEach { (k, v) -> put(k, v) } })
            })
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private fun parse(raw: String): List<Medication> = runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)

            val daysArr = o.optJSONArray("days")
            val days = if (daysArr == null) emptySet()
            else (0 until daysArr.length()).map { daysArr.getInt(it) }.toSet()

            val logObj = o.optJSONObject("log")
            val log = if (logObj == null) emptyMap()
            else logObj.keys().asSequence().associateWith { logObj.getString(it) }

            Medication(
                id = o.getLong("id"),
                name = o.getString("name"),
                dosageMg = o.optInt("dosageMg", 0),
                amount = o.optInt("amount", 1),
                time = o.optString("time", "20:00"),
                days = days,
                log = log,
            )
        }
    }.getOrDefault(emptyList())

    private fun migrateLegacy(): List<Medication> {
        val raw = prefs.getString(LEGACY_KEY, "").orEmpty()
        if (raw.isBlank()) return emptyList()

        val mgRe = Regex("""(\d+)\s*mg""", RegexOption.IGNORE_CASE)
        val amtRe = Regex("""(\d+)\s*(tablet|capsule|cap|pill)""", RegexOption.IGNORE_CASE)

        return raw.split(";;").mapNotNull { line ->
            val p = line.split("|")
            if (p.size < 7) return@mapNotNull null
            val days = p[5].split(",")
                .mapNotNull { DAY_ABBR.indexOf(it.trim()).takeIf { idx -> idx >= 0 } }
                .toSet()
            Medication(
                id = p[0].toLongOrNull() ?: System.currentTimeMillis(),
                name = p[1],
                dosageMg = mgRe.find(p[2])?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                amount = amtRe.find(p[2])?.groupValues?.get(1)?.toIntOrNull() ?: 1,
                time = p[4],
                days = days,
            )
        }
    }

    private companion object {
        const val KEY = "items_v2"
        const val LEGACY_KEY = "list"
    }
}
