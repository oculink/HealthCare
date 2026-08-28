package com.fyp.healthcare

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Medication Reminders",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = "Reminds you to take your medication on time" }
            )
        }
    }

    fun show(context: Context, notificationId: Int, title: String, text: String) {
        ensureChannel(context)

        // tapping the notification opens the app
        val openApp = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // TODO: replace with a branded icon
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(notificationId, notification)
    }
}

// ===== Runs when a reminder time arrives =====
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val medId = inputData.getLong(KEY_MED_ID, -1L)
        if (medId == -1L) return Result.success()

        // med was deleted -> nothing to fire, chain stops here
        val med = MedicationManager(applicationContext).get(medId) ?: return Result.success()

        // don't nag if the user already logged this dose today
        if (med.actionOn(dateKey()) == null) {
            NotificationHelper.show(
                context = applicationContext,
                notificationId = medId.toInt(),
                title = "Time for ${med.name}",
                text = "${med.dosageText} • ${formatTime12(med.time)}",
            )
        }

        // queue the next matching day
        ReminderScheduler.scheduleNext(applicationContext, med)
        return Result.success()
    }

    companion object {
        const val KEY_MED_ID = "med_id"
    }
}

// ===== Figures out WHEN to fire =====
object ReminderScheduler {

    /** (Re)schedule the single next fire for this med; cancels it if there's no upcoming day. */
    fun scheduleNext(context: Context, med: Medication) {
        val fireAt = nextOccurrence(med)
        if (fireAt == null) {
            cancel(context, med.id)
            return
        }

        val delay = fireAt - System.currentTimeMillis()
        if (delay <= 0) return

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_MED_ID to med.id))
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(med.id),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(context: Context, id: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(id))
    }

    /** Re-arm every med's reminder. Safe (and cheap) to call on every app launch. */
    fun syncAll(context: Context) {
        MedicationManager(context).getAll().forEach { scheduleNext(context, it) }
    }

    private fun workName(id: Long) = "med_reminder_$id"
    private const val TAG = "med_reminder"

    // next date+time that matches the med's repeat days & time
    private fun nextOccurrence(med: Medication): Long? {
        if (med.days.isEmpty()) return null
        val now = Calendar.getInstance()

        for (offset in 0..7) {
            val c = (now.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, med.hour)
                set(Calendar.MINUTE, med.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (med.isScheduledOn(c) && c.timeInMillis > now.timeInMillis) return c.timeInMillis
        }
        return null
    }
}
