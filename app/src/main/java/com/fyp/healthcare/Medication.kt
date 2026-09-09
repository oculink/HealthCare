package com.fyp.healthcare

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * How a medication is taken. Drives the labels/units on the Add Medication form
 * (strength unit + what "one dose" is counted in) and how [Medication.dosageText] reads.
 */
enum class MedRoute(
    val label: String,
    val strengthUnit: String,
    val doseLabel: String,
    val doseNoun: String,
) {
    ORAL("Oral", "mg", "Tablets / capsules per dose", "tablet"),
    TOPICAL("Topical (cream / gel)", "%", "Applications per dose", "application"),
    INJECTABLE("Injectable", "mg/mL", "mL per dose", "mL"),
    INHALED("Inhaled", "mcg/puff", "Puffs per dose", "puff"),
    SUBLINGUAL("Sublingual / buccal", "mg", "Tablets per dose", "tablet"),
    NASAL("Nasal spray", "mcg/spray", "Sprays per dose", "spray"),
    RECTAL("Rectal / vaginal", "mg", "Units per dose", "suppository"),
    OPHTHALMIC("Eye / ear drops", "%", "Drops per dose", "drop"),
    TRANSDERMAL("Transdermal patch", "mcg/hr", "Patches", "patch"),
    OTHER("Other", "", "Amount per dose", "dose");

    companion object {
        fun of(name: String?): MedRoute = entries.firstOrNull { it.name == name } ?: ORAL
    }
}

/**
 * One medication and its weekly reminder schedule.
 *
 * [schedule] maps a day (0 = Sunday .. 6 = Saturday) to the list of times ("HH:mm")
 * the dose is due that day. A day that isn't a key (or has an empty list) is skipped.
 * So a medication can be, e.g. Mon 06:00 & 17:00, Tue 08:00, Fri 06:00/14:00/20:00.
 *
 * [log] records what the user did for one specific dose:
 *   "yyyy-MM-dd HH:mm" -> "taken" | "missed".
 * A dose that isn't in the log is still pending (and shows as "Missed" on its own
 * once enough time has passed - see [slotState]).
 */
data class Medication(
    val id: Long,
    val name: String,
    val strength: String,
    val amount: Int,
    val schedule: Map<Int, List<String>> = emptyMap(),
    val log: Map<String, String> = emptyMap(),
    val description: String = "",
    val route: String = "ORAL",
) {
    val medRoute: MedRoute get() = MedRoute.of(route)

    /** Days (0=Sun..6=Sat) this medication is scheduled on. */
    val scheduledDays: Set<Int>
        get() = schedule.filterValues { it.isNotEmpty() }.keys

    /** Sorted, de-duplicated times for a given day. */
    fun timesOn(day: Int): List<String> =
        schedule[day].orEmpty().distinct().sortedBy(::hhmmMinutes)

    /** Every distinct time across the week, earliest first. */
    val allTimes: List<String>
        get() = schedule.values.flatten().distinct().sortedBy(::hhmmMinutes)

    val earliestMinutes: Int
        get() = allTimes.firstOrNull()?.let(::hhmmMinutes) ?: 0

    /** Non-empty days with their times, ordered Sun to Sat. */
    fun weeklyPlan(): List<Pair<Int, List<String>>> =
        (0..6).mapNotNull { d -> timesOn(d).takeIf { it.isNotEmpty() }?.let { d to it } }

    val dosageText: String
        get() {
            val r = medRoute
            val noun = if (amount == 1) r.doseNoun else "${r.doseNoun}s"
            val count = "$amount $noun"
            if (strength.isBlank()) return count
            val s = when {
                r.strengthUnit.isEmpty() -> strength
                r.strengthUnit == "%" -> "$strength%"
                else -> "$strength ${r.strengthUnit}"
            }
            return "$s · $count"
        }

    fun isScheduledOn(cal: Calendar): Boolean =
        (cal.get(Calendar.DAY_OF_WEEK) - 1) in scheduledDays

    fun actionOn(doseKey: String): String? = log[doseKey]
}

enum class DoseState { TAKEN, MISSED, SOON, UPCOMING, OFF }

private const val SOON_BEFORE_MIN = 60
private const val LATE_GRACE_MIN = 120

/** State of one specific dose slot ([time], "HH:mm") for today. */
fun Medication.slotState(time: String, now: Calendar = Calendar.getInstance()): DoseState {
    when (log[doseKey(now, time)]) {
        "taken" -> return DoseState.TAKEN
        "missed" -> return DoseState.MISSED
    }
    val today = now.get(Calendar.DAY_OF_WEEK) - 1
    if (time !in timesOn(today)) return DoseState.OFF

    val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val diff = hhmmMinutes(time) - nowMin
    return when {
        diff > SOON_BEFORE_MIN -> DoseState.UPCOMING
        diff >= -LATE_GRACE_MIN -> DoseState.SOON
        else -> DoseState.MISSED
    }
}

