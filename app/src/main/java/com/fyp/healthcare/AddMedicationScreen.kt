package com.fyp.healthcare

import com.fyp.healthcare.ui.theme.appBackground
import com.fyp.healthcare.ui.theme.glossyTopBar
import com.fyp.healthcare.ui.theme.glossyFieldColors
import com.fyp.healthcare.ui.theme.glossyFieldWell
import com.fyp.healthcare.ui.theme.GlossyButton
import com.fyp.healthcare.ui.theme.glossySurface
import com.fyp.healthcare.ui.theme.glossyChip
import com.fyp.healthcare.ui.theme.glossyBadge
import com.fyp.healthcare.ui.theme.themed
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

private val BrandBlue = Color(0xFF2A6DE1)
private val CardWhite: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val ScreenBackground: Color @Composable get() = themed(Color(0xFFEFF1F6), Color(0xFF121316))
private val FieldBackground: Color @Composable get() = themed(Color(0xFFEDEFF4), Color(0xFF262730))
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
private val PlaceholderGray: Color @Composable get() = themed(Color(0xFFA6ACB8), Color(0xFF6A7079))

/**
 * Step 1 of adding/editing a medication: name, route, strength, amount, description.
 * "Next" hands a draft [Medication] (schedule still empty) to [onNext] — the Reminder
 * Times screen fills in the weekly schedule and does the actual save.
 *
 * The medication name can be chosen from [MedicationCatalog] via the picker screen
 * ([onChooseMedication]); its result comes back through [pickedName] / [pickedDescription]
 * / [pickedCustom], which the host clears via [onPickConsumed] once applied.
 */
