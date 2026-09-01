package com.fyp.healthcare

import com.fyp.healthcare.ui.theme.appBackground
import com.fyp.healthcare.ui.theme.glossyTopBar
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
import androidx.compose.foundation.layout.fillMaxHeight
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
    onBackClick: () -> Unit,
) {
    var selectedPeriod by remember { mutableStateOf("Weekly") }
    val dayIds = remember(selectedPeriod) { periodDayIds(selectedPeriod) }

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

            PeriodSelector(selectedPeriod) { selectedPeriod = it }

            Text(
                "Averages across all users of the app.",
                fontSize = 12.sp,
                color = LabelGray,
            )

            // ===== Heart Rate =====
            val hrSlots = slotsOf("hr")
            val hrStat = community["hr"]
            Text("Heart Rate (BPM)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glossySurface(RoundedCornerShape(20.dp), CardWhite)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        if (selectedPeriod == "Weekly") "This week · all users" else "This month · all users",
                        fontSize = 12.sp,
                        color = LabelGray,
                        modifier = Modifier.weight(1f),
                    )
                    hrSlots.lastOrNull { it != null }?.let {
                        Text(
                            "Latest ${it.roundToInt()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                MetricChart(points = hrSlots, labels = periodLabels(selectedPeriod))
            }

            // ===== Global Min / Avg / Max =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glossySurface(RoundedCornerShape(20.dp), CardWhite)
                    .padding(16.dp)
            ) {
                Text(
                    "All users" +
                        (hrStat?.let { "  ·  ${formatCount(it.count)} readings" } ?: ""),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = LabelGray,
                )
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth()) {
                    StatCell("Min", hrStat?.min?.roundToInt()?.toString(), BrandBlue, Modifier.weight(1f))
                    StatCell("Avg", hrStat?.avg?.roundToInt()?.toString(), GoodGreen, Modifier.weight(1f))
                    StatCell("Max", hrStat?.max?.roundToInt()?.toString(), BadRed, Modifier.weight(1f))
                }
            }

            // ===== Other Metrics =====
            Text("Other Metrics", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
            MetricCard(
                "Blood Pressure (systolic)",
                community["sys"]?.avg?.let { "${it.roundToInt()}" },
                trendOf(slotsOf("sys")),
                progressOf(community["sys"]?.avg, 90.0, 160.0),
            )
            MetricCard(
                "Blood Sugar",
                community["sugar"]?.avg?.let { trimNumber(it.toFloat()) },
                trendOf(slotsOf("sugar")),
                progressOf(community["sugar"]?.avg, 70.0, 180.0),
            )
            MetricCard(
                "Oxygen Level",
                community["oxy"]?.avg?.let { "${it.roundToInt()}%" },
                trendOf(slotsOf("oxy")),
                progressOf(community["oxy"]?.avg, 90.0, 100.0),
            )

            if (community.isEmpty() && daily.isEmpty()) {
                Text(
                    "No community readings yet. Record your vitals to start the averages.",
                    fontSize = 12.sp,
                    color = LabelGray,
                )
            }
        }
    }
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

private fun progressOf(value: Double?, low: Double, high: Double): Float? {
    value ?: return null
    return (((value - low) / (high - low)).coerceIn(0.0, 1.0)).toFloat()
}

/** Trend from the day-slot series: last present point vs the one before it. */
private fun trendOf(slots: List<Float?>): String? {
    val present = slots.filterNotNull()
    if (present.size < 2) return null
    val delta = present.last() - present[present.size - 2]
    return when {
        delta > 0.5f -> "+${trimNumber(delta)}"
        delta < -0.5f -> "-${trimNumber(-delta)}"
        else -> "Stable"
    }
}

private fun trimNumber(v: Float): String =
    if (v == v.toLong().toFloat()) v.toLong().toString()
    else String.format("%.1f", v)

/** 1240 -> "1,240" */
private fun formatCount(n: Long): String = "%,d".format(n)

// ===== Chart =====

@Composable
private fun MetricChart(points: List<Float?>, labels: List<String>) {
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Timeline,
                    contentDescription = null,
                    tint = LabelGray,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text("No data yet for this period", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray)
                Spacer(Modifier.height(2.dp))
                Text("Averages appear as users record readings", fontSize = 11.sp, color = LabelGray)
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

@Composable
private fun MetricCard(
    label: String,
    value: String?,
    trend: String?,
    progress: Float?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glossySurface(RoundedCornerShape(20.dp), CardWhite)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark,
                modifier = Modifier.weight(1f)
            )
            Text(value ?: "--", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
        }
        Spacer(Modifier.height(2.dp))
        Text("All users average", fontSize = 11.sp, color = LabelGray)
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            Text(
                trend ?: "--",
                fontSize = 12.sp,
                color = trendColor(trend),
                fontWeight = FontWeight.Medium
            )
        }
        if (progress != null) {
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(TrackGray)
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .background(BrandBlue)
                )
            }
        }
    }
}

@Composable
private fun trendColor(trend: String?): Color = when {
    trend == null -> LabelGray
    trend.contains("+") -> GoodGreen
    trend.contains("-") -> BadRed
    else -> LabelGray
}
