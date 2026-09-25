package com.fyp.healthcare.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * How much surface treatment the app paints: [CALM] is flat fills with hairline edges,
 * [STANDARD] is the app's glossy look, [RICH] pushes the same recipe further - deeper shadow,
 * brighter top light, more visible texture.
 *
 * This used to be a two-value "icon style", which was a misleading name: it never just styled
 * icons, it switched off card gradients, texture, bloom, rims, shadows and the background
 * pattern all at once. The three levels are now the recipe, and the individual layers each have
 * their own switch below.
 */
enum class Decoration { CALM, STANDARD, RICH }

/**
 * App-wide theme choice. The screens use hardcoded palettes rather than MaterialTheme
 * tokens, so [themed] is the switch they call to pick a light/dark colour.
 */
object AppTheme {

    var mode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    var decoration by mutableStateOf(Decoration.STANDARD)
        private set

    /** Drop shadows under cards, tiles and buttons. Off still leaves every edge readable. */
    var shadows by mutableStateOf(true)
        private set

    /** The faint leaf/fern watermark scattered across glossy surfaces. */
    var leafPatterns by mutableStateOf(true)
        private set

    /** The dense scattered pattern behind the whole app. */
    var backgroundPattern by mutableStateOf(true)
        private set

    /** True when surfaces should be painted flat rather than glossy. */
    val calm: Boolean get() = decoration == Decoration.CALM

    /** True when the glossy recipe should be pushed harder than usual. */
    val rich: Boolean get() = decoration == Decoration.RICH

    /** Load the saved choices - call once from MainActivity.onCreate. */
    fun init(context: Context) {
        val p = prefs(context)
        mode = runCatching {
            ThemeMode.valueOf(p.getString(KEY, null) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
        decoration = readDecoration(p)
        // Someone arriving from the old Minimal setting should land on the picture they had,
        // not on a fully decorated screen they never asked for.
        val glossyByDefault = decoration != Decoration.CALM
        shadows = p.getBoolean(KEY_SHADOWS, glossyByDefault)
        leafPatterns = p.getBoolean(KEY_LEAF, glossyByDefault)
        backgroundPattern = p.getBoolean(KEY_BG, glossyByDefault)
    }

    /**
     * Reads the new key, falling back to the old two-value "icon style" so anyone who already
     * chose something keeps roughly what they had: Normal becomes Standard, Minimal becomes
     * Calm. Rich is new, so nothing maps to it.
     */
    private fun readDecoration(p: android.content.SharedPreferences): Decoration {
        p.getString(KEY_DECORATION, null)?.let { stored ->
            runCatching { Decoration.valueOf(stored) }.getOrNull()?.let { return it }
        }
        return when (p.getString(KEY_ICONS, null)) {
            "MINIMAL" -> Decoration.CALM
            "NORMAL" -> Decoration.STANDARD
            else -> Decoration.STANDARD
        }
    }

    fun setMode(context: Context, newMode: ThemeMode) {
        mode = newMode
        prefs(context).edit().putString(KEY, newMode.name).apply()
    }

    fun setDecoration(context: Context, value: Decoration) {
        decoration = value
        prefs(context).edit().putString(KEY_DECORATION, value.name).apply()
    }

    fun setShadows(context: Context, value: Boolean) {
        shadows = value
        prefs(context).edit().putBoolean(KEY_SHADOWS, value).apply()
    }

    fun setLeafPatterns(context: Context, value: Boolean) {
        leafPatterns = value
        prefs(context).edit().putBoolean(KEY_LEAF, value).apply()
    }

    fun setBackgroundPattern(context: Context, value: Boolean) {
        backgroundPattern = value
        prefs(context).edit().putBoolean(KEY_BG, value).apply()
    }

    val isDark: Boolean
        @Composable get() = when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }

    private fun prefs(c: Context) = c.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private const val KEY = "theme_mode"
    private const val KEY_ICONS = "icon_style"
    private const val KEY_DECORATION = "decoration"
    private const val KEY_SHADOWS = "card_shadows"
    private const val KEY_LEAF = "leaf_patterns"
    private const val KEY_BG = "background_pattern"
}

/** Pick a colour based on the current theme. Used by the screens' palettes. */
@Composable
fun themed(light: Color, dark: Color): Color = if (AppTheme.isDark) dark else light

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryBlue,
    tertiary = HeartRateRed,
    background = Color(0xFF121316),
    surface = Color(0xFF1C1D22),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE8E9EC),
    onSurface = Color(0xFFE8E9EC),
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryBlue,
    tertiary = HeartRateRed,
    background = BackgroundWhite,
    surface = SurfaceWhite,
    onPrimary = Color.White,
    onSecondary = PrimaryBlue,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

@Composable
fun HealthCareTheme(
    darkTheme: Boolean = AppTheme.isDark,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
