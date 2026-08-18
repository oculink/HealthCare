package com.fyp.healthcare

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.Calendar
import java.util.concurrent.TimeUnit

// ===== Shows the actual phone notification =====
object NotificationHelper {

    private const val CHANNEL_ID = "med_reminders"

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Medication Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you to take your medication on time"
            }

            nm.createNotificationChannel(channel)
        }
    }

    fun show(
        context: Context,
        notificationId: Int,
        title: String,
        text: String
    ) {
        ensureChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // TODO: replace with your own icon later
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId, notification)
    }
}

// ===== Runs automatically when the reminder time arrives =====
class ReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val medId = inputData.getLong("med_id", -1L)

        if (medId == -1L) {
            return Result.success()
        }

        val med = MedicationManager(context)
            .getAll()
            .firstOrNull { it.id == medId }
            ?: return Result.success()

        NotificationHelper.show(
            context = context,
            notificationId = med.id.hashCode(),
            title = "Time for ${med.name} 💊",
            text = "${med.dosage} • ${formatTime12(med.time)}"
        )

        // Repeat: automatically schedule the next matching day
        ReminderScheduler.scheduleNext(context, med)

        return Result.success()
    }
}

// ===== Figures out WHEN to fire the notification =====
object ReminderScheduler {

    fun scheduleNext(context: Context, med: Medication) {
        val target = nextOccurrence(med) ?: return

        val delayMs = target.timeInMillis - System.currentTimeMillis()

        if (delayMs <= 0) return

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("med_id" to med.id))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "med_reminder_${med.id}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    // Next date+time that matches the med's repeat days & time
    private fun nextOccurrence(med: Medication): Calendar? {
        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val days = med.days.split(",").map { it.trim() }

        val p = med.time.split(":")
        val hour = p.getOrNull(0)?.toIntOrNull() ?: 20
        val minute = p.getOrNull(1)?.toIntOrNull() ?: 0

        val now = Calendar.getInstance()

        for (offset in 0..7) {
            val cal = now.clone() as Calendar

            cal.add(Calendar.DAY_OF_YEAR, offset)
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            val dayName = dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]

            if (dayName in days && cal.after(now)) {
                return cal
            }
        }

        return null
    }
}