package io.github.hishamhashmi100.yoink.ui.theme

import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.content.ContextWrapper
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.WindowManager
import androidx.annotation.FontRes
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.color.utilities.DynamicColor
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.SchemeContent
import com.google.android.material.color.utilities.SchemeExpressive
import com.google.android.material.color.utilities.SchemeFruitSalad
import com.google.android.material.color.utilities.SchemeMonochrome
import com.google.android.material.color.utilities.SchemeNeutral
import com.google.android.material.color.utilities.SchemeRainbow
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.google.android.material.color.utilities.SchemeVibrant
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.data.Appearance
import io.github.hishamhashmi100.yoink.data.Contrast
import io.github.hishamhashmi100.yoink.data.CornerStyle
import io.github.hishamhashmi100.yoink.data.FontStyle
import io.github.hishamhashmi100.yoink.data.GlassBehind
import io.github.hishamhashmi100.yoink.data.GlassStyle
import io.github.hishamhashmi100.yoink.data.PaletteStyle
import io.github.hishamhashmi100.yoink.data.Settings
import io.github.hishamhashmi100.yoink.data.ThemeMode
import io.github.hishamhashmi100.yoink.data.YOINK_RED

/** The brand red from docs/logo.svg, used as the Material 3 seed color. */
val YoinkRed = Color(YOINK_RED)

// Hand-tuned scheme for the default look: Yoink red with the balanced palette.
private val LightColors = lightColorScheme(
    primary = Color(0xFFC00012),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF775652),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD5),
    onSecondaryContainer = Color(0xFF2C1512),
    tertiary = Color(0xFF705C2E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFCDFA6),
    onTertiaryContainer = Color(0xFF261A00),
    background = Color(0xFFFFF8F7),
    onBackground = Color(0xFF231918),
    surface = Color(0xFFFFF8F7),
    onSurface = Color(0xFF231918),
    surfaceVariant = Color(0xFFF5DDDA),
    onSurfaceVariant = Color(0xFF534341),
    outline = Color(0xFF857371),
    outlineVariant = Color(0xFFD8C2BF),
    surfaceDim = Color(0xFFE8D6D3),
    surfaceBright = Color(0xFFFFF8F7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF0EF),
    surfaceContainer = Color(0xFFFCEAE8),
    surfaceContainerHigh = Color(0xFFF6E4E2),
    surfaceContainerHighest = Color(0xFFF1DEDC),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFE7BDB7),
    onSecondary = Color(0xFF442926),
    secondaryContainer = Color(0xFF5D3F3B),
    onSecondaryContainer = Color(0xFFFFDAD5),
    tertiary = Color(0xFFDFC38C),
    onTertiary = Color(0xFF3E2E04),
    tertiaryContainer = Color(0xFF574419),
    onTertiaryContainer = Color(0xFFFCDFA6),
    background = Color(0xFF1A1110),
    onBackground = Color(0xFFF1DEDC),
    surface = Color(0xFF1A1110),
    onSurface = Color(0xFFF1DEDC),
    surfaceVariant = Color(0xFF534341),
    onSurfaceVariant = Color(0xFFD8C2BF),
    outline = Color(0xFFA08C8A),
    outlineVariant = Color(0xFF534341),
    surfaceDim = Color(0xFF1A1110),
    surfaceBright = Color(0xFF423735),
    surfaceContainerLowest = Color(0xFF140C0B),
    surfaceContainerLow = Color(0xFF231918),
    surfaceContainer = Color(0xFF271D1C),
    surfaceContainerHigh = Color(0xFF322827),
    surfaceContainerHighest = Color(0xFF3D3231),
)

@Composable
fun isDarkTheme(appearance: Appearance): Boolean = when {
    appearance.pureBlack -> true
    appearance.theme == ThemeMode.SYSTEM -> isSystemInDarkTheme()
    else -> appearance.theme == ThemeMode.DARK
}

@Composable
fun YoinkTheme(settings: Settings, backdrop: Boolean = true, content: @Composable () -> Unit) =
    YoinkTheme(settings.appearance, backdrop, content)

/**
 * @param backdrop draw the glass backdrop behind [content]. Off for windows that float over other apps,
 * like the share sheet, where whatever is behind serves as the backdrop.
 */
