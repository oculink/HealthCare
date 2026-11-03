package com.fyp.healthcare

import com.fyp.healthcare.i18n.Text
import com.fyp.healthcare.ui.theme.appBackground
import com.fyp.healthcare.ui.theme.glossyTopBar
import com.fyp.healthcare.ui.theme.glossySurface
import com.fyp.healthcare.ui.theme.glossyBadge
import com.fyp.healthcare.i18n.Language
import com.fyp.healthcare.ui.theme.GlossyButton
import com.fyp.healthcare.ui.theme.themed
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fyp.healthcare.ui.theme.AppIconBadge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
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
private val RangeTrack: Color @Composable get() = themed(Color(0xFFF0F2F7), Color(0xFF262830))
private val GoodGreen = Color(0xFF2E9E6B)

/** "Nov 2026", "Oct 2026", ... newest first. */
private fun monthLabels(count: Int): List<String> {
    val fmt = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    return (0 until count).map { back ->
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -back)
        }
        fmt.format(cal.time)
    }
}

/** "Nov 2026" for one month, "Jun 2026 to Nov 2026" for a run of months. */
private fun monthRangeLabel(months: Int, labels: List<String>): String =
    if (months <= 1) labels.first() else "${labels[months - 1]} to ${labels.first()}"

private fun monthlyCountLabel(months: Int): String =
    if (months == 1) "1 monthly report" else "$months monthly reports"

