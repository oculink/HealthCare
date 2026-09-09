package com.fyp.healthcare

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.SleepSegmentRequest

/**
 * PHONE-BASED SLEEP TRACKING — a fallback until the bedside mmWave radar is connected.
 *
 * Uses Google Play Services' **Sleep API** (`ActivityRecognition`): an on-device model over
 * the phone's motion + ambient-light sensors that infers the main nightly sleep period from
 * "dark + still + no interaction for hours". Results arrive at [SleepReceiver]:
 *   - `SleepClassifyEvent` — ~every 10 min overnight (confidence the user is asleep). Ignored
 *     for now; stored for a possible future "efficiency" figure.
 *   - `SleepSegmentEvent`  — once after waking: the night's start / end / duration. This is
 *     what populates the "Sleep" row.
 *
 * Accuracy is actigraphy-class: fine for duration / bedtime trends, **no sleep stages**, and
 * only as good as where the phone spends the night. The UI shows a note that the figure is a
 * phone estimate, not a radar reading. The radar will replace this.
 *
 * SELF MODE ONLY (like [StepTracker]); a caregiver reads the patient's synced value. The
 * subscription intentionally OUTLIVES the app being closed (the segment lands mid-morning) —
 * it's only torn down on sign-out or when the account becomes a caregiver.
 */
object SleepTracker {

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACTIVITY_RECOGNITION,
            ) == PackageManager.PERMISSION_GRANTED

    /** Subscribe to Sleep API updates. No-op in caretaker mode or without permission. Idempotent. */
    fun register(context: Context) {
        if (Session.isCaretakerMode) return
        if (!hasPermission(context)) return
        val app = context.applicationContext
        runCatching {
            ActivityRecognition.getClient(app)
                .requestSleepSegmentUpdates(
                    pendingIntent(app),
                    SleepSegmentRequest.getDefaultSleepSegmentRequest(),
                )
                .addOnFailureListener {
                    android.util.Log.w("SleepTracker", "requestSleepSegmentUpdates failed", it)
                }
        }
    }

    /** Stop Sleep API updates (sign-out / entering caretaker mode). */
    fun unregister(context: Context) {
        val app = context.applicationContext
        runCatching {
            ActivityRecognition.getClient(app).removeSleepSegmentUpdates(pendingIntent(app))
        }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, SleepReceiver::class.java)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags = flags or PendingIntent.FLAG_MUTABLE
        return PendingIntent.getBroadcast(context, SLEEP_REQUEST_CODE, intent, flags)
    }

    private const val SLEEP_REQUEST_CODE = 5471
}
