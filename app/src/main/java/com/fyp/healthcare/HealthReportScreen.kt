package com.fyp.healthcare

import com.fyp.healthcare.ui.theme.appBackground
import com.fyp.healthcare.ui.theme.glossyTopBar
import com.fyp.healthcare.ui.theme.glossySurface
import com.fyp.healthcare.ui.theme.glossyBadge
import com.fyp.healthcare.ui.theme.GlossyButton
import com.fyp.healthcare.ui.theme.themed
import android.widget.Toast
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fyp.healthcare.ui.theme.AppIconBadge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BrandBlue = Color(0xFF2A6DE1)
private val ReportIndigo = Color(0xFF6C5CE7)
private val CardWhite: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
private val DividerGray: Color @Composable get() = themed(Color(0xFFE5E8EE), Color(0xFF2A2C33))
private val ShareBlueBg: Color @Composable get() = themed(Color(0xFFE7F0FE), Color(0xFF20304B))
private val SaveGreenBg: Color @Composable get() = themed(Color(0xFFE0F5EC), Color(0xFF1E3A30))
private val GoodGreen = Color(0xFF2E9E6B)

/**
 * HEALTH REPORT — reached from the Home "Health Report" quick action.
 *
 * "Generate" builds a two-day [HealthReport] (yesterday + today) from the local vitals history,
 * the health profile and the medication log, previews it, and offers a two-page PDF to share
 * with a doctor or save to the device. Everything is offline and derived from real recorded data.
 */
