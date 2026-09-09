package com.fyp.healthcare

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

/**
 * UC-04 "Analyze Health Data".
 *
 * Takes the recorded vitals history and produces, for the most recent reading:
 *  - a per-metric judgement against predefined reference ranges (step 2–3)
 *  - a short-term trend for each metric vs. the mean of the previous readings (step 4)
 *  - an overall status flag Normal / Warning / Critical (step 5)
 *
 * All local + offline. The same [analyze] runs on the caregiver side over the patient's
 * hydrated history, which is how the "instant warning to Caregiver Remote Monitoring"
 * (A2, the <<extend>>) is surfaced without a server / push.
 *
 * A1 "insufficient data": a metric with fewer than [MIN_HISTORY_FOR_TREND] prior readings
 * is still classified, but its [VitalTrend] is [VitalTrend.INSUFFICIENT] and
 * [ReadingAnalysis.hasEnoughHistory] is false.
 */

enum class VitalLevel { NORMAL, WARNING, CRITICAL, UNKNOWN }
enum class VitalTrend { RISING, FALLING, STABLE, INSUFFICIENT }

data class MetricAnalysis(
    val key: String,
    val label: String,
    val display: String,
    val unit: String,
    val level: VitalLevel,
    val statusText: String,
    val note: String,
    val trend: VitalTrend,
    val recentAvg: String?,
)

data class ReadingAnalysis(
    val timestamp: Long,
    val metrics: List<MetricAnalysis>,
    val overall: VitalLevel,
    val hasEnoughHistory: Boolean,
    val summary: String,
) {
    val abnormal: List<MetricAnalysis>
        get() = metrics.filter { it.level == VitalLevel.WARNING || it.level == VitalLevel.CRITICAL }
    val worst: MetricAnalysis?
        get() = abnormal.minByOrNull { if (it.level == VitalLevel.CRITICAL) 0 else 1 }
}

object HealthAnalysis {

    private const val MIN_HISTORY_FOR_TREND = 3
    private const val TREND_LOOKBACK = 5
    private const val TREND_REL_THRESHOLD = 0.08

