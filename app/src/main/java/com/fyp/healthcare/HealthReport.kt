package com.fyp.healthcare

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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
 * HEALTH REPORT
 *
 * A two-day snapshot (yesterday + today) the patient — or their linked caregiver — can
 * generate on demand and hand to a doctor. Built entirely from data already on the device:
 * the recorded vitals history ([HealthDataManager.history]), the health profile and the
 * medication log. Offline, and never invents numbers.
 *
 * Page 1: patient info, overall status, a 6-metric summary and a 2x2 grid of trend charts.
 * Page 2: written health insights. Status wording ("Good / Normal / Low / High / Critical")
 * is the same [VitalStatus] scale the Home dashboard uses.
 */

/** Which vital a card / insight / chart belongs to — drives its icon and accent colour. */
enum class MetricKind(val display: String, @get:DrawableRes val icon: Int, val accent: Int) {
    HEART_RATE("Heart Rate", R.drawable.ic_rpt_heart, 0xFFE5484D.toInt()),
    BLOOD_PRESSURE("Blood Pressure", R.drawable.ic_rpt_gauge, 0xFFEA6A2C.toInt()),
    BLOOD_SUGAR("Blood Sugar", R.drawable.ic_rpt_droplet, 0xFFD6336C.toInt()),
    OXYGEN("Oxygen Level", R.drawable.ic_rpt_air, 0xFF2A6DE1.toInt()),
    TEMPERATURE("Temperature", R.drawable.ic_rpt_thermostat, 0xFF2E9E6B.toInt()),
    WEIGHT("Weight", R.drawable.ic_rpt_weight, 0xFF7048E8.toInt()),
    RELIABILITY("Data Reliability", R.drawable.ic_rpt_clipboard, 0xFF2A6DE1.toInt()),
    ADHERENCE("Medication", R.drawable.ic_rpt_check, 0xFF2E9E6B.toInt());
}

