package com.fyp.healthcare

import android.app.TimePickerDialog
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val BrandBlue = Color(0xFF2A6DE1)
private val CardWhite = Color(0xFFFFFFFF)
private val ScreenBackground = Color(0xFFEFF1F6)
private val FieldBackground = Color(0xFFEDEFF4)
private val TextDark = Color(0xFF1B1D23)
private val LabelGray = Color(0xFF5F6673)
private val PlaceholderGray = Color(0xFFA6ACB8)

@Composable
fun AddMedicationScreen(
    medManager: MedicationManager,
    onBackClick: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("") }
    var time24 by remember { mutableStateOf("20:00") }
    var timeDisplay by remember { mutableStateOf("8:00 PM") }
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5)) } // Mon-Fri default
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val dayLetters = listOf("S", "M", "T", "W", "T", "F", "S")
    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Column(modifier = Modifier.fillMaxSize().background(ScreenBackground)) {

        // ===== Blue top bar =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrandBlue)
                .statusBarsPadding()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "Add Medication",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(48.dp))
        }

        // ===== Form card =====
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardWhite)
                    .padding(20.dp)
            ) {
                Text("Medication Details", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(Modifier.height(4.dp))
                Text("Fill in the details for your reminder", fontSize = 12.sp, color = LabelGray)
                Spacer(Modifier.height(20.dp))

                MedField(name, { name = it }, "Medication Name", "e.g. Amlodipine", "💊")
                MedField(dosage, { dosage = it }, "Dosage", "e.g. 5mg — 1 tablet", "️")
                MedField(frequency, { frequency = it }, "Frequency", "e.g. Once daily", "🔄")

                // ===== Reminder time (opens the phone's time picker) =====
                Text("Reminder Time", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardWhite)
                        .clickable {
                            val cal = Calendar.getInstance()
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    time24 = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                                    val c = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, hour)
                                        set(Calendar.MINUTE, minute)
                                    }
                                    timeDisplay = SimpleDateFormat("h:mm a", Locale.getDefault()).format(c.time)
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                false
                            ).show()
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFFDEAEA)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⏰", fontSize = 20.sp)
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
                        .background(CardWhite)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dayLetters.forEachIndexed { index, letter ->
                        val selected = index in selectedDays
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(if (selected) BrandBlue else FieldBackground)
                                .clickable {
                                    selectedDays = if (selected) selectedDays - index else selectedDays + index
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                letter,
                                color = if (selected) Color.White else LabelGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                errorMessage?.let {
                    Text(
                        it,
                        color = Color(0xFFD32F2F),
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        // TODO: this medication will also be uploaded to the SQL database
                        when {
                            name.isBlank() || dosage.isBlank() || frequency.isBlank() ->
                                errorMessage = "Please fill in all fields"
                            selectedDays.isEmpty() ->
                                errorMessage = "Select at least one repeat day"
                            else -> {
                                val daysText = selectedDays.sorted().map { dayNames[it] }.joinToString(", ")
                                medManager.add(name, dosage, frequency, time24, daysText)
                                onSaved()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("💊  Save Medication", fontSize = 16.sp, color = Color.White)
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
    emoji: String // TODO: replace with asset image later
) {
    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray)
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder, color = PlaceholderGray) },
        leadingIcon = { Text(emoji, fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp)) },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = FieldBackground,
            unfocusedContainerColor = FieldBackground,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent
        )
    )
    Spacer(Modifier.height(14.dp))
}