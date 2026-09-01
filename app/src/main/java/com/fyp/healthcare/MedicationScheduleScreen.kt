package com.fyp.healthcare

import com.fyp.healthcare.ui.theme.appBackground
import com.fyp.healthcare.ui.theme.glossyTopBar
import com.fyp.healthcare.ui.theme.GlossyButton
import com.fyp.healthcare.ui.theme.glossySurface
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fyp.healthcare.ui.theme.themed

private val BrandBlue = Color(0xFF2A6DE1)
private val CardWhite: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val ScreenBackground: Color @Composable get() = themed(Color(0xFFEFF1F6), Color(0xFF121316))
private val FieldBackground: Color @Composable get() = themed(Color(0xFFEDEFF4), Color(0xFF262730))
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
private val ChipBlueBg: Color @Composable get() = themed(Color(0xFFE1ECFF), Color(0xFF23324D))

/**
 * Step 2 of adding/editing a medication: the weekly reminder schedule.
 *
 * Starts at "once every day" and lets the user add extra times, and set different
 * times per day (e.g. Mon 6am & 5pm, Tue 8am, Fri 6am/2pm/8pm). Large text and
 * controls — it's the screen an older user touches most often.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicationScheduleScreen(
    draft: Medication,
    isEdit: Boolean,
    onBackClick: () -> Unit,
    onSave: (Medication) -> Unit,
) {
    val context = LocalContext.current

    var schedule by remember {
        mutableStateOf(
            draft.schedule.takeIf { it.isNotEmpty() } ?: (0..6).associateWith { listOf("08:00") }
        )
    }

    fun setDay(day: Int, times: List<String>) {
        schedule = schedule.toMutableMap().apply {
            val cleaned = times.distinct().sortedBy(::hhmmMinutes)
            if (cleaned.isEmpty()) remove(day) else put(day, cleaned)
        }
    }

    fun openPicker(initial: String, onPicked: (String) -> Unit) {
        TimePickerDialog(
            context,
            { _, h, m -> onPicked("%02d:%02d".format(h, m)) },
            hhmmMinutes(initial) / 60,
            hhmmMinutes(initial) % 60,
            DateFormat.is24HourFormat(context),
        ).show()
    }

    val hasAnyTime = schedule.values.any { it.isNotEmpty() }

    Column(modifier = Modifier.fillMaxSize().appBackground()) {

        // ===== Blue top bar =====
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
                "Reminder Times",
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ===== What we're scheduling =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glossySurface(RoundedCornerShape(18.dp), CardWhite)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                com.fyp.healthcare.ui.theme.AppIconBadge(draft.medRoute.icon, BrandBlue, size = 44.dp, iconSize = 24.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(draft.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text(draft.dosageText, fontSize = 14.sp, color = LabelGray)
                }
            }

            Text(
                "When should we remind you?",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "It's set to once a day. Add more times, or give a day its own times.",
                fontSize = 13.sp,
                color = LabelGray,
            )

            // ===== Quick: same time every day =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glossySurface(RoundedCornerShape(16.dp), CardWhite)
                    .clickable {
                        openPicker("08:00") { t -> schedule = (0..6).associateWith { listOf(t) } }
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    "Set every day to one time",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDark,
                    modifier = Modifier.weight(1f),
                )
                Text("Choose", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
            }

            // ===== Per-day =====
            (0..6).forEach { day ->
                val times = schedule[day].orEmpty().sortedBy(::hhmmMinutes)
                DayCard(
                    dayName = DAY_NAMES_LONG[day],
                    times = times,
                    onToggle = { on -> if (on) setDay(day, listOf("08:00")) else setDay(day, emptyList()) },
                    onEditTime = { old -> openPicker(old) { new -> setDay(day, times - old + new) } },
                    onRemoveTime = { t -> setDay(day, times - t) },
                    onAddTime = { openPicker("12:00") { t -> setDay(day, times + t) } },
                )
            }

            Spacer(Modifier.height(4.dp))

            GlossyButton(
                onClick = { onSave(draft.copy(schedule = schedule)) },
                enabled = hasAnyTime,
                modifier = Modifier.fillMaxWidth(),
                color = BrandBlue,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isEdit) "Save Changes" else "Save Medication",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            if (!hasAnyTime) {
                Text(
                    "Turn on at least one day.",
                    fontSize = 13.sp,
                    color = Color(0xFFD32F2F),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayCard(
    dayName: String,
    times: List<String>,
    onToggle: (Boolean) -> Unit,
    onEditTime: (String) -> Unit,
    onRemoveTime: (String) -> Unit,
    onAddTime: () -> Unit,
) {
    val on = times.isNotEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glossySurface(RoundedCornerShape(16.dp), CardWhite)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                dayName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (on) TextDark else LabelGray,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = on,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = BrandBlue,
                ),
            )
        }

        if (on) {
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                times.forEach { t ->
                    Row(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(ChipBlueBg)
                            .clickable { onEditTime(t) }
                            .padding(start = 14.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            formatTime12(t),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue,
                        )
                        IconButton(onClick = { onRemoveTime(t) }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove time", tint = BrandBlue, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(FieldBackground)
                        .clickable { onAddTime() }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = LabelGray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add time", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LabelGray)
                }
            }
        } else {
            Spacer(Modifier.height(4.dp))
            Text("No reminder this day", fontSize = 13.sp, color = LabelGray)
        }
    }
}
