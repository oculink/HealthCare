package com.fyp.healthcare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

private val BrandBlue = Color(0xFF2A6DE1)
private val BrandBlueLight = Color(0xFFCFE0FA)
private val CardWhite = Color(0xFFFFFFFF)
private val ScreenBackground = Color(0xFFEFF1F6)
private val TextDark = Color(0xFF1B1D23)
private val LabelGray = Color(0xFF5F6673)
private val GoodGreen = Color(0xFF2E9E6B)
private val MedOrange = Color(0xFFF59E0B)

@Composable
fun MedicationAddedScreen(
    medManager: MedicationManager,
    onAddAnother: () -> Unit,
    onBackToReminders: () -> Unit
) {
    val med = medManager.getAll().lastOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))

        // Orange pill circle
        Box(
            modifier = Modifier.size(140.dp).clip(CircleShape).background(MedOrange.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(110.dp).clip(CircleShape).background(MedOrange),
                contentAlignment = Alignment.Center
            ) {
                Text("💊", fontSize = 44.sp) // TODO: replace with asset image
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "Medication\nAdded!",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(10.dp))

        Text(
            "Your reminder has been set.\nYou'll be notified at the scheduled time.",
            fontSize = 13.sp,
            color = LabelGray,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        // Summary card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardWhite)
                .padding(16.dp)
        ) {
            Text(
                "💊 ${med?.name ?: "--"} ${med?.dosage ?: ""}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(Modifier.height(6.dp))
            Text("⏰ Reminder: ${formatTime12(med?.time ?: "20:00")}", fontSize = 12.sp, color = LabelGray)
            Spacer(Modifier.height(4.dp))
            Text("🔄 Repeat: ${med?.days ?: "--"}", fontSize = 12.sp, color = LabelGray)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(GoodGreen.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("✓ Confirmed", fontSize = 10.sp, color = GoodGreen, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onAddAnother,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
        ) {
            Text("+ Add Another Medication", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = onBackToReminders,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlueLight, contentColor = BrandBlue)
        ) {
            Text("Back to Reminders", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}