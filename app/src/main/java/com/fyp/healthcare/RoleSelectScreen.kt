package com.fyp.healthcare

import com.fyp.healthcare.ui.theme.GlossyButton
import com.fyp.healthcare.ui.theme.appBackground
import com.fyp.healthcare.ui.theme.glossyChip
import com.fyp.healthcare.ui.theme.glossySurface
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fyp.healthcare.ui.theme.AppTheme
import com.fyp.healthcare.ui.theme.IconStyle
import com.fyp.healthcare.ui.theme.ThemeMode
import com.fyp.healthcare.ui.theme.themed

private val BrandBlue = Color(0xFF2A6DE1)
private val CardWhite: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val ScreenBackground: Color @Composable get() = themed(Color(0xFFEFF1F6), Color(0xFF121316))
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))

/**
 * Shown once, right after Google sign-in, before onboarding. Splits the two journeys:
 * a Patient uses the app for themselves; a Caretaker links to a patient's account and
 * manages it on their behalf.
 */
@Composable
fun RoleSelectScreen(
    onPatient: () -> Unit,
    onCaretaker: () -> Unit,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    BackHandler { /* no going back past the role choice */ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .appBackground()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(72.dp))
        Text("Who's using CareApp?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(Modifier.height(8.dp))
        Text(
            "Pick the option that fits you. You can change this later in Settings.",
            fontSize = 13.sp,
            color = LabelGray,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))

        RoleCard(
            icon = Icons.Filled.Person,
            title = "I'm the Patient",
            subtitle = "Track my own health, medication and emergency profile.",
            onClick = onPatient,
        )
        Spacer(Modifier.height(16.dp))
        RoleCard(
            icon = Icons.Filled.SupervisorAccount,
            title = "I'm a Caregiver",
            subtitle = "Link to a family member's account with their code and help manage it.",
            onClick = onCaretaker,
        )

        Spacer(Modifier.weight(1f))

        SettingLabel("Style & theme")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glossySurface(RoundedCornerShape(14.dp), CardWhite)
                .padding(4.dp),
        ) {
            StyleOption("System", AppTheme.mode == ThemeMode.SYSTEM, Modifier.weight(1f)) {
                AppTheme.setMode(context, ThemeMode.SYSTEM)
            }
            StyleOption("Light", AppTheme.mode == ThemeMode.LIGHT, Modifier.weight(1f)) {
                AppTheme.setMode(context, ThemeMode.LIGHT)
            }
            StyleOption("Dark", AppTheme.mode == ThemeMode.DARK, Modifier.weight(1f)) {
                AppTheme.setMode(context, ThemeMode.DARK)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glossySurface(RoundedCornerShape(14.dp), CardWhite)
                .padding(4.dp),
        ) {
            StyleOption("Normal", AppTheme.iconStyle == IconStyle.NORMAL, Modifier.weight(1f)) {
                AppTheme.setIconStyle(context, IconStyle.NORMAL)
            }
            StyleOption("Minimal", AppTheme.iconStyle == IconStyle.MINIMAL, Modifier.weight(1f)) {
                AppTheme.setIconStyle(context, IconStyle.MINIMAL)
            }
        }
        Spacer(Modifier.height(12.dp))

        GlossyButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF5F6673),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.White)
            Spacer(Modifier.size(8.dp))
            Text("Sign out", fontSize = 16.sp, color = Color.White)
        }
        Text(
            "Signed in with Google — sign out to switch account.",
            fontSize = 12.sp,
            color = LabelGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = LabelGray,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
    )
}

@Composable
private fun StyleOption(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .glossyChip(selected, BrandBlue, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else LabelGray,
        )
    }
}

@Composable
private fun RoleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glossySurface(RoundedCornerShape(20.dp), CardWhite)
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.fyp.healthcare.ui.theme.AppIconBadge(icon, BrandBlue, size = 52.dp, iconSize = 26.dp)
        Spacer(Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Text(subtitle, fontSize = 12.sp, color = LabelGray)
        }
        Spacer(Modifier.size(8.dp))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = LabelGray,
        )
    }
}
