package com.fyp.healthcare.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val glossShape = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height * 0.80f)
    quadraticTo(size.width / 2f, size.height * 1.18f, 0f, size.height * 0.80f)
    close()
}

/**
 * The app's standard icon badge. Renders as an old-iOS glossy tile when the user has
 * [IconStyle.NORMAL] selected, or a flat tinted square when [IconStyle.MINIMAL].
 *
 * Drop-in for the old `Box { Icon(tint = c) }` pattern - pass the same accent [tint].
 */
@Composable
fun AppIconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = size * 0.52f,
) {
    val shape = RoundedCornerShape(size * 0.30f)

    if (AppTheme.iconStyle == IconStyle.MINIMAL) {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(tint.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
        }
        return
    }

    val top = lerp(tint, Color.White, 0.32f)
    val bottom = lerp(tint, Color.Black, 0.14f)
    Box(
        modifier = modifier
            .size(size)
            .shadow(2.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(top, tint, bottom)))
            .border(1.dp, Color.White.copy(alpha = 0.25f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.52f)
                .align(Alignment.TopCenter)
                .clip(glossShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f)),
                    ),
                ),
        )
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSize))
    }
}
