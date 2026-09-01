package com.fyp.healthcare

import com.fyp.healthcare.ui.theme.appBackground
import com.fyp.healthcare.ui.theme.glossyTopBar
import com.fyp.healthcare.ui.theme.glossyChip
import com.fyp.healthcare.ui.theme.glossyFieldColors
import com.fyp.healthcare.ui.theme.glossyFieldWell
import com.fyp.healthcare.ui.theme.GlossyButton
import com.fyp.healthcare.ui.theme.glossySurface
import com.fyp.healthcare.ui.theme.themed
import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Locale

private val BrandBlue = Color(0xFF2A6DE1)
private val CardWhite: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val ScreenBackground: Color @Composable get() = themed(Color(0xFFEFF1F6), Color(0xFF121316))
private val FieldBackground: Color @Composable get() = themed(Color(0xFFEDEFF4), Color(0xFF262730))
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
private val PlaceholderGray: Color @Composable get() = themed(Color(0xFFA6ACB8), Color(0xFF6A7079))
private val ErrRed = Color(0xFFD32F2F)

/**
 * Used for both first-run onboarding ([firstRun] = true, shown right after Google sign-in)
 * and later edits from the Profile page ([firstRun] = false).
 *
 * On first run: Name, Birth date, Height, Weight and Gender are required; Blood type is optional.
 * On a later edit everything is optional.
 */
