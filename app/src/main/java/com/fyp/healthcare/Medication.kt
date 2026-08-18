package com.fyp.healthcare

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class Medication(
    val id: Long,
    val name: String,
    val dosage: String,
    val frequency: String,
    val time: String,   // 24h format "HH:mm"
    val days: String,   // "Mon, Wed, Fri"
    val status: String  // "pending" | "taken" | "missed"
)

class MedicationManager(context: Context) {

    // =====================================================================
    // TODO (FUTURE - SQL DATABASE):
    //  Every medication AND every taken/missed log will also be uploaded
    //  to the SQL database, so the family caregiver account can see the
    //  patient's reminder schedule and adherence history (logs).
    //  For now everything is stored locally in SharedPreferences.
    // =====================================================================

    private val prefs: SharedPreferences =
        context.getSharedPreferences("medications", Context.MODE_PRIVATE)

    fun getAll(): List<Medication> {
        val raw = prefs.getString("list", "") ?: ""
        if (raw.isBlank()) return emptyList()

        return raw.split(";;").mapNotNull { line ->
            val p = line.split("|")
            if (p.size >= 7) {
                Medication(
                    p[0].toLongOrNull() ?: 0L,
                    p[1],
                    p[2],
                    p[3],
                    p[4],
                    p[5],
                    p[6]
                )
            } else null
        }
    }

    fun add(
        name: String,
        dosage: String,
        frequency: String,
        time: String,
        days: String
    ): Medication {
        val meds = getAll().toMutableList()
        val med = Medication(
            id = System.currentTimeMillis(),
            name = name,
            dosage = dosage,
            frequency = frequency,
            time = time,
            days = days,
            status = "pending"
        )

        meds.add(med)
        save(meds)

        return med
    }

    fun setStatus(id: Long, status: String) {
        // TODO: this taken/missed log entry will also be uploaded to the database
        save(
            getAll().map {
                if (it.id == id) it.copy(status = status) else it
            }
        )
    }

    private fun save(meds: List<Medication>) {
        val raw = meds.joinToString(";;") {
            "${it.id}|${it.name}|${it.dosage}|${it.frequency}|${it.time}|${it.days}|${it.status}"
        }

        prefs.edit().putString("list", raw).apply()
    }
}

// "20:00" -> "8:00 PM"
fun formatTime12(time24: String): String {
    val p = time24.split(":")

    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, p.getOrNull(0)?.toIntOrNull() ?: 0)
        set(Calendar.MINUTE, p.getOrNull(1)?.toIntOrNull() ?: 0)
    }

    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
}