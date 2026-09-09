package com.fyp.healthcare

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.SleepClassifyEvent
import com.google.android.gms.location.SleepSegmentEvent

/**
 * Receives Android Sleep API broadcasts (see [SleepTracker]). Manifest-registered so it fires
 * even when the app process is dead - the nightly [SleepSegmentEvent] typically arrives
 * mid-morning, long after the app was last open.
 */
class SleepReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext

        Session.init(app)
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
                ActivityDataManager(app).appendSleepClassify(SleepClassifyEvent.extractEvents(intent))
            }
        }
    }
}
