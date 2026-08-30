package com.fyp.healthcare

import com.fyp.healthcare.ui.theme.themed
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SensorOccupied
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BrandBlue = Color(0xFF2A6DE1)
private val CardWhite: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val ScreenBackground: Color @Composable get() = themed(Color(0xFFEFF1F6), Color(0xFF121316))
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
private val TrackGray: Color @Composable get() = themed(Color(0xFFE5E8EE), Color(0xFF2E3038))
private val GoodGreen = Color(0xFF2E9E6B)
private val AlertRed = Color(0xFFD32F2F)

/**
 * Activity Monitoring. Every metric here is meant to be driven by the mmWave
 * radar — see [ActivityDataManager]. Until it's wired up they all read `null`
 * and the screen shows "waiting for radar" placeholders.
 */
@Composable
fun ActivityScreen(
    activity: ActivityDataManager,
    onBackClick: () -> Unit,
) {
    @Suppress("UNUSED_VARIABLE")
    val dataVersion = Session.dataVersion // recompose when the cache is refreshed
    val steps = activity.steps()
    val goal = activity.stepGoal()
    val falls = activity.fallCount()
    val movements = activity.movementCount()
    val idleMin = activity.idleMinutes()
    val hourly = activity.hourlyActivity()
    val sleep = activity.sleep()
    val connected = activity.isRadarConnected()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
    ) {
        // ===== Blue top bar =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrandBlue)
                .statusBarsPadding()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "Activity Monitoring",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(48.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text("Daily Activity Summary", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(Modifier.height(2.dp))
                Text(
                    SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date()),
                    fontSize = 12.sp,
                    color = LabelGray,
                )
            }

            RadarStatusCard(
                connected = connected,
                presence = activity.presenceDetected(),
                lastSync = activity.lastSyncLabel(),
            )

            // ===== Step gauge =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardWhite)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RadarActivityGauge(steps = steps, goal = goal)
                Spacer(Modifier.height(12.dp))
                Text(
                    if (steps != null)
                        "${percent(steps, goal)}% of daily goal (${format(goal)})"
                    else
                        "Waiting for radar step data",
                    fontSize = 12.sp,
                    color = LabelGray,
                )
            }

            // ===== Calories / Distance / Active time =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardWhite)
                    .padding(vertical = 16.dp),
            ) {
                StatCell(Icons.Filled.Warning, AlertRed, falls?.let(::format) ?: "--", "Falls", Modifier.weight(1f))
                CellDivider()
                StatCell(Icons.Filled.Vibration, BrandBlue, movements?.let(::format) ?: "--", "Movements", Modifier.weight(1f))
                CellDivider()
                StatCell(Icons.Filled.Bed, GoodGreen, idleMin?.let(::formatDuration) ?: "--", "Idle Time", Modifier.weight(1f))
            }

            // ===== Hourly activity =====
            Text("Hourly Activity", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardWhite)
                    .padding(16.dp),
            ) {
                HourlyActivityChart(hourly)
            }

            // ===== Sleep =====
            Text("Sleep Last Night", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
            SleepCard(sleep)

            Spacer(Modifier.height(4.dp))
        }
    }
}

// =====================================================================
// Radar status
// =====================================================================