@Composable
fun AddMedicationScreen(
    medManager: MedicationManager,
    editId: Long?,
    onBackClick: () -> Unit,
    onNext: (Medication) -> Unit,
    onChooseMedication: () -> Unit = {},
    pickedName: String? = null,
    pickedDescription: String? = null,
    pickedRoute: String? = null,
    pickedCustom: Boolean = false,
    onPickConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val editing = remember(editId) { editId?.let { medManager.get(it) } }
    val isEdit = editing != null

    var name by remember { mutableStateOf(editing?.name ?: "") }
    var description by remember { mutableStateOf(editing?.description ?: "") }
    var route by remember { mutableStateOf(MedRoute.of(editing?.route)) }
    var customMode by remember { mutableStateOf(isEdit) }

    LaunchedEffect(pickedName, pickedDescription, pickedRoute, pickedCustom) {
        when {
            pickedCustom -> {
                name = ""
                description = ""
                route = MedRoute.OTHER
                customMode = true
                onPickConsumed()
            }
            pickedName != null -> {
                name = pickedName
                description = pickedDescription.orEmpty()
                route = MedRoute.of(pickedRoute)
                customMode = true
                onPickConsumed()
            }
        }
    }

    var strength by remember { mutableStateOf(editing?.strength ?: "") }
    var amount by remember { mutableStateOf(editing?.amount?.takeIf { it > 0 }?.toString() ?: "") }
    var routeMenuOpen by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                if (isEdit) "Edit Medication" else "Add Medication",
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
                Text("Medication Details", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(Modifier.height(4.dp))
                Text("Fill in the details for your reminder", fontSize = 12.sp, color = LabelGray)
                Spacer(Modifier.height(20.dp))

                Text("Medication Name", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray)
                Spacer(Modifier.height(6.dp))
                if (customMode) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth().glossyFieldWell(RoundedCornerShape(14.dp)),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, color = TextDark),
                        placeholder = { Text("e.g. Amlodipine", color = PlaceholderGray) },
                        leadingIcon = { Icon(Icons.Filled.Medication, contentDescription = null, tint = LabelGray, modifier = Modifier.padding(start = 12.dp)) },
                        shape = RoundedCornerShape(14.dp),
                        colors = glossyFieldColors(accent = BrandBlue),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Choose from medication list",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        modifier = Modifier.clickable { onChooseMedication() },
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glossyFieldWell(RoundedCornerShape(12.dp))
                            .clickable { onChooseMedication() }
                            .padding(horizontal = 12.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Medication, contentDescription = null, tint = LabelGray, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            name.ifBlank { "Choose medication" },
                            fontSize = 15.sp,
                            color = if (name.isBlank()) PlaceholderGray else TextDark,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = LabelGray)
                    }
                }
                Spacer(Modifier.height(14.dp))

                Text("How it's taken", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray)
                Spacer(Modifier.height(6.dp))
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glossyFieldWell(RoundedCornerShape(12.dp))
                            .clickable { routeMenuOpen = true }
                            .padding(horizontal = 12.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(route.icon, contentDescription = null, tint = LabelGray, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(route.label, fontSize = 15.sp, color = TextDark, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = LabelGray)
                    }
                    DropdownMenu(expanded = routeMenuOpen, onDismissRequest = { routeMenuOpen = false }) {
                        MedRoute.entries.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r.label) },
                                leadingIcon = { Icon(r.icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                onClick = { route = r; routeMenuOpen = false },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                MedField(
                    description, { description = it },
                    "Description (optional)", "What it's for, e.g. Blood pressure",
                    Icons.Filled.Description, singleLine = false,
                )

                val strengthLabel = when {
                    route == MedRoute.OTHER -> "Strength / amount (optional)"
                    route.strengthUnit.isEmpty() -> "Strength (optional)"
                    else -> "Strength (${route.strengthUnit})"
                }
                val strengthHint = when (route) {
                    MedRoute.ORAL, MedRoute.SUBLINGUAL, MedRoute.RECTAL -> "e.g. 500"
                    MedRoute.INHALED, MedRoute.NASAL -> "e.g. 100"
                    MedRoute.TRANSDERMAL -> "e.g. 25"
                    MedRoute.TOPICAL, MedRoute.OPHTHALMIC -> "e.g. 0.5"
                    MedRoute.INJECTABLE -> "e.g. 100"
                    MedRoute.OTHER -> "e.g. 5 mg, 1 sachet"
                }
                MedField(
                    strength,
                    { strength = if (route == MedRoute.OTHER) it else it.filter { c -> c.isDigit() || c == '.' } },
                    strengthLabel, strengthHint, Icons.Filled.Medication,
                    if (route == MedRoute.OTHER) KeyboardType.Text else KeyboardType.Decimal,
                )
                MedField(
                    amount, { amount = it.filter(Char::isDigit) },
                    route.doseLabel, "e.g. 1", Icons.Filled.Numbers, KeyboardType.Number,
                )

                Spacer(Modifier.height(2.dp))

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
                        val amt = amount.toIntOrNull()
                        val strengthOk = strength.isBlank() ||
                            route == MedRoute.OTHER ||
                            (strength.toDoubleOrNull()?.let { it > 0.0 } == true)
                        errorMessage = when {
                            name.isBlank() -> "Enter a medication name"
                            !strengthOk -> "Strength must be a positive number"
                            amt == null || amt !in 1..99 -> "${route.doseLabel} must be a number between 1 and 99"
                            else -> null
                        }
                        if (errorMessage != null) return@GlossyButton

                        onNext(
                            Medication(
                                id = editing?.id ?: medManager.nextId(),
                                name = name.trim(),
                                strength = strength.trim(),
                                amount = amt!!,
                                schedule = editing?.schedule ?: emptyMap(),
                                log = editing?.log ?: emptyMap(),
                                description = description.trim(),
                                route = route.name,
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = BrandBlue,
                ) {
                    Text("Next", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White)
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
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, color = TextDark),
        placeholder = { Text(placeholder, color = PlaceholderGray) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = LabelGray, modifier = Modifier.padding(start = 12.dp)) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = glossyFieldColors(accent = BrandBlue),
    )
    Spacer(Modifier.height(14.dp))
}
