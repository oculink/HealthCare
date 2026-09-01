package com.fyp.healthcare

import com.fyp.healthcare.ui.theme.appBackground
import com.fyp.healthcare.ui.theme.GlossyButton
import com.fyp.healthcare.ui.theme.glossySurface
import com.fyp.healthcare.ui.theme.themed
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BrandBlue = Color(0xFF2A6DE1)
private val BrandBlueLight: Color @Composable get() = themed(Color(0xFFCFE0FA), Color(0xFF2A3A57))
private val CardWhite: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val ScreenBackground: Color @Composable get() = themed(Color(0xFFEFF1F6), Color(0xFF121316))
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
private val GoodGreen = Color(0xFF2E9E6B)
private val MedOrange = Color(0xFFF59E0B)

@Composable
fun MedicationAddedScreen(
    medManager: MedicationManager,
    medId: Long,
    onAddAnother: () -> Unit,
    onBackToReminders: () -> Unit
) {
    val med = remember(medId) { medManager.get(medId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .appBackground()
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
                Icon(
                    Icons.Filled.Medication,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(52.dp)
                )
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
                .glossySurface(RoundedCornerShape(20.dp), CardWhite)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(med?.medRoute?.icon ?: Icons.Filled.Medication, contentDescription = null, tint = TextDark, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    med?.name ?: "--",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }
            Spacer(Modifier.height(6.dp))
            SummaryLine(Icons.Filled.LocalPharmacy, med?.dosageText ?: "--")
            Spacer(Modifier.height(4.dp))
            SummaryLine(Icons.Filled.Schedule, med?.scheduleSummary() ?: "--")
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(GoodGreen.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GoodGreen, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Confirmed", fontSize = 10.sp, color = GoodGreen, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        GlossyButton(
            onClick = onAddAnother,
            modifier = Modifier.fillMaxWidth(),
            color = BrandBlue,
        ) {
            Text("+ Add Another Medication", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(10.dp))

        GlossyButton(
            onClick = onBackToReminders,
            modifier = Modifier.fillMaxWidth(),
            color = BrandBlueLight,
            contentColor = BrandBlue,
        ) {
            Text("Back to Reminders", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SummaryLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = LabelGray, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 12.sp, color = LabelGray)
    }
}