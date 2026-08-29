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

    fun logAction(id: Long, time: String, action: String?) {
        medManager.logStatus(id, action, doseKey(Calendar.getInstance(), time))
        medManager.get(id)?.let { ReminderScheduler.scheduleNext(context, it) }
        refresh++
    }

    fun deleteMed(id: Long) {
        medManager.delete(id)
        ReminderScheduler.cancel(context, id)
        refresh++
    }

    val todayIdx = now.get(Calendar.DAY_OF_WEEK) - 1
    // one entry per (medication, time) scheduled today, earliest first
    val todayDoses = meds
        .flatMap { m -> m.timesOn(todayIdx).map { m to it } }
        .sortedWith(compareBy({ hhmmMinutes(it.second) }, { it.first.name.lowercase() }))
    val otherMeds = meds.filter { it.scheduledDays.isNotEmpty() && todayIdx !in it.scheduledDays }
    val takenToday = todayDoses.count { (m, t) -> m.slotState(t, now) == DoseState.TAKEN }

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
                    "$takenToday of ${todayDoses.size} taken",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (todayDoses.isNotEmpty() && takenToday == todayDoses.size) GoodGreen else LabelGray,
                )
            }

            if (todayDoses.isEmpty()) {
                Text(
                    "Nothing scheduled for today.",
                    fontSize = 13.sp,
                    color = LabelGray,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            } else {
                todayDoses.forEach { (med, time) ->
                    DoseCard(med, time, now, ::logAction, onEditClick, ::deleteMed)
                }
            }

            if (otherMeds.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Other Days", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                otherMeds.forEach { med ->
                    MedSummaryCard(med, onEditClick, ::deleteMed)
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

/** One card for one dose of one medication that's due today at [time]. */
@Composable
private fun DoseCard(
    med: Medication,
    time: String,
    now: Calendar,
    onAction: (Long, String, String?) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val state = med.slotState(time, now)
    val tint = stateColor(state)
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    // (button label, log action) the user tapped — held until they confirm the popup
    var pendingAction by remember { mutableStateOf<Pair<String, String?>?>(null) }

    // A logged dose recolours the whole card: green when taken, red when missed.
    val cardOverlay = when (state) {
        DoseState.TAKEN -> GoodGreen.copy(alpha = 0.28f)
        DoseState.MISSED -> BadRed.copy(alpha = 0.28f)
        else -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .background(cardOverlay)
            .alpha(if (state == DoseState.OFF) 0.6f else 1f)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(med.medRoute.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    formatTime12(time),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state == DoseState.SOON) BrandBlue else TextDark,
                )
                Spacer(Modifier.height(1.dp))
                Text(med.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(med.dosageText, fontSize = 12.sp, color = LabelGray)
                if (med.description.isNotBlank()) {
                    Text(med.description, fontSize = 11.sp, color = LabelGray)
                }
            }
            // "Taken" / "Missed" no longer get a pill here — the card colour + the
            // chip by the buttons carry that. Other states still show their pill.
            if (state != DoseState.TAKEN && state != DoseState.MISSED) {
                StatusPill(state, tint)
            }
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state == DoseState.TAKEN || state == DoseState.MISSED) {
                    val (chipColor, chipLabel) =
                        if (state == DoseState.TAKEN) GoodGreen to "Taken" else BadRed to "Missed"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(chipColor.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(chipLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = chipColor)
                    }
                    // push the action button(s) to the far right
                    Spacer(Modifier.weight(1f))
                }
                actions.forEachIndexed { index, (label, action) ->
                    val primary = index == actions.lastIndex && action != null
                    val color = when (action) {
                        "missed" -> BadRed
                        "taken" -> GoodGreen
                        else -> LabelGray   // "Undo"
                    }
                    if (primary) {
                        Button(
                            onClick = { pendingAction = label to action },
                            colors = ButtonDefaults.buttonColors(containerColor = color),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        ) {
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        TextButton(
                            onClick = { pendingAction = label to action },
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

    pendingAction?.let { (label, action) ->
        val doseLabel = "${med.name} · ${formatTime12(time)}"
        val (title, body, confirmColor) = when (action) {
            "taken" -> Triple(
                "Mark as taken?",
                "Log the $doseLabel dose as taken.",
                GoodGreen,
            )
            "missed" -> Triple(
                "Skip this dose?",
                "Mark the $doseLabel dose as skipped.",
                BadRed,
            )
            else -> Triple(
                "Undo?",
                "Clear the log for $doseLabel — it'll show as due again.",
                BrandBlue,
            )
        }
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(title) },
            text = { Text(body) },
            confirmButton = {
                TextButton(onClick = { onAction(med.id, time, action); pendingAction = null }) {
                    Text(label, color = confirmColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) { Text("Cancel") }
            },
        )
    }
}

/** Info-only card for a medication with no dose today (shows the weekly plan). */
@Composable
private fun MedSummaryCard(
    med: Medication,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(BrandBlue.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(med.medRoute.icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(med.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Text(med.dosageText, fontSize = 12.sp, color = LabelGray)
            Spacer(Modifier.height(2.dp))
            Text(med.scheduleSummary(), fontSize = 11.sp, color = LabelGray)
        }
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
    // TAKEN / MISSED are shown by recolouring the card + a chip near the buttons.
    val (icon, label) = when (state) {
        DoseState.SOON -> Icons.Filled.Schedule to "Due now"
        DoseState.UPCOMING -> Icons.Filled.Schedule to "Upcoming"
        DoseState.OFF -> Icons.Filled.EventBusy to "Not today"
        DoseState.TAKEN, DoseState.MISSED -> return
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
