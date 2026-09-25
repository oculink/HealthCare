package com.fyp.healthcare.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryBlue = Color(0xFF4A80F0)
val SecondaryBlue = Color(0xFFE8F0FE)
val BackgroundWhite = Color(0xFFF8F9FE)
val SurfaceWhite = Color(0xFFFFFFFF)

val HeartRateRed = Color(0xFFFF5252)
val BloodPressureGreen = Color(0xFF4CAF50)
val SleepPurple = Color(0xFF7C4DFF)
val BloodSugarOrange = Color(0xFFFF9800)

val TextPrimary = Color(0xFF202124)
val TextSecondary = Color(0xFF70757A)

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

/**
 * The colour a vital status word is drawn in. Lives here rather than inside each screen so that
 * "High" cannot quietly mean orange on Home and something else in the reading history.
 */
@Composable
fun vitalStatusColor(status: String?): Color = when (status) {
    "Good" -> Color(0xFF2E9E6B)
    "Normal" -> Color(0xFF2A6DE1)
    "Low" -> Color(0xFFFF9800)
    "High" -> Color(0xFFE64A19)
    "Critical" -> Color(0xFFD32F2F)
    else -> themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
}
