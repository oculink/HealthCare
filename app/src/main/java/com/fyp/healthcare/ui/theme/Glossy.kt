package com.fyp.healthcare.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Skeuomorphic "glossy" surface for cards and tappable tiles - a top-lit vertical
 * gradient, a bright rim highlight and a soft drop shadow, so panels read as raised,
 * tactile objects. Tuned for older eyes: real depth cues, strong edges, generous
 * shadow. On [Decoration.CALM] it falls back to a flat fill with a hairline edge.
 *
 * Drop-in replacement for the common `.clip(shape).background(CardWhite)` pattern:
 * `.glossySurface(RoundedCornerShape(20.dp), CardWhite)`.
 *
 * [glow] dresses the edge in a state colour - a ring on every Android version, plus a
 * coloured drop shadow on API 28+, where the platform can tint one. Its alpha scales the
 * treatment so a caller can animate it: the ring thins away to nothing, while the shadow
 * fades back to an ordinary black one instead of disappearing, so the dimmest point of a
 * pulse is a plain resting card. The flat [Decoration.CALM] fallback gets the ring only,
 * since its whole point is the absence of cast depth. Leave it null for a card that is in no
 * particular state.
 *
 * [glowPhase] is how a caller animates that ring without recomposing: pass a lambda that
 * reads the animated value, and it is evaluated in the draw phase, so a pulsing card repaints
 * but never rebuilds its modifier chain or its shadow. The cast shadow stays at the lit
 * colour for the duration instead of breathing with the ring.
 */
@Composable
fun Modifier.glossySurface(
    shape: Shape = RoundedCornerShape(20.dp),
    color: Color = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22)),
    elevation: Dp = 6.dp,
    glow: Color? = null,
    glowPhase: (() -> Float)? = null,
): Modifier {
    val dark = AppTheme.isDark
    val ring = glow?.let { it.copy(alpha = it.alpha * (if (dark) 0.62f else 0.48f)) }

    if (AppTheme.calm) {
        val base = if (AppTheme.shadows) this.shadow(1.dp, shape, clip = false) else this
        val flat = base
            .clip(shape)
            .background(color, shape)
            .border(1.dp, if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f), shape)
        return when {
            ring == null -> flat
            glowPhase == null -> flat.border(2.dp, ring, shape)
            else -> flat.glowingRing(shape, ring, 2.dp, glowPhase)
        }
    }

    val fill = if (dark) {
        Brush.verticalGradient(
            0f to lerp(color, Color.White, 0.12f),
            0.10f to lerp(color, Color.White, 0.05f),
            0.55f to color,
            1f to lerp(color, Color.Black, 0.28f),
        )
    } else {
        Brush.verticalGradient(
            0f to lerp(color, Color.White, 0.85f),
            0.12f to lerp(color, Color.White, 0.45f),
            0.55f to color,
            1f to lerp(color, Color(0xFF9AA7BD), 0.16f),
        )
    }
    val rim = Brush.verticalGradient(
        0f to Color.White.copy(alpha = if (dark) 0.28f else 0.90f),
        0.5f to Color.White.copy(alpha = 0f),
    )
    // Kept as two calls rather than one with `glow ?: Color.Black` so a card with no glow
    // goes through the exact same platform path it always did.
    //
    // The shadow is tinted by blending up from opaque black rather than by lowering its
    // alpha: Android paints one with setOutlineAmbient/SpotShadowColor, so a translucent
    // colour means no shadow at all, and a fading glow would quietly flatten the card at the
    // bottom of every cycle instead of returning it to its resting elevation.
    val lift = elevation
    val lifted = when {
        !AppTheme.shadows -> this
        glow == null -> this.shadow(lift, shape, clip = false)
        else -> this.shadow(
            lift + 3.dp,
            shape,
            clip = false,
            ambientColor = lerp(Color.Black, glow.copy(alpha = 1f), glow.alpha),
            spotColor = lerp(Color.Black, glow.copy(alpha = 1f), glow.alpha),
        )
    }
    val surfaced = lifted
        .clip(shape)
        .background(fill, shape)
        .border(1.dp, rim, shape)
    return when {
        ring == null -> surfaced
        glowPhase == null -> surfaced.border(1.dp, ring, shape)
        else -> surfaced.glowingRing(shape, ring, 1.dp, glowPhase)
    }
}

/**
 * The animated form of `border(width, color, shape)`: draws the same ring, but reads its
 * alpha from [phase] inside the draw phase, so a once-per-frame pulse repaints the card
 * without recomposing it. Mirrors `Modifier.border`'s geometry exactly, so the ring looks
 * identical at full strength.
 */