/**
 * Health Report, reached from the Home "Health Report" quick action.
 *
 * "Generate" builds one [HealthReport] for each of the chosen 1-6 calendar months from the
 * local vitals history, health profile and medication log. The current month plus the
 * previous months are previewed separately, and each month can be shared or saved as its
 * own PDF. Everything is offline and derived from real recorded data.
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
    val patientUid = Session.controlledPatientUid ?: userManager.currentAccount()?.uid
    val patientId = remember(dataVersion, patientName, patientUid) {
        HealthReport.patientIdFor(patientUid, patientName)
    }
    val monthLabel = remember(Language.current) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    }
    val monthChips = remember(Language.current) { monthLabels(HealthReport.MAX_MONTHS) }

    var rangeMonths by remember { mutableStateOf(HealthReport.MIN_MONTHS) }
    var reports by remember { mutableStateOf<List<HealthReport>>(emptyList()) }
    var selectedMonth by remember { mutableStateOf(0) }
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
                        Text(
                            "$monthLabel  ·  ${monthlyCountLabel(rangeMonths)}",
                            fontSize = 13.sp,
                            color = LabelGray,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            reports.firstOrNull()?.let {
                                "Generated ${it.generatedAtLabel}  ·  ${reports.size} PDF${if (reports.size == 1) "" else "s"} ready"
                            } ?: "One PDF for each calendar month",
                            fontSize = 11.sp,
                            color = if (reports.isNotEmpty()) GoodGreen else LabelGray,
                            fontWeight = if (reports.isNotEmpty()) FontWeight.Medium else FontWeight.Normal,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Report months",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LabelGray,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for (months in HealthReport.MIN_MONTHS..HealthReport.MAX_MONTHS) {
                        val selected = months == rangeMonths
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) BrandBlue else RangeTrack)
                                .selectable(
                                    selected = selected,
                                    enabled = !generating,
                                    role = Role.RadioButton,
                                ) {
                                    if (months != rangeMonths) {
                                        rangeMonths = months
                                        // The old preview covers different months now.
                                        reports = emptyList()
                                        selectedMonth = 0
                                    }
                                }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$months mo",
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else TextDark,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    "${monthlyCountLabel(rangeMonths)} · ${monthRangeLabel(rangeMonths, monthChips)}",
                    fontSize = 11.sp,
                    color = LabelGray,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Each PDF covers a full calendar month. The current month runs to today.",
                    fontSize = 11.sp,
                    color = LabelGray,
                )

                Spacer(Modifier.height(16.dp))

                GlossyButton(
                    onClick = {
                        if (!generating) {
                            val chosenMonths = rangeMonths
                            generating = true
                            scope.launch {
                                delay(1100)
                                reports = HealthReport.generateMonths(
                                    profile, readings, meds, patientName, chosenMonths,
                                    patientId = patientId,
                                )
                                selectedMonth = 0
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
                            if (reports.isEmpty()) {
                                if (rangeMonths == 1) "Generate Report" else "Generate $rangeMonths Reports"
                            } else "Regenerate",
                            fontSize = 15.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            val generated = reports
            when {
                generating -> GeneratingCard(rangeMonths)

                generated.isNotEmpty() -> {
                    val selectedIndex = selectedMonth.coerceIn(0, generated.lastIndex)
                    val r = generated[selectedIndex]

                    Text("Generated Reports", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    GeneratedReportsCard(
                        reports = generated,
                        selectedIndex = selectedIndex,
                        onSelect = { selectedMonth = it },
                    )

                    Text("${r.periodLabel} Preview", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
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
                    Text(
                        if (generated.size == 1) {
                            "One PDF for ${r.periodLabel}."
                        } else {
                            "${generated.size} separate PDFs · ${monthRangeLabel(generated.size, generated.map { it.monthShortLabel })}."
                        },
                        fontSize = 12.sp,
                        color = LabelGray,
                    )

                    if (generated.size > 1) {
                        ShareRow(
                            icon = Icons.Filled.Share,
                            tint = BrandBlue,
                            title = "Share all ${generated.size} PDFs",
                            subtitle = "Send every monthly report together",
                            pillBg = ShareBlueBg,
                            pillColor = BrandBlue,
                        ) {
                            runCatching { HealthReportExport.shareMany(context, generated) }
                                .onFailure { toast(context, "Couldn't share the reports") }
                        }
                        ShareRow(
                            icon = Icons.Filled.Download,
                            tint = GoodGreen,
                            title = "Download all ${generated.size} PDFs",
                            subtitle = "Save each month as its own file",
                            pillBg = SaveGreenBg,
                            pillColor = GoodGreen,
                        ) {
                            val saved = HealthReportExport.downloadMany(context, generated)
                            toast(
                                context,
                                when {
                                    saved.isEmpty() -> "Couldn't save the reports"
                                    saved.size == generated.size -> "Saved ${saved.size} PDFs to your device"
                                    else -> "Saved ${saved.size} of ${generated.size} PDFs"
                                },
                            )
                        }

                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Selected month · ${r.periodLabel}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LabelGray,
                        )
                        ShareRow(
                            icon = Icons.Filled.Share,
                            tint = BrandBlue,
                            title = "Share ${r.monthShortLabel} only",
                            subtitle = "Send just this month's PDF",
                            pillBg = ShareBlueBg,
                            pillColor = BrandBlue,
                        ) {
                            runCatching { HealthReportExport.share(context, r) }
                                .onFailure { toast(context, "Couldn't share the report") }
                        }
                        ShareRow(
                            icon = Icons.Filled.Download,
                            tint = GoodGreen,
                            title = "Download ${r.monthShortLabel} only",
                            subtitle = "Save just this month's PDF",
                            pillBg = SaveGreenBg,
                            pillColor = GoodGreen,
                        ) {
                            val where = HealthReportExport.download(context, r)
                            toast(context, if (where != null) "Saved to $where" else "Couldn't save the report")
                        }
                    } else {
                        ShareRow(
                            icon = Icons.Filled.Share,
                            tint = BrandBlue,
                            title = "Share with Doctor",
                            subtitle = "Send the PDF by email or another app",
                            pillBg = ShareBlueBg,
                            pillColor = BrandBlue,
                        ) {
                            runCatching { HealthReportExport.share(context, r) }
                                .onFailure { toast(context, "Couldn't share the report") }
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
                    }

                    Spacer(Modifier.height(4.dp))
                }

                else -> Text(
                    "Tap Generate to build ${monthlyCountLabel(rangeMonths)} for " +
                        "${monthRangeLabel(rangeMonths, monthChips)}. Each PDF covers a full " +
                        "calendar month, and the current month runs to today.",
                    fontSize = 13.sp,
                    color = LabelGray,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun GeneratingCard(months: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glossySurface(RoundedCornerShape(20.dp), CardWhite)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = BrandBlue, strokeWidth = 2.dp)
        Spacer(Modifier.width(14.dp))
        Text(
            if (months == 1) "Analysing your recorded data…"
            else "Building $months monthly reports…",
            fontSize = 14.sp,
            color = LabelGray,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun GeneratedReportsCard(
    reports: List<HealthReport>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glossySurface(RoundedCornerShape(20.dp), CardWhite)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${reports.size} monthly PDF${if (reports.size == 1) "" else "s"} ready",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                )
                Spacer(Modifier.height(2.dp))
                Text("Tap a month to preview it", fontSize = 11.sp, color = LabelGray)
            }
            Box(
                modifier = Modifier
                    .glossyBadge(BrandBlue, RoundedCornerShape(50.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    "${reports.size} file${if (reports.size == 1) "" else "s"}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = BrandBlue,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            reports.forEachIndexed { index, report ->
                val selected = index == selectedIndex
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) BrandBlue else RangeTrack)
                        .clickable(role = Role.RadioButton) { onSelect(index) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        report.monthShortLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) Color.White else TextDark,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        when {
                            report.isCurrentMonth -> "Month to date"
                            report.readingCount == 0 -> "No readings"
                            else -> report.readingCountLabel
                        },
                        fontSize = 9.sp,
                        color = if (selected) Color.White.copy(alpha = 0.82f) else LabelGray,
                    )
                }
            }
        }
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
                "CareRadar Patient Health Report  ·  ${report.periodLabel}",
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
            Text(
                buildString {
                    append("Generated ${report.generatedAtLabel}  ·  ${report.windowLabel}")
                    if (report.isCurrentMonth) append("  ·  month to date")
                },
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.78f),
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
                if (report.hasData) "End of preview" else "No readings in this month",
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
            .clickable(role = Role.Button) { onClick() }
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