@Composable
private fun RadarStatusCard(connected: Boolean, presence: Boolean?, lastSync: String?) {
    val statusColor = if (connected) GoodGreen else LabelGray
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(BrandBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Radar, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("mmWave Radar Sensor", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(
                    if (connected) "Streaming activity data" else "Not connected yet",
                    fontSize = 12.sp,
                    color = LabelGray,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (connected) "Online" else "Offline",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(TrackGray))
        Spacer(Modifier.height(12.dp))
        InfoRow(Icons.Filled.SensorOccupied, "Presence", presenceLabel(presence))
        Spacer(Modifier.height(8.dp))
        InfoRow(Icons.Filled.Radar, "Last sync", lastSync ?: "Never")
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = LabelGray, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 12.sp, color = LabelGray, modifier = Modifier.weight(1f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextDark)
    }
}

private fun presenceLabel(presence: Boolean?): String = when (presence) {
    true -> "Detected"
    false -> "Clear"
    null -> "--"
}

// =====================================================================
// Step gauge — the "graphical" centrepiece.
// A circular progress ring over a radar-style backdrop. With no data it
// runs an idle sweep, like a radar scanning for a signal.
// =====================================================================

@Composable
private fun RadarActivityGauge(steps: Int?, goal: Int, modifier: Modifier = Modifier) {
    val hasData = steps != null
    val progress = if (hasData) (steps!!.toFloat() / goal).coerceIn(0f, 1f) else 0f

    val transition = rememberInfiniteTransition(label = "radar")
    val sweepAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )

    val trackColor = TrackGray   // hoist out of the DrawScope lambda (not composable)

    Box(modifier = modifier.size(196.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val radius = (size.minDimension - stroke) / 2f
            val c = center

            // concentric radar rings
            for (i in 1..3) {
                drawCircle(
                    color = BrandBlue.copy(alpha = 0.07f),
                    radius = radius * i / 3f,
                    center = c,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            // track
            drawCircle(
                color = trackColor,
                radius = radius,
                center = c,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            if (hasData) {
                drawArc(
                    color = BrandBlue,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    topLeft = Offset(c.x - radius, c.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            } else {
                rotate(degrees = sweepAngle, pivot = c) {
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(BrandBlue.copy(alpha = 0f), BrandBlue.copy(alpha = 0.55f)),
                            startX = c.x,
                            endX = c.x + radius,
                        ),
                        start = c,
                        end = Offset(c.x + radius, c.y),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                steps?.let(::format) ?: "--",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
            )
            Text("steps", fontSize = 12.sp, color = LabelGray)
        }
    }
}

// =====================================================================
// Hourly bar chart
// =====================================================================

@Composable
private fun HourlyActivityChart(data: List<Int>?) {
    if (data == null || data.all { it == 0 }) {
        EmptyBlock(
            Icons.Filled.BarChart,
            "No activity data yet",
            "Hourly movement picked up by the radar will be charted here",
        )
        return
    }

    val hours = 6..21
    val max = (hours.maxOf { data.getOrElse(it) { 0 } }).coerceAtLeast(1)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (h in hours) {
                val frac = (data.getOrElse(h) { 0 }.toFloat() / max).coerceIn(0.02f, 1f)
                val peak = data.getOrElse(h) { 0 } == max
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(frac)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (peak) BrandBlue else BrandBlue.copy(alpha = 0.18f)),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (h in hours) {
                Text(
                    if (h % 3 == 0) hourLabel(h) else "",
                    fontSize = 9.sp,
                    color = LabelGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun hourLabel(h: Int): String = when {
    h == 0 -> "12a"
    h < 12 -> "${h}a"
    h == 12 -> "12p"
    else -> "${h - 12}p"
}

// =====================================================================
// Sleep
// =====================================================================

@Composable
private fun SleepCard(sleep: SleepSummary?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF7C4DFF).copy(alpha = if (sleep == null) 0.10f else 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Bedtime,
                contentDescription = null,
                tint = Color(0xFF7C4DFF).copy(alpha = if (sleep == null) 0.5f else 1f),
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        if (sleep == null) {
            Column {
                Text("--", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(
                    "Sleep tracking needs the bedside radar",
                    fontSize = 12.sp,
                    color = LabelGray,
                )
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Text(formatDuration(sleep.totalMinutes), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${formatTime12(sleep.bedTime)} – ${formatTime12(sleep.wakeTime)}",
                    fontSize = 12.sp,
                    color = LabelGray,
                )
            }
            SleepQualityPill(sleep.quality)
        }
    }
}

@Composable
private fun SleepQualityPill(quality: String) {
    val color = when (quality) {
        "Good" -> GoodGreen
        "Fair" -> Color(0xFFFF9800)
        else -> AlertRed
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(quality, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

// =====================================================================
// Small shared pieces
// =====================================================================

@Composable
private fun StatCell(
    icon: ImageVector,
    tint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Text(label, fontSize = 10.sp, color = LabelGray, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CellDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(48.dp)
            .background(TrackGray),
    )
}

@Composable
private fun EmptyBlock(icon: ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = LabelGray, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(6.dp))
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, fontSize = 11.sp, color = LabelGray, textAlign = TextAlign.Center)
    }
}

// ---- formatting ----

private fun format(n: Int): String = String.format(Locale.getDefault(), "%,d", n)
private fun percent(value: Int, of: Int): Int =
    if (of <= 0) 0 else ((value.toFloat() / of) * 100).toInt().coerceIn(0, 999)