private fun Modifier.glowingRing(
    shape: Shape,
    color: Color,
    width: Dp,
    phase: () -> Float,
): Modifier = this.drawWithContent {
    drawContent()
    val a = phase().coerceIn(0f, 1f)
    if (a <= 0.001f) return@drawWithContent
    val ink = color.copy(alpha = color.alpha * a)
    val stroke = Stroke(width = width.toPx())
    when (val outline = shape.createOutline(size, layoutDirection, this)) {
        is Outline.Rectangle ->
            drawRect(ink, topLeft = outline.rect.topLeft, size = outline.rect.size, style = stroke)
        is Outline.Rounded -> {
            val rr = outline.roundRect
            drawRoundRect(
                color = ink,
                topLeft = Offset(rr.left, rr.top),
                size = Size(rr.width, rr.height),
                cornerRadius = rr.topLeftCornerRadius,
                style = stroke,
            )
        }
        is Outline.Generic -> drawPath(outline.path, ink, style = stroke)
    }
}

/**
 * The darkest band of a [glossySurface] built on [color] - the gradient's bottom stop, where a
 * card's label actually sits. Ink painted on the card should be measured against this rather
 * than against [color] itself: in light appearance the band is a shade of grey, enough to pull a
 * borderline colour under 4.5:1.
 */
@Composable
fun glossySurfaceFloor(color: Color): Color =
    if (AppTheme.isDark) color else lerp(color, Color(0xFF9AA7BD), 0.16f)

/**
 * A tappable tile that is its own accent colour, for actions where the surface itself carries the
 * affordance rather than a white card holding a tinted badge. Currently uncalled - Home's Quick
 * Actions used this until they moved to [glossySurface] with a coloured [glow] - kept because the
 * fill stays AA-by-construction for white content, which [glossySurface]'s ring does not do.
 *
 * Two differences from [glossySurface], which is tuned for white info cards. The fill is
 * [color] deepened until white content on it stays readable - see [tileFill]. And the top
 * highlight is far shallower, since [glossySurface]'s white wash would fade out a white icon
 * sitting high on a tall tile. [Decoration.CALM] -> flat fill + hairline edge.
 */
@Composable
fun Modifier.glossyTile(
    color: Color,
    shape: Shape = RoundedCornerShape(18.dp),
    elevation: Dp = 6.dp,
): Modifier {
    val dark = AppTheme.isDark
    val c = tileFill(color)

    if (AppTheme.calm) {
        val base = if (AppTheme.shadows) this.shadow(1.dp, shape, clip = false) else this
        return base
            .clip(shape)
            .background(c, shape)
            .border(1.dp, if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.12f), shape)
    }

    val fill = if (dark) {
        Brush.verticalGradient(
            0f to lerp(c, Color.White, 0.14f),
            0.14f to lerp(c, Color.White, 0.06f),
            0.55f to c,
            1f to lerp(c, Color.Black, 0.26f),
        )
    } else {
        Brush.verticalGradient(
            0f to lerp(c, Color.White, 0.34f),
            0.14f to lerp(c, Color.White, 0.10f),
            0.55f to c,
            1f to lerp(c, Color.Black, 0.22f),
        )
    }
    val rim = Brush.verticalGradient(
        0f to Color.White.copy(alpha = if (dark) 0.28f else 0.45f),
        0.5f to Color.White.copy(alpha = 0f),
    )
    val lifted = if (AppTheme.shadows) {
        this.shadow(elevation, shape, clip = false)
    } else this
    return lifted
        .clip(shape)
        .background(fill, shape)
        .border(1.dp, rim, shape)
}

/**
 * The [glossyTile] fill: [color] pulled toward black just far enough that white text on it
 * clears WCAG AA's 4.5:1, from a 0.22 floor so every accent sits at one even depth. The floor
 * covers every accent in the app today; the search only matters if a future one is bright
 * enough to need more, and at 10-11sp anything under 4.5:1 is a smudge to the eyes this UI is
 * tuned for.
 */
private fun tileFill(color: Color): Color {
    var t = 0.22f
    while (t < 0.60f && contrastRatio(Color.White, lerp(color, Color.Black, t)) < 4.5f) t += 0.02f
    return lerp(color, Color.Black, t)
}

/** WCAG contrast ratio between two opaque colours. */
private fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    val hi = if (la >= lb) la else lb
    val lo = if (la >= lb) lb else la
    return (hi + 0.05f) / (lo + 0.05f)
}

/**
 * [color] pushed just far enough away from [on] to clear WCAG AA's 4.5:1, stopping at the first
 * step that passes so the hue barely moves. It darkens on a light surface and lightens on a dark
 * one, since a mid-tone accent sits on the right side of white but the wrong side of near-black.
 * The counterpart of [tileFill] for content that has to keep its own colour instead of the
 * surface's - an icon and label painted in a card's accent, where the card is white in one
 * appearance and near-black in the other and cannot carry the same ink both times.
 */
fun readableOn(color: Color, on: Color): Color {
    val away = if (on.luminance() > 0.5f) Color.Black else Color.White
    var t = 0f
    while (t < 0.80f && contrastRatio(lerp(color, away, t), on) < 4.5f) t += 0.02f
    return lerp(color, away, t)
}

