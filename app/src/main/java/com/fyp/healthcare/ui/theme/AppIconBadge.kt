package com.fyp.healthcare.ui.theme

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// A bare glyph reads lighter than the tile it replaced, so it grows back toward that weight.
private const val BareGlyphScale = 1.24f

// Glow radius as a fraction of the glyph, so it scales with the icon instead of swamping a
// 16dp glyph or vanishing behind a 34dp one.
private const val GlowRadiusFraction = 0.18f

// Peak opacity of the blurred copy. The sharp glyph covers the middle of it, so this only
// governs how bright the ring around the silhouette reads.
private const val GlowAlpha = 0.40f

// The pre-31 fallback stacks enlarged copies rather than blurring. These are scaled to match
// GlowAlpha so a device on the fallback path doesn't end up glowing brighter than one blurring.
private const val GlowFarAlpha = 0.07f
private const val GlowNearAlpha = 0.11f

/**
 * The app's standard accent icon: the bare glyph in [tint], no tile. On [Decoration.STANDARD] and above the
 * glyph glows - a blurred copy of its own silhouette in the same colour, so the light follows
 * the shape rather than sitting on a coloured disc. On [Decoration.CALM] there is no glow,
 * since that mode is the one without dressing. Either way there is no square.
 *
 * [size] still reserves the same square box the badge used to, so layouts don't shift; the glyph
 * grows by [BareGlyphScale] from [iconSize] toward the visual weight the tile carried.
 *
 * Blurring is a hardware render effect and only exists on API 31+, so older devices stack a
 * couple of enlarged, faint copies of the vector instead. That fallback is plain drawing with no
 * offscreen buffer, so it is the cheaper of the two - just a chunkier halo.
 */
@Composable
fun AppIconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = size * 0.52f,
) {
    val glyph = (iconSize * BareGlyphScale).coerceAtMost(size)
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        if (!AppTheme.calm) {
            SilhouetteGlow(icon, tint, glyph, (glyph * GlowRadiusFraction).coerceAtMost((size - glyph) / 2f))
        }
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(glyph))
    }
}

/**
 * [glyph] drawn twice over: a blurred copy of the same vector behind the sharp one, which is what
 * makes the halo trace the outline.
 *
 * The copy gets a box padded by the blur radius, because a layer sized to the glyph would clip
 * the glow at its own edges. [TileMode.Decal] matters just as much: the default `Clamp` stretches
 * the blurred content out to the layer edges, which reads as a smeared square rather than a glow.
 * Decal is API 31+ only, but so is the blur, so the same gate covers both.
 */
@Composable
private fun SilhouetteGlow(icon: ImageVector, tint: Color, glyph: Dp, radius: Dp) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint.copy(alpha = GlowAlpha),
            modifier = Modifier
                .size(glyph + radius * 2f)
                .graphicsLayer {
                    renderEffect = BlurEffect(radius.toPx(), radius.toPx(), TileMode.Decal)
                },
        )
    } else {
        Icon(
            icon,
            contentDescription = null,
            tint = tint.copy(alpha = GlowFarAlpha),
            modifier = Modifier.size(glyph * 1.45f),
        )
        Icon(
            icon,
            contentDescription = null,
            tint = tint.copy(alpha = GlowNearAlpha),
            modifier = Modifier.size(glyph * 1.22f),
        )
    }
}
