package com.fyp.healthcare.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * A "recessed well" for text inputs - the visual opposite of [glossySurface]: the
 * field looks pressed *into* the card, with an inner top shadow, a dark upper rim
 * and a lit lower rim. Gives older users a clear, tactile "type here" target that
 * stands apart from the surrounding surface. On [IconStyle.MINIMAL] it's a plain
 * soft-filled box.
 *
 * Use together with [glossyFieldColors] (which makes the text field's own
 * container/borders transparent so this well shows through):
 * ```
 * OutlinedTextField(
 *     modifier = Modifier.fillMaxWidth().glossyFieldWell(),
 *     shape = RoundedCornerShape(14.dp),
 *     colors = glossyFieldColors(),
 *     ...
 * )
 * ```
 */
@Composable
fun Modifier.glossyFieldWell(shape: Shape = RoundedCornerShape(14.dp)): Modifier {
    val dark = AppTheme.isDark
    val base = if (dark) Color(0xFF191B21) else Color(0xFFE7EAF1)

    if (AppTheme.iconStyle == IconStyle.MINIMAL) {
        return this
            .clip(shape)
            .background(base, shape)
            .border(1.dp, if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.07f), shape)
    }

    val fill = Brush.verticalGradient(
        0f to (if (dark) lerp(base, Color.Black, 0.45f) else lerp(base, Color(0xFF9AA7BD), 0.35f)),
        0.22f to base,
        1f to (if (dark) lerp(base, Color.White, 0.04f) else lerp(base, Color.White, 0.7f)),
    )
    val rim = Brush.verticalGradient(
        0f to (if (dark) Color.Black.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.16f)),
        0.55f to Color.White.copy(alpha = 0f),
        1f to (if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.85f)),
    )
    return this
        .clip(shape)
        .background(fill, shape)
        .drawWithCache {
            val h = 11.dp.toPx()
            val innerShadow = Brush.verticalGradient(
                0f to Color.Black.copy(alpha = if (dark) 0.30f else 0.13f),
                1f to Color.Transparent,
                endY = h,
            )
            onDrawBehind { drawRect(innerShadow, size = Size(size.width, h)) }
        }
        .border(1.5.dp, rim, shape)
}

/**
 * Text-field colours that pair with [glossyFieldWell]: transparent container and
 * outline (so the well is what you see), with readable text, a clear accent
 * cursor / focused label, and a soft focus tint on the accent.
 */
@Composable
fun glossyFieldColors(accent: Color = PrimaryBlue): TextFieldColors {
    val dark = AppTheme.isDark
    val text = if (dark) Color(0xFFE8E9EC) else Color(0xFF1B1D23)
    val label = if (dark) Color(0xFF9BA1AC) else Color(0xFF5F6673)
    val clear = Color.Transparent
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = clear,
        unfocusedContainerColor = clear,
        disabledContainerColor = clear,
        errorContainerColor = clear,
        focusedBorderColor = clear,
        unfocusedBorderColor = clear,
        disabledBorderColor = clear,
        errorBorderColor = clear,
        focusedTextColor = text,
        unfocusedTextColor = text,
        disabledTextColor = label,
        cursorColor = accent,
        focusedLeadingIconColor = accent,
        unfocusedLeadingIconColor = label,
        focusedTrailingIconColor = accent,
        unfocusedTrailingIconColor = label,
        focusedLabelColor = accent,
        unfocusedLabelColor = label,
        focusedPlaceholderColor = label.copy(alpha = 0.7f),
        unfocusedPlaceholderColor = label.copy(alpha = 0.7f),
    )
}
