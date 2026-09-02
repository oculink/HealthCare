package com.fyp.healthcare

import com.fyp.healthcare.ui.theme.appBackground
import com.fyp.healthcare.ui.theme.glossyTopBar
import com.fyp.healthcare.ui.theme.glossyBadge
import com.fyp.healthcare.ui.theme.glossySurface
import com.fyp.healthcare.ui.theme.themed
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private val BrandBlue = Color(0xFF2A6DE1)
private val CardWhite: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val ScreenBackground: Color @Composable get() = themed(Color(0xFFEFF1F6), Color(0xFF121316))
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
private val FieldBackground: Color @Composable get() = themed(Color(0xFFEDEFF4), Color(0xFF262730))
private val TrackGray: Color @Composable get() = themed(Color(0xFFE5E8EE), Color(0xFF2E3038))
private val GoodGreen = Color(0xFF2E9E6B)
private val BadRed = Color(0xFFD32F2F)

private val WEEK_LABELS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
private const val DAY_MS = 24L * 60 * 60 * 1000

/**
 * Health Trends is a 100% GLOBAL view: every number and the chart come from the anonymous
 * community aggregate (`stats/vitals` all-time + `stats/vitals/daily/{date}` per day), so the
 * screen looks identical on every account. It is NOT a per-user screen.
 *
 * Chart point for a day = that day's average across all users (e.g. one user logs 73, another
 * 71 -> the point sits at 72). Both flows are live snapshot listeners, so the numbers move in
 * real time as anyone records a reading.
 */
@Composable
fun HealthTrendsScreen(
    healthData: HealthDataManager,
    onBackClick: () -> Unit,
) {
    // UC-04 (<<include>>): analysis of this account's own latest reading.
    val analysis = remember(Session.dataVersion) { healthData.analyzeLatest() }
    var selectedPeriod by remember { mutableStateOf("Weekly") }
    // UC-05: which metric the single chart shows, and whether it plots the global
    // (all-users) average or this account's own recorded readings.
    var selectedMetric by remember { mutableStateOf(Metric.HEART_RATE) }
    var chartSource by remember { mutableStateOf(ChartSource.GLOBAL) }
    val dayIds = remember(selectedPeriod) { periodDayIds(selectedPeriod) }
    val localReadings = remember(Session.dataVersion) { healthData.history() }

    val community by remember(Session.dataVersion) { Cloud.communityStatsFlow() }
        .collectAsState(initial = emptyMap())

    val daily by remember(selectedPeriod, Session.dataVersion) {
        Cloud.communityDailyFlow(dayIds.first(), dayIds.last())
    }.collectAsState(initial = emptyList())

    val byDay = remember(daily) { daily.associateBy { it.dayId } }
    fun slotsOf(metric: String): List<Float?> =
        dayIds.map { id -> byDay[id]?.avg?.get(metric)?.toFloat() }

    Column(modifier = Modifier.fillMaxSize().appBackground()) {

        // ===== Blue top bar =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glossyTopBar(BrandBlue)
                .statusBarsPadding()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "Health Trends",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(48.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ===== Your latest analysis (per-user) =====
            LatestAnalysisCard(analysis)

            PeriodSelector(selectedPeriod) { selectedPeriod = it }

            // ===== The switchable metric chart =====
            val metric = selectedMetric
            val window = remember(selectedPeriod) { periodWindow(selectedPeriod) }
            val (startMs, slotCount) = window

            val slots: List<Float?> = when (chartSource) {
                ChartSource.GLOBAL -> slotsOf(metric.communityKey)
                ChartSource.LOCAL -> localSlots(localReadings, startMs, slotCount, metric.select)
            }
            val stat: Stat? = when (chartSource) {
                ChartSource.GLOBAL -> community[metric.communityKey]?.let {
                    Stat(it.min.toFloat(), it.avg.toFloat(), it.max.toFloat(), it.count.toInt())
                }
                ChartSource.LOCAL -> localStat(localReadings, startMs, slotCount, metric.select)
            }
            val isGlobal = chartSource == ChartSource.GLOBAL
            val sourceWord = if (isGlobal) "all users" else "you"
            val periodWord = if (selectedPeriod == "Weekly") "This week" else "This month"

            // metric title + "Change type"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${metric.title} (${metric.unit})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedMetric = metric.next() }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.SwapVert, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Change type", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glossySurface(RoundedCornerShape(20.dp), CardWhite)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$periodWord · ${if (isGlobal) "all users" else "your readings"}",
                        fontSize = 12.sp,
                        color = LabelGray,
                        modifier = Modifier.weight(1f),
                    )
                    slots.lastOrNull { it != null }?.let {
                        Text(
                            "Latest ${it.roundToInt()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                MetricChart(
                    points = slots,
                    labels = periodLabels(selectedPeriod),
                    emptyPrimary = if (isGlobal) "No community data yet for this period"
                        else "No ${metric.title.lowercase()} recorded for this period",
                    emptySecondary = if (isGlobal) "Averages appear as users record readings"
                        else "Record your vitals to build your own trend line",
                )
            }

            // ===== Global / Local toggle (under the chart) =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { chartSource = chartSource.toggle() }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (isGlobal) "Showing the global average — tap for your own readings"
                    else "Showing your own readings — tap for the global average",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = BrandBlue,
                )
            }

            // ===== Min / Avg / Max for the selected metric + source =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glossySurface(RoundedCornerShape(20.dp), CardWhite)
                    .padding(16.dp)
            ) {
                Text(
                    (if (isGlobal) "All users" else "Your readings") +
                        (stat?.let { "  ·  ${formatCount(it.count.toLong())} ${if (it.count == 1) "reading" else "readings"}" } ?: ""),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = LabelGray,
                )
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth()) {
                    StatCell("Min", stat?.min?.roundToInt()?.toString(), BrandBlue, Modifier.weight(1f))
                    StatCell("Avg", stat?.avg?.roundToInt()?.toString(), GoodGreen, Modifier.weight(1f))
                    StatCell("Max", stat?.max?.roundToInt()?.toString(), BadRed, Modifier.weight(1f))
                }
            }

            // A1: no records for the selected period / source
            if (slots.all { it == null } && stat == null) {
                Text(
                    if (isGlobal)
                        "No community readings for ${metric.title.lowercase()} in this period yet."
                    else
                        "No ${metric.title.lowercase()} readings recorded for this period.",
                    fontSize = 12.sp,
                    color = LabelGray,
                )
            }
        }
    }
}