@Composable
fun EditProfileScreen(
    userManager: UserManager,
    profileManager: ProfileManager,
    firstRun: Boolean,
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val existing = remember { profileManager.get() }
    val fallbackName = remember { userManager.currentAccount()?.name.orEmpty() }

    var name by remember { mutableStateOf(existing.name.ifBlank { fallbackName }) }
    var birthDate by remember { mutableStateOf(existing.birthDate) }   // ISO "yyyy-MM-dd" or ""
    var sex by remember { mutableStateOf(existing.sex) }
    var bloodType by remember { mutableStateOf(existing.bloodType) }
    var heightCm by remember { mutableStateOf(existing.heightCm) }
    var weightKg by remember { mutableStateOf(existing.weightKg) }
    var allergies by remember { mutableStateOf(joinTags(existing.allergies)) }
    var conditions by remember { mutableStateOf(joinTags(existing.conditions)) }
    val contacts = remember { mutableStateListOf<EmergencyContact>().apply { addAll(existing.emergencyContacts) } }
    var error by remember { mutableStateOf<String?>(null) }

    // During onboarding the system back gesture maps to the header's "Go back"
    // (which signs the half-finished account out), rather than silently doing nothing.
    BackHandler(enabled = firstRun) { onBackClick() }

    fun openDatePicker() {
        val p = birthDate.split("-")
        val y = p.getOrNull(0)?.toIntOrNull() ?: 1960
        val m = (p.getOrNull(1)?.toIntOrNull() ?: 1) - 1
        val d = p.getOrNull(2)?.toIntOrNull() ?: 1
        DatePickerDialog(
            context,
            { _, year, month, day ->
                birthDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
            },
            y, m, d,
        ).apply {
            datePicker.maxDate = System.currentTimeMillis() // no future birth dates
        }.show()
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
            if (firstRun) {
                Spacer(Modifier.width(16.dp))
            } else {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }
            Text(
                if (firstRun) "Set Up Your Profile" else "Edit Health Profile",
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrandBlue.copy(alpha = 0.10f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (firstRun)
                        "This helps CareApp and your Emergency Profile. You can change all of it later in Settings."
                    else
                        "Changes here also update your Emergency Profile.",
                    fontSize = 12.sp,
                    color = TextDark,
                )
            }
            Spacer(Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glossySurface(RoundedCornerShape(24.dp), CardWhite)
                    .padding(20.dp),
            ) {
                Field("Name", name, { name = it }, "Your name")

                // ----- Birth date: dropdown button opening a date picker -----
                Text("Date of birth", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glossyFieldWell(RoundedCornerShape(12.dp))
                        .clickable { openDatePicker() }
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = LabelGray, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        formatBirthDate(birthDate).ifBlank { "Select date" },
                        fontSize = 15.sp,
                        color = if (birthDate.isBlank()) PlaceholderGray else TextDark,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = LabelGray)
                }
                Spacer(Modifier.height(14.dp))

                Text("Gender", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("Male", "Female", "Other").forEach { option ->
                        val selected = sex == option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .glossyChip(selected, BrandBlue, RoundedCornerShape(10.dp), unselectedFill = FieldBackground)
                                .clickable { sex = if (selected) "" else option }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                option,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) Color.White else LabelGray,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                Field("Height (cm)", heightCm, { heightCm = it.filter(Char::isDigit).take(3) }, "e.g. 170", KeyboardType.Number)
                Field("Weight (kg)", weightKg, { weightKg = it.filter { c -> c.isDigit() || c == '.' }.take(5) }, "e.g. 68", KeyboardType.Decimal)
                Field("Blood type (optional)", bloodType, { bloodType = it.take(3).uppercase() }, "e.g. O+")

                SectionDivider("Emergency Profile")

                Field("Allergies (optional)", allergies, { allergies = it }, "e.g. Penicillin, Seafood")
                Field("Medical conditions (optional)", conditions, { conditions = it }, "e.g. Diabetic, Hypertension")

                Text("Emergency contacts", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray)
                Spacer(Modifier.height(2.dp))
                Text("People to call from your Emergency Profile", fontSize = 11.sp, color = PlaceholderGray)
                Spacer(Modifier.height(10.dp))

                contacts.forEachIndexed { index, contact ->
                    ContactEditor(
                        contact = contact,
                        onChange = { contacts[index] = it },
                        onRemove = { contacts.removeAt(index) },
                    )
                }
                if (contacts.size < 5) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { contacts.add(EmergencyContact()) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add contact", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                    }
                }
                Spacer(Modifier.height(4.dp))

                error?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = ErrRed, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(16.dp))

                GlossyButton(
                    onClick = {
                        val heightVal = heightCm.toIntOrNull()
                        val weightVal = weightKg.toDoubleOrNull()

                        error = if (firstRun) when {
                            name.isBlank() -> "Enter your name"
                            birthDate.isBlank() -> "Pick your date of birth"
                            sex.isBlank() -> "Pick a gender"
                            heightVal == null || heightVal !in 50..250 -> "Height must be 50–250 cm"
                            weightVal == null || weightVal !in 1.0..400.0 -> "Weight must be 1–400 kg"
                            else -> null
                        } else null

                        if (error != null) return@GlossyButton

                        profileManager.save(
                            HealthProfile(
                                name = name,
                                bloodType = bloodType,
                                heightCm = heightCm,
                                weightKg = weightKg,
                                birthDate = birthDate,
                                sex = sex,
                                allergies = splitTags(allergies),
                                conditions = splitTags(conditions),
                                emergencyContacts = contacts.toList(),
                            )
                        )
                        onSaved()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = BrandBlue,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(if (firstRun) "Continue" else "Save Changes", fontSize = 16.sp, color = Color.White)
                }

                if (firstRun) {
                    Spacer(Modifier.height(10.dp))
                    GlossyButton(
                        onClick = onBackClick,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF5F6673),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Go back", fontSize = 16.sp, color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionDivider(title: String) {
    Spacer(Modifier.height(2.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(FieldBackground))
    Spacer(Modifier.height(16.dp))
    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun ContactEditor(
    contact: EmergencyContact,
    onChange: (EmergencyContact) -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FieldBackground)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Contact", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LabelGray, modifier = Modifier.weight(1f))
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Remove contact", tint = LabelGray, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        MiniField(contact.name, { onChange(contact.copy(name = it)) }, "Name")
        Spacer(Modifier.height(8.dp))
        MiniField(contact.relation, { onChange(contact.copy(relation = it)) }, "Relationship (e.g. Wife, Doctor)")
        Spacer(Modifier.height(8.dp))
        MiniField(contact.phone, { onChange(contact.copy(phone = it.filter { c -> c.isDigit() || c in "+ -" })) }, "Phone number", KeyboardType.Phone)
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun MiniField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().glossyFieldWell(RoundedCornerShape(14.dp)),
        singleLine = true,
        placeholder = { Text(placeholder, color = PlaceholderGray, fontSize = 13.sp) },
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, color = TextDark),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = glossyFieldColors(accent = BrandBlue),
    )
}

@Composable
private fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
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
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = glossyFieldColors(accent = BrandBlue),
    )
    Spacer(Modifier.height(14.dp))
}
