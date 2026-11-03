package com.fyp.healthcare.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * The shared page background for every screen: a base colour and a soft top-lit
 * vertical gradient. Works in light and dark.
 *
 * When the background pattern is on ([AppTheme.backgroundPattern]) it also scatters a
 * dense, randomised health-motif texture on top - many small hearts, plus-crosses,
 * pulse lines, pills, droplets, rings, cells and helices, each jittered, rotated
 * and scaled by a deterministic per-cell hash (so it looks hand-strewn but stays
 * stable and cached). Same spirit as a WhatsApp chat wallpaper, but our own.
 * With the pattern off the screen keeps just the plain gradient.
 *
 * Replaces the old `.background(ScreenBackground)` on each screen's root.
 *
 * Performance: every motif is baked into one [Path], with each ornament's jitter,
 * rotation and scale applied to its geometry up front. A frame is then three draw
 * calls (fill, sheen, motifs) instead of ~700, and drawing allocates nothing. The
 * geometry is memoised per screen size, so it is only built once - not again on
 * every recomposition.
 */
@Composable
fun Modifier.appBackground(): Modifier {
    val dark = AppTheme.isDark
    val patterned = AppTheme.backgroundPattern
    val base = if (dark) Color(0xFF121316) else Color(0xFFEFF1F6)
    val sheenTop = if (dark) Color(0xFF1B1D22) else Color(0xFFF7F8FB)
    val sheenBottom = if (dark) Color(0xFF0E0F12) else Color(0xFFE7E9EF)
    val motif = if (dark) Color.White.copy(alpha = 0.070f) else Color.Black.copy(alpha = 0.080f)

    return this.drawWithCache {
        val w = size.width
        val h = size.height
        val cell = 34.dp.toPx()
        val baseSize = 20.dp.toPx()

        val motifs = backgroundMotifs(w, h, patterned, cell, baseSize)
        val sheen = Brush.verticalGradient(listOf(sheenTop, base, sheenBottom))
        val motifStroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)

        onDrawBehind {
            drawRect(base)
            drawRect(sheen)
            if (patterned) drawPath(motifs, motif, style = motifStroke)
        }
    }
}

private data class GeometryKey(
    val w: Int,
    val h: Int,
    val patterned: Boolean,
    val cell: Int,
    val baseSize: Int,
)

private val geometryCache = object : LinkedHashMap<GeometryKey, Path>(4, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<GeometryKey, Path>) =
        size > 4
}

/**
 * Build (or reuse) the motif geometry for a [w] x [h] px screen. Keyed on the size,
 * the pattern switch and the px cell size, so a recomposition - or a second screen the
 * same size - reuses the paths instead of rebuilding ~300 of them.
 */
private fun backgroundMotifs(
    w: Float,
    h: Float,
    patterned: Boolean,
    cell: Float,
    baseSize: Float,
): Path {
    val key = GeometryKey(
        w = w.roundToInt(),
        h = h.roundToInt(),
        patterned = patterned,
        cell = cell.roundToInt(),
        baseSize = baseSize.roundToInt(),
    )
    geometryCache[key]?.let { return it }

    val motifs = Path()
    if (patterned) {
        val jitter = cell * 0.5f
        val cols = (w / cell).roundToInt() + 2
        val rows = (h / cell).roundToInt() + 2
        for (gy in -1 until rows) {
            for (gx in -1 until cols) {
                val seed = hash(gx, gy)
                if (seed % 6 == 0) continue
                val rx = frac(seed) - 0.5f
                val ry = frac(seed shr 8) - 0.5f
                val rot = frac(seed shr 16) * 360f
                val scale = 0.6f + frac(seed shr 24) * 0.9f
                val cx = gx * cell + cell / 2f + rx * 2f * jitter
                val cy = gy * cell + cell / 2f + ry * 2f * jitter
                val s = baseSize * scale

                // Local geometry, exactly as the old per-frame draw code built it...
                val local = when ((seed shr 4) % 8) {
                    0 -> heartPath(s)
                    1 -> crossPath(s)
                    2 -> pulsePath(s * 1.3f)
                    3 -> pillPath(s * 1.1f)
                    4 -> dropletPath(s)
                    5 -> ringPath(s)
                    6 -> cellPath(s)
                    else -> helixPath(s * 1.2f)
                }
                // ...then rotated about its own origin and moved into place once, here.
                local.transform(placementMatrix(cx, cy, rot))
                motifs.addPath(local)
            }
        }
    }

    return motifs.also { geometryCache[key] = it }
}