/** One line of the report's summary grid. */
data class ReportMetric(
    val kind: MetricKind,
    /** Formatted value with unit, e.g. "74 BPM". `null` when nothing was recorded. */
    val value: String?,
    val status: ReportStatus,
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

enum class OverallStatus(val label: String) { GOOD("All Good"), ATTENTION("Needs Attention") }

/** One point on a trend chart — a day's average for that metric. */
data class TrendPoint(
    val dateLabel: String,   // "3 Sep"
    val caption: String,     // "Yesterday" / "Today"
    val value: Float,        // plotted value (systolic, for blood pressure)
    val display: String,     // label drawn above the point ("108/92")
)

/** One trend chart: a metric plotted across the last two days. */
data class TrendSeries(
    val kind: MetricKind,
    val unit: String,        // "BPM"
    val axisMax: Float,
    val points: List<TrendPoint>,
)

/** One written insight on page 2. */
data class ReportInsight(val kind: MetricKind, val body: String) {
    val title: String get() = kind.display
}

data class HealthReport(
    val periodLabel: String,       // "September 2026"
    val windowLabel: String,       // "1 Sep – 2 Sep"
    val patientName: String,
    val patientAge: String,        // "66"  ("" when unset)
    val patientSex: String,        // "Male"
    val patientBloodType: String,  // "O+"
    val generatedAt: Long,
    val readingCount: Int,
    val overall: OverallStatus,
    val overallDetail: String,
    val metrics: List<ReportMetric>,
    val trends: List<TrendSeries>,
    val allergies: List<String>,
    val conditions: List<String>,
    val insights: List<ReportInsight>,
) {
    val hasData: Boolean get() = metrics.any { it.value != null }

    val allergiesLabel: String get() = allergies.joinToString(", ").ifBlank { "None recorded" }
    val conditionsLabel: String get() = conditions.joinToString(", ").ifBlank { "None recorded" }

    val patientLine: String
        get() = buildList {
            if (patientAge.isNotBlank()) add("Age $patientAge")
            if (patientSex.isNotBlank()) add(patientSex)
            if (patientBloodType.isNotBlank()) add("Blood type $patientBloodType")
        }.joinToString("  ·  ")

    val generatedAtLabel: String
        get() = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(generatedAt))

    val generatedDateLabel: String
        get() = SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(generatedAt))

    companion object {

        /** Build the report for TODAY + YESTERDAY from local data. Pure — no I/O. */
        fun generate(
            profile: HealthProfile,
            readings: List<HealthDataManager.Reading>,
            medications: List<Medication>,
            patientName: String,
        ): HealthReport {
            val now = Calendar.getInstance()
            val todayStart = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val yesterdayStart = (todayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
            val yesterdayCal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }

            val recent = readings.filter { it.timestamp >= yesterdayStart.timeInMillis }

            fun sameDay(ts: Long, c: Calendar): Boolean {
                val a = Calendar.getInstance().apply { timeInMillis = ts }
                return a.get(Calendar.YEAR) == c.get(Calendar.YEAR) &&
                    a.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR)
            }

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
                ),
                ReportMetric(
                    MetricKind.BLOOD_PRESSURE,
                    if (sys != null && dia != null) "${sys.roundToInt()}/${dia.roundToInt()} mmHg" else null,
                    if (sys != null && dia != null)
                        ReportStatus.fromVital(VitalStatus.bloodPressure(sys.roundToInt(), dia.roundToInt()))
                    else ReportStatus.NONE,
                ),
                ReportMetric(
                    MetricKind.BLOOD_SUGAR,
                    sugar?.let { "${trimReportNumber(it)} mg/dL" },
                    sugar?.let { bloodSugarStatus(it) } ?: ReportStatus.NONE,
                ),
                ReportMetric(
                    MetricKind.OXYGEN,
                    oxygen?.let { "${it.roundToInt()}%" },
                    oxygen?.let { ReportStatus.fromVital(VitalStatus.oxygen(it.roundToInt())) } ?: ReportStatus.NONE,
                ),
                ReportMetric(
                    MetricKind.TEMPERATURE,
                    temperature?.let { "${trimReportNumber(it)} °C" },
                    temperature?.let { temperatureStatus(it) } ?: ReportStatus.NONE,
                ),
                ReportMetric(
                    MetricKind.WEIGHT,
                    profile.weightKg.trim().takeIf { it.isNotBlank() }?.let { "$it kg" },
                    if (profile.weightKg.isBlank()) ReportStatus.NONE else ReportStatus.STABLE,
                ),
            )

            val dateFmt = SimpleDateFormat("d MMM", Locale.getDefault())
            fun series(
                kind: MetricKind, unit: String, axisMax: Float,
                select: (HealthDataManager.Reading) -> Float?,
                display: (List<HealthDataManager.Reading>) -> String,
            ): TrendSeries {
                val days = listOf(yesterdayCal to "Yesterday", now to "Today")
                val points = days.mapNotNull { (cal, caption) ->
                    val dayReadings = recent.filter { sameDay(it.timestamp, cal) }
                    val v = avg(dayReadings, select) ?: return@mapNotNull null
                    TrendPoint(
                        dateLabel = dateFmt.format(cal.time),
                        caption = caption,
                        value = v,
                        display = display(dayReadings),
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
                series(MetricKind.BLOOD_SUGAR, "mg/dL", 150f, selSugar) { d ->
                    avg(d, selSugar)?.let { trimReportNumber(it) } ?: "--"
                },
                series(MetricKind.OXYGEN, "%", 100f, selOxy) { d ->
                    avg(d, selOxy)?.roundToInt()?.toString() ?: "--"
                },
            )

            val concerning = setOf(ReportStatus.LOW, ReportStatus.HIGH, ReportStatus.CRITICAL)
            val flagged = metrics.filter { it.status in concerning }
            val overall = if (flagged.isEmpty()) OverallStatus.GOOD else OverallStatus.ATTENTION
            val overallDetail = when {
                !metrics.any { it.value != null } ->
                    "No vitals were recorded in the last two days."
                flagged.isEmpty() ->
                    "Every reading recorded over the last two days is within its recommended range."
                flagged.size == 1 ->
                    "${flagged.first().kind.display} is outside the recommended range."
                else ->
                    "Some readings are outside the recommended range."
            }

            val windowLabel =
                "${dateFmt.format(yesterdayCal.time)} – ${dateFmt.format(now.time)}"

            return HealthReport(
                periodLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(now.time),
                windowLabel = windowLabel,
                patientName = patientName.ifBlank { "Patient" },
                patientAge = profile.age?.toString().orEmpty(),
                patientSex = profile.sex.trim(),
                patientBloodType = profile.bloodType.trim(),
                generatedAt = System.currentTimeMillis(),
                readingCount = recent.size,
                overall = overall,
                overallDetail = overallDetail,
                metrics = metrics,
                trends = trends,
                allergies = profile.allergies,
                conditions = profile.conditions,
                insights = buildInsights(recent.size, metrics, medications, yesterdayCal, now),
            )
        }

        private fun buildInsights(
            readingCount: Int,
            metrics: List<ReportMetric>,
            medications: List<Medication>,
            yesterday: Calendar,
            today: Calendar,
        ): List<ReportInsight> {
            if (readingCount == 0) {
                return listOf(
                    ReportInsight(
                        MetricKind.RELIABILITY,
                        "No vitals were recorded yesterday or today. Record readings from the home " +
                            "screen so the next report can show a full picture.",
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
                        "A steady couple of days — every vital recorded over the last two days " +
                            "stayed within a healthy range.",
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
                            "logged over the last two days — more frequent recordings make the averages " +
                            "more reliable.",
                    )
                )
            }

            adherenceNote(medications, yesterday, today)?.let {
                insights.add(ReportInsight(MetricKind.ADHERENCE, it))
            }

            return insights
        }

        private fun adherenceNote(medications: List<Medication>, yesterday: Calendar, today: Calendar): String? {
            val days = setOf(dateKey(yesterday), dateKey(today))
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
            return "Medication adherence over the last two days: $taken of $total logged doses taken ($pct%)."
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
                        "Your average resting heart rate of $v is right in the healthy zone — a sign of a well-rested heart."
                    ReportStatus.NORMAL ->
                        "Your average heart rate of $v is within the normal adult range, sitting near its upper end."
                    ReportStatus.LOW ->
                        "Your average heart rate of $v is below 60 BPM. That can be normal if you're very active, but tell your doctor if it comes with dizziness or fatigue."
                    ReportStatus.HIGH ->
                        "Your average heart rate of $v runs above a typical resting rate. Caffeine, stress and broken sleep all push it up — worth a mention if it stays there."
                    ReportStatus.CRITICAL ->
                        "Your average heart rate of $v is well outside the safe resting range. Please have this checked by a doctor soon."
                    else -> null
                }

                MetricKind.BLOOD_PRESSURE -> when (metric.status) {
                    ReportStatus.GOOD ->
                        "Your average blood pressure of $v is in the ideal range — keep doing what you're doing."
                    ReportStatus.NORMAL ->
                        "Your average blood pressure of $v is acceptable but a little above optimal. Less salt and a daily walk help keep it down."
                    ReportStatus.LOW ->
                        "Your average blood pressure of $v is on the low side. Mention it to your doctor if you feel light-headed when standing up."
                    ReportStatus.HIGH ->
                        "Your average blood pressure of $v is above the recommended level. If it keeps reading this high, review it with your doctor."
                    ReportStatus.CRITICAL ->
                        "Your average blood pressure of $v is in a dangerous range — please get medical advice promptly."
                    else -> null
                }

                MetricKind.BLOOD_SUGAR -> when (metric.status) {
                    ReportStatus.GOOD, ReportStatus.NORMAL ->
                        "Your average blood sugar of $v sits comfortably within a normal range."
                    ReportStatus.LOW ->
                        "Your average blood sugar of $v is below normal. Frequent lows matter, especially if you take diabetes medication — bring this up with your doctor."
                    ReportStatus.HIGH ->
                        "Your average blood sugar of $v is above the normal range. If it stays there, ask your doctor about an HbA1c test."
                    ReportStatus.CRITICAL ->
                        "Your average blood sugar of $v is far outside the safe range. Please contact a doctor."
                    else -> null
                }

                MetricKind.OXYGEN -> when (metric.status) {
                    ReportStatus.GOOD, ReportStatus.NORMAL ->
                        "Your average blood oxygen of $v is healthy — 95% or above is exactly where you want it."
                    ReportStatus.LOW ->
                        "Your average blood oxygen of $v is a little under the usual 95–100%. A single low reading is often just poor sensor contact, but a sustained dip is worth mentioning."
                    ReportStatus.CRITICAL ->
                        "Your average blood oxygen of $v is low enough to need medical attention — please follow up with a doctor."
                    else -> null
                }

                MetricKind.TEMPERATURE -> when (metric.status) {
                    ReportStatus.GOOD, ReportStatus.NORMAL ->
                        "Your average body temperature of $v is normal, with no sign of a fever."
                    ReportStatus.LOW ->
                        "Your average body temperature of $v reads below normal. Check how you're taking the measurement, and mention it if readings stay low."
                    ReportStatus.HIGH ->
                        "Your average body temperature of $v is slightly raised — that points to a mild fever if it continues."
                    ReportStatus.CRITICAL ->
                        "Your average body temperature of $v suggests a significant fever — or, if the readings are low, hypothermia. Seek medical advice."
                    else -> null
                }

                MetricKind.WEIGHT -> when (metric.status) {
                    ReportStatus.STABLE ->
                        "Weight on file is $v, taken from your profile rather than a fresh reading. Keep it current in Edit Profile so future reports can show the trend."
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

// =====================================================================
//  Export — a plain-text body for the share sheet and a two-page PDF.
// =====================================================================

object HealthReportExport {

    private fun fileNameFor(report: HealthReport): String =
        "CareApp-Health-Report-${report.periodLabel.replace(" ", "-")}.pdf"

    /** A readable text version, used as the share-sheet body / email text. */
    fun plainText(report: HealthReport): String = buildString {
        appendLine("CareApp Health Report")
        appendLine("${report.periodLabel}  (${report.windowLabel})")
        appendLine("Patient: ${report.patientName}")
        if (report.patientLine.isNotBlank()) appendLine(report.patientLine.replace("  ·  ", " · "))
        appendLine("Generated ${report.generatedAtLabel} · ${report.readingCount} readings over the last two days")
        appendLine()
        appendLine("OVERALL: ${report.overall.label} — ${report.overallDetail}")
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
        appendLine("Generated by CareApp. This summary is not a substitute for professional medical advice.")
    }

    // ---- shared drawing helpers ----------------------------------------------------------

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 40f

    private val INK = 0xFF1B1D23.toInt()
    private val BODY = 0xFF3B4250.toInt()
    private val MUTED = 0xFF737B88.toInt()
    private val HAIRLINE = 0xFFE4E8EF.toInt()
    private val BRAND = 0xFF2A6DE1.toInt()
    private val PANEL = 0xFFF1F5FB.toInt()
    private val WHITE = 0xFFFFFFFF.toInt()

    private fun statusColor(s: ReportStatus): Int = when (s) {
        ReportStatus.GOOD -> 0xFF2E9E6B.toInt()
        ReportStatus.NORMAL -> BRAND
        ReportStatus.LOW -> 0xFFF08C00.toInt()
        ReportStatus.HIGH -> 0xFFEA6A2C.toInt()
        ReportStatus.CRITICAL -> 0xFFD32F2F.toInt()
        ReportStatus.STABLE -> 0xFF64748B.toInt()
        ReportStatus.NONE -> MUTED
    }

    private fun sans(weight: String): Typeface =
        Typeface.create(weight, Typeface.NORMAL)

    private class Paints {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        val text = Paint(Paint.ANTI_ALIAS_FLAG)

        fun t(size: Float, color: Int, tf: Typeface, align: Paint.Align = Paint.Align.LEFT): Paint =
            Paint(text).apply {
                textSize = size; this.color = color; typeface = tf; textAlign = align
            }
    }

    /** Render a vector drawable to a tinted square bitmap for the PDF canvas. */
    private fun icon(context: Context, @DrawableRes id: Int, sizePx: Int, tint: Int): Bitmap {
        val d = ResourcesCompat.getDrawable(context.resources, id, null)!!.mutate()
        DrawableCompat.setTint(d, tint)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, sizePx, sizePx)
        d.draw(Canvas(bmp))
        return bmp
    }

    private fun withAlpha(color: Int, alpha: Int): Int = (alpha shl 24) or (color and 0x00FFFFFF)

    private fun Canvas.roundRect(l: Float, t: Float, r: Float, b: Float, rad: Float, p: Paint) {
        drawRoundRect(RectF(l, t, r, b), rad, rad, p)
    }

    /** A soft-tinted circle with a centred icon. */
    private fun Canvas.iconChip(context: Context, cx: Float, cy: Float, chip: Float, iconId: Int, accent: Int, paints: Paints) {
        paints.fill.color = withAlpha(accent, 34)
        drawCircle(cx, cy, chip / 2f, paints.fill)
        val ip = (chip * 0.58f).toInt().coerceAtLeast(1)
        val bmp = icon(context, iconId, ip, accent)
        drawBitmap(bmp, cx - ip / 2f, cy - ip / 2f, null)
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

    private fun drawFooter(canvas: Canvas, context: Context, report: HealthReport, paints: Paints, page: Int, total: Int) {
        val y = PAGE_H - 54f
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, paints.stroke.apply { color = HAIRLINE; strokeWidth = 1f })
        val shield = icon(context, R.drawable.ic_rpt_shield, 16, MUTED)
        canvas.drawBitmap(shield, MARGIN, y + 12f, null)
        canvas.drawText(
            "Generated by CareApp. Not a substitute for professional medical advice.",
            MARGIN + 22f, y + 24f, paints.t(8.5f, MUTED, sans("sans-serif")),
        )
        canvas.drawText(
            "CareApp  ·  Personal Health Monitoring System",
            MARGIN + 22f, y + 38f, paints.t(8.5f, MUTED, sans("sans-serif-medium")),
        )
        canvas.drawText(
            "Generated ${report.generatedDateLabel}",
            PAGE_W - MARGIN, y + 24f, paints.t(8.5f, MUTED, sans("sans-serif"), Paint.Align.RIGHT),
        )
        canvas.drawText(
            "Page $page of $total",
            PAGE_W - MARGIN, y + 38f, paints.t(8.5f, MUTED, sans("sans-serif"), Paint.Align.RIGHT),
        )
    }

    private fun drawSectionTitle(canvas: Canvas, context: Context, x: Float, y: Float, iconId: Int, title: String, paints: Paints) {
        canvas.iconChip(context, x + 11f, y - 4f, 24f, iconId, BRAND, paints)
        canvas.drawText(title.uppercase(Locale.US), x + 30f, y, paints.t(11.5f, BRAND, sans("sans-serif-medium")).apply { letterSpacing = 0.06f })
    }

    // ---- PDF -----------------------------------------------------------------------------

    /** Render the report to a two-page A4 PDF in cacheDir/reports and return the file. */
    fun buildPdf(context: Context, report: HealthReport): File {
        val doc = PdfDocument()
        val paints = Paints()

        drawPageOne(doc, context, report, paints)
        drawPageTwo(doc, context, report, paints)

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, fileNameFor(report))
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun drawPageOne(doc: PdfDocument, context: Context, report: HealthReport, paints: Paints) {
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        val c = page.canvas
        val right = PAGE_W - MARGIN

        // ---- header ----
        // empty icon placeholder (app icon still being designed)
        paints.stroke.color = HAIRLINE; paints.stroke.strokeWidth = 1.5f
        c.roundRect(MARGIN, 36f, MARGIN + 46f, 82f, 12f, paints.stroke)

        c.drawText("HEALTH REPORT", MARGIN + 62f, 60f, paints.t(23f, INK, sans("sans-serif-black")))
        c.drawText("Personal Health Report", MARGIN + 62f, 77f, paints.t(10.5f, MUTED, sans("sans-serif")))

        val calBmp = icon(context, R.drawable.ic_rpt_calendar, 15, BRAND)
        val periodW = paints.t(13f, BRAND, sans("sans-serif-medium")).measureText(report.periodLabel)
        c.drawBitmap(calBmp, right - periodW - 21f, 47f, null)
        c.drawText(report.periodLabel, right, 60f, paints.t(13f, BRAND, sans("sans-serif-medium"), Paint.Align.RIGHT))

        c.drawLine(MARGIN, 96f, right, 96f, paints.stroke.apply { color = HAIRLINE; strokeWidth = 1f })

        // ---- patient info + overall status ----
        val cardsTop = 112f
        val cardsBottom = 258f
        val leftW = 300f
        val gap = 14f
        val rightX = MARGIN + leftW + gap

        // left panel
        paints.fill.color = PANEL
        c.roundRect(MARGIN, cardsTop, MARGIN + leftW, cardsBottom, 14f, paints.fill)
        c.iconChip(context, MARGIN + 34f, cardsTop + 34f, 30f, R.drawable.ic_rpt_person, BRAND, paints)
        c.drawText("PATIENT INFORMATION", MARGIN + 56f, cardsTop + 22f, paints.t(8.5f, BRAND, sans("sans-serif-medium")).apply { letterSpacing = 0.06f })
        c.drawText(report.patientName, MARGIN + 56f, cardsTop + 44f, paints.t(18f, INK, sans("sans-serif-black")))

        val infoLeft = MARGIN + 20f
        val infoRight = MARGIN + leftW - 12f
        var infoX = infoLeft
        var infoY = cardsTop + 70f
        val lp = paints.t(9.5f, MUTED, sans("sans-serif"))
        val vp = paints.t(9.5f, INK, sans("sans-serif-medium"))
        fun chip(iconId: Int, label: String, value: String) {
            val lw = lp.measureText("$label ")
            val w = 16f + lw + vp.measureText(value) + 15f
            if (infoX > infoLeft && infoX + w > infoRight) {   // wrap to a second line
                infoX = infoLeft
                infoY += 18f
            }
            c.drawBitmap(icon(context, iconId, 12, MUTED), infoX, infoY - 10f, null)
            c.drawText("$label ", infoX + 16f, infoY, lp)
            c.drawText(value, infoX + 16f + lw, infoY, vp)
            infoX += w
        }
        if (report.patientAge.isNotBlank()) chip(R.drawable.ic_rpt_calendar, "Age:", report.patientAge)
        if (report.patientSex.isNotBlank()) chip(R.drawable.ic_rpt_person, "Gender:", report.patientSex)
        chip(R.drawable.ic_rpt_droplet, "Blood Type:", report.patientBloodType.ifBlank { "—" })

        val genDate = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(report.generatedAt))
        c.drawText(
            "Generated $genDate   ·   ${report.readingCount} readings, last 2 days",
            infoLeft, maxOf(infoY + 22f, cardsTop + 98f), paints.t(8.5f, MUTED, sans("sans-serif")),
        )

        // right panel — overall status
        val attention = report.overall == OverallStatus.ATTENTION
        val accent = if (attention) 0xFFF08C00.toInt() else 0xFF2E9E6B.toInt()
        paints.fill.color = withAlpha(accent, 24)
        c.roundRect(rightX, cardsTop, right, cardsBottom, 14f, paints.fill)
        val statusIcon = if (attention) R.drawable.ic_rpt_warning else R.drawable.ic_rpt_check
        c.drawText("OVERALL HEALTH STATUS", rightX + 18f, cardsTop + 22f, paints.t(8.5f, withAlpha(accent, 235), sans("sans-serif-medium")).apply { letterSpacing = 0.05f })
        val sBmp = icon(context, statusIcon, 26, accent)
        c.drawBitmap(sBmp, rightX + 18f, cardsTop + 34f, null)
        c.drawText(report.overall.label, rightX + 18f, cardsTop + 82f, paints.t(15f, accent, sans("sans-serif-black")))
        wrap(report.overallDetail, paints.t(9f, BODY, sans("sans-serif")), right - rightX - 34f)
            .take(4)
            .forEachIndexed { i, line ->
                c.drawText(line, rightX + 18f, cardsTop + 100f + i * 12f, paints.t(9f, BODY, sans("sans-serif")))
            }

        // ---- health summary ----
        drawSectionTitle(c, context, MARGIN, 288f, R.drawable.ic_rpt_bars, "Health Summary", paints)
        val gridTop = 302f
        val colGap = 12f
        val cardW = (right - MARGIN - 2 * colGap) / 3f
        val cardH = 86f
        val rowGap = 12f
        report.metrics.forEachIndexed { i, m ->
            val col = i % 3
            val row = i / 3
            val x = MARGIN + col * (cardW + colGap)
            val y = gridTop + row * (cardH + rowGap)
            paints.fill.color = WHITE
            c.roundRect(x, y, x + cardW, y + cardH, 12f, paints.fill)
            paints.stroke.color = HAIRLINE; paints.stroke.strokeWidth = 1f
            c.roundRect(x, y, x + cardW, y + cardH, 12f, paints.stroke)

            c.iconChip(context, x + 26f, y + cardH / 2f, 34f, m.kind.icon, m.kind.accent, paints)
            val tx = x + 50f
            c.drawText(m.kind.display, tx, y + 26f, paints.t(9.5f, MUTED, sans("sans-serif")))

            if (m.value != null) {
                val parts = m.value.split(" ", limit = 2)
                val numP = paints.t(16f, INK, sans("sans-serif-black"))
                c.drawText(parts[0], tx, y + 47f, numP)
                if (parts.size > 1) {
                    c.drawText(
                        " ${parts[1]}", tx + numP.measureText(parts[0]), y + 47f,
                        paints.t(9f, MUTED, sans("sans-serif")),
                    )
                }
            } else {
                c.drawText("--", tx, y + 47f, paints.t(16f, MUTED, sans("sans-serif-black")))
            }

            val sc = statusColor(m.status)
            val pill = m.status.label.uppercase(Locale.US)
            val pillP = paints.t(8f, sc, sans("sans-serif-medium")).apply { letterSpacing = 0.04f }
            val pw = pillP.measureText(pill)
            paints.fill.color = withAlpha(sc, 28)
            c.roundRect(tx, y + 56f, tx + pw + 16f, y + 72f, 8f, paints.fill)
            c.drawText(pill, tx + 8f, y + 67f, pillP)
        }

        // ---- health trends (2x2) ----
        val trendsTitleY = gridTop + 2 * cardH + rowGap + 34f
        drawSectionTitle(c, context, MARGIN, trendsTitleY, R.drawable.ic_rpt_chart, "Health Trends — ${report.periodLabel}", paints)

        val tTop = trendsTitleY + 14f
        val tGap = 12f
        val tW = (right - MARGIN - tGap) / 2f
        val tH = 118f
        report.trends.forEachIndexed { i, s ->
            val col = i % 2
            val row = i / 2
            val x = MARGIN + col * (tW + tGap)
            val y = tTop + row * (tH + tGap)
            drawMiniChart(c, x, y, tW, tH, s, paints)
        }

        drawFooter(c, context, report, paints, 1, 2)
        doc.finishPage(page)
    }

    private fun drawMiniChart(c: Canvas, x: Float, y: Float, w: Float, h: Float, s: TrendSeries, paints: Paints) {
        paints.fill.color = WHITE
        c.roundRect(x, y, x + w, y + h, 12f, paints.fill)
        paints.stroke.color = HAIRLINE; paints.stroke.strokeWidth = 1f
        c.roundRect(x, y, x + w, y + h, 12f, paints.stroke)

        val accent = s.kind.accent
        val titleP = paints.t(10.5f, accent, sans("sans-serif-medium"))
        c.drawText(s.kind.display, x + 14f, y + 20f, titleP)
        c.drawText(
            " (${s.unit})", x + 14f + titleP.measureText(s.kind.display), y + 20f,
            paints.t(8.5f, MUTED, sans("sans-serif")),
        )

        val plotL = x + 34f
        val plotR = x + w - 14f
        val plotT = y + 30f
        val plotB = y + h - 28f

        // gridlines + y labels
        val gp = paints.t(7f, MUTED, sans("sans-serif"), Paint.Align.RIGHT)
        paints.stroke.color = HAIRLINE; paints.stroke.strokeWidth = 0.8f
        for (k in 0..3) {
            val frac = k / 3f
            val gy = plotB - frac * (plotB - plotT)
            c.drawLine(plotL, gy, plotR, gy, paints.stroke)
            c.drawText((s.axisMax * frac).roundToInt().toString(), plotL - 4f, gy + 2.5f, gp)
        }

        if (s.points.isEmpty()) {
            c.drawText(
                "No readings", (plotL + plotR) / 2f, (plotT + plotB) / 2f,
                paints.t(9f, MUTED, sans("sans-serif"), Paint.Align.CENTER),
            )
            return
        }

        fun px(i: Int): Float =
            if (s.points.size == 1) (plotL + plotR) / 2f
            else plotL + (plotR - plotL) * (0.22f + 0.56f * i / (s.points.size - 1))
        fun py(v: Float): Float = plotB - (v / s.axisMax).coerceIn(0f, 1f) * (plotB - plotT)

        if (s.points.size >= 2) {
            paints.stroke.color = accent; paints.stroke.strokeWidth = 2f
            val path = Path()
            s.points.forEachIndexed { i, p ->
                val ppx = px(i); val ppy = py(p.value)
                if (i == 0) path.moveTo(ppx, ppy) else path.lineTo(ppx, ppy)
            }
            c.drawPath(path, paints.stroke)
        }

        s.points.forEachIndexed { i, p ->
            val ppx = px(i); val ppy = py(p.value)
            paints.fill.color = accent
            c.drawCircle(ppx, ppy, 3f, paints.fill)
            // label above the point, unless that would clash with the title — then below
            val labelY = if (ppy - 12f < plotT) ppy + 15f else ppy - 8f
            c.drawText(p.display, ppx, labelY, paints.t(9f, INK, sans("sans-serif-medium"), Paint.Align.CENTER))
            c.drawText(p.dateLabel, ppx, plotB + 12f, paints.t(7.5f, MUTED, sans("sans-serif"), Paint.Align.CENTER))
            c.drawText(p.caption, ppx, plotB + 21f, paints.t(6.5f, MUTED, sans("sans-serif"), Paint.Align.CENTER))
        }
    }

    private fun drawPageTwo(doc: PdfDocument, context: Context, report: HealthReport, paints: Paints) {
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 2).create())
        val c = page.canvas
        val right = PAGE_W - MARGIN

        // slim header
        c.drawText("HEALTH REPORT", MARGIN, 54f, paints.t(13f, INK, sans("sans-serif-black")))
        c.drawText(
            "  ·  ${report.patientName}  ·  ${report.windowLabel}", MARGIN + paints.t(13f, INK, sans("sans-serif-black")).measureText("HEALTH REPORT"),
            54f, paints.t(10f, MUTED, sans("sans-serif")),
        )
        c.drawLine(MARGIN, 66f, right, 66f, paints.stroke.apply { color = HAIRLINE; strokeWidth = 1f })

        // ---- medical background ----
        drawSectionTitle(c, context, MARGIN, 92f, R.drawable.ic_rpt_clipboard, "Medical Background", paints)
        var by = 106f
        val bgBodyP = paints.t(10f, INK, sans("sans-serif-medium"))
        listOf("Allergies" to report.allergiesLabel, "Conditions" to report.conditionsLabel).forEach { (k, v) ->
            paints.fill.color = WHITE
            c.roundRect(MARGIN, by, right, by + 34f, 10f, paints.fill)
            paints.stroke.color = HAIRLINE; paints.stroke.strokeWidth = 1f
            c.roundRect(MARGIN, by, right, by + 34f, 10f, paints.stroke)
            c.drawText(k, MARGIN + 16f, by + 21f, paints.t(9.5f, MUTED, sans("sans-serif")))
            wrap(v, bgBodyP, right - MARGIN - 120f).take(1).forEach { line ->
                c.drawText(line, MARGIN + 96f, by + 21f, bgBodyP)
            }
            by += 34f + 8f
        }

        drawSectionTitle(c, context, MARGIN, by + 18f, R.drawable.ic_rpt_bulb, "Health Insights", paints)

        var y = by + 32f
        val boxL = MARGIN
        val boxR = right
        val pad = 16f
        val textX = boxL + 58f
        val textW = boxR - textX - pad

        // outer container
        val containerTop = y
        // measure total height first
        val bodyP = paints.t(9.5f, BODY, sans("sans-serif"))
        val rows = report.insights.map { it to wrap(it.body, bodyP, textW) }
        var totalH = 0f
        rows.forEach { (_, lines) -> totalH += 24f + lines.size * 13f + 12f }
        paints.fill.color = WHITE
        c.roundRect(boxL, containerTop, boxR, containerTop + totalH, 14f, paints.fill)
        paints.stroke.color = HAIRLINE; paints.stroke.strokeWidth = 1f
        c.roundRect(boxL, containerTop, boxR, containerTop + totalH, 14f, paints.stroke)

        y = containerTop
        rows.forEachIndexed { idx, (insight, lines) ->
            val rowTop = y
            val rowH = 24f + lines.size * 13f + 12f
            if (idx > 0) c.drawLine(boxL + pad, rowTop, boxR - pad, rowTop, paints.stroke.apply { color = HAIRLINE; strokeWidth = 1f })
            c.iconChip(context, boxL + pad + 15f, rowTop + rowH / 2f, 32f, insight.kind.icon, insight.kind.accent, paints)
            c.drawText(insight.title, textX, rowTop + 22f, paints.t(10.5f, insight.kind.accent, sans("sans-serif-medium")))
            lines.forEachIndexed { i, line ->
                c.drawText(line, textX, rowTop + 37f + i * 13f, bodyP)
            }
            y += rowH
        }

        drawFooter(c, context, report, paints, 2, 2)
        doc.finishPage(page)
    }

    // ---- share / download --------------------------------------------------------------

    fun share(context: Context, report: HealthReport) {
        val file = buildPdf(context, report)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "CareApp Health Report — ${report.periodLabel}")
            putExtra(android.content.Intent.EXTRA_TEXT, plainText(report))
            clipData = ClipData.newUri(context.contentResolver, "Health report", uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share health report"))
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
}
