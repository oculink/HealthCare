package com.fyp.healthcare

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fyp.healthcare.ui.theme.themed

/**
 * EMERGENCY PROFILE
 *
 * A single at-a-glance screen for a first responder / bystander: who the patient is,
 * the vitals that matter (blood type, weight, height), allergies, conditions, current
 * medications, and one-tap dialling for the patient's emergency contacts + 999.
 *
 * Everything here is read from the health profile (onboarding + Edit Profile) and the
 * medication list — no radar data. Reached from the Home "Emergency" quick action.
 */

private val BrandBlue = Color(0xFF2A6DE1)
private val CriticalRed = Color(0xFFE23539)
private val CallGreen = Color(0xFF1FA971)
private val Amber = Color(0xFFE08600)

private val CardWhite: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val ScreenBackground: Color @Composable get() = themed(Color(0xFFEFF1F6), Color(0xFF121316))
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
private val ChipRedBg: Color @Composable get() = themed(Color(0xFFFFE5E5), Color(0xFF3A2323))
private val ChipAmberBg: Color @Composable get() = themed(Color(0xFFFFF0DB), Color(0xFF3A3323))
private val CallPillBg: Color @Composable get() = themed(Color(0xFFE0F8ED), Color(0xFF1F3A30))

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmergencyProfileScreen(
    userManager: UserManager,
    profileManager: ProfileManager,
    medManager: MedicationManager,
    onBackClick: () -> Unit,
    onEditProfile: () -> Unit,
) {
    val context = LocalContext.current
    val account = remember { userManager.currentAccount() }
    val profile = remember { profileManager.get() }
    val meds = remember { medManager.getAll() }
    val name = profile.name.ifBlank { account?.name ?: "—" }

    fun dial(number: String) {
        val digits = number.filter { it.isDigit() || it == '+' }
        if (digits.isBlank()) return
        runCatching {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits")))
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(ScreenBackground)) {

        // ===== Header =====
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
                "Emergency Profile",
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

            // ===== Patient info =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardWhite)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AccountAvatar(
                    photoUrl = account?.photoUrl,
                    initials = profileInitials(name),
                    size = 64.dp,
                    background = BrandBlue,
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(Modifier.height(3.dp))
                    Text(patientLine(profile), fontSize = 13.sp, color = LabelGray)
                    account?.email?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(2.dp))
                        Text(it, fontSize = 11.sp, color = LabelGray.copy(alpha = 0.7f))
                    }
                }
            }

            // ===== Critical vitals =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardWhite)
                    .padding(vertical = 16.dp, horizontal = 8.dp),
            ) {
                VitalCell(Icons.Filled.Bloodtype, CriticalRed, value(profile.bloodType), "Blood Type", Modifier.weight(1f))
                VitalCell(Icons.Filled.MonitorWeight, BrandBlue, profile.weightKg.ifBlank { "—" }.let { if (it == "—") it else "$it kg" }, "Weight", Modifier.weight(1f))
                VitalCell(Icons.Filled.Height, BrandBlue, profile.heightCm.ifBlank { "—" }.let { if (it == "—") it else "$it cm" }, "Height", Modifier.weight(1f))
            }

            // ===== Allergies =====
            Section("Allergies") {
                if (profile.allergies.isEmpty()) {
                    EmptyLine("No known allergies recorded")
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        profile.allergies.forEach { Chip("⚠  $it", CriticalRed, ChipRedBg) }
                    }
                }
            }

            // ===== Medical conditions =====
            Section("Medical Conditions") {
                if (profile.conditions.isEmpty()) {
                    EmptyLine("No conditions recorded")
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        profile.conditions.forEach { Chip(it, Amber, ChipAmberBg) }
                    }
                }
            }

            // ===== Current medications =====
            Section("Current Medications") {
                if (meds.isEmpty()) {
                    EmptyLine("No medications added")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        meds.forEach { med ->
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(med.medRoute.icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("${med.name} · ${med.dosageText}", fontSize = 13.sp, color = TextDark)
                                    if (med.description.isNotBlank()) {
                                        Text(med.description, fontSize = 11.sp, color = LabelGray)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ===== Emergency contacts =====
            Section("Emergency Contacts") {
                if (profile.emergencyContacts.isEmpty()) {
                    EmptyLine("No contacts added")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Add contact",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        modifier = Modifier.clickable { onEditProfile() },
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        profile.emergencyContacts.forEach { c ->
                            ContactRow(c) { dial(c.phone) }
                        }
                    }
                }
            }

            // ===== Emergency call =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CriticalRed)
                    .clickable { dial("999") }
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Emergency, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("EMERGENCY CALL", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(2.dp))
                Text("Dials emergency services (999)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

// ===== pieces =====

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardWhite)
                .padding(16.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun VitalCell(icon: ImageVector, tint: Color, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark, textAlign = TextAlign.Center)
        Text(label, fontSize = 10.sp, color = LabelGray, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Chip(text: String, fg: Color, bg: Color) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

@Composable
private fun EmptyLine(text: String) {
    Text(text, fontSize = 13.sp, color = LabelGray)
}

@Composable
private fun ContactRow(contact: EmergencyContact, onCall: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(BrandBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                contact.relation.ifBlank { "Contact" }.let { r ->
                    if (contact.name.isBlank()) r else "${contact.name} ($r)"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark,
            )
            if (contact.phone.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text("📞  ${contact.phone}", fontSize = 12.sp, color = LabelGray)
            }
        }
        if (contact.phone.isNotBlank()) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(CallPillBg)
                    .clickable { onCall() }
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Call, contentDescription = null, tint = CallGreen, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Call", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CallGreen)
            }
        }
    }
}

private fun value(s: String): String = s.trim().ifBlank { "—" }

private fun patientLine(p: HealthProfile): String {
    val bits = buildList {
        p.age?.let { add("Age: $it") }
        p.sex.trim().takeIf { it.isNotEmpty() }?.let { add(it) }
    }
    return if (bits.isEmpty()) "Age & sex not set" else bits.joinToString("  ·  ")
}