@Composable
fun HealthReportScreen(
    userManager: UserManager,
    profileManager: ProfileManager,
    healthData: HealthDataManager,
    medManager: MedicationManager,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataVersion = Session.dataVersion

    val profile = remember(dataVersion) { profileManager.get() }
    val readings = remember(dataVersion) { healthData.history() }
    val meds = remember(dataVersion) { medManager.getAll() }
    val patientName = remember(dataVersion) {
        if (Session.isCaretakerMode) Session.controlledPatientName.ifBlank { "Patient" }
        else profile.name.ifBlank { userManager.currentAccount()?.name ?: "Patient" }
    }
    val monthLabel = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()) }

    var report by remember { mutableStateOf<HealthReport?>(null) }
    var generating by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().appBackground()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glossyTopBar(BrandBlue)
                .statusBarsPadding()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "Health Report",
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glossySurface(RoundedCornerShape(20.dp), CardWhite)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIconBadge(Icons.Filled.Description, ReportIndigo, size = 56.dp, iconSize = 28.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Health Report", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        Spacer(Modifier.height(2.dp))
                        Text("$monthLabel  ·  last 2 days", fontSize = 13.sp, color = LabelGray)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            report?.let { "Generated · ${it.generatedAtLabel}" } ?: "Built from your recorded data",
                            fontSize = 11.sp,
                            color = if (report != null) GoodGreen else LabelGray,
                            fontWeight = if (report != null) FontWeight.Medium else FontWeight.Normal,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                GlossyButton(
                    onClick = {
                        if (!generating) {
                            generating = true
                            scope.launch {
                                delay(1100)
                                report = HealthReport.generate(profile, readings, meds, patientName)
                                generating = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = BrandBlue,
                    enabled = !generating,
                ) {
                    if (generating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Generating…", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (report == null) "Generate Report" else "Regenerate",
                            fontSize = 15.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            when {
                generating -> GeneratingCard()

                report != null -> {
                    val r = report!!

                    Text("Report Preview", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    PreviewCard(r)

                    if (r.insights.isNotEmpty()) {
                        Text("Health Insights", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glossySurface(RoundedCornerShape(20.dp), CardWhite)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            r.insights.forEach { insight ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        Icons.Filled.Lightbulb,
                                        contentDescription = null,
                                        tint = ReportIndigo,
                                        modifier = Modifier.size(16.dp).padding(top = 2.dp),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            insight.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextDark,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(insight.body, fontSize = 13.sp, color = LabelGray, lineHeight = 18.sp)
                                    }
                                }
                            }
                        }
                    }

                    Text("Share Options", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    ShareRow(
                        icon = Icons.Filled.Share,
                        tint = BrandBlue,
                        title = "Share with Doctor",
                        subtitle = "Send the PDF via email or apps",
                        pillBg = ShareBlueBg,
                        pillColor = BrandBlue,
                    ) {
                        runCatching { HealthReportExport.share(context, r) }
                            .onFailure { toast(context, "Couldn't prepare the report to share") }
                    }
                    ShareRow(
                        icon = Icons.Filled.Download,
                        tint = GoodGreen,
                        title = "Download PDF",
                        subtitle = "Save a copy to your device",
                        pillBg = SaveGreenBg,
                        pillColor = GoodGreen,
                    ) {
                        val where = HealthReportExport.download(context, r)
                        toast(context, if (where != null) "Saved to $where" else "Couldn't save the report")
                    }

                    Spacer(Modifier.height(4.dp))
                }

                else -> Text(
                    "Tap Generate to build a two-day summary (yesterday and today) from your " +
                        "recorded vitals, profile and medication log.",
                    fontSize = 13.sp,
                    color = LabelGray,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun GeneratingCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glossySurface(RoundedCornerShape(20.dp), CardWhite)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = BrandBlue, strokeWidth = 2.dp)
        Spacer(Modifier.width(14.dp))
        Text("Analysing your recorded data…", fontSize = 14.sp, color = LabelGray, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PreviewCard(report: HealthReport) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glossySurface(RoundedCornerShape(20.dp), CardWhite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .glossyTopBar(BrandBlue)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                "CareApp Health Report  ·  ${report.periodLabel}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                buildString {
                    append(report.patientName)
                    if (report.patientLine.isNotBlank()) append("  ·  ${report.patientLine}")
                },
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.85f),
            )
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("Medical Background", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(Modifier.height(6.dp))
            BackgroundRow("Allergies", report.allergiesLabel)
            Box(Modifier.fillMaxWidth().height(1.dp).background(DividerGray))
            BackgroundRow("Conditions", report.conditionsLabel)
            Spacer(Modifier.height(16.dp))

            Text("Summary", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(Modifier.height(6.dp))
            report.metrics.forEachIndexed { index, metric ->
                if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(DividerGray))
                MetricRow(metric)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (report.hasData) "— End of Preview —" else "— No readings in the last 2 days —",
                fontSize = 11.sp,
                color = LabelGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BackgroundRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, fontSize = 13.sp, color = LabelGray, modifier = Modifier.width(92.dp))
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextDark,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricRow(metric: ReportMetric) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(metric.kind.display, fontSize = 13.sp, color = LabelGray, modifier = Modifier.weight(1f))
        Text(
            metric.value ?: "--",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark,
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .glossyBadge(statusColor(metric.status), RoundedCornerShape(50.dp))
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text(
                metric.status.label,
                fontSize = 10.sp,
                color = statusColor(metric.status),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ShareRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    pillBg: Color,
    pillColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glossySurface(RoundedCornerShape(18.dp), CardWhite)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconBadge(icon, tint, size = 42.dp, iconSize = 21.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = LabelGray)
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(pillBg)
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Go", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = pillColor)
            Spacer(Modifier.width(4.dp))
            Text("→", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = pillColor)
        }
    }
}

@Composable
private fun statusColor(status: ReportStatus): Color = when (status) {
    ReportStatus.GOOD -> GoodGreen
    ReportStatus.NORMAL -> BrandBlue
    ReportStatus.LOW -> Color(0xFFFF9800)
    ReportStatus.HIGH -> Color(0xFFE64A19)
    ReportStatus.CRITICAL -> Color(0xFFD32F2F)
    ReportStatus.STABLE -> LabelGray
    ReportStatus.NONE -> LabelGray
}

private fun toast(context: android.content.Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
