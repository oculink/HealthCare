package com.fyp.healthcare

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
private val CardWhite = Color(0xFFFFFFFF)
private val ScreenBackground = Color(0xFFEFF1F6)
private val TextDark = Color(0xFF1B1D23)
private val LabelGray = Color(0xFF5F6673)

/**
 * FUTURE ROADMAP (HOME):
 * - Will replace emojis into images/icons (asset images) in the future.
 * - VitalStatus ranges are simplified adult averages — verify/cite medical sources later.
 * - Logout button on top middle is for TESTING — keep it until proper session handling exists.
 */

@Composable
fun HomeScreen(
    userManager: UserManager,
    healthData: HealthDataManager,
    onLogoutClick: () -> Unit,
    onNavigate: (String) -> Unit
) {
    // Name comes from the sign up page
    val fullName = userManager.getLoggedInUser()
        ?.let { userManager.getFullName(it) } ?: "Guest"

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

            // ===== Header card =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(BrandBlue)
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${greetingEmoji(part)}  Good $part,",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$fullName 👋", // TODO: emoji will be replaced with asset image
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
                // Avatar with initials of the name
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(BrandBlueDark),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials(fullName), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
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
                    Text(
                        "⚠️ No health data yet — record your first readings",
                        color = LabelGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            }

            // ===== Vitals cards (numbers/status are null for now) =====
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VitalCard("💚", heartRate, "BPM", heartStatus, Color(0xFF2E9E6B), Modifier.weight(1f))
                VitalCard("🩸", bloodPressure, "mmHg", bpStatus, Color(0xFFD32F2F), Modifier.weight(1f))
                VitalCard("💙", oxygen, "%", oxygenStatus, Color(0xFF2A6DE1), Modifier.weight(1f))
            }

            // ===== Quick Actions =====
            Text("Quick Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Row(
                modifier = Modifier.height(IntrinsicSize.Max), // all cards match the tallest one
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard("📊", "Record Data", Color(0xFF2A6DE1), Modifier.weight(1f)) { onNavigate("record_data") }
                QuickActionCard("📈", "View Trends", Color(0xFF2E9E6B), Modifier.weight(1f)) { onNavigate("view_trends") }
                QuickActionCard("💊", "Medication", Color(0xFFFF9800), Modifier.weight(1f)) { onNavigate("medication") }
                QuickActionCard("🆘", "Emergency", Color(0xFFD32F2F), Modifier.weight(1f)) { onNavigate("emergency") }
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
                SummaryRow("👣", "Steps Walked", steps)
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE5E8EE)))
                SummaryRow("🔥", "Calories", calories)
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE5E8EE)))
                SummaryRow("🌙", "Sleep", sleep)
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

        // ===== Logout button — KEEP on top middle for testing =====
        Button(
            onClick = onLogoutClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 4.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text("Logout", fontSize = 11.sp, color = Color.White)
        }
    }
}

// ===== Small building blocks =====

@Composable
private fun VitalCard(
    emoji: String,
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
            Text(emoji, fontSize = 18.sp) // TODO: replace with asset image
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
    emoji: String,
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
            Text(emoji, fontSize = 22.sp) // TODO: replace with asset image
            Text(label, fontSize = 11.sp, color = LabelGray, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SummaryRow(emoji: String, label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 16.sp) // TODO: replace with asset image
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

private fun greetingEmoji(part: String): String = when (part) {
    "Morning" -> "☀️"
    "Afternoon" -> "🌤️"
    "Evening" -> "🌇"
    else -> "🌙"
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

private fun statusColor(status: String?): Color = when (status) {
    "Good" -> Color(0xFF2E9E6B)
    "Normal" -> Color(0xFF2A6DE1)
    "Low" -> Color(0xFFFF9800)
    "High" -> Color(0xFFE64A19)
    "Critical" -> Color(0xFFD32F2F)
    else -> LabelGray
}

