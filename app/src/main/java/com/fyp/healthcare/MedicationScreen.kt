package com.fyp.healthcare

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
private val GoodGreen = Color(0xFF2E9E6B)
private val BadRed = Color(0xFFD32F2F)

@Composable
fun MedicationScreen(
    medManager: MedicationManager,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit
) {
    var refresh by remember { mutableStateOf(0) }

    val meds = remember(refresh) {
        medManager.getAll()
    }

    val takenCount = meds.count { displayStatus(it) == "Taken" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        // your existing UI stays the same
        // ===== Blue top bar =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrandBlue)
                .statusBarsPadding()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "Medication Reminder",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlueDark, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("+ Add", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ===== Today header =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Today — ${SimpleDateFormat("EEE, d MMMM", Locale.getDefault()).format(Date())}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "$takenCount of ${meds.size} taken",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = GoodGreen
                )
            }

            if (meds.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardWhite)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💊", fontSize = 28.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("No medications yet", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LabelGray)
                        Text("Tap + Add to create your first reminder", fontSize = 11.sp, color = LabelGray)
                    }
                }
            } else {
                meds.forEach { med ->
                    MedicationCard(
                        med = med,
                        medManager = medManager,
                        onStatusChanged = { refresh++ }
                    )
                }            }

            // ===== Add New Medication card =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardWhite)
                    .clickable { onAddClick() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(BrandBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = BrandBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Add New Medication", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                    Spacer(Modifier.height(2.dp))
                    Text("Tap to set name, dosage & reminder time", fontSize = 12.sp, color = LabelGray)
                }
            }
        }
    }
}

@Composable
private fun MedicationCard(
    med: Medication,
    medManager: MedicationManager,
    onStatusChanged: () -> Unit
) {
    val status = displayStatus(med)
    val tint = medStatusColor(status)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text("💊", fontSize = 20.sp) // TODO: replace with asset image
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(med.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(Modifier.height(2.dp))
                Text(med.dosage, fontSize = 12.sp, color = LabelGray)
                Spacer(Modifier.height(2.dp))
                Text("🕐 ${formatTime12(med.time)}", fontSize = 11.sp, color = LabelGray)
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Status pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(tint.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        when (status) {
                            "Taken" -> "✓ Taken"
                            "Missed" -> "✗ Missed"
                            "Soon" -> "⏰ Soon"
                            else -> "🕐 Upcoming"
                        },
                        fontSize = 10.sp,
                        color = tint,
                        fontWeight = FontWeight.Medium
                    )
                }
                // Taken / Miss buttons only while still pending
                if (status == "Soon" || status == "Upcoming") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                medManager.setStatus(med.id, "taken")
                                onStatusChanged()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoodGreen.copy(alpha = 0.15f),
                                contentColor = GoodGreen
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("✓ Taken", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                medManager.setStatus(med.id, "missed")
                                onStatusChanged()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BadRed.copy(alpha = 0.15f),
                                contentColor = BadRed
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("✗ Miss", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Pending meds become "Soon" within 1 hour of their time, "Missed" after it passes
private fun displayStatus(med: Medication): String {
    return when (med.status.trim().lowercase()) {
        "taken" -> "Taken"
        "missed" -> "Missed"
        else -> {
            val now = Calendar.getInstance()
            val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

            val p = med.time.trim().split(":")
            val medMin = (p.getOrNull(0)?.toIntOrNull() ?: 0) * 60 +
                    (p.getOrNull(1)?.toIntOrNull() ?: 0)

            when {
                nowMin > medMin -> "Missed"
                medMin - nowMin <= 60 -> "Soon"
                else -> "Upcoming"
            }
        }
    }
}

private fun medStatusColor(status: String): Color = when (status) {
    "Taken" -> GoodGreen
    "Missed" -> BadRed
    "Soon" -> BrandBlue
    else -> LabelGray
}

// Small helper so the blue bar fills behind the status bar (the gap fix)