/** A one-line human summary of the whole weekly schedule. */
fun Medication.scheduleSummary(): String {
    val plan = weeklyPlan()
    if (plan.isEmpty()) return "No reminder times set"
    val sameEveryDay = plan.map { it.second }.distinct().size == 1
    return if (sameEveryDay) {
        "${daysLabel(scheduledDays)} · " + plan.first().second.joinToString(", ") { formatTime12(it) }
    } else {
        plan.joinToString("   ·   ") { (d, ts) ->
            "${DAY_NAMES_SHORT[d]} " + ts.joinToString(", ") { formatTime12(it) }
        }
    }
}

val DAY_NAMES_SHORT = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
val DAY_NAMES_LONG =
    listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

/** "HH:mm" -> minutes past midnight. */
fun hhmmMinutes(t: String): Int {
    val p = t.split(":")
    return (p.getOrNull(0)?.trim()?.toIntOrNull() ?: 0) * 60 +
        (p.getOrNull(1)?.trim()?.toIntOrNull() ?: 0)
}

fun daysLabel(days: Set<Int>): String = when {
    days.isEmpty() -> "—"
    days.size == 7 -> "Every day"
    days == setOf(1, 2, 3, 4, 5) -> "Weekdays"
    days == setOf(0, 6) -> "Weekends"
    else -> days.sorted().joinToString(", ") { DAY_NAMES_SHORT[it] }
}

fun dateKey(cal: Calendar = Calendar.getInstance()): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

/** Log key for one dose: "2026-08-29 06:00". */
fun doseKey(cal: Calendar, time: String): String = "${dateKey(cal)} $time"

/** "20:00" -> "8:00 PM" */
fun formatTime12(time24: String): String {
    val p = time24.split(":")
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, p.getOrNull(0)?.trim()?.toIntOrNull() ?: 0)
        set(Calendar.MINUTE, p.getOrNull(1)?.trim()?.toIntOrNull() ?: 0)
    }
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
}

/**
 * @param forSelf when true, always use the signed-in account's own store (base prefs file),
 *        ignoring caretaker mode. Used by the reminder scheduler / worker so a caretaker's
 *        phone only ever fires its own medication notifications, never a linked patient's.
 */
class MedicationManager(context: Context, forSelf: Boolean = false) {

    //  Each medication AND every entry in `log` (taken/missed per dose) also
    //  syncs to Firestore (users/{uid}/medications), so a linked family
    //  caregiver can see and manage the patient's schedule + adherence.
    //  Locally it is stored as JSON in SharedPreferences; in caretaker mode
    //  the patient's copy lives in a separate `medications__<patientUid>` file.

    private val prefs: SharedPreferences =
        context.getSharedPreferences(
            if (forSelf) "medications" else scopedPrefsName("medications"),
            Context.MODE_PRIVATE,
        )

    /** All meds, earliest daily dose first (then name). */
    fun getAll(): List<Medication> =
        load().sortedWith(compareBy({ it.earliestMinutes }, { it.name.lowercase() }))

    fun get(id: Long): Medication? = load().firstOrNull { it.id == id }

    /** An id for a not-yet-saved draft (used by the two-step Add flow). */
    fun nextId(): Long = newId()

    /** Insert a new medication or replace an existing one (matched by id). */
    fun upsert(med: Medication): Medication {
        val fixed = med.copy(
            name = med.name.trim(),
            strength = med.strength.trim(),
            description = med.description.trim(),
        )
        val all = load()
        val next = if (all.any { it.id == fixed.id }) {
            all.map { if (it.id == fixed.id) fixed else it }
        } else {
            all + fixed
        }
        save(next)
        pushMed(fixed)
        return fixed
    }

    fun update(med: Medication) {
        upsert(med)
    }

    fun delete(id: Long) {
        save(load().filterNot { it.id == id })
        Cloud.userDoc?.collection("medications")?.document(id.toString())?.delete()
    }

    /** action = "taken" | "missed" | null (null clears the entry - i.e. "undo"). */
    fun logStatus(id: Long, action: String?, doseKey: String) {
        save(load().map { m ->
            if (m.id != id) m
            else m.copy(log = m.log.toMutableMap().apply {
                if (action == null) remove(doseKey) else put(doseKey, action)
            })
        })
        get(id)?.let { pushMed(it) }
    }

    fun syncAllToCloud() = load().forEach { pushMed(it) }

    /** Replace the local list with [meds] pulled from Firestore (hydration). No re-push. */
    fun hydrateLocal(meds: List<Medication>) = save(meds)

