package com.fyp.healthcare

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fyp.healthcare.ui.theme.themed

private val ActiveGreen = Color(0xFF2E9E6B)
private val CardWhite: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
private val IdleGray: Color @Composable get() = themed(Color(0xFF9AA1AC), Color(0xFF6A7079))

/**
 * The "Monitoring" status strip at the bottom of Home — mirrors the Figma design.
 *
 *  - Caretaker mode → green, names the patient being managed, with a way back.
 *  - Patient with a linked caregiver → green "Monitoring active" + the caregiver's name
 *    and photo. No action (status only).
 *  - Patient with nobody linked → grey "Monitoring not active".
 */
@Composable
fun MonitoringStatusCard(
    caretakers: List<FamilyLink.CaretakerInfo>,
    modifier: Modifier = Modifier,
) {
    when {
        Session.isCaretakerMode -> CaretakerModeCard(modifier)
        caretakers.isNotEmpty() -> ActiveCard(caretakers, modifier)
        else -> InactiveCard(modifier)
    }
}

@Composable
private fun CaretakerModeCard(modifier: Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ActiveGreen.copy(alpha = 0.12f))
            .border(1.dp, ActiveGreen.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Dot()
        Spacer(Modifier.width(10.dp))
        AccountAvatar(
            photoUrl = Session.controlledPatientPhoto,
            initials = profileInitials(Session.controlledPatientName.ifBlank { "P" }),
            size = 36.dp,
            background = ActiveGreen,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Monitoring active", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ActiveGreen)
            Text(
                "Managing ${Session.controlledPatientName.ifBlank { "the patient" }}'s account",
                fontSize = 12.sp,
                color = LabelGray,
            )
        }
    }
}

@Composable
private fun ActiveCard(caretakers: List<FamilyLink.CaretakerInfo>, modifier: Modifier) {
    val first = caretakers.first()
    val extra = caretakers.size - 1
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ActiveGreen.copy(alpha = 0.12f))
            .border(1.dp, ActiveGreen.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Dot()
        Spacer(Modifier.width(10.dp))
        AccountAvatar(
            photoUrl = first.photoUrl,
            initials = profileInitials(first.name),
            size = 36.dp,
            background = ActiveGreen,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Monitoring active", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ActiveGreen)
            Text(
                if (extra > 0) "${first.name} and $extra other${if (extra == 1) "" else "s"}"
                else first.name,
                fontSize = 12.sp,
                color = LabelGray,
            )
        }
    }
}

@Composable
private fun InactiveCard(modifier: Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(IdleGray.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.MonitorHeart, contentDescription = null, tint = IdleGray, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Monitoring not active", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LabelGray)
            Text("No caregiver linked to this account", fontSize = 12.sp, color = LabelGray)
        }
    }
}

@Composable
private fun Dot() {
    Box(
        Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(ActiveGreen),
    )
}
