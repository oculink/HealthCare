package com.fyp.healthcare

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Local, phone-based reminders for medical appointments — the same WorkManager approach
 * [ReminderScheduler] uses for medication.
 *
 * One appointment fires at most one reminder, [Appointment.remindMinutesBefore] before the
 * start time. No push / server involved (Spark plan) — see the FYP report. A caretaker's
 * phone never schedules a linked patient's appointment reminders: [scheduleNext] no-ops in
 * caretaker mode unless [force]d, and [syncAll] / [AppointmentReminderWorker] only ever
 * touch the signed-in account's own list.
 */
object AppointmentReminderScheduler {

    private const val TAG = "appt_reminder"
    private fun workName(id: Long) = "appt_reminder_$id"

    /** (Re)schedule the reminder for one appointment; cancels it if nothing is due. */
    fun scheduleNext(context: Context, appt: Appointment, force: Boolean = false) {
        if (Session.isCaretakerMode && !force) return

        val fireAt = appt.reminderFireAt()
        val delay = fireAt?.minus(System.currentTimeMillis())
        if (fireAt == null || delay == null || delay <= 0L) {
            cancel(context, appt.id)
            return
        }

        val request = OneTimeWorkRequestBuilder<AppointmentReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(AppointmentReminderWorker.KEY_ID to appt.id))
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(appt.id),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(context: Context, id: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(id))
    }

    /** Re-arm every appointment reminder. Cheap + safe to call on every app launch. */
    fun syncAll(context: Context) {
        AppointmentManager(context, forSelf = true).getAll()
            .forEach { scheduleNext(context, it, force = true) }
    }
}

class AppointmentReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_ID, -1L)
        if (id == -1L) return Result.success()

        val appt = AppointmentManager(applicationContext, forSelf = true).get(id)
            ?: return Result.success()
        if (appt.isCancelled || appt.isCompleted) return Result.success()

        val timeFmt = SimpleDateFormat("EEE d MMM · h:mm a", Locale.getDefault())
        val place = appt.clinicName.ifBlank { appt.address }
        NotificationHelper.show(
            context = applicationContext,
            notificationId = APPT_NOTIF_BASE + id.toInt(),
            title = "Appointment ${relativeDayLabel(appt.startMillis).lowercase(Locale.getDefault())}",
            text = buildString {
                append(appt.doctorLine)
                append(" — ")
                append(timeFmt.format(Date(appt.startMillis)))
                if (place.isNotBlank()) append(" · $place")
            },
            channelId = NotificationHelper.CHANNEL_APPOINTMENTS,
        )
        return Result.success()
    }

    companion object {
        const val KEY_ID = "appt_id"
        private const val APPT_NOTIF_BASE = 900_000
    }
}
