package com.fyp.healthcare

import com.fyp.healthcare.ui.theme.themed
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val BrandBlue = Color(0xFF2A6DE1)
private val BrandBlueDark = Color(0xFF1E50C8)
private val CardWhite: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val ScreenBackground: Color @Composable get() = themed(Color(0xFFEFF1F6), Color(0xFF121316))
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))

/**
 * FUTURE ROADMAP (HOME):
 * - Will replace emojis into images/icons (asset images) in the future.
 * - VitalStatus ranges are simplified adult averages — verify/cite medical sources later.
 * - Tapping the blue header card opens the Profile page (sign out lives there now).
 */

@Composable
fun HomeScreen(
    userManager: UserManager,
    profileManager: ProfileManager,
    healthData: HealthDataManager,
    onNavigate: (String) -> Unit
) {
    // Preferred name from onboarding, falling back to the Google account name
    val fullName = profileManager.get().name
        .ifBlank { userManager.currentAccount()?.name ?: "there" }

    // TODO: replace with real data later.
    // This null state will also be reused for the offline state later.
    // Readings now come from the Record Data page (null until first recording —
    // this same null state will be reused for offline later)
    val heartRate = healthData.getHeartRate()
    val bloodPressure = healthData.getBloodPressure()
    val oxygen = healthData.getOxygen()
    val heartStatus = heartRate?.toIntOrNull()?.let { VitalStatus.heartRate(it) }
    val bpStatus = bloodPressure?.let { bp ->
        val parts = bp.split("/")
        val sys = parts.getOrNull(0)?.trim()?.toIntOrNull()
        val dia = parts.getOrNull(1)?.trim()?.toIntOrNull()
        if (sys != null && dia != null) VitalStatus.bloodPressure(sys, dia) else null
    }
    val oxygenStatus = oxygen?.toIntOrNull()?.let { VitalStatus.oxygen(it) }
    val steps: String? = null
    val calories: String? = null
    val sleep: String? = null
    val hasData = heartRate != null || bloodPressure != null || oxygen != null


    val part = dayPart()

    Box(modifier = Modifier.fillMaxSize().background(ScreenBackground)) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ===== Header card (tap -> Profile) =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onNavigate("profile") }
                    .background(BrandBlue)
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            greetingIcon(part),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Good $part,",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        fullName,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        todayDate(), // date from the phone
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                // Google account photo, falling back to the name initials
                AccountAvatar(
                    photoUrl = userManager.currentAccount()?.photoUrl,
                    initials = initials(fullName),
                    size = 48.dp,
                    background = BrandBlueDark,
                )
            }

            // ===== Alert banner (only shows while there is no data yet) =====
            if (!hasData) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE9EDF2))
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .width(4.dp)
                            .height(18.dp)
                            .background(LabelGray)
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = LabelGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "No health data yet — record your first readings",
                        color = LabelGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            }

            // ===== Vitals cards (numbers/status are null for now) =====
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VitalCard(Icons.Filled.MonitorHeart, heartRate, "BPM", heartStatus, Color(0xFF2E9E6B), Modifier.weight(1f))
                VitalCard(Icons.Filled.Bloodtype, bloodPressure, "mmHg", bpStatus, Color(0xFFD32F2F), Modifier.weight(1f))
                VitalCard(Icons.Filled.Air, oxygen, "%", oxygenStatus, Color(0xFF2A6DE1), Modifier.weight(1f))
            }

            // ===== Quick Actions =====
            Text("Quick Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Row(
                modifier = Modifier.height(IntrinsicSize.Max), // all cards match the tallest one
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(Icons.Filled.EditNote, "Record Data", Color(0xFF2A6DE1), Modifier.weight(1f)) { onNavigate("record_data") }
                QuickActionCard(Icons.AutoMirrored.Filled.TrendingUp, "View Trends", Color(0xFF2E9E6B), Modifier.weight(1f)) { onNavigate("view_trends") }
                QuickActionCard(Icons.Filled.Medication, "Medication", Color(0xFFFF9800), Modifier.weight(1f)) { onNavigate("medication") }
                QuickActionCard(Icons.Filled.Emergency, "Emergency", Color(0xFFD32F2F), Modifier.weight(1f)) { onNavigate("emergency") }
            }

            // ===== Today's Summary =====
            Text("Today's Summary", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardWhite)
                    .padding(horizontal = 20.dp)
            ) {
                SummaryRow(Icons.AutoMirrored.Filled.DirectionsWalk, "Steps Walked", steps)
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE5E8EE)))
                SummaryRow(Icons.Filled.LocalFireDepartment, "Calories", calories)
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE5E8EE)))
                SummaryRow(Icons.Filled.Bedtime, "Sleep", sleep)
            }

            // ===== Family Caregiver (static placeholder for now) =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardWhite)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFDCE7FB)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("FA", color = BrandBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Family Caregiver", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(Modifier.height(2.dp))
                    Text("● Monitoring Active", fontSize = 12.sp, color = Color(0xFF2E9E6B)) // TODO: real status later
                }
                Button(
                    onClick = { /* TODO */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDCE7FB),
                        contentColor = BrandBlue
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("View", fontSize = 12.sp)
                }
            }
        }
    }
}

// ===== Small building blocks =====

@Composable
private fun VitalCard(
    icon: ImageVector,
    value: String?,
    unit: String,
    status: String?,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Text(value ?: "--", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Text(unit, fontSize = 11.sp, color = LabelGray)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(statusColor(status).copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                status ?: "No data",
                fontSize = 10.sp,
                color = statusColor(status),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight() // stretch to the Row's max height
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).background(tint))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // fill leftover space...
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically) // ...and center content in it
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Text(label, fontSize = 11.sp, color = LabelGray, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SummaryRow(icon: ImageVector, label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = LabelGray, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 13.sp, color = LabelGray, modifier = Modifier.weight(1f))
        Text(value ?: "--", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
    }
}

// ===== Date & time helpers =====

// Morning 4:01am-12:00pm | Afternoon 12:01pm-5:00pm
// Evening 5:01pm-9:00pm | Night 9:01pm-4:00am
private fun dayPart(): String {
    val cal = Calendar.getInstance()
    val minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    return when {
        minutes in (4 * 60 + 1)..(12 * 60) -> "Morning"
        minutes in (12 * 60 + 1)..(17 * 60) -> "Afternoon"
        minutes in (17 * 60 + 1)..(21 * 60) -> "Evening"
        else -> "Night"
    }
}

private fun greetingIcon(part: String): ImageVector = when (part) {
    "Morning" -> Icons.Filled.WbSunny
    "Afternoon" -> Icons.Filled.WbCloudy
    "Evening" -> Icons.Filled.WbTwilight
    else -> Icons.Filled.Bedtime
}

private fun todayDate(): String =
    SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date())

// "Ahmad Rizal Hassan" -> "AR"
private fun initials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

@Composable
private fun statusColor(status: String?): Color = when (status) {
    "Good" -> Color(0xFF2E9E6B)
    "Normal" -> Color(0xFF2A6DE1)
    "Low" -> Color(0xFFFF9800)
    "High" -> Color(0xFFE64A19)
    "Critical" -> Color(0xFFD32F2F)
    else -> LabelGray
}