/** Which vital the single Health Trends chart is showing. "Change type" cycles these. */
private enum class Metric(
    val title: String,
    val unit: String,
    /** key in the community aggregate (`stats/vitals` + daily docs). */
    val communityKey: String,
    /** pull this metric's value out of one recorded reading. */
    val select: (HealthDataManager.Reading) -> Float?,
) {
    HEART_RATE("Heart Rate", "BPM", "hr", { it.heartRateBpm?.toFloat() }),
    BLOOD_PRESSURE("Blood Pressure", "systolic mmHg", "sys", { it.systolic?.toFloat() }),
    BLOOD_SUGAR("Blood Sugar", "mg/dL", "sugar", { it.bloodSugarValue }),
    OXYGEN("Oxygen Level", "%", "oxy", { it.oxygenValue });

    fun next(): Metric = entries[(ordinal + 1) % entries.size]
}

private enum class ChartSource {
    GLOBAL, LOCAL;
    fun toggle() = if (this == GLOBAL) LOCAL else GLOBAL
}

private data class Stat(val min: Float, val avg: Float, val max: Float, val count: Int)

/** Bucket this account's readings into the period's day slots, one mean per day. */
private fun localSlots(
    readings: List<HealthDataManager.Reading>,
    startMs: Long,
    slots: Int,
    select: (HealthDataManager.Reading) -> Float?,
): List<Float?> {
    val buckets = Array(slots) { ArrayList<Float>() }
    readings.forEach { r ->
        val idx = ((r.timestamp - startMs) / DAY_MS).toInt()
        if (idx in 0 until slots) select(r)?.let { buckets[idx].add(it) }
    }
    return buckets.map { b -> if (b.isEmpty()) null else b.average().toFloat() }
}

/** Min / Avg / Max of this account's readings for one metric across the period. */
private fun localStat(
    readings: List<HealthDataManager.Reading>,
    startMs: Long,
    slots: Int,
    select: (HealthDataManager.Reading) -> Float?,
): Stat? {
    val end = startMs + slots.toLong() * DAY_MS
    val values = readings.filter { it.timestamp in startMs until end }.mapNotNull(select)
    if (values.isEmpty()) return null
    return Stat(values.min(), values.average().toFloat(), values.max(), values.size)
}

// =====================================================================
// Period helpers — Weekly = current week (Sun..Sat, 7 slots),
// Monthly = current month (day 1..N).
// =====================================================================

/** Midnight of the first day of the period, then the number of day-slots. */
private fun periodWindow(period: String): Pair<Long, Int> {
    val c = Calendar.getInstance()
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return if (period == "Weekly") {
        val dow = c.get(Calendar.DAY_OF_WEEK) // 1 = Sunday .. 7 = Saturday
        c.add(Calendar.DAY_OF_MONTH, -(dow - 1))
        c.timeInMillis to 7
    } else {
        val slots = c.getActualMaximum(Calendar.DAY_OF_MONTH)
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.timeInMillis to slots
    }
}

private fun periodDayIds(period: String): List<String> {
    val (start, slotCount) = periodWindow(period)
    return List(slotCount) { i -> Cloud.dayIdFor(start + i * DAY_MS) }
}

private fun periodLabels(period: String): List<String> {
    if (period == "Weekly") return WEEK_LABELS
    val (_, slotCount) = periodWindow(period)
    return List(slotCount) { i ->
        val day = i + 1
        if (day == 1 || day % 5 == 0) day.toString() else ""
    }
}

