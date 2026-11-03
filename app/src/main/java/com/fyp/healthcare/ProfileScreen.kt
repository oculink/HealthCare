package com.fyp.healthcare

import com.fyp.healthcare.i18n.Text
import com.fyp.healthcare.ui.theme.appBackground
import com.fyp.healthcare.ui.theme.glossyTopBar
import com.fyp.healthcare.ui.theme.GlossyButton
import com.fyp.healthcare.ui.theme.glossySurface
import com.fyp.healthcare.i18n.AppLanguage
import com.fyp.healthcare.i18n.Language
import com.fyp.healthcare.i18n.tr
import com.fyp.healthcare.i18n.LanguageChip
import com.fyp.healthcare.ui.theme.AppTheme
import com.fyp.healthcare.ui.theme.Decoration
import com.fyp.healthcare.ui.theme.ThemeMode
import com.fyp.healthcare.ui.theme.themed
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

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
    healthData: HealthDataManager,
    medManager: MedicationManager,
    onEditProfile: () -> Unit,
    onOpenFamilyCaregiver: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMedicationHistory: () -> Unit,
    onSignOut: () -> Unit,
    onAccountDeleted: () -> Unit,
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
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLookDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }
    var notificationsOn by remember { mutableStateOf(NotificationPrefs.isEnabled(context)) }

    // Which sign-in doors this account has. Firebase doesn't expose providers as observable
    // state, so `refresh` re-reads them after a successful connect.
    val googleConnected = remember(refresh) { userManager.isGoogleConnected() }
    val usernameAccount = remember(refresh) { userManager.isUsernameAccount() }
    val connectedGoogleEmail = remember(refresh) { userManager.connectedGoogleEmail() }

    val accountScope = rememberCoroutineScope()
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkPassword by remember { mutableStateOf("") }
    var linkBusy by remember { mutableStateOf(false) }
    var linkError by remember { mutableStateOf<String?>(null) }

    var showPwDialog by remember { mutableStateOf(false) }
    var pwCurrent by remember { mutableStateOf("") }
    var pwNew by remember { mutableStateOf("") }
    var pwConfirm by remember { mutableStateOf("") }
    var pwBusy by remember { mutableStateOf(false) }
    var pwError by remember { mutableStateOf<String?>(null) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteConfirmText by remember { mutableStateOf("") }
    var deletePassword by remember { mutableStateOf("") }
    var deleteBusy by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    /**
     * Tapping the row goes straight to the Google picker. The dialog below only opens when
     * Firebase demands a fresh password (a session from an earlier app run, which is the
     * usual case since the session persists) or when something failed, so the common path is
     * one tap and no reading.
     */
    fun runConnectGoogle(password: String? = null) {
        if (linkBusy) return
        linkBusy = true
        linkError = null
        accountScope.launch {
            when (val result = userManager.connectGoogle(context, password)) {
                is LinkResult.Success -> {
                    showLinkDialog = false
                    linkPassword = ""
                    linkError = null
                    refresh++
                }
                is LinkResult.Cancelled -> Unit
                is LinkResult.NeedsPassword -> {
                    linkError = "Confirm your password to finish connecting."
                    showLinkDialog = true
                }
                is LinkResult.Failed -> {
                    linkError = result.message
                    showLinkDialog = true
                }
            }
            linkBusy = false
        }
    }

    fun submitPasswordChange() {
        if (pwBusy) return
        pwError = when {
            pwCurrent.isBlank() -> "Enter your current password."
            pwNew != pwConfirm -> "The two new passwords don't match."
            else -> null
        }
        if (pwError != null) return
        pwBusy = true
        accountScope.launch {
            when (val result = userManager.changePassword(pwCurrent, pwNew)) {
                is SignInResult.Success -> {
                    showPwDialog = false
                    pwCurrent = ""
                    pwNew = ""
                    pwConfirm = ""
                    pwError = null
                }
                is SignInResult.Failed -> pwError = result.message
                is SignInResult.Cancelled -> Unit
            }
            pwBusy = false
        }
    }

    /**
     * Irreversible, so it asks for the username password AND the word DELETE. A Google-only
     * account has no password to type, and `deleteAccount` knows that.
     */
    fun submitDeleteAccount() {
        if (deleteBusy) return
        deleteError = when {
            deleteConfirmText.trim().uppercase() != "DELETE" -> "Type DELETE to confirm."
            usernameAccount && deletePassword.isBlank() -> "Enter your password."
            else -> null
        }
        if (deleteError != null) return
        deleteBusy = true
        accountScope.launch {
            when (val result = userManager.deleteAccount(deletePassword)) {
                is SignInResult.Success -> {
                    // The user is gone; leave no local trace and drop back to the gate.
                    showDeleteDialog = false
                    onAccountDeleted()
                }
                is SignInResult.Failed -> {
                    deleteError = result.message
                    deleteBusy = false
                }
                is SignInResult.Cancelled -> deleteBusy = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .appBackground(),
    ) {
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
                "Profile",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            // The same switcher as Get Started, so the language can be changed from the
            // settings page without hunting through a menu.
            LanguageChip(
                modifier = Modifier.padding(end = 10.dp),
                onColor = true,
            )
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
                        tr("You're viewing ") +
                            Session.controlledPatientName.ifBlank { tr("the patient") } +
                            tr("'s account · changes save to their profile."),
                        fontSize = 12.sp,
                        color = TextDark,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glossySurface(RoundedCornerShape(20.dp), CardWhite)
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
                        else email?.takeIf { it.isNotBlank() }
                            ?: if (account != null) "CareApp account" else "Not signed in",
                        fontSize = 12.sp,
                        color = LabelGray,
                    )
                }
            }

            SectionLabel("Health Profile")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glossySurface(RoundedCornerShape(20.dp), CardWhite)
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
                    InfoRow("Date of birth", formatBirthDate(profile.birthDate).ifBlank { "-" })
                    InfoRow("Age", profile.age?.let { "$it" } ?: "-")
                    InfoRow("Gender", dash(profile.sex))
                    InfoRow("Height", if (profile.heightCm.isNotBlank()) "${profile.heightCm} cm" else "-")
                    InfoRow("Weight", if (profile.weightKg.isNotBlank()) "${profile.weightKg} kg" else "-")
                    InfoRow("Blood type", dash(profile.bloodType))
                }
            }

            val history = remember(Session.dataVersion) {
                healthData.history().sortedByDescending { it.timestamp }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glossySurface(RoundedCornerShape(20.dp), CardWhite)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconCircle(Icons.Filled.History, BrandBlue)
                    Spacer(Modifier.width(12.dp))
                    Text("Reading history", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark, modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenHistory) {
                        Text(
                            if (history.isEmpty()) "Open" else "See all",
                            fontSize = 13.sp, color = BrandBlue, fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (history.isEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Nothing recorded yet · your readings will show here.",
                        fontSize = 12.sp, color = LabelGray,
                    )
                } else {
                    Spacer(Modifier.height(8.dp))
                    history.take(3).forEach { r ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                formatHistoryStamp(r.timestamp),
                                fontSize = 12.sp, color = LabelGray, modifier = Modifier.weight(1f),
                            )
                            Text(readingSummary(r), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        }
                    }
                }
            }

            val medDoses = remember(Session.dataVersion) { medicationDoseLog(medManager) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glossySurface(RoundedCornerShape(20.dp), CardWhite)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconCircle(Icons.Filled.MedicalServices, BrandBlue)
                    Spacer(Modifier.width(12.dp))
                    Text("Medication history", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark, modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenMedicationHistory) {
                        Text(
                            if (medDoses.isEmpty()) "Open" else "See all",
                            fontSize = 13.sp, color = BrandBlue, fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (medDoses.isEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Nothing logged yet · doses marked Taken or Missed show here.",
                        fontSize = 12.sp, color = LabelGray,
                    )
                } else {
                    Spacer(Modifier.height(8.dp))
                    medDoses.take(3).forEach { d ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                formatHistoryStamp(d.timeMillis),
                                fontSize = 12.sp, color = LabelGray, modifier = Modifier.weight(1f),
                            )
                            Text(medDoseSummary(d), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        }
                    }
                }
            }

            SectionLabel("Emergency Contacts")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glossySurface(RoundedCornerShape(20.dp), CardWhite)
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

            SectionLabel("Connections")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glossySurface(RoundedCornerShape(20.dp), CardWhite),
            ) {
                NavRow(
                    Icons.Filled.FamilyRestroom,
                    "Family Caregiver",
                    familyCaregiverValue,
                    tint = Color(0xFF1FA971),
                    onClick = onOpenFamilyCaregiver,
                )
                Divider()
                NavRow(Icons.Filled.Radar, "mmWave Radar Sensor", "Not connected", tint = Color(0xFF7B4BD6))
            }

            // Grouped as one section because they are the same thing to the user: the ways
            // into this account, and the way out of it.
            SectionLabel("Sign-in and security")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glossySurface(RoundedCornerShape(20.dp), CardWhite),
            ) {
                if (usernameAccount) {
                    NavRow(
                        Icons.Filled.Lock,
                        "Change password",
                        "",
                        tint = Color(0xFF2A6DE1),
                        onClick = { showPwDialog = true },
                    )
                    Divider()
                    // Only username accounts get this row: a Google sign-in already IS Google.
                    if (googleConnected) {
                        NavRow(
                            Icons.Filled.VerifiedUser,
                            "Google account",
                            connectedGoogleEmail ?: "Connected",
                            tint = Color(0xFF1FA971),
                        )
                    } else {
                        NavRow(
                            Icons.Filled.VerifiedUser,
                            "Google account",
                            if (linkBusy) "Waiting for Google…" else "Not linked · tap to add",
                            tint = Color(0xFF2A6DE1),
                            onClick = { runConnectGoogle() },
                        )
                    }
                } else {
                    NavRow(
                        Icons.Filled.VerifiedUser,
                        "Google account",
                        email?.takeIf { it.isNotBlank() } ?: "Connected",
                        tint = Color(0xFF1FA971),
                    )
                }
                Divider()
                NavRow(
                    Icons.Filled.DeleteForever,
                    "Delete account",
                    "Erases this account and its data",
                    tint = AlertRed,
                    onClick = { showDeleteDialog = true },
                )
            }

            SectionLabel("Preferences")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glossySurface(RoundedCornerShape(20.dp), CardWhite),
            ) {
                NavRow(
                    Icons.Filled.Language,
                    "Language",
                    Language.current.displayName,
                    tint = Color(0xFF2A6DE1),
                    onClick = { showLanguageDialog = true },
                )
                Divider()
                NavRow(
                    Icons.AutoMirrored.Filled.DirectionsWalk,
                    "Daily step goal",
                    "%,d steps".format(stepGoal),
                    tint = Color(0xFF2A6DE1),
                    onClick = { showGoalDialog = true },
                )
                Divider()
                NavRow(
                    Icons.Filled.DarkMode,
                    "Dark mode",
                    themeLabel(AppTheme.mode),
                    tint = Color(0xFF37474F),
                    onClick = { showThemeDialog = true },
                )
                Divider()
                NavRow(
                    Icons.Filled.AutoAwesome,
                    "Look and feel",
                    lookLabel(AppTheme.decoration),
                    tint = Color(0xFFEE7B2E),
                    onClick = { showLookDialog = true },
                )
                Divider()
                NavRow(
                    Icons.Filled.Notifications,
                    "Notifications",
                    if (notificationsOn) "On" else "Off",
                    tint = if (notificationsOn) Color(0xFFE23539) else LabelGray,
                    onClick = { showNotificationsDialog = true },
                )
            }

            GlossyButton(
                onClick = { confirmSignOut = true },
                modifier = Modifier.fillMaxWidth(),
                color = themed(Color(0xFFFBE9E9), Color(0xFF3A2626)),
                contentColor = AlertRed,
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.Bold)
            }

            Text(
                "CareApp · v0.9.0 (FYP build · radar integration pending)",
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
            title = { Text("Dark mode") },
            text = {
                Column {
                    ThemeMode.entries.forEach { m ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(role = Role.Button) {
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

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Language") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    AppLanguage.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(role = Role.RadioButton) {
                                    Language.set(context, option)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = Language.current == option, onClick = null)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                option.displayName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextDark,
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showLanguageDialog = false }) { Text("Close") } },
        )
    }

    if (showLookDialog) {
        AlertDialog(
            onDismissRequest = { showLookDialog = false },
            title = { Text("Look and feel") },
            text = {
                // Stays open after each change, so the screen behind the dialog updates while
                // the person is choosing rather than closing on the first tap.
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Decoration.entries.forEach { value ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(role = Role.RadioButton) { AppTheme.setDecoration(context, value) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = AppTheme.decoration == value, onClick = null)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(lookLabel(value), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextDark)
                                Text(lookHint(value), fontSize = 12.sp, color = LabelGray, lineHeight = 16.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    // The page's own Divider() is inset for the list rows; inside a dialog a
                    // full-width rule reads better.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(themed(Color(0xFFE5E8EE), Color(0xFF2A2C33))),
                    )
                    Spacer(Modifier.height(4.dp))
                    SwitchRow("Card shadows", AppTheme.shadows) { AppTheme.setShadows(context, it) }
                    SwitchRow("Patterned background", AppTheme.backgroundPattern) { AppTheme.setBackgroundPattern(context, it) }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showLookDialog = false }) { Text("Close") } },
        )
    }

    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = { Text("Notifications") },
            text = {
                Column {
                    Text(
                        "Medication and appointment reminders. Turning this off silences both " +
                            "on this device.",
                        fontSize = 13.sp,
                        color = LabelGray,
                    )
                    Spacer(Modifier.height(10.dp))
                    SwitchRow("Allow notifications", notificationsOn) { enabled ->
                        notificationsOn = enabled
                        NotificationPrefs.setEnabled(context, enabled)
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showNotificationsDialog = false }) { Text("Close") } },
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

    // Fallback, not the entry point: this opens only when Firebase wants a fresh password or
    // a connect attempt failed. Tapping the row goes straight to the Google picker.
    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { if (!linkBusy) showLinkDialog = false },
            title = { Text("Confirm your password") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        "CareApp needs your password before it can attach Google to \"${account?.name}\".",
                        fontSize = 14.sp,
                        color = TextDark,
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = linkPassword,
                        onValueChange = {
                            linkPassword = it
                            linkError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true,
                        enabled = !linkBusy,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { runConnectGoogle(linkPassword.ifBlank { null }) },
                        ),
                    )
                    linkError?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(it, color = AlertRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { runConnectGoogle(linkPassword.ifBlank { null }) },
                    enabled = !linkBusy,
                ) {
                    Text(
                        if (linkBusy) "Connecting…" else "Connect Google",
                        color = BrandBlue,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }, enabled = !linkBusy) { Text("Cancel") }
            },
        )
    }

    if (showPwDialog) {
        AlertDialog(
            onDismissRequest = { if (!pwBusy) showPwDialog = false },
            title = { Text("Change password") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        "Your username stays the same. You'll need the new password next time you sign in.",
                        fontSize = 13.sp,
                        color = LabelGray,
                    )
                    Spacer(Modifier.height(14.dp))
                    PasswordField(
                        label = "Current password",
                        value = pwCurrent,
                        onValueChange = {
                            pwCurrent = it
                            pwError = null
                        },
                        enabled = !pwBusy,
                    )
                    Spacer(Modifier.height(12.dp))
                    PasswordField(
                        label = "New password",
                        value = pwNew,
                        onValueChange = {
                            pwNew = it
                            pwError = null
                        },
                        enabled = !pwBusy,
                    )
                    Spacer(Modifier.height(12.dp))
                    PasswordField(
                        label = "Repeat new password",
                        value = pwConfirm,
                        onValueChange = {
                            pwConfirm = it
                            pwError = null
                        },
                        enabled = !pwBusy,
                        imeAction = ImeAction.Done,
                        onSubmit = { submitPasswordChange() },
                    )
                    pwError?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(it, color = AlertRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { submitPasswordChange() }, enabled = !pwBusy) {
                    Text(
                        if (pwBusy) "Saving…" else "Change password",
                        color = BrandBlue,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showPwDialog = false }, enabled = !pwBusy) { Text("Cancel") }
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!deleteBusy) showDeleteDialog = false },
            title = { Text("Delete this account?") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        "This erases the profile, readings, medications and appointments for " +
                            "\"${account?.name}\". It can't be undone.",
                        fontSize = 14.sp,
                        color = TextDark,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Any caregiver linked to it stops seeing this account. Reports you've already saved or shared stay on the devices holding them.",
                        fontSize = 12.sp,
                        color = LabelGray,
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = deleteConfirmText,
                        onValueChange = {
                            deleteConfirmText = it
                            deleteError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Type DELETE") },
                        singleLine = true,
                        enabled = !deleteBusy,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                        ),
                    )
                    if (usernameAccount) {
                        Spacer(Modifier.height(12.dp))
                        PasswordField(
                            label = "Your password",
                            value = deletePassword,
                            onValueChange = {
                                deletePassword = it
                                deleteError = null
                            },
                            enabled = !deleteBusy,
                            imeAction = ImeAction.Done,
                            onSubmit = { submitDeleteAccount() },
                        )
                    }
                    deleteError?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(it, color = AlertRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { submitDeleteAccount() }, enabled = !deleteBusy) {
                    Text(
                        if (deleteBusy) "Deleting…" else "Delete account",
                        color = AlertRed,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, enabled = !deleteBusy) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    imeAction: ImeAction = ImeAction.Next,
    onSubmit: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
    )
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
                supportingText = { Text("Between 1,000 and 50,000 steps") },
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


