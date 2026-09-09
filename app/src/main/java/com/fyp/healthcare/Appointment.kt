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
import java.util.Date
import java.util.Locale

/**
 * One medical appointment the user (patient) or a linked caregiver has entered by hand.
 *
 * The app never discovers appointments on its own - someone books by phone / at the
 * counter, then records it here. A "doctor" is just the [doctorName] + [clinicName]
 * text; there is no doctor account. Once saved, the entry drives a local reminder
 * ([AppointmentReminderScheduler]) and shows on the Appointments list + Home.
 *
 * Mirrored to Firestore at `users/{uid}/appointments/{id}` (same owner/caretaker path
 * medications use), so patient and caregiver always see the same list.
 */
data class Appointment(
    val id: Long,
    val doctorName: String,
    val specialty: String = "",
    val clinicName: String = "",
    val address: String = "",
    /** Coordinates for the address, when picked from the map / a search suggestion. */
    val lat: Double? = null,
    val lng: Double? = null,
    val startMillis: Long,
    val notes: String = "",
    /** Minutes before [startMillis] to fire the reminder. 0 = no reminder. */
    val remindMinutesBefore: Int = 60,
    /** "upcoming" | "completed" | "cancelled" - set by the user. */
    val status: String = STATUS_UPCOMING,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val isCancelled: Boolean get() = status == STATUS_CANCELLED
    val isCompleted: Boolean get() = status == STATUS_COMPLETED

    /**
     * What to actually show: an untouched appointment whose time has passed reads as
     * [STATE_PAST] even though the user never marked it.
     */
    fun state(now: Long = System.currentTimeMillis()): String = when {
        status == STATUS_CANCELLED -> STATE_CANCELLED
        status == STATUS_COMPLETED -> STATE_COMPLETED
        startMillis < now -> STATE_PAST
        else -> STATE_UPCOMING
    }

    val whenText: String
        get() = "${dateFmt.format(Date(startMillis))} · ${timeFmt.format(Date(startMillis))}"

    /** Doctor + specialty on one line, e.g. "Dr. Lim · Cardiologist". */
    val doctorLine: String
        get() = if (specialty.isBlank()) doctorName else "$doctorName · $specialty"

    companion object {
        const val STATUS_UPCOMING = "upcoming"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_CANCELLED = "cancelled"

        const val STATE_UPCOMING = "upcoming"
        const val STATE_PAST = "past"
        const val STATE_COMPLETED = "completed"
        const val STATE_CANCELLED = "cancelled"

        /** Lead-time options offered on the Add screen - label to minutes. */
        val REMINDER_CHOICES: List<Pair<String, Int>> = listOf(
            "None" to 0,
            "15 min" to 15,
            "1 hour" to 60,
            "1 day" to 24 * 60,
            "2 days" to 48 * 60,
        )

        private val dateFmt get() = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
        private val timeFmt get() = SimpleDateFormat("h:mm a", Locale.getDefault())

        fun reminderLabel(minutes: Int): String =
            REMINDER_CHOICES.firstOrNull { it.second == minutes }?.first
                ?: when {
                    minutes <= 0 -> "None"
                    minutes % (24 * 60) == 0 -> "${minutes / (24 * 60)} day(s)"
                    minutes % 60 == 0 -> "${minutes / 60} hour(s)"
                    else -> "$minutes min"
                }

        /** Map one `users/{uid}/appointments/{doc}` Firestore doc to an [Appointment]. */
        fun appointmentFrom(d: DocumentSnapshot): Appointment? {
            val id = d.getLong("id") ?: d.id.toLongOrNull() ?: return null
            val start = d.getLong("startMillis") ?: return null
            return Appointment(
                id = id,
                doctorName = d.getString("doctorName").orEmpty(),
                specialty = d.getString("specialty").orEmpty(),
                clinicName = d.getString("clinicName").orEmpty(),
                address = d.getString("address").orEmpty(),
                startMillis = start,
                notes = d.getString("notes").orEmpty(),
                lat = d.getDouble("lat"),
                lng = d.getDouble("lng"),
                remindMinutesBefore = (d.getLong("remindMinutesBefore") ?: 60L).toInt().coerceAtLeast(0),
                status = d.getString("status")?.ifBlank { STATUS_UPCOMING } ?: STATUS_UPCOMING,
                createdAt = d.getLong("createdAt") ?: id,
            )
        }
    }
}

/** When the reminder should fire, in wall-clock millis. Null if it's already in the past / disabled. */
fun Appointment.reminderFireAt(): Long? {
    if (remindMinutesBefore <= 0 || isCancelled || isCompleted) return null
    return startMillis - remindMinutesBefore * 60_000L
}

/**
 * "Today" / "Tomorrow" / "In 3 days" / "5 days ago" - a friendly relative day label,
 * falling back to the plain date once it's more than a week out.
 */
fun relativeDayLabel(startMillis: Long, now: Long = System.currentTimeMillis()): String {
    fun midnight(millis: Long) = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val days = ((midnight(startMillis) - midnight(now)) / 86_400_000L).toInt()
    return when {
        days == 0 -> "Today"
        days == 1 -> "Tomorrow"
        days == -1 -> "Yesterday"
        days in 2..7 -> "In $days days"
        days in -7..-2 -> "${-days} days ago"
        else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(startMillis))
    }
}

