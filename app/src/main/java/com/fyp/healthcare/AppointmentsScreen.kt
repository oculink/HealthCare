package com.fyp.healthcare

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.MoreVert

import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Replay
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fyp.healthcare.ui.theme.AppIconBadge
import com.fyp.healthcare.ui.theme.GlossyButton
import com.fyp.healthcare.ui.theme.appBackground
import com.fyp.healthcare.ui.theme.glossyBadge
import com.fyp.healthcare.ui.theme.glossySurface
import com.fyp.healthcare.ui.theme.glossyTopBar
import com.fyp.healthcare.ui.theme.themed
import kotlinx.coroutines.delay

private val BrandBlue = Color(0xFF2A6DE1)
private val BrandBlueDark = Color(0xFF1E50C8)
private val Teal = Color(0xFF0F9E99)
private val CardWhite: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
private val GoodGreen = Color(0xFF2E9E6B)
private val BadRed = Color(0xFFD32F2F)
private val WarnAmber = Color(0xFFEE7B2E)

@Composable
fun AppointmentsScreen(
    apptManager: AppointmentManager,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
) {
    val context = LocalContext.current

    var refresh by remember { mutableIntStateOf(0) }
    val now = remember(refresh, Session.dataVersion) { System.currentTimeMillis() }
    val all = remember(refresh, Session.dataVersion) { apptManager.getAll() }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            refresh++
        }
    }

    val upcoming = all.filter { it.state(now) == Appointment.STATE_UPCOMING }
        .sortedBy { it.startMillis }
    val history = all.filter { it.state(now) != Appointment.STATE_UPCOMING }
        .sortedByDescending { it.startMillis }

    fun mutate(block: () -> Unit) {
        block()
        Session.bumpDataVersion()
        refresh++
    }

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
                "Appointments",
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

        if (all.isEmpty()) {
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
            Text(
                "Upcoming",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
            )
            if (upcoming.isEmpty()) {
                Text("Nothing coming up.", fontSize = 13.sp, color = LabelGray)
            } else {
                upcoming.forEach { appt ->
                    ApptCard(
                        appt = appt,
                        now = now,
                        onEdit = { onEditClick(appt.id) },
                        onMarkAttended = { mutate { apptManager.setStatus(appt.id, Appointment.STATUS_COMPLETED); AppointmentReminderScheduler.cancel(context, appt.id) } },
                        onCancel = { mutate { apptManager.setStatus(appt.id, Appointment.STATUS_CANCELLED); AppointmentReminderScheduler.cancel(context, appt.id) } },
                        onReopen = null,
                        onDelete = { mutate { apptManager.delete(appt.id); AppointmentReminderScheduler.cancel(context, appt.id) } },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glossySurface(RoundedCornerShape(20.dp), CardWhite)
                    .clickable { onAddClick() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIconBadge(Icons.Filled.Add, BrandBlue, size = 40.dp, iconSize = 22.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Add Appointment", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                    Spacer(Modifier.height(2.dp))
                    Text("Doctor, clinic, date & a reminder", fontSize = 12.sp, color = LabelGray)
                }
            }

            if (history.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Past & cancelled", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                history.forEach { appt ->
                    ApptCard(
                        appt = appt,
                        now = now,
                        onEdit = { onEditClick(appt.id) },
                        onMarkAttended = if (appt.state(now) == Appointment.STATE_PAST) {
                            { mutate { apptManager.setStatus(appt.id, Appointment.STATUS_COMPLETED) } }
                        } else null,
                        onCancel = null,
                        onReopen = if (appt.isCancelled || appt.isCompleted) {
                            { mutate { apptManager.setStatus(appt.id, Appointment.STATUS_UPCOMING); apptManager.get(appt.id)?.let { AppointmentReminderScheduler.scheduleNext(context, it) } } }
                        } else null,
                        onDelete = { mutate { apptManager.delete(appt.id); AppointmentReminderScheduler.cancel(context, appt.id) } },
                    )
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
        AppIconBadge(Icons.Filled.CalendarMonth, Teal, size = 72.dp, iconSize = 34.dp)
        Spacer(Modifier.height(14.dp))
        Text("No appointments yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(Modifier.height(4.dp))
        Text(
            "Add an appointment you've booked and we'll remind you before it.",
            fontSize = 13.sp,
            color = LabelGray,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        GlossyButton(onClick = onAddClick, color = BrandBlue) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Add Appointment", color = Color.White)
        }
    }
}

@Composable
private fun ApptCard(
    appt: Appointment,
    now: Long,
    onEdit: () -> Unit,
    onMarkAttended: (() -> Unit)?,
    onCancel: (() -> Unit)?,
    onReopen: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    val state = appt.state(now)
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val (pillColor, pillLabel) = when (state) {
        Appointment.STATE_UPCOMING -> BrandBlue to relativeDayLabel(appt.startMillis, now)
        Appointment.STATE_PAST -> WarnAmber to "Needs update"
        Appointment.STATE_COMPLETED -> GoodGreen to "Attended"
        else -> LabelGray to "Cancelled"
    }
    val faded = state == Appointment.STATE_CANCELLED

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glossySurface(RoundedCornerShape(20.dp), CardWhite)
            .alpha(if (faded) 0.6f else 1f)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            AppIconBadge(Icons.Filled.CalendarMonth, if (faded) LabelGray else Teal, size = 44.dp, iconSize = 22.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(appt.whenText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(Modifier.height(2.dp))
                Text(appt.doctorLine, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                if (appt.clinicName.isNotBlank() || appt.address.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Place, contentDescription = null, tint = LabelGray, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            listOf(appt.clinicName, appt.address).filter { it.isNotBlank() }.joinToString(" · "),
                            fontSize = 11.sp,
                            color = LabelGray,
                        )
                    }
                }
                if (appt.notes.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = LabelGray, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(appt.notes, fontSize = 11.sp, color = LabelGray)
                    }
                }
                if (appt.remindMinutesBefore > 0 && state == Appointment.STATE_UPCOMING) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Reminder ${Appointment.reminderLabel(appt.remindMinutesBefore).lowercase()} before",
                        fontSize = 10.sp,
                        color = LabelGray,
                    )
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = LabelGray)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = { menuOpen = false; onEdit() },
                    )
                    onMarkAttended?.let { action ->
                        DropdownMenuItem(
                            text = { Text("Mark attended") },
                            leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GoodGreen) },
                            onClick = { menuOpen = false; action() },
                        )
                    }
                    onCancel?.let { action ->
                        DropdownMenuItem(
                            text = { Text("Cancel appointment") },
                            leadingIcon = { Icon(Icons.Filled.EventBusy, contentDescription = null, tint = WarnAmber) },
                            onClick = { menuOpen = false; action() },
                        )
                    }
                    onReopen?.let { action ->
                        DropdownMenuItem(
                            text = { Text("Reopen") },
                            leadingIcon = { Icon(Icons.Filled.Replay, contentDescription = null) },
                            onClick = { menuOpen = false; action() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete", color = BadRed) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = BadRed) },
                        onClick = { menuOpen = false; confirmDelete = true },
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .glossyBadge(pillColor, RoundedCornerShape(50.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(pillLabel, fontSize = 10.sp, color = pillColor, fontWeight = FontWeight.Medium)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this appointment?") },
            text = { Text("Removes the ${appt.doctorLine} appointment and its reminder.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("Delete", color = BadRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}