@Composable
fun YoinkTheme(appearance: Appearance, backdrop: Boolean = true, content: @Composable () -> Unit) {
    val customBackdrop = appearance.customBackdrop
    val behind = when {
        // Pure black overrides whatever was chosen behind the glass.
        appearance.pureBlack && appearance.glass != GlassStyle.OFF -> Backdrop(GlassBehind.ONE_COLOR, Color.Black, Color.Black)
        // Each surface style has its own background; Solid can have one or two colors too.
        else -> Backdrop(
            behind = appearance.glassBehind,
            color1 = Color(appearance.backdropColor1),
            color2 = Color(if (appearance.glassBehind == GlassBehind.TWO_COLORS) appearance.backdropColor2 else appearance.backdropColor1),
        )
    }
    val dark = when {
        // Pure black is always dark.
        appearance.pureBlack -> true
        // On the user's own glass colors, "System" picks what suits them so text stays readable (white on
        // dark colors, dark on light ones); Light and Dark are followed as chosen.
        customBackdrop && appearance.theme == ThemeMode.SYSTEM -> (behind.color1.luminance() + behind.color2.luminance()) / 2 < 0.45f
        else -> isDarkTheme(appearance)
    }
    val context = LocalContext.current
    val solid = remember(appearance, dark) { colorScheme(context, appearance.colorSource, dark) }
    val wallpaper = appearance.glassBehind == GlassBehind.WALLPAPER
    val glass = Glass(
        style = appearance.glass,
        dark = dark,
        backdrop = if (behind.behind == GlassBehind.ONE_COLOR || behind.behind == GlassBehind.TWO_COLORS) behind.color1 else solid.background,
        accent = solid.primary,
        opacity = appearance.effectiveGlassOpacity,
        wallpaper = wallpaper && !appearance.pureBlack,
        // The glass is tinted with the user's own colors, as a gradient when there are two.
        colors = if (customBackdrop) listOfNotNull(behind.color1, behind.color2.takeIf { it != behind.color1 }) else emptyList(),
    )
    val colors = remember(solid, glass, customBackdrop) {
        solid.oneAccent().let {
            when {
                glass.enabled -> it.glassy(glass)
                // Solid on the user's own colors: screens are clear over them, cards and bars stay solid.
                customBackdrop -> it.copy(background = Color.Transparent, surface = Color.Transparent)
                else -> it
            }
        }
    }
    val overlay = remember(appearance, glass) {
        if (!glass.enabled) return@remember null
        val darkSolid = colorScheme(context, appearance.colorSource, dark = true)
        val darkGlass = glass.copy(dark = true, backdrop = darkSolid.background, accent = darkSolid.primary)
        val lightSolid = colorScheme(context, appearance.colorSource, dark = false)
        val lightGlass = glass.copy(dark = false, backdrop = lightSolid.background, accent = lightSolid.primary)
        GlassOverlay(
            colors = darkSolid.oneAccent().glassy(darkGlass),
            glass = darkGlass,
            lightColors = lightSolid.oneAccent().glassy(lightGlass),
            lightGlass = lightGlass,
            playerShade = appearance.playerShade,
        )
    }
    val typography = remember(appearance.font) { typography(appearance.font) }
    val showWallpaper = glass.enabled && glass.wallpaper && backdrop

    val view = LocalView.current
    val blurPx = with(LocalDensity.current) { 40.dp.roundToPx() }
    if (!view.isInEditMode) {
        SideEffect {
            view.context.findActivity()?.window?.let { window ->
                // enableEdgeToEdge() follows the system's dark mode; keep the bar icons readable when the app
                // overrides it.
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                    // The navigation buttons (back, home, recents) stay out of the way: a swipe up from the
                    // bottom edge shows them for a moment. Not for the share sheet over another app.
                    if (backdrop) {
                        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        hide(WindowInsetsCompat.Type.navigationBars())
                    }
                }
                if (backdrop && Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
                // "Your wallpaper" behind the glass: let the phone's wallpaper show through the window, and
                // with frosted glass, blur it (Android 12+).
                if (showWallpaper) {
                    window.setFormat(PixelFormat.TRANSLUCENT)
                    window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
                    window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                }
                if (Build.VERSION.SDK_INT >= 31 && backdrop) {
                    window.setBackgroundBlurRadius(if (showWallpaper && glass.style == GlassStyle.FROSTED) blurPx else 0)
                }
            }
        }
    }

    MaterialTheme(colorScheme = colors, shapes = shapes(appearance.corners), typography = typography) {
        CompositionLocalProvider(
            LocalButtonShape provides buttonShape(appearance.corners),
            LocalGlass provides glass,
            LocalBackdropColors provides solid,
            LocalGlassOverlay provides overlay,
            LocalBackdrop provides behind,
        ) {
            // Always the same structure, whatever is drawn behind: switching surface styles mustn't rebuild the
            // screen (which would, for one, scroll the Appearance screen back to the top).
            Box {
                when {
                    // A light wash of the theme over the wallpaper, so text on it stays readable.
                    showWallpaper -> Box(Modifier.fillMaxSize().background(solid.background.copy(alpha = 0.30f)))
                    (glass.enabled || customBackdrop) && backdrop -> GlassBackdrop(solid)
                }
                content()
            }
        }
    }
}

