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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/** The four nature icons stamped as a faint watermark on glossy cards. */
private val NatureIcons
    @Composable get() = listOf(
        Icons.Outlined.LocalFlorist,
        Icons.Outlined.Eco,
        Icons.Outlined.Spa,
        Icons.Outlined.Grass,
        Icons.Outlined.Forest,
    )

/** One nature icon painter, chosen deterministically from [seed]. */
@Composable
private fun rememberNatureMotif(seed: Int): Painter {
    val icons = NatureIcons
    return rememberVectorPainter(icons[abs(seed) % icons.size])
}

/**
 * Offsets, as a fraction of the watermark's own size, that the motif is drawn at to soften it
 * out of focus: the centre plus a ring of eight. Compose has no per-draw blur - RenderEffect is
 * a whole-layer effect and needs API 31+ - so near-coincident copies approximate it instead.
 *
 * This pattern was measured against a real Gaussian rather than guessed: the same shape blurred
 * by hand and compared side by side, which is also what ruled out the obvious cheaper version.
 * Fewer copies spread further apart does not blur, it ghosts - the individual strokes stay crisp
 * and read as double vision. Nine at this spacing is the point where they merge.
 */
private val MotifBlurTaps = listOf(
    Offset.Zero,
    Offset(1f, 0f),
    Offset(0.707f, 0.707f),
    Offset(0f, 1f),
    Offset(-0.707f, 0.707f),
    Offset(-1f, 0f),
    Offset(-0.707f, -0.707f),
    Offset(0f, -1f),
    Offset(0.707f, -0.707f),
)

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
 */
@Composable
fun Modifier.glossySurface(
    shape: Shape = RoundedCornerShape(20.dp),
    color: Color = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22)),
    elevation: Dp = 6.dp,
    glow: Color? = null,
): Modifier {
    val dark = AppTheme.isDark
    val seed = remember { Random.nextInt() }
    val motif = rememberNatureMotif(seed)
    val ring = glow?.let { it.copy(alpha = it.alpha * (if (dark) 0.62f else 0.48f)) }

    if (AppTheme.calm) {
        val base = if (AppTheme.shadows) this.shadow(1.dp, shape, clip = false) else this
        val flat = base
            .clip(shape)
            .background(color, shape)
            .border(1.dp, if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f), shape)
        return if (ring == null) flat else flat.border(2.dp, ring, shape)
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
    val lift = if (AppTheme.rich) elevation + 2.dp else elevation
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
        .satinTexture(dark, seed, color, motif, AppTheme.leafPatterns, AppTheme.rich)
        .border(1.dp, rim, shape)
    return if (ring == null) surfaced else surfaced.border(1.dp, ring, shape)
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
    val seed = remember { Random.nextInt() }
    val motif = rememberNatureMotif(seed)
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
        this.shadow(if (AppTheme.rich) elevation + 2.dp else elevation, shape, clip = false)
    } else this
    return lifted
        .clip(shape)
        .background(fill, shape)
        .satinTexture(dark, seed, c, motif, AppTheme.leafPatterns, AppTheme.rich)
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
 * A "brushed satin" overlay for glossy surfaces, seeded per card so no two look
 * alike: a soft light bloom drifting in from a random point near the top and a
 * field of gently rippling horizontal fibres (each a soft sine wave).
 *
 * The fibre and bloom colours are derived from the surface's own [base] colour -
 * a hair lighter and a hair darker - so the weave reads correctly on a white
 * card, a blue button or a red banner, and in either light or dark appearance,
 * without ever looking like a grey film laid on top. One quiet layer, not two.
 * All randomness is resolved once (per size + seed) and baked into two [Path]s,
 * so it never shimmers.
 */
private fun Modifier.satinTexture(
    dark: Boolean,
    seed: Int,
    base: Color,
    motif: Painter,
    leaf: Boolean = true,
    rich: Boolean = false,
): Modifier = drawWithCache {
    val r = Random(seed)
    val w = size.width
    val h = size.height

    val gap = (3.6f + r.nextFloat() * 2.6f).dp.toPx()
    val amp = (0.7f + r.nextFloat() * 1.6f).dp.toPx()
    val freq = (1.3f + r.nextFloat() * 2.4f) * (2f * Math.PI.toFloat() / w)
    val phase = r.nextFloat() * 2f * Math.PI.toFloat()

    val lightPath = Path()
    val shadePath = Path()
    val segs = 12
    var y = gap * 0.5f
    var i = 0
    while (y < h) {
        val target = if (i % 2 == 0) lightPath else shadePath
        val yOff = y + (r.nextFloat() - 0.5f) * amp
        target.moveTo(0f, yOff + amp * sin(phase + i * 0.5f))
        for (s in 1..segs) {
            val x = w * s / segs
            target.lineTo(x, yOff + amp * sin(x * freq + phase + i * 0.5f))
        }
        y += gap
        i++
    }

    val lightInk = lerp(base, Color.White, if (dark) 0.16f else 0.60f).copy(alpha = 0.55f)
    val shadeInk = lerp(base, Color.Black, if (dark) 0.24f else 0.10f).copy(alpha = 0.40f)

    val bloomHi = lerp(base, Color.White, if (dark) 0.14f else 0.55f)
        .copy(alpha = if (rich) 0.68f else 0.5f)
    val bloom = Brush.radialGradient(
        listOf(bloomHi, bloomHi.copy(alpha = 0f)),
        center = Offset(w * (0.15f + r.nextFloat() * 0.5f), h * (r.nextFloat() * 0.28f)),
        radius = size.maxDimension * (0.65f + r.nextFloat() * 0.7f),
    )
    val hair = Stroke(width = 1f)

    val motifSize = minOf(w, h) * (0.5f + r.nextFloat() * 0.5f)
    val motifX = w * (0.1f + r.nextFloat() * 0.8f) - motifSize / 2f
    val motifY = h * (0.1f + r.nextFloat() * 0.8f) - motifSize / 2f
    val motifRot = r.nextFloat() * 360f
    val motifTint = ColorFilter.tint(if (dark) Color.White else Color.Black)
    // Split across the taps so the overlapping copies add up to the same faintness as the
    // single crisp draw this replaced - the blur softens the edges without darkening the mark.
    val motifAlpha = (if (dark) 0.065f else 0.08f) / MotifBlurTaps.size
    val motifSpread = motifSize * (if (rich) 0.032f else 0.02f)

    onDrawBehind {
        drawRect(bloom)
        drawPath(lightPath, lightInk, style = hair)
        drawPath(shadePath, shadeInk, style = hair)
        if (leaf) {
            translate(motifX, motifY) {
                rotate(motifRot, pivot = Offset(motifSize / 2f, motifSize / 2f)) {
                    with(motif) {
                        MotifBlurTaps.forEach { tap ->
                            translate(tap.x * motifSpread, tap.y * motifSpread) {
                                draw(Size(motifSize, motifSize), alpha = motifAlpha, colorFilter = motifTint)
                            }
                        }
                    }
                }
            }
        }
    }
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
    val seed = remember { Random.nextInt() }
    val motif = rememberNatureMotif(seed)
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
            .then(
                if (minimal) Modifier
                else Modifier.satinTexture(AppTheme.isDark, seed, c, motif, AppTheme.leafPatterns, AppTheme.rich),
            )
            .clickable(enabled = enabled, onClick = onClick)
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
