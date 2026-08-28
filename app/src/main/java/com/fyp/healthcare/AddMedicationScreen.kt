package com.fyp.healthcare

import com.fyp.healthcare.ui.theme.themed
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val BrandBlue = Color(0xFF2A6DE1)
private val CardWhite: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val ScreenBackground: Color @Composable get() = themed(Color(0xFFEFF1F6), Color(0xFF121316))
private val FieldBackground: Color @Composable get() = themed(Color(0xFFEDEFF4), Color(0xFF262730))
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
private val PlaceholderGray: Color @Composable get() = themed(Color(0xFFA6ACB8), Color(0xFF6A7079))

/**
 * Handles both "add" and "edit" — pass [editId] to load an existing medication.
 */
@Composable
fun AddMedicationScreen(
    medManager: MedicationManager,
    editId: Long?,
    onBackClick: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    val context = LocalContext.current
    val editing = remember(editId) { editId?.let { medManager.get(it) } }
    val isEdit = editing != null

    var name by remember { mutableStateOf(editing?.name ?: "") }
    var dosageMg by remember { mutableStateOf(editing?.dosageMg?.takeIf { it > 0 }?.toString() ?: "") }
    var amount by remember { mutableStateOf(editing?.amount?.takeIf { it > 0 }?.toString() ?: "") }
    var time24 by remember { mutableStateOf(editing?.time ?: "20:00") }
    var timeDisplay by remember { mutableStateOf(formatTime12(editing?.time ?: "20:00")) }
    var selectedDays by remember { mutableStateOf(editing?.days ?: setOf(1, 2, 3, 4, 5)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val dayLetters = listOf("S", "M", "T", "W", "T", "F", "S")

    Column(modifier = Modifier.fillMaxSize().background(ScreenBackground)) {

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
                if (isEdit) "Edit Medication" else "Add Medication",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(48.dp))
        }

        // ===== Form card =====
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardWhite)
                    .padding(20.dp),
            ) {
                Text("Medication Details", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(Modifier.height(4.dp))
                Text("Fill in the details for your reminder", fontSize = 12.sp, color = LabelGray)
                Spacer(Modifier.height(20.dp))

                MedField(name, { name = it }, "Medication Name", "e.g. Amlodipine", Icons.Filled.Medication)
                MedField(
                    dosageMg, { dosageMg = it.filter(Char::isDigit) },
                    "Dosage (mg)", "e.g. 500", Icons.Filled.Medication, KeyboardType.Number,
                )
                MedField(
                    amount, { amount = it.filter(Char::isDigit) },
                    "Amount per dose (tablets)", "e.g. 1", Icons.Filled.Numbers, KeyboardType.Number,
                )

                // ===== Reminder time (opens the phone's time picker) =====
                Text("Reminder Time", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(FieldBackground)
                        .clickable {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    time24 = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                                    timeDisplay = formatTime12(time24)
                                },
                                time24.substringBefore(":").toIntOrNull() ?: 20,
                                time24.substringAfter(":").toIntOrNull() ?: 0,
                                DateFormat.is24HourFormat(context),
                            ).show()
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFFDEAEA)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = Color(0xFFD32F2F))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Set Reminder Time", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
                        Spacer(Modifier.height(2.dp))
                        Text(timeDisplay, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                    }
                }
                Spacer(Modifier.height(14.dp))

                // ===== Repeat days =====
                Text("Repeat Days", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(FieldBackground)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    dayLetters.forEachIndexed { index, letter ->
                        val selected = index in selectedDays
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(if (selected) BrandBlue else CardWhite)
                                .clickable {
                                    selectedDays =
                                        if (selected) selectedDays - index else selectedDays + index
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                letter,
                                color = if (selected) Color.White else LabelGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(daysLabel(selectedDays), fontSize = 11.sp, color = LabelGray)
                Spacer(Modifier.height(16.dp))

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

                Button(
                    onClick = {
                        val mg = dosageMg.toIntOrNull()
                        val amt = amount.toIntOrNull()
                        errorMessage = when {
                            name.isBlank() -> "Enter a medication name"
                            mg == null || mg !in 1..10_000 -> "Dosage must be 1–10000 mg"
                            amt == null || amt !in 1..20 -> "Amount must be 1–20 tablets"
                            selectedDays.isEmpty() -> "Pick at least one repeat day"
                            else -> null
                        }
                        if (errorMessage != null) return@Button

                        val saved = if (isEdit) {
                            editing!!.copy(
                                name = name,
                                dosageMg = mg!!,
                                amount = amt!!,
                                time = time24,
                                days = selectedDays,
                            ).also { medManager.update(it) }
                        } else {
                            medManager.add(name, mg!!, amt!!, time24, selectedDays)
                        }
                        ReminderScheduler.scheduleNext(context, saved)
                        onSaved(saved.id)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isEdit) "Save Changes" else "Save Medication",
                        fontSize = 16.sp,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun MedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray)
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder, color = PlaceholderGray) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = LabelGray, modifier = Modifier.padding(start = 12.dp)) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = FieldBackground,
            unfocusedContainerColor = FieldBackground,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
        ),
    )
    Spacer(Modifier.height(14.dp))
}
