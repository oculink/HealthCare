package com.fyp.healthcare

import com.fyp.healthcare.i18n.Text
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
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
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
 * Step 1 of adding/editing a medication: name, route, description, how many units are
 * taken each time, and how many times a day. "Next" hands a draft [Medication] to [onNext]
 * - the Reminder Times screen fills in the daily schedule and does the actual save.
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

    // Saveable so going to the catalog (or ahead to Reminder Times and back) doesn't
    // reset the form. Held as State objects, not `by`: each field reads its own value
    // inside a small composable, so a keystroke repaints one field instead of
    // recomposing the whole screen and rebuilding every card and shadow on it.
    val name = rememberSaveable(editId) { mutableStateOf(editing?.name ?: "") }
    val description = rememberSaveable(editId) { mutableStateOf(editing?.description ?: "") }
    var routeName by rememberSaveable(editId) { mutableStateOf(MedRoute.of(editing?.route).name) }
    var customMode by rememberSaveable(editId) { mutableStateOf(isEdit) }
    val route = MedRoute.of(routeName)

    LaunchedEffect(pickedName, pickedDescription, pickedRoute, pickedCustom) {
        when {
            pickedCustom -> {
                name.value = ""
                description.value = ""
                routeName = MedRoute.OTHER.name
                customMode = true
                onPickConsumed()
            }
            pickedName != null -> {
                name.value = pickedName
                description.value = pickedDescription.orEmpty()
                routeName = MedRoute.of(pickedRoute).name
                customMode = true
                onPickConsumed()
            }
        }
    }

    // Saveable so picking a different medication (which leaves this screen for the
    // catalog) doesn't wipe numbers the user has already typed.
    val amount = rememberSaveable(editId) {
        mutableStateOf(editing?.amount?.takeIf { it > 0 }?.toString() ?: "")
    }
    val consumes = rememberSaveable(editId) {
        mutableStateOf((editing?.consumesPerDayOrDefault ?: 1).toString())
    }
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
                Text("Tell us about this medication", fontSize = 12.sp, color = LabelGray)
                Spacer(Modifier.height(20.dp))

                Text("Medication Name", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray)
                Spacer(Modifier.height(6.dp))
                if (customMode) {
                    MedicationNameField(name) { name.value = it }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Choose from medication list",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        modifier = Modifier.clickable(role = Role.Button) { onChooseMedication() },
                    )
                } else {
                    ChosenMedicationRow(name, onClick = onChooseMedication)
                }
                Spacer(Modifier.height(14.dp))

                Text("How it's taken", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray)
                Spacer(Modifier.height(6.dp))
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glossyFieldWell(RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button) { routeMenuOpen = true }
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
                                onClick = { routeName = r.name; routeMenuOpen = false },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                MedField(
                    description, { description.value = it },
                    "Description (optional)", "What it's for, e.g. Blood pressure",
                    Icons.Filled.Description, singleLine = false,
                )

                MedField(
                    amount, { amount.value = it.filter(Char::isDigit) },
                    route.doseLabel, "e.g. 1", Icons.Filled.Numbers, KeyboardType.Number,
                    hint = "How many you take at each reminder.",
                )
                MedField(
                    consumes, { consumes.value = it.filter(Char::isDigit) },
                    "Consumes per day", "e.g. 4", Icons.Filled.Repeat, KeyboardType.Number,
                    hint = "How many times a day you take it. We'll space the reminders automatically.",
                )

                DosePreview(amount, consumes, route)

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
                        val amt = amount.value.toIntOrNull()
                        val cons = consumes.value.toIntOrNull()
                        val medName = name.value
                        val medDescription = description.value
                        errorMessage = when {
                            medName.isBlank() -> "Enter a medication name"
                            amt == null || amt !in 1..99 ->
                                "${route.doseLabel} must be a number between 1 and 99"
                            cons == null || cons !in 1..MAX_CONSUMES_PER_DAY ->
                                "Consumes per day must be a number between 1 and $MAX_CONSUMES_PER_DAY"
                            else -> null
                        }
                        if (errorMessage != null) return@GlossyButton

                        // Changing the daily count makes the old times meaningless, so let
                        // the next screen rebuild them from the automatic defaults.
                        val countChanged = isEdit &&
                            cons != editing?.consumesPerDayOrDefault

                        onNext(
                            Medication(
                                id = editing?.id ?: medManager.nextId(),
                                name = medName.trim(),
                                strength = editing?.strength.orEmpty(),
                                amount = amt!!,
                                schedule = if (countChanged) emptyMap()
                                else editing?.schedule ?: emptyMap(),
                                log = editing?.log ?: emptyMap(),
                                description = medDescription.trim(),
                                route = route.name,
                                consumesPerDay = cons!!,
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
private fun MedicationNameField(value: State<String>, onValueChange: (String) -> Unit) {
    // Read here, so typing invalidates this field's scope instead of the whole screen.
    OutlinedTextField(
        value = value.value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().glossyFieldWell(RoundedCornerShape(14.dp)),
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, color = TextDark),
        placeholder = { Text("e.g. Amlodipine", color = PlaceholderGray) },
        leadingIcon = { Icon(Icons.Filled.Medication, contentDescription = null, tint = LabelGray, modifier = Modifier.padding(start = 12.dp)) },
        shape = RoundedCornerShape(14.dp),
        colors = glossyFieldColors(accent = BrandBlue),
    )
}

/** The catalogue picker's read-only display row; reads the name in its own scope. */
@Composable
private fun ChosenMedicationRow(name: State<String>, onClick: () -> Unit) {
    val picked = name.value
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glossyFieldWell(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button) { onClick() }
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Medication, contentDescription = null, tint = LabelGray, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            picked.ifBlank { "Choose medication" },
            fontSize = 15.sp,
            color = if (picked.isBlank()) PlaceholderGray else TextDark,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = LabelGray)
    }
}

/**
 * The live "1 tablet × 4 times a day" summary. Reads the two counts in its own scope, so
 * typing in the amount field re-runs this little block rather than the whole form.
 */
@Composable
private fun DosePreview(amount: State<String>, consumes: State<String>, route: MedRoute) {
    val amountPreview = amount.value.toIntOrNull()
    val consumesPreview = consumes.value.toIntOrNull()
    if (amountPreview != null && amountPreview > 0 &&
        consumesPreview != null && consumesPreview > 0
    ) {
        val noun = if (amountPreview == 1) route.doseNoun else "${route.doseNoun}s"
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BrandBlue.copy(alpha = 0.08f))
                .padding(12.dp),
        ) {
            Text(
                "$amountPreview $noun each time × $consumesPreview times a day",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "= ${amountPreview * consumesPreview} $noun a day in total",
                fontSize = 12.sp,
                color = LabelGray,
            )
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun MedField(
    value: State<String>,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    hint: String? = null,
) {
    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray)
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = value.value,
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
    if (hint != null) {
        Spacer(Modifier.height(6.dp))
        Text(hint, fontSize = 12.sp, color = LabelGray)
    }
    Spacer(Modifier.height(14.dp))
}
