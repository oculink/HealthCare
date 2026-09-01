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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * The shared page background for every screen. Gives the app an "old-iOS" feel:
 * a base colour, a soft top-lit vertical gradient, and a faint linen cross-hatch
 * texture (like a subtle fabric weave). Works in light and dark.
 *
 * On the **Normal** icon style ([IconStyle.NORMAL]) it also scatters a dense,
 * randomised health-motif texture on top — many small hearts, plus-crosses,
 * pulse lines, pills, droplets, rings, cells and helices, each jittered, rotated
 * and scaled by a deterministic per-cell hash (so it looks hand-strewn but stays
 * stable and cached). Same spirit as a WhatsApp chat wallpaper, but our own.
 * The Minimal style keeps just the plain weave.
 *
 * Replaces the old `.background(ScreenBackground)` on each screen's root.
 */
@Composable
fun Modifier.appBackground(): Modifier {
    val dark = AppTheme.isDark
    val patterned = AppTheme.iconStyle == IconStyle.NORMAL
    val base = if (dark) Color(0xFF121316) else Color(0xFFEFF1F6)
    // top-lit sheen
    val sheenTop = if (dark) Color(0xFF1B1D22) else Color(0xFFF7F8FB)
    val sheenBottom = if (dark) Color(0xFF0E0F12) else Color(0xFFE7E9EF)
    // weave lines
    val line = if (dark) Color.White.copy(alpha = 0.022f) else Color.Black.copy(alpha = 0.025f)
    // motif ink — stronger than the weave so the little shapes read, still subtle
    val motif = if (dark) Color.White.copy(alpha = 0.070f) else Color.Black.copy(alpha = 0.080f)

    return this.drawWithCache {
        val step = 7.dp.toPx()
        val w = size.width
        val h = size.height
        // small cell -> lots of motifs
        val cell = 34.dp.toPx()
        val jitter = cell * 0.5f
        val baseSize = 20.dp.toPx()
        onDrawBehind {
            drawRect(base)
            drawRect(Brush.verticalGradient(listOf(sheenTop, base, sheenBottom)))
            // diagonal cross-hatch (two directions) — cheap, drawn once per size
            var x = -h
            while (x < w) {
                drawLine(line, Offset(x, 0f), Offset(x + h, h), strokeWidth = 1f)
                drawLine(line, Offset(x + h, 0f), Offset(x, h), strokeWidth = 1f)
                x += step
            }

            if (patterned) {
                val stroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
                val cols = (w / cell).roundToInt() + 2
                val rows = (h / cell).roundToInt() + 2
                for (gy in -1 until rows) {
                    for (gx in -1 until cols) {
                        val seed = hash(gx, gy)
                        // ~1 in 6 cells stays empty, for irregular breathing room
                        if (seed % 6 == 0) continue
                        val rx = frac(seed) - 0.5f
                        val ry = frac(seed shr 8) - 0.5f
                        val rot = frac(seed shr 16) * 360f
                        val scale = 0.6f + frac(seed shr 24) * 0.9f
                        val cx = gx * cell + cell / 2f + rx * 2f * jitter
                        val cy = gy * cell + cell / 2f + ry * 2f * jitter
                        val s = baseSize * scale
                        translate(cx, cy) {
                            rotate(rot, pivot = Offset.Zero) {
                                when ((seed shr 4) % 8) {
                                    0 -> drawPath(heartPath(s), motif, style = stroke)
                                    1 -> drawPath(crossPath(s), motif, style = stroke)
                                    2 -> drawPath(pulsePath(s * 1.3f), motif, style = stroke)
                                    3 -> drawPath(pillPath(s * 1.1f), motif, style = stroke)
                                    4 -> drawPath(dropletPath(s), motif, style = stroke)
                                    5 -> drawRing(s, motif, stroke)
                                    6 -> drawCell(s, motif, stroke)
                                    else -> drawPath(helixPath(s * 1.2f), motif, style = stroke)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
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
    // rungs
    moveTo(-half * 0.32f, -half * 0.5f); lineTo(half * 0.32f, -half * 0.5f)
    moveTo(-half * 0.38f, 0f); lineTo(half * 0.38f, 0f)
    moveTo(-half * 0.32f, half * 0.5f); lineTo(half * 0.32f, half * 0.5f)
}

private fun DrawScope.drawRing(s: Float, color: Color, stroke: Stroke) {
    drawCircle(color, radius = s / 2f, center = Offset.Zero, style = stroke)
    drawCircle(color, radius = s / 6f, center = Offset.Zero, style = stroke)
}

private fun DrawScope.drawCell(s: Float, color: Color, stroke: Stroke) {
    val r = s / 2f
    drawCircle(color, radius = r, center = Offset.Zero, style = stroke)
    drawCircle(color, radius = r * 0.34f, center = Offset(-r * 0.2f, r * 0.15f), style = stroke)
    drawCircle(color, radius = r * 0.18f, center = Offset(r * 0.35f, -r * 0.3f), style = stroke)
}