@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LabelGray)
}

@Composable
private fun IconCircle(icon: ImageVector, tint: Color) {
    com.fyp.healthcare.ui.theme.AppIconBadge(icon, tint, size = 38.dp, iconSize = 20.dp)
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
private fun NavRow(
    icon: ImageVector,
    title: String,
    value: String,
    tint: Color = BrandBlue,
    onClick: (() -> Unit)? = null,
) {
    // The value is measured before the weighted title, so a long one ("Erases this account and
    // its data", a long email, the Malay translation) used to eat the whole row and squeeze the
    // label down to one character per line on a narrow screen. Measure both strings as they will
    // actually be rendered and only put them side by side when the pair genuinely fits; otherwise
    // the value drops under the title, where both keep a full-width line.
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val titleStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
    val valueStyle = TextStyle(fontSize = 13.sp)
    val titleWidth = with(density) { measurer.measure(AnnotatedString(tr(title)), titleStyle).size.width.toDp() }
    val valueWidth = with(density) { measurer.measure(AnnotatedString(tr(value)), valueStyle).size.width.toDp() }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Row padding + icon + the two 12dp gaps + the chevron (6dp gap + 18dp glyph).
        val fixedWidth = 32.dp + 38.dp + 12.dp + 12.dp + (if (onClick != null) 24.dp else 0.dp)
        val sideBySide = titleWidth + valueWidth + 8.dp <= maxWidth - fixedWidth

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconCircle(icon, tint)
            Spacer(Modifier.width(12.dp))
            if (sideBySide) {
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                // The translations run longer than the English, so keep a gap between the label and
                // its value instead of letting the two run together when the title fits on one line.
                Spacer(Modifier.width(12.dp))
                Text(
                    value,
                    fontSize = 13.sp,
                    color = LabelGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        value,
                        fontSize = 13.sp,
                        color = LabelGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (onClick != null) {
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = LabelGray, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(themed(Color(0xFFE5E8EE), Color(0xFF2A2C33))),
    )
}

private fun dash(s: String): String = s.trim().ifBlank { "-" }

private fun themeLabel(m: ThemeMode): String = when (m) {
    ThemeMode.SYSTEM -> "System default"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private fun lookLabel(d: Decoration): String = when (d) {
    Decoration.CALM -> "Calm"
    Decoration.STANDARD -> "Standard"
}

private fun lookHint(d: Decoration): String = when (d) {
    Decoration.CALM -> "Flat colours and thin edges, no texture or depth"
    Decoration.STANDARD -> "Soft gradients and gentle shadows"
}

/** A switch whose whole row is the tap target, with the label read out beside it. */
@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Switch) { onChange(!checked) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 15.sp, color = TextDark, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun formatHistoryStamp(millis: Long): String =
    SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(millis))

/**
 * The values a reading actually holds, joined for the compact row on this page. Only the three
 * vitals Home also shows - a longer list would just wrap in the narrow space beside the date.
 */
private fun readingSummary(r: HealthDataManager.Reading): String =
    listOfNotNull(
        r.heartRate.trim().takeIf { it.isNotEmpty() }?.let { "$it bpm" },
        r.bloodPressure.trim().takeIf { it.isNotEmpty() },
        r.oxygen.trim().takeIf { it.isNotEmpty() }?.let { "$it%" },
    ).joinToString("  ·  ").ifBlank { "-" }

/** The one-line version of a logged dose, for the compact row on this page. */
private fun medDoseSummary(d: MedicationDose): String =
    "${d.medication.name} · ${if (d.taken) "Taken" else "Missed"}"