private val LocalButtonShape = staticCompositionLocalOf<CornerBasedShape> { CircleShape }

/** Material buttons are always pill shaped; Yoink's follow the chosen corner style. */
val ButtonShape: CornerBasedShape
    @Composable @ReadOnlyComposable get() = LocalButtonShape.current

fun colorScheme(context: Context, appearance: Appearance, dark: Boolean): ColorScheme {
    val wallpaper = appearance.dynamicColor && Build.VERSION.SDK_INT >= 31
    val standard = appearance.palette == PaletteStyle.TONAL && appearance.contrast == Contrast.STANDARD
    val scheme = when {
        // The exact system palette, so Yoink matches the rest of the phone.
        wallpaper && standard -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        wallpaper -> generate(wallpaperColor(context) ?: dynamicLightColorScheme(context).primary.toArgb(), appearance, dark)
        appearance.seedColor == YOINK_RED && standard -> if (dark) DarkColors else LightColors
        else -> generate(appearance.seedColor, appearance, dark)
    }
    return if (dark && appearance.pureBlack) scheme.black() else scheme
}

private fun wallpaperColor(context: Context): Int? = if (Build.VERSION.SDK_INT >= 27) {
    runCatching {
        WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor?.toArgb()
    }.getOrNull()
} else {
    null
}

@SuppressLint("RestrictedApi")
private fun generate(seed: Int, appearance: Appearance, dark: Boolean): ColorScheme {
    val source = Hct.fromInt(seed)
    val contrast = appearance.contrast.level
    val s: DynamicScheme = when (appearance.palette) {
        PaletteStyle.TONAL -> SchemeTonalSpot(source, dark, contrast)
        PaletteStyle.VIBRANT -> SchemeVibrant(source, dark, contrast)
        PaletteStyle.EXPRESSIVE -> SchemeExpressive(source, dark, contrast)
        PaletteStyle.FIDELITY -> SchemeContent(source, dark, contrast)
        PaletteStyle.NEUTRAL -> SchemeNeutral(source, dark, contrast)
        PaletteStyle.MONOCHROME -> SchemeMonochrome(source, dark, contrast)
        PaletteStyle.RAINBOW -> SchemeRainbow(source, dark, contrast)
        PaletteStyle.FRUIT_SALAD -> SchemeFruitSalad(source, dark, contrast)
    }
    val m = MaterialDynamicColors()
    fun c(color: DynamicColor) = Color(color.getArgb(s))
    return (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = c(m.primary()),
        onPrimary = c(m.onPrimary()),
        primaryContainer = c(m.primaryContainer()),
        onPrimaryContainer = c(m.onPrimaryContainer()),
        inversePrimary = c(m.inversePrimary()),
        secondary = c(m.secondary()),
        onSecondary = c(m.onSecondary()),
        secondaryContainer = c(m.secondaryContainer()),
        onSecondaryContainer = c(m.onSecondaryContainer()),
        tertiary = c(m.tertiary()),
        onTertiary = c(m.onTertiary()),
        tertiaryContainer = c(m.tertiaryContainer()),
        onTertiaryContainer = c(m.onTertiaryContainer()),
        background = c(m.background()),
        onBackground = c(m.onBackground()),
        surface = c(m.surface()),
        onSurface = c(m.onSurface()),
        surfaceVariant = c(m.surfaceVariant()),
        onSurfaceVariant = c(m.onSurfaceVariant()),
        surfaceTint = c(m.surfaceTint()),
        inverseSurface = c(m.inverseSurface()),
        inverseOnSurface = c(m.inverseOnSurface()),
        error = c(m.error()),
        onError = c(m.onError()),
        errorContainer = c(m.errorContainer()),
        onErrorContainer = c(m.onErrorContainer()),
        outline = c(m.outline()),
        outlineVariant = c(m.outlineVariant()),
        scrim = c(m.scrim()),
        surfaceBright = c(m.surfaceBright()),
        surfaceContainer = c(m.surfaceContainer()),
        surfaceContainerHigh = c(m.surfaceContainerHigh()),
        surfaceContainerHighest = c(m.surfaceContainerHighest()),
        surfaceContainerLow = c(m.surfaceContainerLow()),
        surfaceContainerLowest = c(m.surfaceContainerLowest()),
        surfaceDim = c(m.surfaceDim()),
    )
}