    /**
     * Read the medication list straight from Firestore (users/{uid}/medications) - the inverse
     * of [pushMed]. Used to hydrate the local cache on app open / when a caretaker links.
     */
    suspend fun cloudList(fromServer: Boolean): List<Medication> {
        val col = Cloud.userDoc?.collection("medications") ?: return emptyList()
        val snap = col.get(if (fromServer) Source.SERVER else Source.CACHE).awaitResult()
        return snap.documents.mapNotNull { medicationFrom(it) }
    }

    private fun pushMed(m: Medication) {
        Cloud.userDoc?.collection("medications")?.document(m.id.toString())?.set(
            mapOf(
                "id" to m.id,
                "name" to m.name,
                "strength" to m.strength,
                "route" to m.route,
                "amount" to m.amount,
                "schedule" to m.schedule.mapKeys { it.key.toString() },
                "scheduledDays" to m.scheduledDays.sorted(),
                "log" to m.log,
                "description" to m.description,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        )
    }

    private fun newId(): Long {
        val used = load().map { it.id }.toHashSet()
        var id = System.currentTimeMillis()
        while (id in used) id++
        return id
    }


    private fun load(): List<Medication> {
        prefs.getString(KEY, null)?.let { return parse(it) }

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
                put("strength", m.strength)
                put("route", m.route)
                put("amount", m.amount)
                put("schedule", JSONObject().apply {
                    m.schedule.forEach { (d, times) -> put(d.toString(), JSONArray(times)) }
                })
                put("log", JSONObject().apply { m.log.forEach { (k, v) -> put(k, v) } })
                put("desc", m.description)
            })
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private fun parse(raw: String): List<Medication> = runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)

            val legacyTime = o.optString("time", "")

            val schedObj = o.optJSONObject("schedule")
            val schedule: Map<Int, List<String>> = if (schedObj != null) {
                schedObj.keys().asSequence().mapNotNull { k ->
                    val day = k.toIntOrNull() ?: return@mapNotNull null
                    val times = schedObj.optJSONArray(k) ?: return@mapNotNull null
                    day to (0 until times.length()).map { times.getString(it) }
                }.toMap()
            } else {
                val t = legacyTime.ifBlank { "08:00" }
                val daysArr = o.optJSONArray("days")
                val days = if (daysArr == null) emptySet()
                    else (0 until daysArr.length()).map { daysArr.getInt(it) }.toSet()
                days.associateWith { listOf(t) }
            }

            val logObj = o.optJSONObject("log")
            val rawLog: Map<String, String> = if (logObj == null) emptyMap()
                else logObj.keys().asSequence().associateWith { logObj.getString(it) }
            val log = if (schedObj == null && legacyTime.isNotBlank()) {
                rawLog.mapKeys { (k, _) -> if (k.contains(' ')) k else "$k $legacyTime" }
            } else rawLog

            Medication(
                id = o.getLong("id"),
                name = o.getString("name"),
                strength = o.optString("strength").ifBlank {
                    o.optInt("dosageMg", 0).takeIf { it > 0 }?.toString().orEmpty()
                },
                route = o.optString("route", "ORAL").ifBlank { "ORAL" },
                amount = o.optInt("amount", 1),
                schedule = schedule,
                log = log,
                description = o.optString("desc"),
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
                .mapNotNull { DAY_NAMES_SHORT.indexOf(it.trim()).takeIf { idx -> idx >= 0 } }
                .toSet()
            Medication(
                id = p[0].toLongOrNull() ?: System.currentTimeMillis(),
                name = p[1],
                strength = mgRe.find(p[2])?.groupValues?.get(1).orEmpty(),
                amount = amtRe.find(p[2])?.groupValues?.get(1)?.toIntOrNull() ?: 1,
                schedule = days.associateWith { listOf(p[4]) },
            )
        }
    }

    companion object {
        private const val KEY = "items_v2"
        private const val LEGACY_KEY = "list"

        /** Map one `users/{uid}/medications/{doc}` Firestore doc to a [Medication]. */
        fun medicationFrom(d: DocumentSnapshot): Medication? {
            val id = d.getLong("id") ?: d.id.toLongOrNull() ?: return null
            @Suppress("UNCHECKED_CAST")
            val schedRaw = (d.get("schedule") as? Map<String, List<*>>).orEmpty()
            val schedule = schedRaw.mapNotNull { (k, v) ->
                val day = k.toIntOrNull() ?: return@mapNotNull null
                day to v.mapNotNull { it as? String }
            }.toMap()
            @Suppress("UNCHECKED_CAST")
            val log = (d.get("log") as? Map<String, String>).orEmpty()
            return Medication(
                id = id,
                name = d.getString("name").orEmpty(),
                strength = d.getString("strength").orEmpty(),
                amount = (d.getLong("amount") ?: 1L).toInt().coerceAtLeast(1),
                schedule = schedule,
                log = log,
                description = d.getString("description").orEmpty(),
                route = d.getString("route")?.ifBlank { "ORAL" } ?: "ORAL",
            )
        }
    }
}