/**
 * A large, glossy, high-contrast primary button for the app. Minimum 56dp tall
 * with 17sp semibold text so it stays comfortable for elderly users.
 */
@Composable
fun GlossyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = PrimaryBlue,
    contentColor: Color = Color.White,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val minimal = AppTheme.calm
    val c = if (enabled) color else lerp(color, Color(0xFF9AA7BD), 0.55f)

    val fillMod = if (minimal) {
        Modifier.background(c, shape)
    } else {
        Modifier
            .background(
                Brush.verticalGradient(
                    0f to lerp(c, Color.White, 0.55f),
                    0.14f to lerp(c, Color.White, 0.24f),
                    0.55f to c,
                    1f to lerp(c, Color.Black, 0.18f),
                ),
                shape,
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.55f),
                    0.5f to Color.White.copy(alpha = 0f),
                ),
                shape,
            )
    }

    Row(
        modifier = modifier
            .then(
                if (minimal || !AppTheme.shadows) Modifier
                else Modifier.shadow(if (enabled) 5.dp else 0.dp, shape, clip = false),
            )
            .clip(shape)
            .then(fillMod)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val scope = this
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold)) {
                scope.content()
            }
        }
    }
}

/**
 * A small selectable pill / segment (gender picker, category filter, Normal/Minimal, ...).
 * Selected -> a glossy [accent] gradient with a bright top rim, so the choice pops for
 * older eyes. Unselected -> transparent by default (for chips sitting on their own track),
 * or a soft raised chip of [unselectedFill] when that's given. [Decoration.CALM] -> flat.
 */
@Composable
fun Modifier.glossyChip(
    selected: Boolean,
    accent: Color = PrimaryBlue,
    shape: Shape = RoundedCornerShape(11.dp),
    unselectedFill: Color? = null,
): Modifier {
    val dark = AppTheme.isDark

    if (AppTheme.calm) {
        val bg = when {
            selected -> accent
            unselectedFill != null -> unselectedFill
            else -> Color.Transparent
        }
        return this.clip(shape).background(bg, shape)
    }

    if (selected) {
        return this
            .shadow(3.dp, shape, clip = false)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    0f to lerp(accent, Color.White, 0.50f),
                    0.14f to lerp(accent, Color.White, 0.22f),
                    0.55f to accent,
                    1f to lerp(accent, Color.Black, 0.16f),
                ),
                shape,
            )
            .border(
                1.dp,
                Brush.verticalGradient(0f to Color.White.copy(alpha = 0.5f), 0.5f to Color.White.copy(alpha = 0f)),
                shape,
            )
    }

    val fill = unselectedFill ?: return this.clip(shape)
    return this
        .shadow(1.dp, shape, clip = false)
        .clip(shape)
        .background(
            Brush.verticalGradient(
                0f to lerp(fill, Color.White, if (dark) 0.06f else 0.65f),
                0.5f to fill,
                1f to lerp(fill, if (dark) Color.Black else Color(0xFF9AA7BD), if (dark) 0.20f else 0.12f),
            ),
            shape,
        )
        .border(1.dp, if (dark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f), shape)
}

/**
 * A status badge (Good / High / Critical / Due now / Taken ...) - a soft tinted gradient
 * of [color] with a matching edge, a touch more presence than a flat 12%-alpha wash so
 * it reads at a glance. [Decoration.CALM] -> the plain flat wash.
 */
@Composable
fun Modifier.glossyBadge(color: Color, shape: Shape = RoundedCornerShape(50.dp)): Modifier {
    val dark = AppTheme.isDark
    if (AppTheme.calm) {
        return this.clip(shape).background(color.copy(alpha = 0.13f), shape)
    }
    return this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                0f to color.copy(alpha = if (dark) 0.32f else 0.24f),
                1f to color.copy(alpha = if (dark) 0.15f else 0.11f),
            ),
            shape,
        )
        .border(1.dp, color.copy(alpha = 0.32f), shape)
}

/**
 * The blue top-app-bar treatment: a top-lit vertical gradient of [color] plus a soft
 * dark line along the bottom edge, so the header reads as a raised bar the content
 * scrolls under. Drop-in for `.background(BrandBlue)` on a header Row.
 * [Decoration.CALM] -> plain flat fill.
 */
@Composable
fun Modifier.glossyTopBar(color: Color = PrimaryBlue): Modifier {
    if (AppTheme.calm) return this.background(color)
    return this
        .background(
            Brush.verticalGradient(
                0f to lerp(color, Color.White, 0.16f),
                0.5f to color,
                1f to lerp(color, Color.Black, 0.16f),
            ),
        )
        .drawWithContent {
            drawContent()
            val y = size.height - 0.5f
            drawLine(Color.Black.copy(alpha = 0.18f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
}
