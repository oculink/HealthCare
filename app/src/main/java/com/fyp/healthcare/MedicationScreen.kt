package com.fyp.healthcare

import com.fyp.healthcare.ui.theme.themed
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val BrandBlue = Color(0xFF2A6DE1)
private val BrandBlueDark = Color(0xFF1E50C8)
private val CardWhite: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val ScreenBackground: Color @Composable get() = themed(Color(0xFFEFF1F6), Color(0xFF121316))
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
private val GoodGreen = Color(0xFF2E9E6B)
private val BadRed = Color(0xFFD32F2F)

@Composable
fun MedicationScreen(
    medManager: MedicationManager,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
) {
    val context = LocalContext.current

    // bumping this re-reads storage and re-evaluates every dose's state
    var refresh by remember { mutableStateOf(0) }
    val meds = remember(refresh) { medManager.getAll() }
    val now = remember(refresh) { Calendar.getInstance() }

    // keep "Soon" -> "Missed" etc. moving while the screen is open
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            refresh++
        }
    }

    fun logAction(id: Long, action: String?) {
        medManager.logStatus(id, action)
        medManager.get(id)?.let { ReminderScheduler.scheduleNext(context, it) }
        refresh++
    }

    fun deleteMed(id: Long) {
        medManager.delete(id)
        ReminderScheduler.cancel(context, id)
        refresh++
    }

    val todayMeds = meds.filter { it.isScheduledOn(now) }
    val otherMeds = meds.filterNot { it.isScheduledOn(now) }
    val takenToday = todayMeds.count { it.stateNow(now) == DoseState.TAKEN }

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
                "Medication Reminder",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlueDark, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
        }

        if (meds.isEmpty()) {
            EmptyState(onAddClick)
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ===== Today header =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Today — ${SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date())}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "$takenToday of ${todayMeds.size} taken",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (todayMeds.isNotEmpty() && takenToday == todayMeds.size) GoodGreen else LabelGray,
                )
            }

            if (todayMeds.isEmpty()) {
                Text(
                    "Nothing scheduled for today.",
                    fontSize = 13.sp,
                    color = LabelGray,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            } else {
                todayMeds.forEach { med ->
                    MedicationCard(med, now, ::logAction, onEditClick, ::deleteMed)
                }
            }

            if (otherMeds.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Other Days", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                otherMeds.forEach { med ->
                    MedicationCard(med, now, ::logAction, onEditClick, ::deleteMed)
                }
            }

            // ===== Add New Medication card =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardWhite)
                    .clickable { onAddClick() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(BrandBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Add New Medication", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                    Spacer(Modifier.height(2.dp))
                    Text("Tap to set name, dosage & reminder time", fontSize = 12.sp, color = LabelGray)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EmptyState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(BrandBlue.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Medication, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text("No medications yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(Modifier.height(4.dp))
        Text(
            "Add your first reminder and we'll notify you at the scheduled time.",
            fontSize = 13.sp,
            color = LabelGray,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Add Medication", color = Color.White)
        }
    }
}

@Composable
private fun MedicationCard(
    med: Medication,
    now: Calendar,
    onAction: (Long, String?) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val state = med.stateNow(now)
    val tint = stateColor(state)
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .alpha(if (state == DoseState.OFF) 0.6f else 1f)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Medication, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(med.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(Modifier.height(2.dp))
                Text(med.dosageText, fontSize = 12.sp, color = LabelGray)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = LabelGray, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${formatTime12(med.time)} · ${daysLabel(med.days)}",
                        fontSize = 11.sp,
                        color = LabelGray,
                    )
                }
            }
            StatusPill(state, tint)
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = LabelGray)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = { menuOpen = false; onEdit(med.id) },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = BadRed) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = BadRed) },
                        onClick = { menuOpen = false; confirmDelete = true },
                    )
                }
            }
        }

        val actions: List<Pair<String, String?>> = when (state) {
            DoseState.SOON, DoseState.UPCOMING -> listOf("Skip" to "missed", "Take" to "taken")
            DoseState.MISSED -> listOf("Take late" to "taken")
            DoseState.TAKEN -> listOf("Undo" to null)
            DoseState.OFF -> emptyList()
        }
        if (actions.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                actions.forEachIndexed { index, (label, action) ->
                    val primary = index == actions.lastIndex && action != null
                    val color = when (action) {
                        "missed" -> BadRed
                        "taken" -> GoodGreen
                        else -> LabelGray   // "Undo"
                    }
                    if (primary) {
                        Button(
                            onClick = { onAction(med.id, action) },
                            colors = ButtonDefaults.buttonColors(containerColor = color),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        ) {
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        TextButton(
                            onClick = { onAction(med.id, action) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${med.name}?") },
            text = { Text("This removes the medication and cancels its reminder.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(med.id) }) {
                    Text("Delete", color = BadRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun StatusPill(state: DoseState, tint: Color) {
    val (icon, label) = when (state) {
        DoseState.TAKEN -> Icons.Filled.Check to "Taken"
        DoseState.MISSED -> Icons.Filled.Close to "Missed"
        DoseState.SOON -> Icons.Filled.Schedule to "Due now"
        DoseState.UPCOMING -> Icons.Filled.Schedule to "Upcoming"
        DoseState.OFF -> Icons.Filled.EventBusy to "Not today"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 10.sp, color = tint, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun stateColor(state: DoseState): Color = when (state) {
    DoseState.TAKEN -> GoodGreen
    DoseState.MISSED -> BadRed
    DoseState.SOON -> BrandBlue
    DoseState.UPCOMING -> LabelGray
    DoseState.OFF -> LabelGray
}
