package com.fyp.healthcare

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.DrawableRes
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Health Report.
 *
 * A snapshot of the chosen one to six calendar months the patient, or their linked
 * caregiver, can generate on demand and hand to a doctor. One report is built per month,
 * so six months produces six separate PDFs. Built entirely from data already on the device:
 * the recorded vitals history ([HealthDataManager.history]), the health profile and the
 * medication log. Works offline and never invents numbers.
 *
 * The PDF follows the CareRadar "Patient Health Report" layout: a branded header, the
 * patient strip, four monthly averages, the daily readings log (one block per day), Key
 * Insights / Alerts and a monthly-average bar chart. Long logs paginate and repeat their
 * table header; the insights and graph follow the last log page. The status wording
 * ("Good / Normal / Low / High / Critical") is the same [VitalStatus] scale the Home
 * dashboard uses.
 */

/** Which vital a card / insight / chart belongs to - drives its icon and accent colour. */
enum class MetricKind(val display: String, @get:DrawableRes val icon: Int, val accent: Int) {
    HEART_RATE("Heart Rate", R.drawable.ic_rpt_heart, 0xFFE5484D.toInt()),
    BLOOD_PRESSURE("Blood Pressure", R.drawable.ic_rpt_gauge, 0xFFEA6A2C.toInt()),
    BLOOD_SUGAR("Blood Sugar", R.drawable.ic_rpt_droplet, 0xFFD6336C.toInt()),
    OXYGEN("Oxygen Level", R.drawable.ic_rpt_air, 0xFF2A6DE1.toInt()),
    TEMPERATURE("Temperature", R.drawable.ic_rpt_thermostat, 0xFF2E9E6B.toInt()),
    WEIGHT("Weight", R.drawable.ic_rpt_weight, 0xFF7048E8.toInt()),
    RELIABILITY("Your Records", R.drawable.ic_rpt_clipboard, 0xFF2A6DE1.toInt()),
    ADHERENCE("Medication", R.drawable.ic_rpt_check, 0xFF2E9E6B.toInt());
}

/** One line of the report's summary grid. */
data class ReportMetric(
    val kind: MetricKind,
    /** Formatted value with unit, e.g. "74 BPM". `null` when nothing was recorded. */
    val value: String?,
    val status: ReportStatus,
    /** Primary number behind [value]: systolic for blood pressure, mg/dL for glucose. */
    val rawValue: Float? = null,
)

enum class ReportStatus(val label: String) {
    GOOD("Good"),
    NORMAL("Normal"),
    LOW("Low"),
    HIGH("High"),
    CRITICAL("Critical"),
    STABLE("Stable"),
    NONE("No data");

    companion object {
        fun fromVital(s: String): ReportStatus = when (s) {
            "Good" -> GOOD
            "Normal" -> NORMAL
            "Low" -> LOW
            "High" -> HIGH
            "Critical" -> CRITICAL
            else -> NONE
        }
    }
}

enum class OverallStatus(val label: String) {
    GOOD("All Good"),
    ATTENTION("Needs Attention"),
    NO_DATA("No Data"),
}

/** One point on a trend chart - a day's average for that metric. */
data class TrendPoint(
    val dateLabel: String,
    val caption: String,
    val value: Float,
    val display: String,
)

/** One trend chart: a metric plotted across the selected reporting range. */
data class TrendSeries(
    val kind: MetricKind,
    val unit: String,
    val axisMax: Float,
    val points: List<TrendPoint>,
)
/** One recorded set of vitals, formatted for the PDF's daily readings log. */
data class ReportReading(
    /** "2026-09-01" - groups several readings taken on the same day. */
    val dateKey: String,
    /** "01 Sep 2026" */
    val dateLabel: String,
    /** "08:00 AM" */
    val timeLabel: String,
    /** Raw entered values; blank when the patient left that field empty. */
    val heartRate: String,
    val bloodPressure: String,
    val oxygen: String,
    val temperature: String,
)

/** One written insight shown in the report preview (the PDF uses condensed bullets). */
data class ReportInsight(val kind: MetricKind, val body: String) {
    val title: String get() = kind.display
}

