package com.fyp.healthcare

import com.fyp.healthcare.ui.theme.appBackground
import com.fyp.healthcare.ui.theme.GlossyButton
import com.fyp.healthcare.ui.theme.glossyBadge
import com.fyp.healthcare.ui.theme.glossySurface
import com.fyp.healthcare.ui.theme.themed
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BrandBlue = Color(0xFF2A6DE1)
private val BrandBlueLight: Color @Composable get() = themed(Color(0xFFCFE0FA), Color(0xFF2A3A57))
private val CardWhite: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val ScreenBackground: Color @Composable get() = themed(Color(0xFFEFF1F6), Color(0xFF121316))
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
private val SuccessGreen = Color(0xFF22B573)

@Composable
fun DataRecordedScreen(
    healthData: HealthDataManager,
    onBackToHome: () -> Unit
) {
    val analysis = remember { healthData.analyzeLatest() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .appBackground()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        // Big green check
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(SuccessGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(94.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Data Recorded!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Your readings were saved and checked against the normal ranges.",
            fontSize = 13.sp,
            color = LabelGray,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        // ===== Analysis =====
        if (analysis != null && analysis.metrics.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glossySurface(RoundedCornerShape(20.dp), CardWhite)
                    .padding(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Analysis",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        modifier = Modifier.weight(1f),
                    )
                    LevelChip(analysis.overall)
                }
                Spacer(Modifier.height(6.dp))
                Text(analysis.summary, fontSize = 12.sp, color = LabelGray, lineHeight = 16.sp)

                Spacer(Modifier.height(14.dp))
                analysis.metrics.forEachIndexed { i, m ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x22808080)))
                    MetricRow(m)
                }

                if (!analysis.hasEnoughHistory) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Trends need a few more readings — keep recording regularly.",
                        fontSize = 11.sp,
                        color = LabelGray,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        GlossyButton(
            onClick = onBackToHome,
            modifier = Modifier.fillMaxWidth(),
            color = BrandBlueLight,
            contentColor = BrandBlue,
        ) {
            Text("Back to Dashboard", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LevelChip(level: VitalLevel) {
    val c = level.color()
    Box(
        modifier = Modifier
            .glossyBadge(c, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(level.label(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c)
    }
}

@Composable
private fun MetricRow(m: MetricAnalysis) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(m.label, fontSize = 13.sp, color = TextDark, modifier = Modifier.weight(1f))
            Text("${m.display} ${m.unit}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(Modifier.size(8.dp))
            Text(m.statusText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = m.level.color())
        }
        Spacer(Modifier.height(2.dp))
        Row {
            Text(m.note, fontSize = 11.sp, color = LabelGray, modifier = Modifier.weight(1f))
            if (m.trend != VitalTrend.INSUFFICIENT) {
                Text(
                    "${m.trend.arrow()} ${m.trend.word()}" + (m.recentAvg?.let { " · $it" } ?: ""),
                    fontSize = 11.sp,
                    color = LabelGray,
                )
            }
        }
    }
}