/** translate(cx, cy) * rotate(deg about the local origin) - the same order DrawScope used. */
private fun placementMatrix(cx: Float, cy: Float, degrees: Float): Matrix =
    Matrix().apply {
        translate(cx, cy)
        rotateZ(degrees)
    }

/* ---- deterministic per-cell pseudo-random ---- */

private fun hash(x: Int, y: Int): Int {
    var h = x * 374761393 + y * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    return (h xor (h ushr 16)) and 0x7FFFFFFF
}

/** 0f..1f from the low bits of [v]. */
private fun frac(v: Int): Float = ((v and 0xFFFF) / 65535f)

/* ---- motif shapes, all centred on (0,0), roughly [s] across ---- */

private fun heartPath(s: Float): Path = Path().apply {
    val half = s / 2f
    moveTo(0f, half * 0.9f)
    cubicTo(-s * 0.05f, half * 0.35f, -s, -half * 0.1f, -half, -half * 0.55f)
    cubicTo(-half * 0.2f, -s * 0.5f, 0f, -half * 0.35f, 0f, -half * 0.05f)
    cubicTo(0f, -half * 0.35f, half * 0.2f, -s * 0.5f, half, -half * 0.55f)
    cubicTo(s, -half * 0.1f, s * 0.05f, half * 0.35f, 0f, half * 0.9f)
    close()
}

private fun crossPath(s: Float): Path = Path().apply {
    val a = s / 2f
    val t = s / 6f
    moveTo(-t, -a); lineTo(t, -a); lineTo(t, -t); lineTo(a, -t); lineTo(a, t)
    lineTo(t, t); lineTo(t, a); lineTo(-t, a); lineTo(-t, t); lineTo(-a, t)
    lineTo(-a, -t); lineTo(-t, -t); close()
}

private fun pulsePath(s: Float): Path = Path().apply {
    val half = s / 2f
    moveTo(-half, 0f)
    lineTo(-half * 0.35f, 0f)
    lineTo(-half * 0.15f, -half * 0.55f)
    lineTo(half * 0.05f, half * 0.6f)
    lineTo(half * 0.28f, -half * 0.2f)
    lineTo(half * 0.42f, 0f)
    lineTo(half, 0f)
}

private fun pillPath(s: Float): Path = Path().apply {
    val len = s / 2f
    val r = s / 5f
    addRoundRect(RoundRect(Rect(Offset(-len, -r), Size(len * 2f, r * 2f)), CornerRadius(r, r)))
    moveTo(0f, -r); lineTo(0f, r)
}

private fun dropletPath(s: Float): Path = Path().apply {
    val half = s / 2f
    moveTo(0f, -half)
    cubicTo(half * 0.9f, -half * 0.1f, half * 0.7f, half, 0f, half)
    cubicTo(-half * 0.7f, half, -half * 0.9f, -half * 0.1f, 0f, -half)
    close()
}

private fun helixPath(s: Float): Path = Path().apply {
    val half = s / 2f
    moveTo(-half * 0.5f, -half)
    cubicTo(half * 0.9f, -half * 0.5f, -half * 0.9f, half * 0.5f, half * 0.5f, half)
    moveTo(half * 0.5f, -half)
    cubicTo(-half * 0.9f, -half * 0.5f, half * 0.9f, half * 0.5f, -half * 0.5f, half)
    moveTo(-half * 0.32f, -half * 0.5f); lineTo(half * 0.32f, -half * 0.5f)
    moveTo(-half * 0.38f, 0f); lineTo(half * 0.38f, 0f)
    moveTo(-half * 0.32f, half * 0.5f); lineTo(half * 0.32f, half * 0.5f)
}

/** The old `drawRing`: two concentric circles, as one stroked path. */
private fun ringPath(s: Float): Path = Path().apply {
    addOval(Rect(center = Offset.Zero, radius = s / 2f))
    addOval(Rect(center = Offset.Zero, radius = s / 6f))
}

/** The old `drawCell`: a ring plus two small inner circles, as one stroked path. */
private fun cellPath(s: Float): Path = Path().apply {
    val r = s / 2f
    addOval(Rect(center = Offset.Zero, radius = r))
    addOval(Rect(center = Offset(-r * 0.2f, r * 0.15f), radius = r * 0.34f))
    addOval(Rect(center = Offset(r * 0.35f, -r * 0.3f), radius = r * 0.18f))
}
