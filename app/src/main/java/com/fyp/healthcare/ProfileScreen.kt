package com.fyp.healthcare

import com.fyp.healthcare.ui.theme.AppTheme
import com.fyp.healthcare.ui.theme.ThemeMode
import com.fyp.healthcare.ui.theme.themed
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
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
private val AlertRed = Color(0xFFD32F2F)

@Composable
fun ProfileScreen(
    userManager: UserManager,
    profileManager: ProfileManager,
    activity: ActivityDataManager,
    onEditProfile: () -> Unit,
    onOpenFamilyCaregiver: () -> Unit,
    onSignOut: () -> Unit,
    onBackClick: () -> Unit,
) {
    val account = userManager.currentAccount()
    val email = account?.email

    var refresh by remember { mutableStateOf(0) }
    val dataVersion = Session.dataVersion
    val profile = remember(refresh, dataVersion) { profileManager.get() }
    val stepGoal = remember(refresh, dataVersion) { activity.stepGoal() }

    val linkedCaretakerCount by produceState(
        initialValue = 0,
        key1 = dataVersion,
        key2 = Session.controlledPatientUid,
    ) {
        value = if (Session.isCaretakerMode) 0
        else runCatching { FamilyLink.linkedCaretakers().size }.getOrDefault(0)
    }
    val familyCaregiverValue = when {
        Session.isCaretakerMode -> Session.controlledPatientName.ifBlank { "Linked" }
        linkedCaretakerCount == 1 -> "1 linked"
        linkedCaretakerCount > 1 -> "$linkedCaretakerCount linked"
        else -> "Set up"
    }
    val name = profile.name.ifBlank { account?.name ?: "Guest" }

    val context = LocalContext.current
    var showGoalDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
    ) {
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
                "Profile",
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (Session.isCaretakerMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BrandBlue.copy(alpha = 0.10f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.FamilyRestroom,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Viewing ${Session.controlledPatientName.ifBlank { "the patient" }}'s account — " +
                            "changes save to their profile.",
                        fontSize = 12.sp,
                        color = TextDark,
                    )
                }
            }

            // ===== Account =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardWhite)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AccountAvatar(
                    photoUrl = if (Session.isCaretakerMode) Session.controlledPatientPhoto else account?.photoUrl,
                    initials = profileInitials(name),
                    size = 56.dp,
                    background = BrandBlue,
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (Session.isCaretakerMode) "Patient account · managed by you"
                        else email ?: "Not signed in",
                        fontSize = 12.sp,
                        color = LabelGray,
                    )
                }
            }

            // ===== Health profile =====
            SectionLabel("Health Profile")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardWhite)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconCircle(Icons.Filled.MedicalInformation, BrandBlue)
                    Spacer(Modifier.width(12.dp))
                    Text("Medical details", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark, modifier = Modifier.weight(1f))
                    TextButton(onClick = onEditProfile) {
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (profile.isBlank) "Add" else "Edit", fontSize = 13.sp, color = BrandBlue, fontWeight = FontWeight.Bold)
                    }
                }

                if (profile.isBlank) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Add your date of birth, gender, height and weight. " +
                            "The Emergency Profile screen will use these.",
                        fontSize = 12.sp,
                        color = LabelGray,
                    )
                } else {
                    Spacer(Modifier.height(10.dp))
                    InfoRow("Date of birth", formatBirthDate(profile.birthDate).ifBlank { "—" })
                    InfoRow("Age", profile.age?.let { "$it" } ?: "—")
                    InfoRow("Gender", dash(profile.sex))
                    InfoRow("Height", if (profile.heightCm.isNotBlank()) "${profile.heightCm} cm" else "—")
                    InfoRow("Weight", if (profile.weightKg.isNotBlank()) "${profile.weightKg} kg" else "—")
                    InfoRow("Blood type", dash(profile.bloodType))
                }
            }

            // ===== Emergency contacts =====
            SectionLabel("Emergency Contacts")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardWhite)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconCircle(Icons.Filled.ContactPhone, BrandBlue)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val contacts = profile.emergencyContacts
                        if (contacts.isEmpty()) {
                            Text("No contacts added", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            Spacer(Modifier.height(2.dp))
                            Text("Add people to call in an emergency", fontSize = 12.sp, color = LabelGray)
                        } else {
                            Text(
                                "${contacts.size} contact${if (contacts.size == 1) "" else "s"}",
                                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                contacts.joinToString(", ") { it.name.ifBlank { it.relation } },
                                fontSize = 12.sp, color = LabelGray,
                            )
                        }
                    }
                    TextButton(onClick = onEditProfile) {
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (profile.emergencyContacts.isEmpty()) "Add" else "Edit", fontSize = 13.sp, color = BrandBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ===== Connections =====
            SectionLabel("Connections")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardWhite),
            ) {
                NavRow(
                    Icons.Filled.FamilyRestroom,
                    "Family Caregiver",
                    familyCaregiverValue,
                    onClick = onOpenFamilyCaregiver,
                )
                Divider()
                NavRow(Icons.Filled.Radar, "mmWave Radar Sensor", "Not connected")
            }

            // ===== Preferences =====
            SectionLabel("Preferences")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardWhite),
            ) {
                NavRow(
                    Icons.AutoMirrored.Filled.DirectionsWalk,
                    "Daily step goal",
                    "%,d steps".format(stepGoal),
                    onClick = { showGoalDialog = true },
                )
                Divider()
                NavRow(
                    Icons.Filled.DarkMode,
                    "Appearance",
                    themeLabel(AppTheme.mode),
                    onClick = { showThemeDialog = true },
                )
                Divider()
                NavRow(Icons.Filled.Notifications, "Notifications", "On")
                Divider()
                NavRow(Icons.Filled.Straighten, "Units", "Metric (kg, cm)")
            }

            // ===== Sign out =====
            Button(
                onClick = { confirmSignOut = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlertRed.copy(alpha = 0.10f),
                    contentColor = AlertRed,
                ),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.Bold)
            }

            Text(
                "CareApp · v1.0 (FYP build)",
                fontSize = 11.sp,
                color = LabelGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
        }
    }

    if (showGoalDialog) {
        StepGoalDialog(
            current = stepGoal,
            onDismiss = { showGoalDialog = false },
            onConfirm = {
                activity.setStepGoal(it)
                showGoalDialog = false
                refresh++
            },
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Appearance") },
            text = {
                Column {
                    ThemeMode.entries.forEach { m ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    AppTheme.setMode(context, m)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = AppTheme.mode == m, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(themeLabel(m), fontSize = 15.sp, color = TextDark)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showThemeDialog = false }) { Text("Close") } },
        )
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("You'll need to sign in again to use CareApp.") },
            confirmButton = {
                TextButton(onClick = { confirmSignOut = false; onSignOut() }) {
                    Text("Sign Out", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun StepGoalDialog(current: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf(current.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily step goal") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(6) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text("Between 1,000 and 50,000") },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { text.toIntOrNull()?.let(onConfirm) },
                enabled = text.toIntOrNull()?.let { it in 1_000..50_000 } == true,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ===== small pieces =====

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LabelGray)
}

@Composable
private fun IconCircle(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier.size(38.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, color = LabelGray, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextDark)
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconCircle(icon, BrandBlue)
        Spacer(Modifier.width(12.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = LabelGray)
        if (onClick != null) {
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = LabelGray, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(1.dp).background(Color(0xFFE5E8EE)))
}

private fun dash(s: String): String = s.trim().ifBlank { "—" }

private fun themeLabel(m: ThemeMode): String = when (m) {
    ThemeMode.SYSTEM -> "System default"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}
