package com.fyp.healthcare

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.SleepClassifyEvent
import com.google.android.gms.location.SleepSegmentEvent

/**
 * Receives Android Sleep API broadcasts (see [SleepTracker]). Manifest-registered so it fires
 * even when the app process is dead — the nightly [SleepSegmentEvent] typically arrives
 * mid-morning, long after the app was last open.
 */
class SleepReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext

        // Cold process: load the saved role / caretaker link before deciding what to do.
        Session.init(app)
        // A caregiver's OWN phone sleep says nothing about the patient they monitor.
        if (Session.isCaretakerMode) return

        when {
            SleepSegmentEvent.hasEvents(intent) -> {
                val segments = SleepSegmentEvent.extractEvents(intent)
                val best = segments
                    .filter { it.status == SleepSegmentEvent.STATUS_SUCCESSFUL && it.segmentDurationMillis > 0 }
                    .maxByOrNull { it.endTimeMillis } ?: return
                ActivityDataManager(app).recordSleepSegment(
                    startMillis = best.startTimeMillis,
                    endMillis = best.endTimeMillis,
                    source = "phone",
                )
            }

            SleepClassifyEvent.hasEvents(intent) -> {
                // Not used yet — kept for a future sleep-efficiency estimate.
                ActivityDataManager(app).appendSleepClassify(SleepClassifyEvent.extractEvents(intent))
            }
        }
    }
}