/** 1240 -> "1,240" */
private fun formatCount(n: Long): String = "%,d".format(n)

// ===== Chart =====

@Composable
private fun MetricChart(
    points: List<Float?>,
    labels: List<String>,
    emptyPrimary: String = "No data yet for this period",
    emptySecondary: String = "Averages appear as users record readings",
) {
    val present = points.filterNotNull()
    if (present.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(FieldBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 20.dp),
            ) {
                Icon(
                    Icons.Filled.Timeline,
                    contentDescription = null,
                    tint = LabelGray,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(emptyPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray, textAlign = TextAlign.Center)
                Spacer(Modifier.height(2.dp))
                Text(emptySecondary, fontSize = 11.sp, color = LabelGray, textAlign = TextAlign.Center)
            }
        }
        return
    }

    val dMin = present.min()
    val dMax = present.max()
    val pad = maxOf(4f, (dMax - dMin) * 0.2f)
    val lo = floor(dMin - pad)
    val hi = ceil(dMax + pad).let { if (it <= lo) lo + 1f else it }

    val gridColor = TrackGray
    val lineColor = BrandBlue
    val n = points.size

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.height(180.dp).width(30.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            Text("${hi.roundToInt()}", fontSize = 10.sp, color = LabelGray)
            Text("${((hi + lo) / 2f).roundToInt()}", fontSize = 10.sp, color = LabelGray)
            Text("${lo.roundToInt()}", fontSize = 10.sp, color = LabelGray)
        }
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                val w = size.width
                val h = size.height
                listOf(0f, 0.5f, 1f).forEach { f ->
                    drawLine(gridColor, Offset(0f, h * f), Offset(w, h * f), strokeWidth = 1.dp.toPx())
                }
                fun px(i: Int) = (i + 0.5f) / n * w
                fun py(v: Float) = h - ((v - lo) / (hi - lo)) * h

                var prev: Offset? = null
                points.forEachIndexed { i, v ->
                    if (v == null) {
                        prev = null
                        return@forEachIndexed
                    }
                    val cur = Offset(px(i), py(v))
                    val p = prev
                    if (p != null) {
                        drawLine(lineColor, p, cur, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                    }
                    prev = cur
                }
                points.forEachIndexed { i, v ->
                    if (v != null) drawCircle(lineColor, 3.5.dp.toPx(), Offset(px(i), py(v)))
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                labels.forEach { label ->
                    Text(
                        label,
                        fontSize = 10.sp,
                        color = LabelGray,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ===== Small building blocks =====

@Composable
private fun PeriodSelector(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glossySurface(RoundedCornerShape(14.dp), CardWhite)
            .padding(4.dp)
    ) {
        listOf("Weekly", "Monthly").forEach { period ->
            val isSelected = period == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) BrandBlue else Color.Transparent)
                    .clickable { onSelect(period) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    period,
                    color = if (isSelected) Color.White else LabelGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun StatCell(title: String, value: String?, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(title, fontSize = 12.sp, color = LabelGray)
        Spacer(Modifier.height(4.dp))
        Text(value ?: "--", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ===== UC-04: this account's latest-reading analysis =====

@Composable
private fun LatestAnalysisCard(analysis: ReadingAnalysis?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glossySurface(RoundedCornerShape(20.dp), CardWhite)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Your latest analysis",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.weight(1f),
            )
            if (analysis != null && analysis.metrics.isNotEmpty()) {
                val c = analysis.overall.color()
                Box(
                    modifier = Modifier
                        .glossyBadge(c, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(analysis.overall.label(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c)
                }
            }
        }

        if (analysis == null || analysis.metrics.isEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Record your vitals to see how your readings compare with the normal ranges.",
                fontSize = 12.sp,
                color = LabelGray,
            )
            return@Column
        }

        Spacer(Modifier.height(6.dp))
        Text(analysis.summary, fontSize = 12.sp, color = LabelGray, lineHeight = 16.sp)
        Spacer(Modifier.height(10.dp))

        analysis.metrics.forEach { m ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(m.level.color()))
                Spacer(Modifier.width(8.dp))
                Text(m.label, fontSize = 12.sp, color = TextDark, modifier = Modifier.weight(1f))
                Text("${m.display} ${m.unit}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(Modifier.width(8.dp))
                Text(m.statusText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = m.level.color())
                if (m.trend != VitalTrend.INSUFFICIENT) {
                    Spacer(Modifier.width(6.dp))
                    Text(m.trend.arrow(), fontSize = 11.sp, color = LabelGray)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Recorded ${agoText(analysis.timestamp)}" +
                if (!analysis.hasEnoughHistory) " · trends need more readings" else "",
            fontSize = 11.sp,
            color = LabelGray,
        )
    }
}

private fun agoText(ts: Long): String {
    if (ts <= 0L) return "recently"
    val mins = (System.currentTimeMillis() - ts) / 60_000L
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "$mins min ago"
        mins < 60 * 24 -> "${mins / 60} h ago"
        else -> "${mins / (60 * 24)} d ago"
    }
}
