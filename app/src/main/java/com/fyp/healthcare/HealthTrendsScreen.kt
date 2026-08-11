package com.fyp.healthcare

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BrandBlue = Color(0xFF2A6DE1)
private val CardWhite = Color(0xFFFFFFFF)
private val ScreenBackground = Color(0xFFEFF1F6)
private val TextDark = Color(0xFF1B1D23)
private val LabelGray = Color(0xFF5F6673)
private val GoodGreen = Color(0xFF2E9E6B)
private val BadRed = Color(0xFFD32F2F)

@Composable
fun HealthTrendsScreen(onBackClick: () -> Unit) {
    var selectedPeriod by remember { mutableStateOf("Weekly") }

    // =====================================================================
    // TODO (FUTURE USE CASE - SQL SERVER):
    //  Everything below will be taken from the SQL server.
    //  The server will use math / SQL aggregate functions
    //  (AVG(), MIN(), MAX(), COUNT(), etc.) on ALL users' recorded
    //  readings to calculate the average, min, max and the trend
    //  percentages (+2%, -1%, Stable) shown on this page.
    //  For now everything is null => the page shows blank states.
    // =====================================================================
    val minBpm: String? = null
    val avgBpm: String? = null
    val maxBpm: String? = null
    val chartPoints: List<Float>? = null  // e.g. average BPM per day for the line chart
    val bloodPressure: String? = null
    val bpTrend: String? = null
    val bpProgress: Float? = null         // 0f..1f for the progress bar
    val bloodSugar: String? = null
    val sugarTrend: String? = null
    val sugarProgress: Float? = null
    val oxygen: String? = null
    val oxygenTrend: String? = null
    val oxygenProgress: Float? = null

    Column(modifier = Modifier.fillMaxSize().background(ScreenBackground)) {

        // ===== Blue top bar =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(BrandBlue)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
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

            // ===== Daily / Weekly / Monthly (works, data comes later) =====
            PeriodSelector(selectedPeriod) { selectedPeriod = it }

            // ===== Heart Rate section =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Heart Rate (BPM)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.weight(1f)
                )
                // TODO: later lets the user switch which metric the chart shows
                Text(
                    "⇅ Change",
                    color = BrandBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { /* TODO later */ }
                )
            }

            // Chart card — blank until SQL data arrives
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardWhite)
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEDEFF4)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📈", fontSize = 28.sp) // TODO: replace with real line chart
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "No trend data yet",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = LabelGray
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Averages from the SQL server will be drawn here",
                            fontSize = 11.sp,
                            color = LabelGray
                        )
                    }
                }
            }

            // ===== Min / Avg / Max (SQL: MIN(), AVG(), MAX()) =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardWhite)
                    .padding(16.dp)
            ) {
                StatCell("Min", minBpm, BrandBlue, Modifier.weight(1f))
                StatCell("Avg", avgBpm, GoodGreen, Modifier.weight(1f))
                StatCell("Max", maxBpm, BadRed, Modifier.weight(1f))
            }

            // ===== Other Metrics =====
            Text("Other Metrics", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)

            MetricCard("Blood Pressure", bloodPressure, bpTrend, bpProgress)
            MetricCard("Blood Sugar", bloodSugar, sugarTrend, sugarProgress)
            MetricCard("Oxygen Level", oxygen, oxygenTrend, oxygenProgress)
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
        listOf("Daily", "Weekly", "Monthly").forEach { period ->
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
    progress: Float?
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
        Spacer(Modifier.height(8.dp))
        // Progress bar — empty until SQL data arrives
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFFE5E8EE))
        ) {
            if (progress != null) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(BrandBlue)
                )
            }
        }
    }
}

private fun trendColor(trend: String?): Color = when {
    trend == null -> LabelGray
    trend.contains("+") -> GoodGreen
    trend.contains("-") -> BadRed
    else -> LabelGray
}