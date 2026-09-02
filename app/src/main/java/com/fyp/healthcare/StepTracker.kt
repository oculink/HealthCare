package com.fyp.healthcare

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

/**
 * TODAY'S STEP COUNT — from the phone's hardware step counter (`Sensor.TYPE_STEP_COUNTER`).
 *
 * The sensor reports steps accumulated since the last device reboot, and the OS keeps
 * counting even while the app is closed / in the background. So we don't need a foreground
 * service — we just *sample* it: `MainActivity` registers this listener in `onResume` and
 * unregisters in `onPause`; the hardware keeps counting regardless and we catch up on the
 * next open.
 *
 * "Today" is derived by remembering the raw counter value at the first sample of each
 * calendar day (the baseline): today's steps = current raw − baseline. On a reboot the raw
 * value drops below the baseline, so we re-baseline (steps taken before the reboot that day
 * are lost — acceptable for the FYP).
 *
 * SELF MODE ONLY. A caregiver's phone steps say nothing about the patient they monitor, so
 * [start] no-ops in caretaker mode — the caregiver's Home reads the patient's mirrored
 * count from [ActivityDataManager] instead (hydrated from `users/{patientUid}`).
 *
 * Storage is a dedicated unscoped `steps` prefs file (wiped on sign-out via
 * [Session]'s `DATA_PREFS`), plus a mirror to `users/{uid}` through
 * [ActivityDataManager.recordSteps].
 */
object StepTracker {

    /** Today's step count, or null when unknown (no sensor / permission denied / not sampled yet). */
    var todaySteps by mutableStateOf<Int?>(null)
        private set

    private var sensorManager: SensorManager? = null
    private var listener: SensorEventListener? = null

    /** ACTIVITY_RECOGNITION is a runtime permission from API 29; below that the counter is free. */
    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACTIVITY_RECOGNITION,
            ) == PackageManager.PERMISSION_GRANTED

    /** Begin sampling the step counter. No-op in caretaker mode, without permission, or if already running. */
    fun start(context: Context) {
        if (Session.isCaretakerMode) {
            todaySteps = null
            stop()
            return
        }
        if (listener != null) return
        if (!hasPermission(context)) return

        val app = context.applicationContext
        val sm = app.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return

        // show the last persisted value straight away in case the sensor is slow to fire
        todaySteps = ActivityDataManager(app).steps()

        val l = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val raw = event.values.firstOrNull()?.toLong() ?: return
                val steps = foldReading(app, raw)
                todaySteps = steps
                ActivityDataManager(app).recordSteps(steps)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager = sm
        listener = l
    }

    /** Stop sampling (keeps the last value in memory for a fast redraw on the next start). */
    fun stop() {
        listener?.let { l -> sensorManager?.unregisterListener(l) }
        listener = null
        sensorManager = null
    }

    /** Raw cumulative counter → today's steps, maintaining the stored daily baseline. */
    private fun foldReading(context: Context, raw: Long): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = dateKey()
        val day = prefs.getString(K_DAY, null)
        var base = prefs.getLong(K_BASE, -1L)

        if (day != today || base < 0L || raw < base) {
            base = raw
            prefs.edit().putString(K_DAY, today).putLong(K_BASE, base).apply()
        }
        return (raw - base).toInt().coerceAtLeast(0)
    }

    private const val PREFS = "steps"
    private const val K_DAY = "day"
    private const val K_BASE = "base_raw"
}