/** Dark mode with true black behind everything, for OLED screens. */
private fun ColorScheme.black() = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = surfaceContainerLow.blend(0.5f),
    surfaceContainer = surfaceContainer.blend(0.6f),
    surfaceContainerHigh = surfaceContainerHigh.blend(0.7f),
    surfaceContainerHighest = surfaceContainerHighest.blend(0.8f),
)

private fun Color.blend(amount: Float) = Color(red * amount, green * amount, blue * amount, alpha)

fun shapes(style: CornerStyle): Shapes = when (style) {
    CornerStyle.BUBBLY -> Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(36.dp),
    )
    CornerStyle.ROUND -> Shapes()
    CornerStyle.SOFT -> Shapes(
        extraSmall = RoundedCornerShape(2.dp),
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(6.dp),
        large = RoundedCornerShape(8.dp),
        extraLarge = RoundedCornerShape(12.dp),
    )
    CornerStyle.SQUARE -> RoundedCornerShape(0.dp).let { Shapes(it, it, it, it, it) }
}

fun buttonShape(style: CornerStyle): CornerBasedShape = when (style) {
    CornerStyle.BUBBLY, CornerStyle.ROUND -> CircleShape
    CornerStyle.SOFT -> RoundedCornerShape(8.dp)
    CornerStyle.SQUARE -> RoundedCornerShape(0.dp)
}

fun fontFamily(font: FontStyle): FontFamily = when (font) {
    FontStyle.DEFAULT -> FontFamily.Default
    FontStyle.CONDENSED -> system("sans-serif-condensed")
    FontStyle.SERIF -> FontFamily.Serif
    FontStyle.MONO -> FontFamily.Monospace
    FontStyle.CASUAL -> system("casual")
    FontStyle.CURSIVE -> FontFamily.Cursive
    FontStyle.ROUNDED -> variable(R.font.nunito)
    FontStyle.MODERN -> FontFamily(Font(R.font.poppins))
    FontStyle.ELEGANT -> variable(R.font.playfair_display)
    FontStyle.COMFY -> variable(R.font.comfortaa)
    FontStyle.BUBBLY -> variable(R.font.fredoka)
    FontStyle.SCRIPT -> FontFamily(Font(R.font.pacifico))
    FontStyle.RETRO -> FontFamily(Font(R.font.lobster))
    FontStyle.GROOVY -> FontFamily(Font(R.font.righteous))
    FontStyle.FUTURE -> variable(R.font.orbitron)
    FontStyle.PIXEL -> FontFamily(Font(R.font.press_start_2p))
    FontStyle.COMIC -> FontFamily(Font(R.font.bangers))
    FontStyle.TYPEWRITER -> FontFamily(Font(R.font.special_elite))
}

private fun system(name: String) = FontFamily(Typeface.create(name, Typeface.NORMAL))

/** One variable font file, used at the weights Material's text styles ask for. */
@OptIn(ExperimentalTextApi::class)
private fun variable(@FontRes res: Int) = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(res, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
    },
)

private fun typography(font: FontStyle): Typography {
    val base = Typography()
    if (font == FontStyle.DEFAULT) return base
    val family = fontFamily(font)
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family),
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