    fun analyze(history: List<HealthDataManager.Reading>): ReadingAnalysis? {
        val latest = history.lastOrNull() ?: return null
        val prior = history.dropLast(1)

        val metrics = ArrayList<MetricAnalysis>(5)

        val sys = latest.systolic
        val dia = latest.diastolic
        if (sys != null && dia != null) {
            val j = VitalRules.bloodPressure(sys, dia)
            val priorSys = prior.mapNotNull { it.systolic?.toFloat() }
            metrics += MetricAnalysis(
                key = "bp", label = "Blood Pressure", display = "$sys/$dia", unit = "mmHg",
                level = j.level, statusText = j.status,
                note = rangeNote(j, "systolic $sys, diastolic $dia"),
                trend = trendOf(sys.toFloat(), priorSys),
                recentAvg = recentAvg(sys.toFloat(), priorSys)?.let { "recent avg ${it.roundToInt()} systolic" },
            )
        }

        addNumeric(
            metrics, prior, key = "hr", label = "Heart Rate", unit = "BPM",
            value = latest.heartRateBpm?.toFloat(), display = latest.heartRateBpm?.toString(),
            select = { it.heartRateBpm?.toFloat() },
            judge = { VitalRules.heartRate(it.roundToInt()) },
        )
        addNumeric(
            metrics, prior, key = "sugar", label = "Blood Sugar", unit = "mg/dL",
            value = latest.bloodSugarValue, display = latest.bloodSugarValue?.let(::trimNum),
            select = { it.bloodSugarValue },
            judge = { VitalRules.glucoseMgDl(it) },
        )
        addNumeric(
            metrics, prior, key = "oxy", label = "Oxygen (SpO₂)", unit = "%",
            value = latest.oxygenValue, display = latest.oxygenValue?.let { "${it.roundToInt()}" },
            select = { it.oxygenValue },
            judge = { VitalRules.spo2(it) },
        )
        addNumeric(
            metrics, prior, key = "temp", label = "Temperature", unit = "°C",
            value = latest.temperatureValue, display = latest.temperatureValue?.let(::trimNum),
            select = { it.temperatureValue },
            judge = { VitalRules.tempC(it) },
        )

        val overall = when {
            metrics.any { it.level == VitalLevel.CRITICAL } -> VitalLevel.CRITICAL
            metrics.any { it.level == VitalLevel.WARNING } -> VitalLevel.WARNING
            metrics.any { it.level == VitalLevel.NORMAL } -> VitalLevel.NORMAL
            else -> VitalLevel.UNKNOWN
        }
        val hasEnoughHistory = history.size > MIN_HISTORY_FOR_TREND

        val abnormal = metrics.filter { it.level == VitalLevel.WARNING || it.level == VitalLevel.CRITICAL }
        val summary = when {
            metrics.isEmpty() -> "No readings to analyse yet."
            abnormal.isEmpty() ->
                "All ${metrics.size} recorded ${plural(metrics.size, "reading")} are within the normal range."
            else -> {
                val names = abnormal.joinToString(", ") {
                    "${it.label.substringBefore(" (").lowercase()} (${it.statusText})"
                }
                "${abnormal.size} ${plural(abnormal.size, "reading")} " +
                    "${if (abnormal.size == 1) "needs" else "need"} attention: $names."
            }
        }

        return ReadingAnalysis(latest.timestamp, metrics, overall, hasEnoughHistory, summary)
    }

    private inline fun addNumeric(
        out: MutableList<MetricAnalysis>,
        prior: List<HealthDataManager.Reading>,
        key: String, label: String, unit: String,
        value: Float?, display: String?,
        select: (HealthDataManager.Reading) -> Float?,
        judge: (Float) -> VitalRules.Judgement,
    ) {
        value ?: return
        val j = judge(value)
        val priorValues = prior.mapNotNull(select)
        out += MetricAnalysis(
            key = key, label = label, display = display ?: trimNum(value), unit = unit,
            level = j.level, statusText = j.status,
            note = rangeNote(j, "$display $unit"),
            trend = trendOf(value, priorValues),
            recentAvg = recentAvg(value, priorValues)?.let { "recent avg ${trimNum(it)}" },
        )
    }

    private fun trendOf(current: Float, priors: List<Float>): VitalTrend {
        if (priors.size < MIN_HISTORY_FOR_TREND) return VitalTrend.INSUFFICIENT
        val base = priors.takeLast(TREND_LOOKBACK).average()
        if (base == 0.0) return VitalTrend.STABLE
        val rel = (current - base) / base
        return when {
            rel > TREND_REL_THRESHOLD -> VitalTrend.RISING
            rel < -TREND_REL_THRESHOLD -> VitalTrend.FALLING
            else -> VitalTrend.STABLE
        }
    }

    /** Mean of the last few readings incl. the current one — null when history is too thin. */
    private fun recentAvg(current: Float, priors: List<Float>): Float? {
        if (priors.size < MIN_HISTORY_FOR_TREND) return null
        return (priors + current).takeLast(TREND_LOOKBACK).average().toFloat()
    }

    private fun rangeNote(j: VitalRules.Judgement, measured: String): String = when (j.level) {
        VitalLevel.NORMAL -> "Within the normal range (${j.normalRange})."
        VitalLevel.WARNING, VitalLevel.CRITICAL -> "${j.status} — normal is ${j.normalRange}."
        VitalLevel.UNKNOWN -> "Not enough information."
    }

    private fun plural(n: Int, word: String) = if (n == 1) word else "${word}s"

    private fun trimNum(v: Float): String =
        if (v == v.toLong().toFloat()) v.toLong().toString() else "%.1f".format(v)
}

/**
 * Simplified adult reference ranges. Sources to cite in the FYP report:
 *  - Blood pressure: ACC/AHA 2017 Hypertension Guideline
 *  - Heart rate: American Heart Association (resting adult 60–100 BPM)
 *  - SpO₂: WHO Pulse Oximetry Training Manual / BTS oxygen guideline
 *  - Blood glucose (casual / random, mg/dL): American Diabetes Association
 *  - Body temperature: NICE / standard clinical references
 * These are screening thresholds for a consumer app, NOT a diagnostic tool.
 */
object VitalRules {

