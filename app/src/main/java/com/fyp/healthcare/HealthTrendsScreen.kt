package com.fyp.healthcare

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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

@Composable
fun HealthTrendsScreen(
    healthData: HealthDataManager,
    onBackClick: () -> Unit,
) {
    var selectedPeriod by remember { mutableStateOf("Weekly") }

    // Trends are computed from the user's OWN recorded readings in Firestore
    // (users/{uid}/readings). Cache-first so the screen paints instantly and works offline;
    // then a background server fetch refreshes it. Local history is the last-resort fallback.
    val historyState = produceState<List<HealthDataManager.Reading>?>(initialValue = null, healthData) {
        value = runCatching { healthData.cloudHistory(fromServer = false) }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?: healthData.history()
        runCatching { healthData.cloudHistory(fromServer = true) }
            .getOrNull()
            ?.let { value = it }
    }
    val history = historyState.value

    // Anonymous "all users" baseline (aggregate-only doc stats/vitals).
    val communityState = produceState<Map<String, Cloud.CommunityStat>>(emptyMap()) {
        value = runCatching { Cloud.communityStats() }.getOrDefault(emptyMap())
    }
    val community = communityState.value

    Column(modifier = Modifier.fillMaxSize().background(ScreenBackground)) {

        // ===== Blue top bar =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrandBlue)
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

        if (history == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandBlue)
            }
            return@Column
        }

        val hr = heartRateTrend(history, selectedPeriod)
        val bp = otherMetricTrend(history, selectedPeriod, 90f, 160f) { it.systolic?.toFloat() }
        val sugar = otherMetricTrend(history, selectedPeriod, 70f, 180f) { it.bloodSugarValue }
        val oxygen = otherMetricTrend(history, selectedPeriod, 90f, 100f) { it.oxygenValue }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ===== Weekly / Monthly =====
            PeriodSelector(selectedPeriod) { selectedPeriod = it }

            // ===== Heart Rate section =====
            Text(
                "Heart Rate (BPM)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
            )

            // Chart card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardWhite)
                    .padding(16.dp)
            ) {
                Text(
                    if (selectedPeriod == "Weekly") "This week" else "This month",
                    fontSize = 12.sp,
                    color = LabelGray,
                )
                Spacer(Modifier.height(10.dp))
                HeartRateChart(
                    points = hr.slots,
                    labels = hr.labels,
                )
            }

            // ===== Min / Avg / Max — you vs. all users =====
            val hrCommunity = community["hr"]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardWhite)
                    .padding(16.dp)
            ) {
                if (hrCommunity != null) {
                    Text("You", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LabelGray)
                    Spacer(Modifier.height(6.dp))
                }
                Row(Modifier.fillMaxWidth()) {
                    StatCell("Min", hr.min?.let { "$it" }, BrandBlue, Modifier.weight(1f))
                    StatCell("Avg", hr.avg?.let { "$it" }, GoodGreen, Modifier.weight(1f))
                    StatCell("Max", hr.max?.let { "$it" }, BadRed, Modifier.weight(1f))
                }
                if (hrCommunity != null) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = TrackGray)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "All users  ·  ${formatCount(hrCommunity.count)} readings",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = LabelGray,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth()) {
                        StatCell("Min", "${hrCommunity.min.roundToInt()}", LabelGray, Modifier.weight(1f))
                        StatCell("Avg", "${hrCommunity.avg.roundToInt()}", LabelGray, Modifier.weight(1f))
                        StatCell("Max", "${hrCommunity.max.roundToInt()}", LabelGray, Modifier.weight(1f))
                    }
                }
            }
            Text(
                when (hr.count) {
                    0 -> "No heart-rate readings recorded for this period yet."
                    1 -> "1 reading this period. Record daily to see the trend."
                    else -> "${hr.count} readings this period."
                },
                fontSize = 12.sp,
                color = LabelGray,
            )

            // ===== Other Metrics =====
            Text("Other Metrics", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)

            MetricCard(
                "Blood Pressure (systolic)", bp.latest?.let { "${it.roundToInt()}" }, bp.trendLabel, bp.progress,
                community["sys"]?.let { "All users avg: ${it.avg.roundToInt()}" },
            )
            MetricCard(
                "Blood Sugar", sugar.latest?.let { trimNumber(it) }, sugar.trendLabel, sugar.progress,
                community["sugar"]?.let { "All users avg: ${it.avg.roundToInt()}" },
            )
            MetricCard(
                "Oxygen Level", oxygen.latest?.let { "${it.roundToInt()}%" }, oxygen.trendLabel, oxygen.progress,
                community["oxy"]?.let { "All users avg: ${it.avg.roundToInt()}%" },
            )
        }
    }
}

