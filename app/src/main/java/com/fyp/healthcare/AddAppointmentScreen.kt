package com.fyp.healthcare

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.fyp.healthcare.ui.theme.GlossyButton
import com.fyp.healthcare.ui.theme.appBackground
import com.fyp.healthcare.ui.theme.glossyChip
import com.fyp.healthcare.ui.theme.glossyFieldColors
import com.fyp.healthcare.ui.theme.glossyFieldWell
import com.fyp.healthcare.ui.theme.glossySurface
import com.fyp.healthcare.ui.theme.glossyTopBar
import com.fyp.healthcare.ui.theme.themed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val BrandBlue = Color(0xFF2A6DE1)
private val Teal = Color(0xFF0F9E99)
private val CardWhite: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val TrackBg: Color @Composable get() = themed(Color(0xFFE7EAF1), Color(0xFF23252E))
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
private val PlaceholderGray: Color @Composable get() = themed(Color(0xFFA6ACB8), Color(0xFF6A7079))

/**
 * Add or edit one [Appointment]. Everything is typed by hand — the app does not talk to
 * any clinic system. "Next available" style booking is out of scope (see the FYP report):
 * the user records an appointment they already made, and the app reminds them of it.
 */
@Composable
fun AddAppointmentScreen(
    apptManager: AppointmentManager,
    editId: Long?,
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val editing = remember(editId) { editId?.let { apptManager.get(it) } }
    val isEdit = editing != null

    var doctor by remember { mutableStateOf(editing?.doctorName ?: "") }
    var specialty by remember { mutableStateOf(editing?.specialty ?: "") }
    var clinic by remember { mutableStateOf(editing?.clinicName ?: "") }
    var address by remember { mutableStateOf(editing?.address ?: "") }
    var addrLat by remember { mutableStateOf(editing?.lat) }
    var addrLng by remember { mutableStateOf(editing?.lng) }
    var addrEditing by remember { mutableStateOf(false) }
    var addrSuggestions by remember { mutableStateOf<List<NearbyClinics.Suggestion>>(emptyList()) }
    var showMapPicker by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf(editing?.notes ?: "") }
    var remindMinutes by remember { mutableIntStateOf(editing?.remindMinutesBefore ?: 60) }

    LaunchedEffect(address, addrEditing) {
        if (!addrEditing || address.trim().length < 3) {
            addrSuggestions = emptyList()
            return@LaunchedEffect
        }
        delay(350)
        addrSuggestions = NearbyClinics.suggestAddresses(address)
    }

    val cal = remember {
        Calendar.getInstance().apply {
            editing?.let { timeInMillis = it.startMillis } ?: run {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 10); set(Calendar.MINUTE, 0)
            }
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
    }
    var startMillis by remember { mutableStateOf(cal.timeInMillis) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val dateFmt = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    fun openDatePicker() {
        val c = Calendar.getInstance().apply { timeInMillis = startMillis }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                c.set(Calendar.YEAR, y); c.set(Calendar.MONTH, m); c.set(Calendar.DAY_OF_MONTH, d)
                startMillis = c.timeInMillis
            },
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH),
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 86_400_000L
        }.show()
    }

    fun openTimePicker() {
        val c = Calendar.getInstance().apply { timeInMillis = startMillis }
        TimePickerDialog(
            context,
            { _, h, min ->
                c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, min)
                c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
                startMillis = c.timeInMillis
            },
            c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
            DateFormat.is24HourFormat(context),
        ).show()
    }

    Box(modifier = Modifier.fillMaxSize()) {

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
                if (isEdit) "Edit Appointment" else "Add Appointment",
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
                .imePadding()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glossySurface(RoundedCornerShape(24.dp), CardWhite)
                    .padding(20.dp),
            ) {
                Text("Appointment Details", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(Modifier.height(4.dp))
                Text("Record an appointment you've booked — we'll remind you.", fontSize = 12.sp, color = LabelGray)
                Spacer(Modifier.height(20.dp))

                ApptField(doctor, { doctor = it }, "Doctor / clinician", "e.g. Dr. Sarah Lim", Icons.Filled.Person)
                ApptField(specialty, { specialty = it }, "Specialty (optional)", "e.g. Cardiologist", Icons.Filled.MedicalServices)
                ApptField(clinic, { clinic = it }, "Clinic / hospital (optional)", "e.g. Pantai Hospital KL", Icons.Filled.LocalHospital)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Address (optional)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = LabelGray,
                        modifier = Modifier.weight(1f),
                    )
                    Row(
                        modifier = Modifier
                            .clickable {
                                focusManager.clearFocus()
                                addrEditing = false
                                addrSuggestions = emptyList()
                                showMapPicker = true
                            }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Map, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("See map", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it; addrEditing = true; addrLat = null; addrLng = null },
                    modifier = Modifier.fillMaxWidth().glossyFieldWell(RoundedCornerShape(14.dp)),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 17.sp, color = TextDark),
                    placeholder = { Text("Type an address or use the map", color = PlaceholderGray) },
                    leadingIcon = { Icon(Icons.Filled.Place, contentDescription = null, tint = LabelGray, modifier = Modifier.padding(start = 12.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    shape = RoundedCornerShape(14.dp),
                    colors = glossyFieldColors(accent = BrandBlue),
                )
                addrSuggestions.forEach { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                address = s.label
                                addrLat = s.lat
                                addrLng = s.lon
                                addrEditing = false
                                addrSuggestions = emptyList()
                                focusManager.clearFocus()
                            }
                            .padding(horizontal = 6.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = LabelGray, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            s.label,
                            color = TextDark,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))

                Text("Date", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray)
                Spacer(Modifier.height(6.dp))
                SelectorRow(Icons.Filled.Event, dateFmt.format(Date(startMillis))) { openDatePicker() }
                Spacer(Modifier.height(14.dp))

                Text("Time", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray)
                Spacer(Modifier.height(6.dp))
                SelectorRow(Icons.Filled.Schedule, timeFmt.format(Date(startMillis))) { openTimePicker() }
                Spacer(Modifier.height(14.dp))

                Text("Remind me", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glossySurface(RoundedCornerShape(14.dp), TrackBg, elevation = 0.dp)
                        .horizontalScroll(rememberScrollState())
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Appointment.REMINDER_CHOICES.forEach { (label, minutes) ->
                        val selected = remindMinutes == minutes
                        Box(
                            modifier = Modifier
                                .glossyChip(selected, Teal, RoundedCornerShape(10.dp))
                                .clickable { remindMinutes = minutes }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(
                                label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) Color.White else LabelGray,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (remindMinutes <= 0) "No reminder will be set."
                    else "Notifies you ${Appointment.reminderLabel(remindMinutes).lowercase(Locale.getDefault())} before.",
                    fontSize = 11.sp,
                    color = LabelGray,
                )
                Spacer(Modifier.height(14.dp))

                ApptField(notes, { notes = it }, "Notes (optional)", "Bring referral letter, fasting required…", Icons.AutoMirrored.Filled.Notes, singleLine = false)

                errorMessage?.let {
                    Text(
                        it,
                        color = Color(0xFFD32F2F),
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                }

                GlossyButton(
                    onClick = {
                        errorMessage = when {
                            doctor.isBlank() -> "Enter the doctor or clinic name"
                            !isEdit && startMillis <= System.currentTimeMillis() ->
                                "Pick a date and time in the future"
                            else -> null
                        }
                        if (errorMessage != null) return@GlossyButton

                        val saved = apptManager.upsert(
                            Appointment(
                                id = editing?.id ?: apptManager.nextId(),
                                doctorName = doctor,
                                specialty = specialty,
                                clinicName = clinic,
                                address = address,
                                lat = addrLat,
                                lng = addrLng,
                                startMillis = startMillis,
                                notes = notes,
                                remindMinutesBefore = remindMinutes,
                                status = if (editing?.isCancelled == true || editing?.isCompleted == true) {
                                    if (startMillis > System.currentTimeMillis()) Appointment.STATUS_UPCOMING
                                    else editing.status
                                } else editing?.status ?: Appointment.STATUS_UPCOMING,
                                createdAt = editing?.createdAt ?: System.currentTimeMillis(),
                            )
                        )
                        AppointmentReminderScheduler.scheduleNext(context, saved)
                        Session.bumpDataVersion()
                        onSaved()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = BrandBlue,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isEdit) "Save Changes" else "Save Appointment",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showMapPicker) {
        BackHandler { showMapPicker = false }
        LocationPickerSheet(
            initialAddress = address,
            initialLat = addrLat,
            initialLng = addrLng,
            onDismiss = { showMapPicker = false },
            onConfirm = { a, la, lo ->
                address = a
                addrLat = la
                addrLng = lo
                addrEditing = false
                addrSuggestions = emptyList()
                showMapPicker = false
            },
        )
    }
    }
}

@Composable
private fun ApptField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    singleLine: Boolean = true,
) {
    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray)
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().glossyFieldWell(RoundedCornerShape(14.dp)),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        textStyle = TextStyle(fontSize = 17.sp, color = TextDark),
        placeholder = { Text(placeholder, color = PlaceholderGray) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = LabelGray, modifier = Modifier.padding(start = 12.dp)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        shape = RoundedCornerShape(14.dp),
        colors = glossyFieldColors(accent = BrandBlue),
    )
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun SelectorRow(icon: ImageVector, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glossyFieldWell(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = LabelGray, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(value, fontSize = 15.sp, color = TextDark, modifier = Modifier.weight(1f))
        Text("Change", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
    }
}
