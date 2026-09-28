package com.fyp.healthcare

import com.fyp.healthcare.ui.theme.AppIconBadge
import com.fyp.healthcare.ui.theme.GlossyButton
import com.fyp.healthcare.ui.theme.appBackground
import com.fyp.healthcare.ui.theme.glossyBadge
import com.fyp.healthcare.ui.theme.glossySurface
import com.fyp.healthcare.ui.theme.glossyTopBar
import com.fyp.healthcare.ui.theme.themed
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BrandBlue = Color(0xFF2A6DE1)
private val Teal = Color(0xFF0F9E99)
private val CardWhite: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
private val Hairline: Color @Composable get() = themed(Color(0xFFE5E8EE), Color(0xFF2A2C33))
private val GoodGreen = Color(0xFF2E9E6B)
private val BadRed = Color(0xFFD32F2F)

/** Days shown before the pager turns, matching the Reading History screen. */
private const val DAYS_PER_PAGE = 10

/**
 * One logged dose across any medication: what happened ([taken] vs [missed]) at [timeMillis],
 * plus a pointer back to the medication so the name, dosage and description can be shown.
 */
data class MedicationDose(
    val medication: Medication,
    val doseKey: String,
    val taken: Boolean,
    val timeMillis: Long,
)

/** "2026-08-29 06:00" (the log key [Medication] uses) -> epoch millis, or null if malformed. */
private fun doseKeyMillis(doseKey: String): Long? =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse(doseKey)?.time

/**
 * Every dose the person has ever marked Taken or Missed, newest first. Two doses may share a
 * time on the same day when two medications are due together - each is still its own row.
 */
fun medicationDoseLog(medManager: MedicationManager): List<MedicationDose> =
    medManager.getAll().flatMap { med ->
        med.log.mapNotNull { (key, action) ->
            doseKeyMillis(key)?.let {
                MedicationDose(med, key, action == "taken", it)
            }
        }
    }.sortedByDescending { it.timeMillis }

/**
 * Medication history grouped by day, paged ten days at a time, styled like Reading History so
 * the two pages feel like one family. Only doses actually logged (Taken or Missed on the
 * Medications page) are shown; an empty list is a message, not an error.
 */
@Composable
fun MedicationHistoryScreen(
    medManager: MedicationManager,
    onBackClick: () -> Unit,
) {
    val doses = remember(Session.dataVersion) { medicationDoseLog(medManager) }

    var page by remember { mutableIntStateOf(0) }

    val days = remember(doses) {
        doses.groupBy { dayKey(it.timeMillis) }
            .toList()
            .sortedByDescending { (_, list) -> list.maxOf { it.timeMillis } }
    }
    val pageCount = if (doses.isEmpty()) 1 else (days.size + DAYS_PER_PAGE - 1) / DAYS_PER_PAGE
    val currentPage = page.coerceIn(0, pageCount - 1)
    val visibleDays = days.drop(currentPage * DAYS_PER_PAGE).take(DAYS_PER_PAGE)
    val shownDoses = visibleDays.sumOf { it.second.size }

    val listScroll = rememberScrollState()
    LaunchedEffect(currentPage) { listScroll.scrollTo(0) }

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
                "Medication History",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(48.dp))
        }

        if (doses.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Nothing logged yet", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Mark a dose Taken or Missed on the Medications page and it will appear here, " +
                        "newest first.",
                    fontSize = 13.sp,
                    color = LabelGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(listScroll)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    "$shownDoses dose${if (shownDoses == 1) "" else "s"} shown · " +
                        "${days.size} day${if (days.size == 1) "" else "s"} logged" +
                        if (pageCount > 1) " · page ${currentPage + 1} of $pageCount" else "",
                    fontSize = 13.sp,
                    color = LabelGray,
                )

                visibleDays.forEach { (key, list) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glossySurface(RoundedCornerShape(20.dp), CardWhite)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            dayLabel(key),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                        )
                        list.forEachIndexed { i, dose ->
                            if (i > 0) {
                                Spacer(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
                            }
                            DoseBlock(dose)
                        }
                    }
                }
            }

            if (pageCount > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardWhite)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GlossyButton(
                        onClick = { page = (currentPage - 1).coerceAtLeast(0) },
                        modifier = Modifier.weight(1f),
                        color = BrandBlue,
                        enabled = currentPage > 0,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                    ) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = null, modifier = Modifier.size(22.dp))
                        Text("Newer")
                    }
                    Text(
                        "${currentPage + 1} / $pageCount",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(52.dp),
                    )
                    GlossyButton(
                        onClick = { page = (currentPage + 1).coerceAtMost(pageCount - 1) },
                        modifier = Modifier.weight(1f),
                        color = BrandBlue,
                        enabled = currentPage < pageCount - 1,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                    ) {
                        Text("Older")
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DoseBlock(dose: MedicationDose) {
    val statusColor = if (dose.taken) GoodGreen else BadRed
    val route = dose.medication.medRoute
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(dose.timeMillis)),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = LabelGray,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIconBadge(Icons.Filled.MedicalServices, Teal, size = 26.dp, iconSize = 15.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dose.medication.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${route.label} · ${dose.medication.dosageText}",
                    fontSize = 11.sp,
                    color = LabelGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (dose.medication.description.isNotBlank()) {
                    Text(
                        dose.medication.description,
                        fontSize = 11.sp,
                        color = LabelGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                if (dose.taken) "Taken" else "Missed",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = statusColor,
                modifier = Modifier
                    .glossyBadge(statusColor, RoundedCornerShape(50.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

private const val DAY_MS = 24L * 60 * 60 * 1000

private fun dayFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

private fun dayKey(millis: Long): String = dayFormat().format(Date(millis))

/** Today and Yesterday by name; anything older by weekday and date. */
private fun dayLabel(key: String): String {
    val now = System.currentTimeMillis()
    return when (key) {
        dayKey(now) -> "Today"
        dayKey(now - DAY_MS) -> "Yesterday"
        else -> dayFormat().parse(key)?.let {
            SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(it)
        } ?: key
    }
}