/**
 * @param forSelf when true, always use the signed-in account's own store, ignoring
 *        caretaker mode - used by the reminder scheduler / worker so a caretaker's phone
 *        only ever fires its own appointment notifications, never a linked patient's.
 */
class AppointmentManager(context: Context, forSelf: Boolean = false) {

    // Each appointment mirrors to Firestore (users/{uid}/appointments/{id}) so a linked
    // family caregiver sees and manages the same list. Locally it's JSON in
    // SharedPreferences; in caretaker mode the patient's copy lives in a separate
    // `appointments__<patientUid>` file.
    private val prefs: SharedPreferences =
        context.getSharedPreferences(
            if (forSelf) "appointments" else scopedPrefsName("appointments"),
            Context.MODE_PRIVATE,
        )

    /** All appointments, soonest first. */
    fun getAll(): List<Appointment> = load().sortedBy { it.startMillis }

    /** Upcoming (not past / cancelled / completed), soonest first. */
    fun upcoming(now: Long = System.currentTimeMillis()): List<Appointment> =
        getAll().filter { it.state(now) == Appointment.STATE_UPCOMING }

    /** The very next upcoming appointment, or null. */
    fun next(now: Long = System.currentTimeMillis()): Appointment? = upcoming(now).firstOrNull()

    fun get(id: Long): Appointment? = load().firstOrNull { it.id == id }

    fun nextId(): Long {
        val used = load().map { it.id }.toHashSet()
        var id = System.currentTimeMillis()
        while (id in used) id++
        return id
    }

    /** Insert a new appointment or replace an existing one (matched by id). */
    fun upsert(appt: Appointment): Appointment {
        val fixed = appt.copy(
            doctorName = appt.doctorName.trim(),
            specialty = appt.specialty.trim(),
            clinicName = appt.clinicName.trim(),
            address = appt.address.trim(),
            notes = appt.notes.trim(),
        )
        val all = load()
        val next = if (all.any { it.id == fixed.id }) {
            all.map { if (it.id == fixed.id) fixed else it }
        } else {
            all + fixed
        }
        save(next)
        push(fixed)
        return fixed
    }

    fun setStatus(id: Long, status: String) {
        val appt = get(id) ?: return
        upsert(appt.copy(status = status))
    }

    fun delete(id: Long) {
        save(load().filterNot { it.id == id })
        Cloud.userDoc?.collection("appointments")?.document(id.toString())?.delete()
    }

    fun syncAllToCloud() = load().forEach { push(it) }

    /** Replace the local list with [appts] pulled from Firestore (hydration). No re-push. */
    fun hydrateLocal(appts: List<Appointment>) = save(appts)

    /** Read the appointment list straight from Firestore - the inverse of [push]. */
    suspend fun cloudList(fromServer: Boolean): List<Appointment> {
        val col = Cloud.userDoc?.collection("appointments") ?: return emptyList()
        val snap = col.get(if (fromServer) Source.SERVER else Source.CACHE).awaitResult()
        return snap.documents.mapNotNull { Appointment.appointmentFrom(it) }
    }

    private fun push(a: Appointment) {
        Cloud.userDoc?.collection("appointments")?.document(a.id.toString())?.set(
            mapOf(
                "id" to a.id,
                "doctorName" to a.doctorName,
                "specialty" to a.specialty,
                "clinicName" to a.clinicName,
                "address" to a.address,
                "lat" to a.lat,
                "lng" to a.lng,
                "startMillis" to a.startMillis,
                "notes" to a.notes,
                "remindMinutesBefore" to a.remindMinutesBefore,
                "status" to a.status,
                "createdAt" to a.createdAt,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        )
    }


    private fun load(): List<Appointment> = prefs.getString(KEY, null)?.let(::parse) ?: emptyList()

    private fun save(appts: List<Appointment>) {
        val arr = JSONArray()
        appts.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("doctorName", a.doctorName)
                put("specialty", a.specialty)
                put("clinicName", a.clinicName)
                put("address", a.address)
                a.lat?.let { put("lat", it) }
                a.lng?.let { put("lng", it) }
                put("startMillis", a.startMillis)
                put("notes", a.notes)
                put("remindMinutesBefore", a.remindMinutesBefore)
                put("status", a.status)
                put("createdAt", a.createdAt)
            })
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private fun parse(raw: String): List<Appointment> = runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val start = o.optLong("startMillis", 0L).takeIf { it > 0L } ?: return@mapNotNull null
            Appointment(
                id = o.getLong("id"),
                doctorName = o.optString("doctorName"),
                specialty = o.optString("specialty"),
                clinicName = o.optString("clinicName"),
                address = o.optString("address"),
                lat = o.optDouble("lat", Double.NaN).takeIf { !it.isNaN() },
                lng = o.optDouble("lng", Double.NaN).takeIf { !it.isNaN() },
                startMillis = start,
                notes = o.optString("notes"),
                remindMinutesBefore = o.optInt("remindMinutesBefore", 60).coerceAtLeast(0),
                status = o.optString("status", Appointment.STATUS_UPCOMING)
                    .ifBlank { Appointment.STATUS_UPCOMING },
                createdAt = o.optLong("createdAt", o.getLong("id")),
            )
        }
    }.getOrDefault(emptyList())

    private companion object {
        const val KEY = "items_v1"
    }
}
