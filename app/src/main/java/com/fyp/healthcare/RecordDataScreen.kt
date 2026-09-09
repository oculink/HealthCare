package com.fyp.healthcare

import com.fyp.healthcare.ui.theme.appBackground
import com.fyp.healthcare.ui.theme.glossyTopBar
import com.fyp.healthcare.ui.theme.glossyFieldColors
import com.fyp.healthcare.ui.theme.glossyFieldWell
import com.fyp.healthcare.ui.theme.GlossyButton
import com.fyp.healthcare.ui.theme.glossySurface
import com.fyp.healthcare.ui.theme.themed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BrandBlue = Color(0xFF2A6DE1)
private val CardWhite: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val ScreenBackground: Color @Composable get() = themed(Color(0xFFEFF1F6), Color(0xFF121316))
private val FieldBackground: Color @Composable get() = themed(Color(0xFFEDEFF4), Color(0xFF262730))
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
private val PlaceholderGray: Color @Composable get() = themed(Color(0xFFA6ACB8), Color(0xFF6A7079))


@Composable
fun RecordDataScreen(
    healthData: HealthDataManager,
    onBackClick: () -> Unit,
    onSaved: () -> Unit
) {
    var bloodPressure by remember { mutableStateOf("") }
    var bloodSugar by remember { mutableStateOf("") }
    var heartRate by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }
    var oxygen by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().appBackground()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glossyTopBar(BrandBlue)
                .statusBarsPadding()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "Record Health Data",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(48.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
                .glossySurface(RoundedCornerShape(24.dp), CardWhite)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(20.dp)
        ) {
            Text("Enter Today's Readings", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(Modifier.height(4.dp))
            Text("All data is securely saved & analyzed", fontSize = 12.sp, color = LabelGray)
            Spacer(Modifier.height(20.dp))

            ReadingField(bloodPressure, { bloodPressure = it }, "Blood Pressure", "e.g. 120/80 mmHg", Icons.Filled.Bloodtype, KeyboardType.Phone)
            ReadingField(bloodSugar, { bloodSugar = it }, "Blood Sugar", "e.g. 95 mg/dL", Icons.Filled.Cookie, KeyboardType.Decimal)
            ReadingField(heartRate, { heartRate = it }, "Heart Rate", "e.g. 72 BPM", Icons.Filled.MonitorHeart, KeyboardType.Number)
            ReadingField(temperature, { temperature = it }, "Temperature", "e.g. 36.6 °C", Icons.Filled.DeviceThermostat, KeyboardType.Decimal)
            ReadingField(oxygen, { oxygen = it }, "Oxygen Level", "e.g. 98 %", Icons.Filled.Air, KeyboardType.Number)

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

            Spacer(Modifier.height(12.dp))

            GlossyButton(
                onClick = {
                    val problems = mutableListOf<String>()

                    val hr = heartRate.toIntOrNull()
                    if (hr == null || hr !in 20..250)
                        problems.add("Heart Rate must be a whole number between 20 and 250")

                    val bpParts = bloodPressure.split("/")
                    val sys = bpParts.getOrNull(0)?.trim()?.toIntOrNull()
                    val dia = bpParts.getOrNull(1)?.trim()?.toIntOrNull()
                    if (bpParts.size != 2 || sys == null || dia == null || sys !in 70..250 || dia !in 40..150)
                        problems.add("Blood Pressure must look like 120/80 (top 70-250, bottom 40-150)")

                    val oxy = oxygen.toIntOrNull()
                    if (oxy == null || oxy !in 50..100)
                        problems.add("Oxygen Level must be a whole number between 50 and 100")

                    val sugar = bloodSugar.toDoubleOrNull()
                    if (sugar == null || sugar !in 20.0..600.0)
                        problems.add("Blood Sugar must be a number between 20 and 600")

                    val temp = temperature.toDoubleOrNull()
                    if (temp == null || temp !in 30.0..45.0)
                        problems.add("Temperature must be a number between 30 and 45")

                    if (problems.isNotEmpty()) {
                        errorMessage = problems.first()
                    } else {
                        healthData.saveReadings(
                            bloodPressure, bloodSugar, heartRate,
                            temperature, oxygen
                        )
                        onSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                color = BrandBlue,
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Save Readings", fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun ReadingField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray)
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().glossyFieldWell(RoundedCornerShape(14.dp)),
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, color = TextDark),
        placeholder = { Text(placeholder, color = PlaceholderGray) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = LabelGray, modifier = Modifier.padding(start = 12.dp)) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = glossyFieldColors(accent = BrandBlue)
    )
    Spacer(Modifier.height(14.dp))
}