// =====================================================================
// Trend math — all done locally from the recorded reading history.
// Weekly  = current week, slots = days Sun..Sat (7).
// Monthly = current month, slots = day-of-month 1..N (N = 28/29/30/31).
// =====================================================================

private data class HeartRateTrend(
    val slots: List<Float?>,   // averaged BPM per day-slot, null = no reading that day
    val labels: List<String>,  // x-axis labels ("" = draw nothing)
    val min: Int?,
    val avg: Int?,
    val max: Int?,
    val count: Int,            // number of individual readings in the window
)

private data class MetricTrend(
    val latest: Float?,
    val trendLabel: String?,   // "+3", "-2", "Stable", or null
    val progress: Float?,      // 0f..1f for the bar, or null
)

/** Midnight today, then the first slot's start + the number of day-slots for the period. */
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

private const val DAY_MS = 24L * 60 * 60 * 1000

private fun heartRateTrend(history: List<HealthDataManager.Reading>, period: String): HeartRateTrend {
    val (start, slotCount) = periodWindow(period)
    val end = start + slotCount * DAY_MS

    val sums = FloatArray(slotCount)
    val counts = IntArray(slotCount)
    val all = ArrayList<Int>()

    for (r in history) {
        val bpm = r.heartRateBpm ?: continue
        if (bpm !in 20..250) continue
        if (r.timestamp < start || r.timestamp >= end) continue
        val slot = ((r.timestamp - start) / DAY_MS).toInt().coerceIn(0, slotCount - 1)
        sums[slot] += bpm
        counts[slot]++
        all.add(bpm)
    }

    val slots = List(slotCount) { i -> if (counts[i] == 0) null else sums[i] / counts[i] }
    val labels =
        if (period == "Weekly") WEEK_LABELS
        else List(slotCount) { i ->
            val day = i + 1
            // ~6 evenly spaced ticks; skip the last-day rule so it never collides with day 30
            if (day == 1 || day % 5 == 0) day.toString() else ""
        }

    return HeartRateTrend(
        slots = slots,
        labels = labels,
        min = all.minOrNull(),
        max = all.maxOrNull(),
        avg = if (all.isEmpty()) null else all.average().roundToInt(),
        count = all.size,
    )
}

private fun otherMetricTrend(
    history: List<HealthDataManager.Reading>,
    period: String,
    rangeLow: Float,
    rangeHigh: Float,
    select: (HealthDataManager.Reading) -> Float?,
): MetricTrend {
    val (start, slotCount) = periodWindow(period)
    val end = start + slotCount * DAY_MS
    val values = history
        .filter { it.timestamp in start until end }
        .mapNotNull(select)
    if (values.isEmpty()) return MetricTrend(null, null, null)

    val latest = values.last()
    val progress = ((latest - rangeLow) / (rangeHigh - rangeLow)).coerceIn(0f, 1f)
    val trend: String? = if (values.size < 2) "Stable" else {
        val delta = latest - values[values.size - 2]
        when {
            delta > 0.5f -> "+${trimNumber(delta)}"
            delta < -0.5f -> "-${trimNumber(-delta)}"
            else -> "Stable"
        }
    }
    return MetricTrend(latest, trend, progress)
}

private fun trimNumber(v: Float): String =
    if (v == v.toLong().toFloat()) v.toLong().toString()
    else String.format("%.1f", v)

/** 1240 -> "1,240" */
private fun formatCount(n: Long): String =
    "%,d".format(n)

// ===== Chart =====

@Composable
private fun HeartRateChart(points: List<Float?>, labels: List<String>) {
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
                Text("No heart-rate data yet", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray)
                Spacer(Modifier.height(2.dp))
                Text("Record your heart rate to see the graph", fontSize = 11.sp, color = LabelGray)
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
        // y-axis labels
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
            .clip(RoundedCornerShape(14.dp))
            .background(CardWhite)
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
    communityLine: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
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
        if (communityLine != null) {
            Spacer(Modifier.height(2.dp))
            Text(communityLine, fontSize = 11.sp, color = LabelGray)
        }
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