    data class Judgement(val level: VitalLevel, val status: String, val normalRange: String)

    fun heartRate(bpm: Int): Judgement {
        val range = "60–100 BPM"
        return when {
            bpm < 40 || bpm > 130 -> Judgement(VitalLevel.CRITICAL, "Critical", range)
            bpm < 60 -> Judgement(VitalLevel.WARNING, "Low", range)
            bpm <= 90 -> Judgement(VitalLevel.NORMAL, "Good", range)
            bpm <= 100 -> Judgement(VitalLevel.NORMAL, "Normal", range)
            else -> Judgement(VitalLevel.WARNING, "High", range)
        }
    }

    fun bloodPressure(sys: Int, dia: Int): Judgement {
        val range = "below 130/85 mmHg"
        return when {
            sys >= 180 || dia >= 120 || sys < 80 || dia < 50 ->
                Judgement(VitalLevel.CRITICAL, "Critical", range)
            sys >= 140 || dia >= 90 -> Judgement(VitalLevel.WARNING, "High", range)
            sys >= 130 || dia >= 85 -> Judgement(VitalLevel.WARNING, "Elevated", range)
            sys < 90 || dia < 60 -> Judgement(VitalLevel.WARNING, "Low", range)
            sys >= 120 -> Judgement(VitalLevel.NORMAL, "Normal", range)
            else -> Judgement(VitalLevel.NORMAL, "Good", range)
        }
    }

    fun glucoseMgDl(v: Float): Judgement {
        val range = "70–140 mg/dL"
        return when {
            v < 54f || v > 300f -> Judgement(VitalLevel.CRITICAL, "Critical", range)
            v < 70f -> Judgement(VitalLevel.WARNING, "Low", range)
            v > 200f -> Judgement(VitalLevel.WARNING, "High", range)
            v > 140f -> Judgement(VitalLevel.WARNING, "Elevated", range)
            else -> Judgement(VitalLevel.NORMAL, "Good", range)
        }
    }

    fun spo2(v: Float): Judgement {
        val range = "95–100%"
        return when {
            v < 90f -> Judgement(VitalLevel.CRITICAL, "Critical", range)
            v < 95f -> Judgement(VitalLevel.WARNING, "Low", range)
            else -> Judgement(VitalLevel.NORMAL, "Good", range)
        }
    }

    fun tempC(v: Float): Judgement {
        val range = "36.1–37.5 °C"
        return when {
            v < 35f || v >= 39f -> Judgement(VitalLevel.CRITICAL, "Critical", range)
            v >= 37.6f -> Judgement(VitalLevel.WARNING, "Fever", range)
            v < 36.1f -> Judgement(VitalLevel.WARNING, "Low", range)
            else -> Judgement(VitalLevel.NORMAL, "Normal", range)
        }
    }
}


fun VitalLevel.color(): Color = when (this) {
    VitalLevel.NORMAL -> Color(0xFF2E9E6B)
    VitalLevel.WARNING -> Color(0xFFEE7B2E)
    VitalLevel.CRITICAL -> Color(0xFFD32F2F)
    VitalLevel.UNKNOWN -> Color(0xFF8A8F98)
}

fun VitalLevel.label(): String = when (this) {
    VitalLevel.NORMAL -> "Normal"
    VitalLevel.WARNING -> "Warning"
    VitalLevel.CRITICAL -> "Critical"
    VitalLevel.UNKNOWN -> "No data"
}

fun VitalTrend.arrow(): String = when (this) {
    VitalTrend.RISING -> "▲"
    VitalTrend.FALLING -> "▼"
    VitalTrend.STABLE -> "→"
    VitalTrend.INSUFFICIENT -> ""
}

fun VitalTrend.word(): String = when (this) {
    VitalTrend.RISING -> "rising"
    VitalTrend.FALLING -> "falling"
    VitalTrend.STABLE -> "stable"
    VitalTrend.INSUFFICIENT -> "not enough history"
}