data class HealthReport(
    /** Calendar month covered, e.g. "November 2026". */
    val periodLabel: String,
    /** Compact month label for selectors, e.g. "Nov 2026". */
    val monthShortLabel: String,
    /** Full dates the report covers, e.g. "1 Nov 2026 - 30 Nov 2026". */
    val windowLabel: String,
    /** First instant of the calendar month. */
    val monthStart: Long,
    /** First instant of the following month (exclusive). */
    val monthEnd: Long,
    /** True when this report covers the month currently in progress. */
    val isCurrentMonth: Boolean,
    val patientName: String,
    val patientAge: String,
    val patientSex: String,
    val patientBloodType: String,
    val generatedAt: Long,
    val readingCount: Int,
    val overall: OverallStatus,
    val overallDetail: String,
    val metrics: List<ReportMetric>,
    val trends: List<TrendSeries>,
    val allergies: List<String>,
    val conditions: List<String>,
    val insights: List<ReportInsight>,
    /** Every recorded set in the month, oldest first - the PDF's daily readings log. */
    val readings: List<ReportReading> = emptyList(),
    /** Stable, non-identifying ID shown in the PDF header, e.g. "CR-10023". */
    val patientId: String = "",
) {
    val hasData: Boolean get() = readingCount > 0

    val allergiesLabel: String get() = allergies.joinToString(", ").ifBlank { "None on file" }
    val conditionsLabel: String get() = conditions.joinToString(", ").ifBlank { "None on file" }

    val patientLine: String
        get() = buildList {
            if (patientAge.isNotBlank()) add("Age $patientAge")
            if (patientSex.isNotBlank()) add(patientSex)
            if (patientBloodType.isNotBlank()) add("Blood type $patientBloodType")
        }.joinToString("  ·  ")

    val generatedAtLabel: String
        get() = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(generatedAt))

    /** "1 reading" / "12 readings" - used in the export header. */
    val readingCountLabel: String
        get() = if (readingCount == 1) "1 reading" else "$readingCount readings"

    /** "November 2026" / "November 2026 so far" - for use mid-sentence. */
    val periodLowerLabel: String
        get() = if (isCurrentMonth) "$periodLabel so far" else periodLabel

    companion object {

        /** User-selectable number of monthly reports. */
        const val MIN_MONTHS = 1
        const val MAX_MONTHS = 6

        /** Trend charts show this many averaged buckets within each month. */
        private const val TREND_BUCKETS = 5

        /**
         * Build one report per calendar month, newest first: the current month plus the
         * previous [rangeMonths] - 1 months. All reports share the same generated time.
         * Pure - no I/O.
         */
        fun generateMonths(
            profile: HealthProfile,
            readings: List<HealthDataManager.Reading>,
            medications: List<Medication>,
            patientName: String,
            rangeMonths: Int = MIN_MONTHS,
            nowMillis: Long = System.currentTimeMillis(),
            patientId: String = "",
        ): List<HealthReport> {
            val months = rangeMonths.coerceIn(MIN_MONTHS, MAX_MONTHS)
            val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val currentMonth = (now.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            return (0 until months).map { back ->
                val start = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, -back) }
                buildMonth(
                    profile, readings, medications, patientName,
                    start, now, nowMillis, back == 0, patientId,
                )
            }
        }

        /**
         * A stable patient ID for the report header. Derived from the account uid (or the
         * patient's name when signed out) so it never exposes the uid itself and stays the
         * same every time a report is generated.
         */
        fun patientIdFor(uid: String?, name: String): String {
            val seed = uid?.takeIf { it.isNotBlank() } ?: name.ifBlank { "patient" }
            var hash = 7
            seed.forEach { hash = (hash * 31 + it.code) and 0x7FFFFFFF }
            return "CR-" + (10000 + hash % 90000)
        }

        /** Build one report for the calendar month beginning at [start]. */
        private fun buildMonth(
            profile: HealthProfile,
            readings: List<HealthDataManager.Reading>,
            medications: List<Medication>,
            patientName: String,
            start: Calendar,
            now: Calendar,
            generatedAtMillis: Long,
            isCurrentMonth: Boolean,
            patientId: String,
        ): HealthReport {
            val monthStart = (start.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val nextMonth = (monthStart.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            val lastDay = (monthStart.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            val monthStartMillis = monthStart.timeInMillis
            val monthEndExclusive = nextMonth.timeInMillis
            // A month still in progress only has data up to now; past months are complete.
            val cutoff = if (isCurrentMonth) now.timeInMillis.coerceAtMost(monthEndExclusive - 1)
            else monthEndExclusive - 1

            val recent = readings.filter {
                it.timestamp >= monthStartMillis && it.timestamp <= cutoff
            }

            val periodLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthStart.time)
            val monthShortLabel = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(monthStart.time)
            val monthPhrase = if (isCurrentMonth) "$periodLabel so far" else periodLabel

            fun avg(from: List<HealthDataManager.Reading>, select: (HealthDataManager.Reading) -> Float?): Float? {
                val v = from.mapNotNull(select)
                return if (v.isEmpty()) null else v.sum() / v.size
            }

            val selSys: (HealthDataManager.Reading) -> Float? = { it.systolic?.toFloat() }
            val selDia: (HealthDataManager.Reading) -> Float? =
                { it.bloodPressure.split("/").getOrNull(1)?.trim()?.toFloatOrNull() }
            val selHr: (HealthDataManager.Reading) -> Float? = { it.heartRateBpm?.toFloat() }
            val selSugar: (HealthDataManager.Reading) -> Float? = { it.bloodSugarValue }
            val selOxy: (HealthDataManager.Reading) -> Float? = { it.oxygenValue }
            val selTemp: (HealthDataManager.Reading) -> Float? = { it.temperature.trim().toFloatOrNull() }

            val hr = avg(recent, selHr)
            val sys = avg(recent, selSys)
            val dia = avg(recent, selDia)
            val sugar = avg(recent, selSugar)
            val oxygen = avg(recent, selOxy)
            val temperature = avg(recent, selTemp)

            val metrics = listOf(
                ReportMetric(
                    MetricKind.HEART_RATE,
                    hr?.let { "${it.roundToInt()} BPM" },
                    hr?.let { ReportStatus.fromVital(VitalStatus.heartRate(it.roundToInt())) } ?: ReportStatus.NONE,
                    hr,
                ),
                ReportMetric(
                    MetricKind.BLOOD_PRESSURE,
                    if (sys != null && dia != null) "${sys.roundToInt()}/${dia.roundToInt()} mmHg" else null,
                    if (sys != null && dia != null)
                        ReportStatus.fromVital(VitalStatus.bloodPressure(sys.roundToInt(), dia.roundToInt()))
                    else ReportStatus.NONE,
                    sys,
                ),
                ReportMetric(
                    MetricKind.BLOOD_SUGAR,
                    sugar?.let { "${BloodSugar.formatMmol(it)} mmol/L" },
                    sugar?.let { bloodSugarStatus(it) } ?: ReportStatus.NONE,
                    sugar,
                ),
                ReportMetric(
                    MetricKind.OXYGEN,
                    oxygen?.let { "${it.roundToInt()}%" },
                    oxygen?.let { ReportStatus.fromVital(VitalStatus.oxygen(it.roundToInt())) } ?: ReportStatus.NONE,
                    oxygen,
                ),
                ReportMetric(
                    MetricKind.TEMPERATURE,
                    temperature?.let { "${trimReportNumber(it)} °C" },
                    temperature?.let { temperatureStatus(it) } ?: ReportStatus.NONE,
                    temperature,
                ),
                ReportMetric(
                    MetricKind.WEIGHT,
                    profile.weightKg.trim().takeIf { it.isNotBlank() }?.let { "$it kg" },
                    if (profile.weightKg.isBlank()) ReportStatus.NONE else ReportStatus.STABLE,
                    profile.weightKg.trim().toFloatOrNull(),
                ),
            )

            val dateFmt = SimpleDateFormat("d MMM", Locale.getDefault())
            // Spread the covered days across five averaged buckets. A month in progress
            // charts month-to-date; a finished month charts its full calendar span.
            val spanStart = monthStartMillis
            val spanEnd = if (isCurrentMonth) maxOf(now.timeInMillis, spanStart + TREND_BUCKETS)
            else monthEndExclusive
            val bucketSpan = (spanEnd - spanStart) / TREND_BUCKETS.toDouble()
            val buckets = (0 until TREND_BUCKETS).map { i ->
                val from = spanStart + (i * bucketSpan).toLong()
                val to = if (i == TREND_BUCKETS - 1) spanEnd
                else spanStart + ((i + 1) * bucketSpan).toLong()
                from to to
            }

            fun series(
                kind: MetricKind, unit: String, axisMax: Float,
                select: (HealthDataManager.Reading) -> Float?,
                transform: (Float) -> Float = { it },
                display: (List<HealthDataManager.Reading>) -> String,
            ): TrendSeries {
                val points = buckets.mapNotNull { (from, to) ->
                    val bucketReadings = recent.filter { r ->
                        r.timestamp >= from &&
                            if (to == spanEnd) r.timestamp <= to else r.timestamp < to
                    }
                    val v = avg(bucketReadings, select) ?: return@mapNotNull null
                    val labelCal = Calendar.getInstance().apply {
                        timeInMillis = (to - 1L).coerceAtLeast(from)
                    }
                    TrendPoint(
                        dateLabel = dateFmt.format(labelCal.time),
                        caption = "",
                        value = transform(v),
                        display = display(bucketReadings),
                    )
                }
                return TrendSeries(kind, unit, axisMax, points)
            }

            val trends = listOf(
                series(MetricKind.HEART_RATE, "BPM", 120f, selHr) { d ->
                    avg(d, selHr)?.roundToInt()?.toString() ?: "--"
                },
                series(MetricKind.BLOOD_PRESSURE, "mmHg", 180f, selSys) { d ->
                    val s = avg(d, selSys)?.roundToInt(); val di = avg(d, selDia)?.roundToInt()
                    if (s != null && di != null) "$s/$di" else "--"
                },
                series(
                    MetricKind.BLOOD_SUGAR, "mmol/L", 10f, selSugar,
                    transform = { BloodSugar.mgDlToMmol(it) },
                ) { d ->
                    avg(d, selSugar)?.let { BloodSugar.formatMmol(it) } ?: "--"
                },
                series(MetricKind.OXYGEN, "%", 100f, selOxy) { d ->
                    avg(d, selOxy)?.roundToInt()?.toString() ?: "--"
                },
            )

            val concerning = setOf(ReportStatus.LOW, ReportStatus.HIGH, ReportStatus.CRITICAL)
            val flagged = metrics.filter { it.status in concerning }
            val overall = when {
                recent.isEmpty() -> OverallStatus.NO_DATA
                flagged.isEmpty() -> OverallStatus.GOOD
                else -> OverallStatus.ATTENTION
            }
            val overallDetail = when {
                recent.isEmpty() ->
                    "There are no vitals logged in $monthPhrase."
                flagged.isEmpty() ->
                    "Every reading you logged during $monthPhrase is within its recommended range."
                flagged.size == 1 ->
                    "${flagged.first().kind.display} sits outside the recommended range."
                else ->
                    "A few readings sit outside the recommended range."
            }

            val windowFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
            val windowLabel =
                "${windowFmt.format(monthStart.time)} - ${windowFmt.format(lastDay.time)}"

            // Adherence only counts days that have happened, even for the current month.
            val coverageEnd = if (isCurrentMonth) now else lastDay

            // Readings for the PDF's daily log, grouped by day at render time.
            val dateKeyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val logDateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val logTimeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val logReadings = recent.map { r ->
                ReportReading(
                    dateKey = dateKeyFmt.format(Date(r.timestamp)),
                    dateLabel = logDateFmt.format(Date(r.timestamp)),
                    timeLabel = logTimeFmt.format(Date(r.timestamp)),
                    heartRate = r.heartRate.trim(),
                    bloodPressure = r.bloodPressure.trim(),
                    oxygen = r.oxygen.trim(),
                    temperature = r.temperature.trim(),
                )
            }

            return HealthReport(
                periodLabel = periodLabel,
                monthShortLabel = monthShortLabel,
                windowLabel = windowLabel,
                monthStart = monthStartMillis,
                monthEnd = monthEndExclusive,
                isCurrentMonth = isCurrentMonth,
                patientName = patientName.ifBlank { "Patient" },
                patientAge = profile.age?.toString().orEmpty(),
                patientSex = profile.sex.trim(),
                patientBloodType = profile.bloodType.trim(),
                generatedAt = generatedAtMillis,
                readingCount = recent.size,
                overall = overall,
                overallDetail = overallDetail,
                metrics = metrics,
                trends = trends,
                allergies = profile.allergies,
                conditions = profile.conditions,
                insights = buildInsights(
                    recent.size, metrics, medications,
                    monthStart, coverageEnd, monthPhrase,
                ),
                readings = logReadings,
                patientId = patientId,
            )
        }

        private fun buildInsights(
            readingCount: Int,
            metrics: List<ReportMetric>,
            medications: List<Medication>,
            monthStart: Calendar,
            coverageEnd: Calendar,
            monthPhrase: String,
        ): List<ReportInsight> {

            if (readingCount == 0) {
                return listOf(
                    ReportInsight(
                        MetricKind.RELIABILITY,
                        "There are no vitals logged in $monthPhrase. You can record them from " +
                            "the home screen, so the next report has a full picture.",
                    )
                )
            }

            val insights = mutableListOf<ReportInsight>()
            val withData = metrics.filter { it.value != null }
            val concerning = setOf(ReportStatus.CRITICAL, ReportStatus.HIGH, ReportStatus.LOW)

            if (withData.size >= 3 && withData.none { it.status in concerning }) {
                insights.add(
                    ReportInsight(
                        MetricKind.RELIABILITY,
                        "A steady stretch · every vital logged during $monthPhrase stayed within " +
                            "a healthy range.",
                    )
                )
            }

            withData
                .sortedBy { SEVERITY_ORDER[it.status] ?: Int.MAX_VALUE }
                .forEach { m -> insightBody(m)?.let { insights.add(ReportInsight(m.kind, it)) } }

            if (readingCount in 1..4) {
                insights.add(
                    ReportInsight(
                        MetricKind.RELIABILITY,
                        "Only $readingCount ${if (readingCount == 1) "reading was" else "readings were"} " +
                            "logged during $monthPhrase · more readings make these averages " +
                            "more reliable.",
                    )
                )
            }

            adherenceNote(medications, monthStart, coverageEnd, monthPhrase)?.let {
                insights.add(ReportInsight(MetricKind.ADHERENCE, it))
            }

            return insights
        }

        private fun adherenceNote(
            medications: List<Medication>,
            monthStart: Calendar,
            coverageEnd: Calendar,
            monthPhrase: String,
        ): String? {
            val day = (monthStart.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val lastDay = (coverageEnd.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val days = mutableSetOf<String>()
            while (!day.after(lastDay)) {
                days.add(dateKey(day))
                day.add(Calendar.DAY_OF_YEAR, 1)
            }

            var taken = 0
            var missed = 0
            medications.forEach { med ->
                med.log.forEach { (key, action) ->
                    if (key.substringBefore(' ') in days) when (action) {
                        "taken" -> taken++
                        "missed" -> missed++
                    }
                }
            }
            val total = taken + missed
            if (total == 0) return null
            val pct = (taken * 100.0 / total).roundToInt()
            return "Medication during $monthPhrase: you took $taken of $total logged doses ($pct%)."
        }

        private val SEVERITY_ORDER = mapOf(
            ReportStatus.CRITICAL to 0,
            ReportStatus.HIGH to 1,
            ReportStatus.LOW to 2,
            ReportStatus.NORMAL to 3,
            ReportStatus.GOOD to 4,
            ReportStatus.STABLE to 5,
        )

        /** A sentence written for this specific metric AND status. Null when there's no reading. */
        private fun insightBody(metric: ReportMetric): String? {
            val v = metric.value ?: return null
            return when (metric.kind) {

                MetricKind.HEART_RATE -> when (metric.status) {
                    ReportStatus.GOOD ->
                        "Your average resting heart rate of $v sits right in the healthy zone · that's a well-rested heart."
                    ReportStatus.NORMAL ->
                        "Your average heart rate of $v is inside the normal adult range, close to the upper end."
                    ReportStatus.LOW ->
                        "Your average heart rate of $v is below 60 BPM. That can be normal if you're very active · tell your doctor if it comes with dizziness or fatigue."
                    ReportStatus.HIGH ->
                        "Your average heart rate of $v runs above a typical resting rate. Caffeine, stress and poor sleep all push it up · worth a mention if it stays there."
                    ReportStatus.CRITICAL ->
                        "Your average heart rate of $v is well outside the safe resting range · please have a doctor check it soon."
                    else -> null
                }

                MetricKind.BLOOD_PRESSURE -> when (metric.status) {
                    ReportStatus.GOOD ->
                        "Your average blood pressure of $v sits in the ideal range · keep it up"
                    ReportStatus.NORMAL ->
                        "Your average blood pressure of $v is okay, just a little above optimal. Less salt and a daily walk both help keep it down."
                    ReportStatus.LOW ->
                        "Your average blood pressure of $v is on the low side · mention it to your doctor if you feel light-headed on standing."
                    ReportStatus.HIGH ->
                        "Your average blood pressure of $v is above the recommended level · if it keeps reading this high, talk it over with your doctor."
                    ReportStatus.CRITICAL ->
                        "Your average blood pressure of $v is in a dangerous range · please get medical advice promptly"
                    else -> null
                }

                MetricKind.BLOOD_SUGAR -> when (metric.status) {
                    ReportStatus.GOOD, ReportStatus.NORMAL ->
                        "Your average blood sugar of $v sits comfortably in a normal range."
                    ReportStatus.LOW ->
                        "Your average blood sugar of $v is below normal. Frequent lows matter, especially if you take diabetes medication · bring this up with your doctor."
                    ReportStatus.HIGH ->
                        "Your average blood sugar of $v is above the normal range · if it stays there, ask your doctor about an HbA1c test."
                    ReportStatus.CRITICAL ->
                        "Your average blood sugar of $v is far outside the safe range · please contact a doctor."
                    else -> null
                }

                MetricKind.OXYGEN -> when (metric.status) {
                    ReportStatus.GOOD, ReportStatus.NORMAL ->
                        "Your average blood oxygen of $v is healthy · 95% or above is where you want it."
                    ReportStatus.LOW ->
                        "Your average blood oxygen of $v is a little under the usual 95-100%. One low reading is often just poor sensor contact · a sustained dip is worth mentioning."
                    ReportStatus.CRITICAL ->
                        "Your average blood oxygen of $v is low enough to need medical attention · please follow up with a doctor"
                    else -> null
                }

                MetricKind.TEMPERATURE -> when (metric.status) {
                    ReportStatus.GOOD, ReportStatus.NORMAL ->
                        "Your average body temperature of $v is normal, with no sign of fever."
                    ReportStatus.LOW ->
                        "Your average body temperature of $v reads below normal. Check how you're taking the reading, and mention it if it stays low."
                    ReportStatus.HIGH ->
                        "Your average body temperature of $v is slightly raised · that points to a mild fever if it carries on."
                    ReportStatus.CRITICAL ->
                        "Your average body temperature of $v suggests a significant fever · or, if the readings are low, hypothermia. Seek medical advice."
                    else -> null
                }

                MetricKind.WEIGHT -> when (metric.status) {
                    ReportStatus.STABLE ->
                        "Weight on file is $v, from your profile rather than a fresh reading. Keep it up to date in Edit Profile so later reports can show the trend."
                    else -> null
                }

                else -> null
            }
        }

        private fun bloodSugarStatus(mgdl: Float): ReportStatus = when {
            mgdl < 54f || mgdl > 250f -> ReportStatus.CRITICAL
            mgdl < 70f -> ReportStatus.LOW
            mgdl > 140f -> ReportStatus.HIGH
            else -> ReportStatus.NORMAL
        }

        private fun temperatureStatus(celsius: Float): ReportStatus = when {
            celsius < 35f || celsius >= 39f -> ReportStatus.CRITICAL
            celsius >= 37.6f -> ReportStatus.HIGH
            celsius < 36f -> ReportStatus.LOW
            else -> ReportStatus.NORMAL
        }
    }
}

/** 98.0 -> "98", 97.5 -> "97.5" */
private fun trimReportNumber(v: Float): String =
    if (v == v.toLong().toFloat()) v.toLong().toString() else String.format(Locale.US, "%.1f", v)

//  Export - a plain-text body for the share sheet and the branded PDF.

object HealthReportExport {

    private fun fileNameFor(report: HealthReport): String =
        "CareRadar-Patient-Health-Report-${report.periodLabel.replace(" ", "-")}.pdf"

    /** A readable text version, used as the share-sheet body / email text. */
    fun plainText(report: HealthReport): String = buildString {
        appendLine("CareRadar Patient Health Report")
        appendLine("${report.periodLabel}  (${report.windowLabel})")
        appendLine("Patient: ${report.patientName}")
        if (report.patientLine.isNotBlank()) appendLine(report.patientLine.replace("  ·  ", " · "))
        appendLine("Generated ${report.generatedAtLabel} · ${report.readingCountLabel} logged during ${report.periodLowerLabel}")
        appendLine()
        appendLine("OVERALL: ${report.overall.label} · ${report.overallDetail}")
        appendLine()
        appendLine("MEDICAL BACKGROUND")
        appendLine("- Allergies: ${report.allergiesLabel}")
        appendLine("- Conditions: ${report.conditionsLabel}")
        appendLine()
        appendLine("SUMMARY")
        report.metrics.forEach { m ->
            appendLine("- ${m.kind.display}: ${m.value ?: "--"}  (${m.status.label})")
        }
        if (report.insights.isNotEmpty()) {
            appendLine()
            appendLine("HEALTH INSIGHTS")
            report.insights.forEach { appendLine("- ${it.title}: ${it.body}") }
        }
        appendLine()
        appendLine("Generated by CareRadar. This report isn't a substitute for professional medical advice.")
    }


    // ----- PDF layout -------------------------------------------------------

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 20f
    private const val CONTENT_W = PAGE_W - 2 * MARGIN
    private const val CONTENT_R = PAGE_W - MARGIN
    private const val CONTENT_TOP = 40f
    private const val BOTTOM = 782f
    private const val FOOTER_TOP = 792f
    private const val HERO_H = 68f
    private const val SECTION_H = 23f
    private const val TABLE_HEAD_H = 32f
    private const val ROW_H = 19f

    private val INK = 0xFF14263F.toInt()
    private val BODY = 0xFF33465F.toInt()
    private val MUTED = 0xFF6B7C93.toInt()
    private val HAIRLINE = 0xFFDCE6F3.toInt()
    private val NAVY = 0xFF123E7C.toInt()
    private val BRAND = 0xFF2A6DE1.toInt()
    private val PANEL = 0xFFF4F8FE.toInt()
    private val WHITE = 0xFFFFFFFF.toInt()
    private val GREEN = 0xFF2E9E6B.toInt()
    private val AMBER = 0xFFE07A1F.toInt()
    private val RED = 0xFFD3454B.toInt()
    private val TABLE_HEAD = 0xFFE8F0FB.toInt()
    private val TABLE_ALT = 0xFFF7FAFF.toInt()

    private fun sans(weight: String): Typeface = Typeface.create(weight, Typeface.NORMAL)

    private class Paints {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        val text = Paint(Paint.ANTI_ALIAS_FLAG)

        fun t(size: Float, color: Int, tf: Typeface, align: Paint.Align = Paint.Align.LEFT): Paint =
            Paint(text).apply {
                textSize = size; this.color = color; typeface = tf; textAlign = align
            }
    }

    /** Render a vector drawable to a tinted bitmap for the PDF canvas. */
    private fun icon(context: Context, @DrawableRes id: Int, px: Int, tint: Int): Bitmap {
        val d = ResourcesCompat.getDrawable(context.resources, id, null)!!.mutate()
        DrawableCompat.setTint(d, tint)
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, px, px)
        d.draw(Canvas(bmp))
        return bmp
    }

    private fun withAlpha(color: Int, alpha: Int): Int = (alpha shl 24) or (color and 0x00FFFFFF)

    private fun Canvas.roundRect(l: Float, t: Float, r: Float, b: Float, rad: Float, p: Paint) {
        drawRoundRect(RectF(l, t, r, b), rad, rad, p)
    }

    /** Icons are rasterised at 3x then filtered down, so they stay crisp when printed. */
    private fun drawIcon(
        c: Canvas, context: Context, @DrawableRes id: Int,
        x: Float, y: Float, size: Float, tint: Int,
    ) {
        if (size <= 0f) return
        val px = (size * 3f).roundToInt().coerceAtLeast(12)
        c.drawBitmap(
            icon(context, id, px, tint), null, RectF(x, y, x + size, y + size),
            Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true },
        )
    }

    /** Decode a raster drawable (e.g. the app logo) into a bitmap; null if missing. */
    private fun rasterBitmap(context: Context, @DrawableRes id: Int): Bitmap? =
        runCatching { BitmapFactory.decodeResource(context.resources, id) }.getOrNull()

    /** Draw [bmp] into the rounded rect (l,t,r,b), scaled to fill and corner-clipped. */
    private fun Canvas.drawRoundedBitmap(bmp: Bitmap, l: Float, t: Float, r: Float, b: Float, rad: Float) {
        val save = save()
        clipPath(Path().apply { addRoundRect(RectF(l, t, r, b), rad, rad, Path.Direction.CW) })
        drawBitmap(bmp, null, RectF(l, t, r, b), Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true })
        restoreToCount(save)
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        val out = mutableListOf<String>()
        var line = StringBuilder()
        text.split(" ").forEach { word ->
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) <= maxWidth) line = StringBuilder(candidate)
            else {
                if (line.isNotEmpty()) out.add(line.toString())
                line = StringBuilder(word)
            }
        }
        if (line.isNotEmpty()) out.add(line.toString())
        return out
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) end--
        return text.substring(0, end).trimEnd() + "…"
    }

    /** Shrink [text] to [minSize], then ellipsize, so long values never cross a divider. */
    private fun fitText(text: String, paint: Paint, maxWidth: Float, minSize: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var size = paint.textSize
        while (size > minSize && paint.measureText(text) > maxWidth) {
            size -= 0.4f
            paint.textSize = size
        }
        return ellipsize(text, paint, maxWidth)
    }

    /** Render the report to an A4 PDF in cacheDir/reports and return the file. */
    fun buildPdf(context: Context, report: HealthReport): File {
        val doc = PdfDocument()
        PdfRenderer(doc, context, report, Paints()).render()
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, fileNameFor(report))
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    /**
     * Paints the report page by page.
     *
     * Page 1 carries the branded header, the patient strip, the four monthly averages and the
     * daily readings log. Continuation pages repeat the log header. The last page of the log is
     * followed by Key Insights / Alerts and the monthly-average graph when they fit, otherwise
     * they move to a fresh page. No reading is ever dropped: long logs simply paginate.
     */
    private class PdfRenderer(
        private val doc: PdfDocument,
        private val context: Context,
        private val report: HealthReport,
        private val paints: Paints,
    ) {
        private var pageNo = 1
        private lateinit var page: PdfDocument.Page
        private lateinit var c: Canvas

        /** Decoded once per export - the header, running header and footer all use it. */
        private val logo: Bitmap? by lazy { rasterBitmap(context, R.drawable.ic_app_logo) }

        fun render() {
            startPage()
            drawHero()
            drawPatientCard()
            drawBackgroundStrip()
            drawSummary()
            drawLogThenTail()
            finishPage()
        }

        private fun startPage() {
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            c = page.canvas
            if (pageNo > 1) drawRunningHeader()
        }

        private fun finishPage() {
            drawFooter()
            doc.finishPage(page)
            pageNo++
        }

        private fun nextPage() {
            finishPage()
            startPage()
        }

        // ----- page 1 blocks -------------------------------------------------

        private fun drawHero() {
            val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, PAGE_W.toFloat(), HERO_H,
                    0xFF0B336C.toInt(), 0xFF1D4E9B.toInt(), Shader.TileMode.CLAMP,
                )
            }
            c.drawRect(0f, 0f, PAGE_W.toFloat(), HERO_H, bg)

            paints.fill.color = 0x12FFFFFF
            c.drawPath(
                Path().apply {
                    moveTo(248f, 0f); lineTo(352f, 0f)
                    lineTo(206f, HERO_H); lineTo(102f, HERO_H)
                    close()
                },
                paints.fill,
            )
            paints.fill.color = 0x0AFFFFFF
            c.drawPath(
                Path().apply {
                    moveTo(374f, 0f); lineTo(420f, 0f)
                    lineTo(274f, HERO_H); lineTo(228f, HERO_H)
                    close()
                },
                paints.fill,
            )

            paints.fill.color = WHITE
            c.roundRect(MARGIN, 13f, MARGIN + 42f, 55f, 9f, paints.fill)
            logo?.let {
                c.drawRoundedBitmap(it, MARGIN + 3f, 16f, MARGIN + 39f, 52f, 6f)
            }
            c.drawText("CareRadar", MARGIN + 52f, 35f, paints.t(18f, WHITE, sans("sans-serif-black")))
            c.drawText(
                "Monitor · Care · Live Better",
                MARGIN + 52f, 47f,
                paints.t(7f, withAlpha(WHITE, 205), sans("sans-serif")),
            )

            paints.stroke.color = withAlpha(WHITE, 70)
            paints.stroke.strokeWidth = 0.8f
            c.drawLine(MARGIN + 174f, 14f, MARGIN + 174f, 54f, paints.stroke)

            c.drawText(
                "Patient Health Report", MARGIN + 188f, 34f,
                paints.t(17.5f, WHITE, sans("sans-serif-black")),
            )
            c.drawText(
                "For Medical Professional", MARGIN + 189f, 47f,
                paints.t(8.6f, withAlpha(WHITE, 215), sans("sans-serif")),
            )

            drawDoctor(MARGIN + 474f, 2f)
        }

        /** A simple white-on-navy doctor silhouette for the header, drawn with paths. */
        private fun drawDoctor(x: Float, y: Float) {
            paints.fill.color = WHITE
            c.drawCircle(x + 30f, y + 12f, 7.5f, paints.fill)
            c.drawRect(x + 25.5f, y + 17f, x + 34.5f, y + 25f, paints.fill)
            c.drawPath(
                Path().apply {
                    moveTo(x + 9f, y + 63f)
                    lineTo(x + 11f, y + 42f)
                    cubicTo(x + 12f, y + 27f, x + 19f, y + 21f, x + 30f, y + 21f)
                    cubicTo(x + 41f, y + 21f, x + 48f, y + 27f, x + 49f, y + 42f)
                    lineTo(x + 51f, y + 63f)
                    close()
                },
                paints.fill,
            )

            paints.fill.color = 0xFF123E7C.toInt()
            c.drawPath(
                Path().apply {
                    moveTo(x + 30f, y + 22f)
                    lineTo(x + 23f, y + 44f)
                    lineTo(x + 30f, y + 37f)
                    lineTo(x + 37f, y + 44f)
                    close()
                },
                paints.fill,
            )

            paints.stroke.color = withAlpha(0xFF0B2F5F.toInt(), 190)
            paints.stroke.strokeWidth = 1.4f
            c.drawLine(x + 21f, y + 27f, x + 17f, y + 47f, paints.stroke)
            c.drawLine(x + 39f, y + 27f, x + 43f, y + 47f, paints.stroke)

            paints.stroke.color = 0xFF0B2F5F.toInt()
            paints.stroke.strokeWidth = 1.6f
            c.drawArc(RectF(x + 20f, y + 22f, x + 40f, y + 40f), 200f, 140f, false, paints.stroke)
            c.drawLine(x + 37f, y + 35f, x + 37f, y + 49f, paints.stroke)
            c.drawCircle(x + 37f, y + 51f, 2.1f, paints.stroke)

            paints.fill.color = 0xFF123E7C.toInt()
            c.drawRect(x + 39f, y + 50f, x + 43f, y + 58f, paints.fill)
            c.drawRect(x + 36f, y + 53f, x + 46f, y + 55f, paints.fill)
        }

        private fun drawPatientCard() {
            val top = 76f
            val bottom = 144f
            paints.fill.color = PANEL
            c.roundRect(MARGIN, top, CONTENT_R, bottom, 9f, paints.fill)
            paints.stroke.color = HAIRLINE
            paints.stroke.strokeWidth = 0.9f
            c.roundRect(MARGIN, top, CONTENT_R, bottom, 9f, paints.stroke)

            paints.fill.color = 0xFFDCE9FB.toInt()
            c.drawCircle(MARGIN + 26f, 110f, 18.5f, paints.fill)
            drawIcon(c, context, R.drawable.ic_rpt_person, MARGIN + 16f, 100f, 20f, 0xFF3E67A8.toInt())

            paints.stroke.color = 0xFFD9E4F3.toInt()
            paints.stroke.strokeWidth = 0.8f
            c.drawLine(MARGIN + 262f, 86f, MARGIN + 262f, 134f, paints.stroke)
            c.drawLine(MARGIN + 402f, 86f, MARGIN + 402f, 134f, paints.stroke)

            val labelP = paints.t(7.3f, 0xFF5C77A6.toInt(), sans("sans-serif"))

            c.drawText("Patient Name", MARGIN + 56f, 94f, labelP)
            val nameP = paints.t(11.8f, INK, sans("sans-serif-black"))
            c.drawText(fitText(report.patientName, nameP, 196f, 9f), MARGIN + 56f, 108f, nameP)

            c.drawText("Age / Gender", MARGIN + 56f, 122f, labelP)
            val ageText = listOf(report.patientAge, report.patientSex)
                .filter { it.isNotBlank() }
                .joinToString(" / ")
                .ifBlank { "-" }
            val ageP = paints.t(8.9f, INK, sans("sans-serif-medium"))
            c.drawText(fitText(ageText, ageP, 196f, 7.5f), MARGIN + 56f, 135f, ageP)

            c.drawText("Patient ID", MARGIN + 278f, 94f, labelP)
            val idP = paints.t(9.4f, INK, sans("sans-serif-medium"))
            c.drawText(report.patientId.ifBlank { "-" }, MARGIN + 278f, 108f, idP)

            c.drawText("Report Period", MARGIN + 278f, 122f, labelP)
            val periodP = paints.t(8.2f, INK, sans("sans-serif-medium"))
            c.drawText(
                fitText(periodRange(), periodP, CONTENT_R - (MARGIN + 278f) - 150f, 7f),
                MARGIN + 278f, 135f, periodP,
            )

            drawIcon(c, context, R.drawable.ic_rpt_calendar, MARGIN + 418f, 97f, 20f, 0xFF4A5F9E.toInt())
            c.drawText("Report Generated", MARGIN + 446f, 102f, labelP)
            val genP = paints.t(8.2f, INK, sans("sans-serif-medium"))
            c.drawText(
                fitText(generatedLabel(), genP, CONTENT_R - (MARGIN + 446f) - 4f, 6.8f),
                MARGIN + 446f, 117f, genP,
            )
        }

        private fun periodRange(): String {
            val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            return "${fmt.format(Date(report.monthStart))} – ${fmt.format(Date(report.monthEnd - 1))}"
        }

        private fun generatedLabel(): String =
            SimpleDateFormat("d MMMM yyyy, h:mm a", Locale.getDefault()).format(Date(report.generatedAt))

        /** Blood type / allergies / conditions / weight: one compact row under the patient card. */
        private fun drawBackgroundStrip() {
            val top = 150f
            val bottom = 174f
            paints.fill.color = WHITE
            c.roundRect(MARGIN, top, CONTENT_R, bottom, 7f, paints.fill)
            paints.stroke.color = HAIRLINE
            paints.stroke.strokeWidth = 0.9f
            c.roundRect(MARGIN, top, CONTENT_R, bottom, 7f, paints.stroke)

            val xs = floatArrayOf(MARGIN, MARGIN + 92f, MARGIN + 264f, MARGIN + 462f)
            paints.stroke.color = 0xFFE7EEF8.toInt()
            paints.stroke.strokeWidth = 0.8f
            for (i in 1 until xs.size) c.drawLine(xs[i], top + 5f, xs[i], bottom - 5f, paints.stroke)

            backgroundCell(xs[0], xs[1], R.drawable.ic_rpt_droplet, "Blood type", report.patientBloodType.ifBlank { "-" }, 0xFF2A6DE1.toInt())
            backgroundCell(xs[1], xs[2], R.drawable.ic_rpt_warning, "Allergies", report.allergiesLabel, 0xFFE06A2C.toInt())
            backgroundCell(xs[2], xs[3], R.drawable.ic_rpt_clipboard, "Conditions", report.conditionsLabel, 0xFF7048E8.toInt())
            backgroundCell(
                xs[3], CONTENT_R, R.drawable.ic_rpt_weight, "Weight",
                report.metrics.firstOrNull { it.kind == MetricKind.WEIGHT }?.value ?: "-",
                0xFF2E9E6B.toInt(),
            )
        }

        private fun backgroundCell(
            l: Float, r: Float, @DrawableRes iconId: Int,
            label: String, value: String, accent: Int,
        ) {
            drawIcon(c, context, iconId, l + 8f, 157f, 9f, accent)
            val labelP = paints.t(6.4f, MUTED, sans("sans-serif"))
            c.drawText("$label:", l + 20f, 166f, labelP)
            val valueP = paints.t(7.2f, INK, sans("sans-serif-medium"))
            val valueX = l + 20f + labelP.measureText("$label:") + 3f
            c.drawText(ellipsize(value, valueP, r - valueX - 8f), valueX, 166f, valueP)
        }

        private data class SummaryCard(
            val kind: MetricKind,
            val label: String,
            val normal: String,
        )

        private fun drawSummary() {
            drawSectionBar(MARGIN, 186f, R.drawable.ic_rpt_bars, "Monthly Summary")

            val cards = listOf(
                SummaryCard(MetricKind.HEART_RATE, "Average Heart Rate", "Normal: 60–100"),
                SummaryCard(MetricKind.BLOOD_PRESSURE, "Average Blood Pressure", "Normal: <120/80"),
                SummaryCard(MetricKind.OXYGEN, "Average SpO₂", "Normal: 95–100%"),
                SummaryCard(MetricKind.TEMPERATURE, "Average Body Temperature", "Normal: 36.1–37.2 °C"),
            )
            val gap = 7f
            val cardW = (CONTENT_W - gap * 3) / 4f
            val top = 213f
            val bottom = 271f
            cards.forEachIndexed { i, card ->
                val x = MARGIN + i * (cardW + gap)
                val bg: Int
                val line: Int
                val accent: Int
                when (card.kind) {
                    MetricKind.HEART_RATE -> {
                        bg = 0xFFFDEEF1.toInt(); line = 0xFFF3CBD5.toInt(); accent = 0xFFE5484D.toInt()
                    }
                    MetricKind.BLOOD_PRESSURE -> {
                        bg = 0xFFEAF2FE.toInt(); line = 0xFFC9DCF8.toInt(); accent = 0xFF2A6DE1.toInt()
                    }
                    MetricKind.OXYGEN -> {
                        bg = 0xFFF2EDFC.toInt(); line = 0xFFD9CBF3.toInt(); accent = 0xFF7048E8.toInt()
                    }
                    else -> {
                        bg = 0xFFFDF3E6.toInt(); line = 0xFFF2DCBA.toInt(); accent = 0xFFEA6A2C.toInt()
                    }
                }
                paints.fill.color = bg
                c.roundRect(x, top, x + cardW, bottom, 8f, paints.fill)
                paints.stroke.color = line
                paints.stroke.strokeWidth = 0.9f
                c.roundRect(x, top, x + cardW, bottom, 8f, paints.stroke)

                drawIcon(c, context, card.kind.icon, x + 9f, top + 9f, 13f, accent)

                val labelP = paints.t(6.9f, BODY, sans("sans-serif-medium"))
                c.drawText(fitText(card.label, labelP, cardW - 37f, 5.8f), x + 27f, top + 16f, labelP)

                val metric = report.metrics.firstOrNull { it.kind == card.kind }
                val (main, unit) = cardValue(metric)
                val valueP = paints.t(13.2f, INK, sans("sans-serif-black"))
                c.drawText(main, x + 27f, top + 37f, valueP)
                if (unit.isNotBlank()) {
                    c.drawText(
                        " $unit", x + 27f + valueP.measureText(main), top + 37f,
                        paints.t(7.2f, 0xFF263A55.toInt(), sans("sans-serif")),
                    )
                }
                c.drawText("(${card.normal})", x + 10f, top + 51f, paints.t(6.3f, MUTED, sans("sans-serif")))
            }
        }

        private fun cardValue(metric: ReportMetric?): Pair<String, String> {
            val v = metric?.value ?: return "--" to ""
            return when (metric.kind) {
                MetricKind.HEART_RATE -> v.removeSuffix(" BPM").trim() to "bpm"
                MetricKind.BLOOD_PRESSURE -> v.removeSuffix(" mmHg").trim().replace("/", " / ") to "mmHg"
                MetricKind.OXYGEN -> v.removeSuffix("%").trim() to "%"
                MetricKind.TEMPERATURE -> v.removeSuffix(" °C").trim() to "°C"
                else -> v to ""
            }
        }

        private fun drawSectionBar(x: Float, y: Float, @DrawableRes iconId: Int, title: String) {
            paints.fill.color = NAVY
            c.roundRect(x, y, x + CONTENT_W, y + SECTION_H, 6f, paints.fill)
            drawIcon(c, context, iconId, x + 9f, y + 6f, 11f, WHITE)
            c.drawText(title, x + 26f, y + 15.6f, paints.t(10.2f, WHITE, sans("sans-serif-medium")))
        }

        // ----- daily readings log -------------------------------------------

        private fun drawLogThenTail() {
            val rows = report.readings
            var barTop = 283f
            drawSectionBar(MARGIN, barTop, R.drawable.ic_rpt_clipboard, "Daily Readings Log")
            var tableTop = barTop + SECTION_H

            var index = 0
            var tableBottom: Float
            if (rows.isEmpty()) {
                tableBottom = drawLogTable(emptyList(), tableTop)
            } else {
                while (true) {
                    val rowsTop = tableTop + TABLE_HEAD_H
                    val capacity = ((BOTTOM - rowsTop) / ROW_H).toInt().coerceAtLeast(1)
                    val end = minOf(index + capacity, rows.size)
                    tableBottom = drawLogTable(rows.subList(index, end), tableTop)
                    index = end
                    if (index >= rows.size) break
                    nextPage()
                    barTop = CONTENT_TOP
                    drawSectionBar(MARGIN, barTop, R.drawable.ic_rpt_clipboard, "Daily Readings Log  ·  continued")
                    tableTop = barTop + SECTION_H
                }
            }

            var y = tableBottom
            val insightH = insightsBlockHeight()
            if (BOTTOM - y >= insightH + 12f) {
                y += 12f
                drawInsightPanels(y)
                y += insightH
            } else {
                nextPage()
                y = CONTENT_TOP
                drawInsightPanels(y)
                y += insightH
            }

            val graphH = SECTION_H + 5f + 150f
            if (BOTTOM - y >= graphH + 10f) {
                drawGraph(y + 10f)
            } else {
                nextPage()
                drawGraph(CONTENT_TOP)
            }
        }

        /**
         * Draws one page's worth of the readings log and returns its bottom edge. The date is
         * printed once per day and centred over that day's rows, so three readings on
         * 1 September read as one block rather than repeating the date three times.
         */
        private fun drawLogTable(rows: List<ReportReading>, top: Float): Float {
            val l = MARGIN
            val r = CONTENT_R
            val bottom = top + TABLE_HEAD_H + (if (rows.isEmpty()) 44f else rows.size * ROW_H)

            paints.fill.color = WHITE
            c.roundRect(l, top, r, bottom, 7f, paints.fill)

            val save = c.save()
            c.clipPath(Path().apply { addRoundRect(RectF(l, top, r, bottom), 7f, 7f, Path.Direction.CW) })

            paints.fill.color = TABLE_HEAD
            c.drawRect(l, top, r, top + TABLE_HEAD_H, paints.fill)

            val fracs = floatArrayOf(0.22f, 0.17f, 0.14f, 0.17f, 0.14f, 0.16f)
            val xs = FloatArray(7)
            xs[0] = l
            for (i in 0..5) xs[i + 1] = xs[i] + CONTENT_W * fracs[i]

            val headerIcons = intArrayOf(
                R.drawable.ic_rpt_calendar, -1, R.drawable.ic_rpt_heart,
                R.drawable.ic_rpt_gauge, R.drawable.ic_rpt_air, R.drawable.ic_rpt_thermostat,
            )
            val names = arrayOf("Date", "Time", "Heart Rate", "Blood Pressure", "SpO₂", "Temperature")
            val units = arrayOf("", "", "(bpm)", "(mmHg)", "(%)", "(°C)")
            val headP = paints.t(6.9f, NAVY, sans("sans-serif-medium"))
            val unitP = paints.t(6.2f, MUTED, sans("sans-serif"))
            val singleP = paints.t(7.9f, NAVY, sans("sans-serif-medium"))
            for (i in 0..5) {
                when (i) {
                    0 -> drawIcon(c, context, headerIcons[i], xs[i] + 7f, top + 10.5f, 9f, 0xFF3E67A8.toInt())
                    1 -> drawClock(xs[i] + 7f, top + 10.5f, 9f, 0xFF3E67A8.toInt())
                    else -> drawIcon(c, context, headerIcons[i], xs[i] + 7f, top + 8.5f, 9f, 0xFF3E67A8.toInt())
                }
                if (i <= 1) {
                    c.drawText(names[i], xs[i] + 19f, top + 20f, singleP)
                } else {
                    c.drawText(names[i], xs[i] + 19f, top + 13.5f, headP)
                    c.drawText(units[i], xs[i] + 19f, top + 24f, unitP)
                }
                if (i > 0) {
                    paints.stroke.color = 0xFFD6E3F6.toInt()
                    paints.stroke.strokeWidth = 0.7f
                    c.drawLine(xs[i], top + 6f, xs[i], top + TABLE_HEAD_H - 6f, paints.stroke)
                }
            }
            paints.stroke.color = 0xFFD6E3F6.toInt()
            paints.stroke.strokeWidth = 0.7f
            c.drawLine(l, top + TABLE_HEAD_H, r, top + TABLE_HEAD_H, paints.stroke)

            val bodyTop = top + TABLE_HEAD_H
            if (rows.isEmpty()) {
                c.drawText(
                    "No readings were recorded in this period.",
                    (l + r) / 2f, bodyTop + 26f,
                    paints.t(8.4f, MUTED, sans("sans-serif"), Paint.Align.CENTER),
                )
            } else {
                val valueP = paints.t(8.2f, INK, sans("sans-serif"), Paint.Align.CENTER)
                val dateP = paints.t(8.2f, NAVY, sans("sans-serif-medium"))
                val timeP = paints.t(8.2f, BODY, sans("sans-serif"))
                val lightLine = 0xFFEFF4FB.toInt()
                val dayLine = 0xFFE2EBF7.toInt()
                val bodyBottom = bodyTop + rows.size * ROW_H
                var i = 0
                while (i < rows.size) {
                    val key = rows[i].dateKey
                    var j = i + 1
                    while (j < rows.size && rows[j].dateKey == key) j++
                    val blockTop = bodyTop + i * ROW_H
                    val blockBottom = blockTop + (j - i) * ROW_H
                    if ((key.takeLast(2).toIntOrNull() ?: 0) % 2 == 0) {
                        paints.fill.color = TABLE_ALT
                        c.drawRect(l, blockTop, r, blockBottom, paints.fill)
                    }
                    c.drawText(rows[i].dateLabel, xs[0] + 9f, (blockTop + blockBottom) / 2f + 2.8f, dateP)
                    for (k in i until j) {
                        val rt = bodyTop + k * ROW_H
                        val base = rt + ROW_H / 2f + 2.8f
                        val row = rows[k]
                        c.drawText(row.timeLabel, xs[1] + 9f, base, timeP)
                        c.drawText(row.heartRate.ifBlank { "—" }, (xs[2] + xs[3]) / 2f, base, valueP)
                        c.drawText(
                            formatBp(row.bloodPressure),
                            (xs[3] + xs[4]) / 2f, base, valueP,
                        )
                        c.drawText(
                            row.oxygen.ifBlank { "—" }.let { if (it == "—") it else "$it%" },
                            (xs[4] + xs[5]) / 2f, base, valueP,
                        )
                        c.drawText(row.temperature.ifBlank { "—" }, (xs[5] + xs[6]) / 2f, base, valueP)
                    }
                    for (k in i until j - 1) {
                        val lineY = bodyTop + (k + 1) * ROW_H
                        paints.stroke.color = lightLine
                        paints.stroke.strokeWidth = 0.6f
                        c.drawLine(l, lineY, r, lineY, paints.stroke)
                    }
                    if (j < rows.size) {
                        paints.stroke.color = dayLine
                        paints.stroke.strokeWidth = 0.8f
                        c.drawLine(l, blockBottom, r, blockBottom, paints.stroke)
                    }
                    i = j
                }
                paints.stroke.color = 0xFFE5EDF9.toInt()
                paints.stroke.strokeWidth = 0.7f
                c.drawLine(xs[1], bodyTop, xs[1], bodyBottom, paints.stroke)
            }

            c.restoreToCount(save)
            paints.stroke.color = HAIRLINE
            paints.stroke.strokeWidth = 1f
            c.roundRect(l, top, r, bottom, 7f, paints.stroke)
            return bottom
        }

        /** "116/74" or "116 / 74" -> "116 / 74"; blank -> em dash. */
        private fun formatBp(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return "—"
            val parts = trimmed.split("/")
            return if (parts.size == 2) "${parts[0].trim()} / ${parts[1].trim()}" else trimmed
        }

        private fun drawClock(x: Float, y: Float, size: Float, color: Int) {
            paints.stroke.color = color
            paints.stroke.strokeWidth = 1f
            c.drawCircle(x + size / 2f, y + size / 2f, size / 2f - 0.7f, paints.stroke)
            c.drawLine(x + size / 2f, y + size / 2f, x + size / 2f, y + size * 0.28f, paints.stroke)
            c.drawLine(x + size / 2f, y + size / 2f, x + size * 0.72f, y + size / 2f, paints.stroke)
        }

        // ----- insights + alerts --------------------------------------------

        private fun insightTexts(): List<String> {
            if (report.readingCount == 0) {
                return listOf(
                    "No vitals were logged in this period.",
                    "Record readings from the home screen so the next report has a full picture.",
                )
            }
            val out = mutableListOf<String>()
            when (report.overall) {
                OverallStatus.GOOD ->
                    out.add("All readings logged in this period are within the normal range.")
                OverallStatus.ATTENTION ->
                    out.add("Some readings fall outside the recommended range · see Alerts.")
                OverallStatus.NO_DATA ->
                    out.add("No vitals were logged in this period.")
            }

            // One line for the four card metrics, only when they are all in range.
            val cardMetrics = listOf(
                MetricKind.HEART_RATE, MetricKind.BLOOD_PRESSURE,
                MetricKind.OXYGEN, MetricKind.TEMPERATURE,
            ).mapNotNull { kind ->
                report.metrics.firstOrNull { it.kind == kind && it.value != null }
            }
            if (cardMetrics.size >= 2 &&
                cardMetrics.all { it.status == ReportStatus.GOOD || it.status == ReportStatus.NORMAL }
            ) {
                val names = cardMetrics.map {
                    when (it.kind) {
                        MetricKind.HEART_RATE -> "heart rate"
                        MetricKind.BLOOD_PRESSURE -> "blood pressure"
                        MetricKind.OXYGEN -> "oxygen"
                        else -> "temperature"
                    }
                }
                val list = if (names.size == 1) names.first()
                else names.dropLast(1).joinToString(", ") + " and " + names.last()
                out.add(
                    list.replaceFirstChar { it.toString().uppercase(Locale.US) } +
                        " all stayed in range."
                )
            }

            report.metrics.firstOrNull { it.kind == MetricKind.BLOOD_SUGAR && it.value != null }?.let {
                out.add("Blood sugar averaged ${it.value}.")
            }

            out.add(
                "${report.readingCount} ${if (report.readingCount == 1) "reading was" else "readings were"} " +
                    "logged in this period."
            )

            report.insights.firstOrNull { it.kind == MetricKind.ADHERENCE }?.body
                ?.substringAfter("you took ", "")
                ?.replace("logged doses", "doses")
                ?.takeIf { it.isNotBlank() }
                ?.let { out.add("Medication: $it") }

            return out.take(5)
        }

        private fun alertTexts(): List<Pair<String, Boolean>> {
            if (report.readingCount == 0) {
                return listOf("No readings were recorded in this period." to false)
            }
            val concerning = setOf(ReportStatus.CRITICAL, ReportStatus.HIGH, ReportStatus.LOW)
            val alerts = report.metrics
                .filter { it.status in concerning && it.value != null }
                .sortedBy { if (it.status == ReportStatus.CRITICAL) 0 else 1 }
                .map { m ->
                    val direction = if (m.status == ReportStatus.LOW) "below" else "above"
                    val text = when (m.kind) {
                        MetricKind.HEART_RATE ->
                            "Average heart rate of ${m.value} is $direction the recommended range."
                        MetricKind.BLOOD_PRESSURE ->
                            "Average blood pressure of ${m.value} is $direction the recommended range."
                        MetricKind.BLOOD_SUGAR ->
                            "Average blood sugar of ${m.value} is $direction the normal range."
                        MetricKind.OXYGEN -> if (m.status == ReportStatus.LOW)
                            "Average blood oxygen of ${m.value} is below the usual 95–100%."
                        else "Average blood oxygen of ${m.value} is outside the usual 95–100%."
                        MetricKind.TEMPERATURE -> if (m.status == ReportStatus.HIGH)
                            "Average body temperature of ${m.value} suggests a mild fever."
                        else "Average body temperature of ${m.value} is below normal."
                        else -> "${m.kind.display} averaged ${m.value} · outside the normal range."
                    }
                    text to true
                }
            return if (alerts.isEmpty()) {
                listOf("No urgent alerts at this time." to false)
            } else {
                alerts.take(4)
            }
        }

        private fun panelWidths(): Pair<Float, Float> {
            val gap = 8f
            val left = (CONTENT_W - gap) * 0.60f
            return left to (CONTENT_W - gap - left)
        }

        private fun panelHeight(rows: List<List<String>>): Float {
            var h = 27f
            rows.forEach { h += maxOf(13.5f, it.size * 9.2f + 5f) + 1.2f }
            return h + 5f
        }

        /** Height of the taller of the two side-by-side panels, so the graph clears both. */
        private fun insightsBlockHeight(): Float {
            val (leftW, rightW) = panelWidths()
            val insightP = paints.t(7.4f, BODY, sans("sans-serif"))
            val alertP = paints.t(7.4f, BODY, sans("sans-serif"))
            val insightH = panelHeight(insightTexts().map { wrap(it, insightP, leftW - 34f) })
            val alertH = panelHeight(alertTexts().map { wrap(it.first, alertP, rightW - 34f) })
            return maxOf(insightH, alertH)
        }

        private fun drawInsightPanels(top: Float) {
            val (leftW, rightW) = panelWidths()
            val gap = 8f
            val insights = insightTexts()
            val alerts = alertTexts()

            val insightP = paints.t(7.4f, 0xFF2F4A73.toInt(), sans("sans-serif"))
            val alertP = paints.t(7.4f, 0xFF7A4A12.toInt(), sans("sans-serif"))
            val insightRows = insights.map { wrap(it, insightP, leftW - 34f) }
            val alertRows = alerts.map { wrap(it.first, alertP, rightW - 34f) }

            drawPanel(
                MARGIN, top, leftW, panelHeight(insightRows),
                "Key Insights", R.drawable.ic_rpt_bulb, BRAND,
                0xFFF3F7FE.toInt(), 0xFFD9E6F8.toInt(), NAVY,
                insightRows, insightP, GREEN, List(insightRows.size) { false },
            )
            drawPanel(
                MARGIN + leftW + gap, top, rightW, panelHeight(alertRows),
                "Alerts / Attention Required", R.drawable.ic_rpt_warning, AMBER,
                0xFFFDF5E9.toInt(), 0xFFF1DCBA.toInt(), 0xFF8A5A16.toInt(),
                alertRows, alertP, AMBER, alerts.map { it.second },
            )
        }

        private fun drawPanel(
            l: Float, top: Float, w: Float, h: Float,
            title: String, @DrawableRes iconId: Int, accent: Int,
            bg: Int, border: Int, titleColor: Int,
            rows: List<List<String>>, bodyP: Paint, badgeColor: Int,
            badges: List<Boolean>,
        ) {
            paints.fill.color = bg
            c.roundRect(l, top, l + w, top + h, 8f, paints.fill)
            paints.stroke.color = border
            paints.stroke.strokeWidth = 0.9f
            c.roundRect(l, top, l + w, top + h, 8f, paints.stroke)

            paints.fill.color = withAlpha(accent, 30)
            c.drawCircle(l + 19f, top + 14f, 8f, paints.fill)
            drawIcon(c, context, iconId, l + 14f, top + 9f, 10f, accent)

            val titleP = paints.t(8.6f, titleColor, sans("sans-serif-medium"))
            c.drawText(ellipsize(title, titleP, w - 40f), l + 32f, top + 17.5f, titleP)

            var y = top + 27f
            rows.forEachIndexed { idx, lines ->
                val rowH = maxOf(13.5f, lines.size * 9.2f + 5f)
                val cy = y + 5f
                if (badges.getOrElse(idx) { false }) {
                    paints.fill.color = badgeColor
                    c.drawCircle(l + 13f, cy, 4.1f, paints.fill)
                    c.drawText(
                        "!", l + 13f, cy + 2.2f,
                        paints.t(5.8f, WHITE, sans("sans-serif-black"), Paint.Align.CENTER),
                    )
                } else {
                    drawCheck(l + 13f, cy)
                }
                lines.forEachIndexed { i, line ->
                    c.drawText(line, l + 22f, y + 8.2f + i * 9.2f, bodyP)
                }
                y += rowH + 1.2f
            }
        }

        private fun drawCheck(cx: Float, cy: Float) {
            paints.fill.color = GREEN
            c.drawCircle(cx, cy, 4.1f, paints.fill)
            paints.stroke.color = WHITE
            paints.stroke.strokeWidth = 1.1f
            c.drawPath(
                Path().apply {
                    moveTo(cx - 2f, cy + 0.1f)
                    lineTo(cx - 0.5f, cy + 1.8f)
                    lineTo(cx + 2.2f, cy - 1.6f)
                },
                paints.stroke,
            )
        }

        // ----- monthly average graph ----------------------------------------

        private data class GraphBar(
            val label: String,
            val unit: String,
            val value: Float?,
            val display: String,
            val axisMax: Float,
            val color: Int,
        )

        private fun graphBars(): List<GraphBar> {
            val hr = report.metrics.firstOrNull { it.kind == MetricKind.HEART_RATE }
            val bp = report.metrics.firstOrNull { it.kind == MetricKind.BLOOD_PRESSURE }
            val sugar = report.metrics.firstOrNull { it.kind == MetricKind.BLOOD_SUGAR }
            val oxygen = report.metrics.firstOrNull { it.kind == MetricKind.OXYGEN }
            val temp = report.metrics.firstOrNull { it.kind == MetricKind.TEMPERATURE }
            return listOf(
                GraphBar(
                    "Blood Pressure", "(mmHg)", bp?.rawValue,
                    bp?.value?.removeSuffix(" mmHg")?.replace("/", " / ") ?: "--",
                    180f, 0xFF3F7CF7.toInt(),
                ),
                GraphBar(
                    // rawValue is stored in mg/dL; the report displays mmol/L.
                    "Blood Sugar", "(mmol/L)",
                    sugar?.rawValue?.let { BloodSugar.mgDlToMmol(it) },
                    sugar?.rawValue?.let { BloodSugar.formatMmol(it) } ?: "--",
                    10f, 0xFFF28A2E.toInt(),
                ),
                GraphBar(
                    "Heart Rate", "(bpm)", hr?.rawValue,
                    hr?.rawValue?.roundToInt()?.toString() ?: "--",
                    150f, 0xFF7A4FC9.toInt(),
                ),
                GraphBar(
                    "Temperature", "(°C)", temp?.rawValue,
                    temp?.rawValue?.let { "${trimReportNumber(it)} °C" } ?: "--",
                    42f, 0xFF43BFB4.toInt(),
                ),
                GraphBar(
                    "SpO₂", "(%)", oxygen?.rawValue,
                    oxygen?.rawValue?.roundToInt()?.let { "$it%" } ?: "--",
                    100f, 0xFF123C6B.toInt(),
                ),
            )
        }

        private fun drawGraph(top: Float) {
            drawSectionBar(MARGIN, top, R.drawable.ic_rpt_chart, "Health Summary Graph")
            val l = MARGIN
            val r = CONTENT_R
            val cardTop = top + SECTION_H + 5f
            val cardBottom = cardTop + 150f
            paints.fill.color = WHITE
            c.roundRect(l, cardTop, r, cardBottom, 8f, paints.fill)
            paints.stroke.color = HAIRLINE
            paints.stroke.strokeWidth = 0.9f
            c.roundRect(l, cardTop, r, cardBottom, 8f, paints.stroke)

            c.drawText(
                "Health Metrics Summary (Monthly Average)",
                (l + r) / 2f, cardTop + 17f,
                paints.t(8.8f, NAVY, sans("sans-serif-black"), Paint.Align.CENTER),
            )

            val bars = graphBars()
            if (bars.all { it.value == null }) {
                c.drawText(
                    "No readings were recorded in this period.",
                    (l + r) / 2f, cardTop + 84f,
                    paints.t(9f, MUTED, sans("sans-serif"), Paint.Align.CENTER),
                )
                return
            }
            val plotL = l + 16f
            val plotR = r - 16f
            val plotT = cardTop + 25f
            val plotB = cardTop + 112f

            paints.stroke.color = 0xFFEDF2F9.toInt()
            paints.stroke.strokeWidth = 0.7f
            for (k in 0..4) {
                val gy = plotB - k / 4f * (plotB - plotT)
                c.drawLine(plotL, gy, plotR, gy, paints.stroke)
            }

            val slot = (plotR - plotL) / 5f
            bars.forEachIndexed { i, bar ->
                val cx = plotL + slot * (i + 0.5f)
                val barW = 26f
                val value = bar.value
                if (value == null || value <= 0f) {
                    c.drawText(
                        "--", cx, plotB - 6f,
                        paints.t(8.2f, MUTED, sans("sans-serif-medium"), Paint.Align.CENTER),
                    )
                } else {
                    val h = (value / bar.axisMax).coerceIn(0.04f, 1f) * (plotB - plotT)
                    paints.fill.color = bar.color
                    c.drawRect(cx - barW / 2f, plotB - h, cx + barW / 2f, plotB, paints.fill)
                    val barTop = plotB - h
                    if (barTop < plotT + 10f) {
                        // A near-full bar has no room above it: print the value inside it.
                        c.drawText(
                            bar.display, cx, barTop + 12f,
                            paints.t(8f, WHITE, sans("sans-serif-black"), Paint.Align.CENTER),
                        )
                    } else {
                        c.drawText(
                            bar.display, cx, barTop - 5f,
                            paints.t(8.2f, INK, sans("sans-serif-black"), Paint.Align.CENTER),
                        )
                    }
                }
                c.drawText(
                    bar.label, cx, plotB + 13f,
                    paints.t(7.2f, NAVY, sans("sans-serif-medium"), Paint.Align.CENTER),
                )
                c.drawText(
                    bar.unit, cx, plotB + 22f,
                    paints.t(6.4f, MUTED, sans("sans-serif"), Paint.Align.CENTER),
                )
            }

            c.drawText(
                "Bars are scaled to each metric's own range · values are monthly averages.",
                (l + r) / 2f, cardBottom - 5f,
                paints.t(6.1f, MUTED, sans("sans-serif"), Paint.Align.CENTER),
            )
        }

        // ----- recurring page furniture -------------------------------------

        private fun drawRunningHeader() {
            logo?.let {
                c.drawRoundedBitmap(it, MARGIN, 14f, MARGIN + 14f, 28f, 3f)
            }
            c.drawText("CareRadar", MARGIN + 20f, 26f, paints.t(9.2f, NAVY, sans("sans-serif-black")))
            c.drawText(
                "Patient Health Report  ·  ${report.patientName}",
                MARGIN + 70f, 26f,
                paints.t(7.8f, MUTED, sans("sans-serif")),
            )
            c.drawText(
                "${report.periodLabel}  ·  ${report.readingCountLabel}  ·  Page $pageNo",
                CONTENT_R, 26f,
                paints.t(7.8f, MUTED, sans("sans-serif"), Paint.Align.RIGHT),
            )
            paints.stroke.color = HAIRLINE
            paints.stroke.strokeWidth = 0.8f
            c.drawLine(MARGIN, 34f, CONTENT_R, 34f, paints.stroke)
        }

        private fun drawFooter() {
            paints.fill.color = 0xFFEDF3FC.toInt()
            c.drawRect(0f, FOOTER_TOP, PAGE_W.toFloat(), PAGE_H.toFloat(), paints.fill)

            paints.stroke.color = 0xFFDDE8FA.toInt()
            paints.stroke.strokeWidth = 11f
            c.drawCircle(60f, 880f, 82f, paints.stroke)
            c.drawCircle(560f, 900f, 108f, paints.stroke)

            logo?.let {
                c.drawRoundedBitmap(it, MARGIN, 802f, MARGIN + 20f, 822f, 4f)
            }
            c.drawText("CareRadar", MARGIN + 26f, 817f, paints.t(12.2f, NAVY, sans("sans-serif-black")))

            paints.stroke.color = 0xFFC9D8EE.toInt()
            paints.stroke.strokeWidth = 0.9f
            c.drawLine(MARGIN + 110f, 802f, MARGIN + 110f, 830f, paints.stroke)
            c.drawLine(MARGIN + 400f, 802f, MARGIN + 400f, 830f, paints.stroke)

            val discP = paints.t(6.5f, 0xFF4E6A92.toInt(), Typeface.create("sans-serif", Typeface.ITALIC))
            c.drawText("This report is generated from CareRadar Health Monitoring System", MARGIN + 118f, 810f, discP)
            c.drawText("and is intended for medical review purposes only.", MARGIN + 118f, 819f, discP)

            val tagP = paints.t(9.4f, NAVY, Typeface.create("serif", Typeface.ITALIC), Paint.Align.RIGHT)
            c.drawText("Better Care. Healthier Tomorrow.", CONTENT_R, 815f, tagP)
            paints.stroke.color = withAlpha(BRAND, 120)
            paints.stroke.strokeWidth = 0.8f
            c.drawPath(
                Path().apply {
                    moveTo(MARGIN + 410f, 823f)
                    cubicTo(455f, 818f, 520f, 827f, CONTENT_R, 821f)
                },
                paints.stroke,
            )
        }
    }

    fun share(context: Context, report: HealthReport) {
        val file = buildPdf(context, report)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "CareRadar Patient Health Report · ${report.periodLabel}")
            putExtra(android.content.Intent.EXTRA_TEXT, plainText(report))
            clipData = ClipData.newUri(context.contentResolver, "Health report", uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share health report"))
    }

    /** Share one PDF per calendar month in a single ACTION_SEND_MULTIPLE sheet. */
    fun shareMany(context: Context, reports: List<HealthReport>) {
        if (reports.isEmpty()) return
        if (reports.size == 1) {
            share(context, reports.first())
            return
        }
        val uris = reports.map { report ->
            val file = buildPdf(context, report)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.toCollection(ArrayList())
        val clip = ClipData.newUri(context.contentResolver, "Health reports", uris.first())
        uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/pdf"
            putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris)
            putExtra(
                android.content.Intent.EXTRA_SUBJECT,
                "CareRadar Patient Health Reports · ${reports.size} monthly reports",
            )
            putExtra(android.content.Intent.EXTRA_TEXT, batchShareText(reports))
            clipData = clip
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share health reports"))
    }

    private fun batchShareText(reports: List<HealthReport>): String = buildString {
        val newest = reports.first()
        val oldest = reports.last()
        appendLine("CareRadar Patient Health Reports")
        appendLine("${reports.size} monthly PDFs · ${oldest.periodLabel} to ${newest.periodLabel}")
        appendLine("Patient: ${newest.patientName}")
        if (newest.patientLine.isNotBlank()) appendLine(newest.patientLine.replace("  ·  ", " · "))
        appendLine("Generated ${newest.generatedAtLabel}")
        appendLine()
        reports.forEach { report ->
            appendLine("- ${report.periodLabel}: ${report.readingCountLabel} · ${report.overall.label}")
        }
        appendLine()
        appendLine("Each calendar month is a separate PDF. Generated by CareRadar.")
    }

    fun download(context: Context, report: HealthReport): String? = runCatching {
        val source = buildPdf(context, report)
        val name = fileNameFor(report)

        if (Build.VERSION.SDK_INT >= 29) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return@runCatching null
            resolver.openOutputStream(uri)?.use { out -> source.inputStream().use { it.copyTo(out) } }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            "Downloads/$name"
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: return@runCatching null
            dir.mkdirs()
            val target = File(dir, name)
            source.copyTo(target, overwrite = true)
            target.absolutePath
        }
    }.getOrNull()

    /** Save each month as its own PDF. Returns the locations that were saved. */
    fun downloadMany(context: Context, reports: List<HealthReport>): List<String> =
        reports.mapNotNull { download(context, it) }
}
