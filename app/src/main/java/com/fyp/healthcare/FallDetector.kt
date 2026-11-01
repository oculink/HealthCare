package com.fyp.healthcare

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Pengesan jatuh berasaskan corak untuk LD2450 (radar 2D, tiada ketinggian).
 * Corak: bergerak laju -> berhenti mendadak -> diam ${STILL_MS}ms -> amaran + kiraan detik.
 */
object FallDetector {

    enum class State { NORMAL, SUSPECTED }

    var state by mutableStateOf(State.NORMAL)
        private set
    var alertDeadlineMs by mutableStateOf(0L)
        private set

    // ── Tetapan (laras mengikut ujian) ──
    private const val FAST_CMS = 80              // dikira "laju" (cm/s)
    private const val MOVE_CMS = 20              // dikira "bergerak" (cm/s)
    private const val GETUP_CMS = 30             // bergerak semula = bangun (cm/s)
    private const val GETUP_HOLD_MS = 2000L      // tempoh bergerak untuk batalkan
    private const val SPIKE_GAP_MS = 2500L       // gerakan laju mesti jadi gerakan terakhir
    private const val STILL_MS = 8000L           // diam selepas gerakan laju
    private const val CANCEL_WINDOW_MS = 20000L  // masa untuk tekan "Saya OK"
    private const val COOLDOWN_MS = 60000L       // jarak minimum antara amaran
    private const val EDGE_ANGLE_DEG = 50.0      // tepi sudut pandang radar
    private const val EDGE_Y_MM = 5200           // tepi jarak jauh
    private const val EDGE_NEAR_MM = 400         // terlalu dekat dengan sensor
    private const val NOTIF_ID = 777001

    private var appCtx: Context? = null
    private var lastFastMs = 0L
    private var lastMoveMs = 0L
    private var movingSince = 0L
    private var lastAlertMs = 0L
    private var lastX = 0
    private var lastY = 0
    private var hadTarget = false

    fun attach(context: Context) {
        appCtx = context.applicationContext
    }

    @Synchronized
    fun onFrame(targets: List<RadarTarget>, now: Long) {
        val maxSpeed = targets.maxOfOrNull { abs(it.speedCms) } ?: 0

        if (targets.isNotEmpty()) {
            lastX = targets[0].xMm
            lastY = targets[0].yMm
            hadTarget = true
        } else if (hadTarget) {
            // Sasaran baru hilang. Jika terakhir dilihat di tepi kawasan,
            // anggap orang keluar bilik, bukan jatuh.
            if (isEdge(lastX, lastY)) lastFastMs = 0L
            hadTarget = false
        }

        if (maxSpeed >= FAST_CMS) lastFastMs = now
        if (maxSpeed >= MOVE_CMS) lastMoveMs = now
        if (maxSpeed >= GETUP_CMS) {
            if (movingSince == 0L) movingSince = now
        } else {
            movingSince = 0L
        }

        when (state) {
            State.NORMAL -> {
                val spikeWasLastMotion =
                    lastFastMs > 0L && lastMoveMs - lastFastMs < SPIKE_GAP_MS
                if (spikeWasLastMotion &&
                    now - lastMoveMs >= STILL_MS &&
                    now - lastAlertMs >= COOLDOWN_MS
                ) {
                    state = State.SUSPECTED
                    alertDeadlineMs = now + CANCEL_WINDOW_MS
                    lastFastMs = 0L
                }
            }

            State.SUSPECTED -> {
                if (movingSince != 0L && now - movingSince >= GETUP_HOLD_MS) {
                    state = State.NORMAL          // orang bangun semula
                    lastFastMs = 0L
                } else if (now >= alertDeadlineMs) {
                    fireAlert(now)
                }
            }
        }
    }

    /** Pengguna tekan "Saya OK". */
    @Synchronized
    fun confirmOk() {
        state = State.NORMAL
        lastFastMs = 0L
        lastMoveMs = 0L
    }

    /** Untuk ujian / demo tanpa perlu jatuh sebenar. */
    @Synchronized
    fun simulate() {
        state = State.SUSPECTED
        alertDeadlineMs = System.currentTimeMillis() + CANCEL_WINDOW_MS
    }

    @Synchronized
    fun reset() {
        state = State.NORMAL
        lastFastMs = 0L
        lastMoveMs = 0L
        movingSince = 0L
        hadTarget = false
    }

    fun todayCount(context: Context): Int {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return if (p.getString(K_DAY, null) == dayKey()) p.getInt(K_COUNT, 0) else 0
    }

    private fun fireAlert(now: Long) {
        state = State.NORMAL
        lastAlertMs = now
        lastFastMs = 0L

        val ctx = appCtx ?: return
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val day = dayKey()
        val count = (if (p.getString(K_DAY, null) == day) p.getInt(K_COUNT, 0) else 0) + 1
        p.edit().putString(K_DAY, day).putInt(K_COUNT, count).apply()

        NotificationHelper.show(
            ctx,
            NOTIF_ID,
            "Amaran jatuh dikesan",
            "Radar mengesan kemungkinan jatuh. Sila semak dengan segera."
        )
    }

    private fun isEdge(x: Int, y: Int): Boolean {
        if (y > EDGE_Y_MM || y < EDGE_NEAR_MM) return true
        val angle = Math.toDegrees(atan2(x.toDouble(), y.toDouble()))
        return abs(angle) > EDGE_ANGLE_DEG
    }

    private fun dayKey(): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    private const val PREFS = "fall_detector"
    private const val K_DAY = "day"
    private const val K_COUNT = "count"
}