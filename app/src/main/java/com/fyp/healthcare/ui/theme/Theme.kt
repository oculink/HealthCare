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

/** Icon treatment app-wide. NORMAL = glossy "old-iOS" tiles, MINIMAL = flat tinted. */
enum class IconStyle { NORMAL, MINIMAL }

/**
 * App-wide theme choice. The screens use hardcoded palettes rather than MaterialTheme
 * tokens, so [themed] is the switch they call to pick a light/dark colour.
 */
object AppTheme {

    var mode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    var iconStyle by mutableStateOf(IconStyle.NORMAL)
        private set

    /** Load the saved choices — call once from MainActivity.onCreate. */
    fun init(context: Context) {
        mode = runCatching {
            ThemeMode.valueOf(prefs(context).getString(KEY, null) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
        iconStyle = runCatching {
            IconStyle.valueOf(prefs(context).getString(KEY_ICONS, null) ?: IconStyle.NORMAL.name)
        }.getOrDefault(IconStyle.NORMAL)
    }

    fun setMode(context: Context, newMode: ThemeMode) {
        mode = newMode
        prefs(context).edit().putString(KEY, newMode.name).apply()
    }

    fun setIconStyle(context: Context, style: IconStyle) {
        iconStyle = style
        prefs(context).edit().putString(KEY_ICONS, style.name).apply()
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
