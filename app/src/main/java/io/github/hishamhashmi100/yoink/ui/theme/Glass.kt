package io.github.hishamhashmi100.yoink.ui.theme

import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import coil3.compose.AsyncImage
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import io.github.hishamhashmi100.yoink.data.GlassBehind
import io.github.hishamhashmi100.yoink.data.GlassStyle
import java.io.File
import kotlin.math.abs
import kotlin.math.max

/** The glass look in effect, for the few places that draw glass panels themselves. */
@Immutable
data class Glass(
    val style: GlassStyle,
    val dark: Boolean,
    val backdrop: Color,
    /** The theme's color, which tints the glass the way colored glass would. */
    val accent: Color = Color.Unspecified,
    /** How solid the glass is, from the Appearance slider (0 = invisible, 1 = solid). */
    val opacity: Float = style.defaultOpacity,
    /** The user's own background colors (one or two), which tint the glass; empty for theme colors. */
    val colors: List<Color> = emptyList(),
) {

    val enabled: Boolean get() = style != GlassStyle.OFF

    /** White (or near-black in dark mode) with a little of the theme's color mixed in. */
    private val base: Color
        get() {
            val neutral = if (dark) Color(0xFF17171C) else Color.White
            return if (accent.isSpecified) lerp(neutral, accent, if (dark) 0.22f else 0.16f) else neutral
        }

    /**
     * The one tint every glass surface uses (cards, bars, dialogs, sheets), so glass looks the same
     * everywhere. Its strength is the user's opacity setting.
     */
    val tint: Color
        get() = if (enabled) base.copy(alpha = opacity) else Color.Transparent

    /**
     * For menus, which pop up over busy thumbnails and can't blur what's behind them: the same glass, a
     * little thicker so their text stays readable.
     */
    val menuTint: Color
        get() = if (enabled) base.copy(alpha = (opacity + 0.35f).coerceAtMost(0.92f)) else Color.Transparent

    val blur: Dp get() = if (style == GlassStyle.FROSTED) 30.dp else 8.dp

    /** The bright rim along the top edge of a pane of glass. */
    val rim: Brush
        get() = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = if (dark) 0.30f else 0.85f),
                Color.White.copy(alpha = if (dark) 0.06f else 0.30f),
            ),
        )

    /**
     * What panels are painted with: with the user's own colors, a gradient of them (softened toward white,
     * or black in dark mode) like colored glass; otherwise the plain [tint].
     */
    val tintBrush: Brush
        get() = tintBrushOn(if (dark) Color(0xFF17171C) else Color.White)

    /** The same colored glass, but starting from [neutral] (the full-screen player's own shade). */
    fun tintBrushOn(neutral: Color): Brush {
        if (!enabled) return SolidColor(Color.Transparent)
        if (colors.isEmpty()) {
            val base = if (accent.isSpecified) lerp(neutral, accent, 0.16f) else neutral
            return SolidColor(base.copy(alpha = opacity))
        }
        val stops = colors.map { lerp(neutral, it, 0.5f).copy(alpha = opacity) }
        return Brush.linearGradient(if (stops.size == 1) stops + stops else stops)
    }

    /**
     * How solid the full-screen player's glass is at [shade] (0 = white, 1 = black): thicker toward either
     * end, so the shade slider makes a real difference. Clear glass stays fairly clear.
     */
    fun playerOpacity(shade: Float): Float = (opacity * (1f + 0.9f * edge(shade))).coerceAtMost(0.96f)

    /**
     * The full-screen player's glass at [shade]: milky white at one end, smoky black at the other. The
     * user's colors tint it most in the middle and fade toward either end, so white looks white and black
     * looks black.
     */
    fun playerTint(shade: Float): Brush {
        if (!enabled) return SolidColor(Color.Transparent)
        val neutral = lerp(Color.White, Color(0xFF0E0E12), shade)
        val alpha = playerOpacity(shade)
        val color = 1f - edge(shade)
        if (colors.isEmpty()) {
            val base = if (accent.isSpecified) lerp(neutral, accent, 0.16f * color) else neutral
            return SolidColor(base.copy(alpha = alpha))
        }
        val stops = colors.map { lerp(neutral, it, 0.12f + 0.4f * color).copy(alpha = alpha) }
        return Brush.linearGradient(if (stops.size == 1) stops + stops else stops)
    }

    /** 0 for a shade in the middle, 1 at either end. */
    private fun edge(shade: Float) = abs(shade - 0.5f) * 2f

    val hazeStyle: HazeStyle
        get() = HazeStyle(
            backgroundColor = backdrop,
            tint = if (colors.isEmpty()) HazeTint(tint) else HazeTint(tintBrush),
            blurRadius = blur,
            noiseFactor = if (style == GlassStyle.FROSTED) 0.08f else 0f,
        )
}

val LocalGlass = staticCompositionLocalOf { Glass(GlassStyle.OFF, dark = false, backdrop = Color.White) }

/** The theme's full palette (before [oneAccent]), for the backdrop's colored blobs. */
val LocalBackdropColors = staticCompositionLocalOf<ColorScheme?> { null }

/**
 * Dark glass with white text, for things that pop up over the app (dialogs, menus, the share sheet).
 * Behind them the app is dimmed or busy, where dark text on light glass can't be read.
 */
class GlassOverlay(
    val colors: ColorScheme,
    val glass: Glass,
    /** The light version (dark text), for the full-screen player when its glass is light and solid enough. */
    val lightColors: ColorScheme,
    val lightGlass: Glass,
    /** The full-screen player's shade, from the Appearance slider (0 = white, 1 = black). */
    val playerShade: Float,
)

val LocalGlassOverlay = staticCompositionLocalOf<GlassOverlay?> { null }

/** Shows [content] in the dark overlay glass when glass is on; unchanged otherwise. */
@Composable
fun OverlayGlass(content: @Composable () -> Unit) {
    val overlay = LocalGlassOverlay.current
    if (overlay == null || !LocalGlass.current.enabled) {
        content()
        return
    }
    GlassTheme(overlay.colors, overlay.glass, content)
}

/** [colors] and [glass] for [content], text color included (MaterialTheme alone leaves text the old color). */
@Composable
fun GlassTheme(colors: ColorScheme, glass: Glass, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, shapes = MaterialTheme.shapes, typography = MaterialTheme.typography) {
        CompositionLocalProvider(
            LocalGlass provides glass,
            LocalContentColor provides colors.onSurface,
            content = content,
        )
    }
}

/** Set by screens whose content can be blurred behind glass panels (see Modifier.hazeSource). */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * A pane of glass: blurs the content behind it when [hazeState] is given, otherwise just tints, and
 * draws the rim. Does nothing with solid surfaces.
 */
fun Modifier.glassPanel(glass: Glass, shape: Shape, hazeState: HazeState? = null): Modifier {
    if (!glass.enabled) return this
    return clip(shape)
        .then(if (hazeState != null) Modifier.lightHaze(hazeState, glass.hazeStyle) else Modifier.background(glass.tintBrush))
        .border(1.dp, glass.rim, shape)
}

/**
 * The blur behind glass, worked out on a smaller copy of what's behind it. It looks the same once blurred,
 * but costs far less on every frame of a scrolling list, which kept scrolling smooth on mid-range phones.
 */
@OptIn(ExperimentalHazeApi::class)
fun Modifier.lightHaze(state: HazeState, style: HazeStyle): Modifier =
    hazeEffect(state, style) { inputScale = HazeInputScale.Auto }

/** An elevated card, or a glass one. */
@Composable
fun YoinkCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val glass = LocalGlass.current
    if (!glass.enabled) {
        ElevatedCard(modifier, content = content)
    } else {
        val shape = MaterialTheme.shapes.medium
        Card(
            modifier = modifier.glassPanel(glass, shape),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            content = content,
        )
    }
}

/** What's drawn behind the glass (see [GlassBehind]). */
@Immutable
data class Backdrop(
    val behind: GlassBehind = GlassBehind.THEME,
    val color1: Color = Color.Unspecified,
    val color2: Color = Color.Unspecified,
    /** For [GlassBehind.PICTURE]: the user's picture (a file). */
    val picture: String? = null,
)

val LocalBackdrop = staticCompositionLocalOf { Backdrop() }

/**
 * Glass needs something colorful behind it: the user's picture, their color or blend of two colors, or else
 * soft blobs of the theme's colors, like a phone wallpaper.
 */
@Composable
fun GlassBackdrop(colors: ColorScheme, modifier: Modifier = Modifier, backdrop: Backdrop = LocalBackdrop.current) {
    if (backdrop.behind == GlassBehind.PICTURE && backdrop.picture != null) {
        // Drawn by the app itself, so the glass can blur it, and every screen covers the one beneath it.
        Box(modifier.fillMaxSize().background(backdrop.color1)) {
            AsyncImage(
                model = File(backdrop.picture),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            // A light wash of the theme, so text on the picture stays readable.
            Box(Modifier.matchParentSize().background(colors.background.copy(alpha = 0.30f)))
        }
        return
    }
    Canvas(modifier.fillMaxSize()) {
        val radius = max(size.width, size.height) * 0.62f
        fun blob(color: Color, alpha: Float, center: Offset, scale: Float = 1f) = drawCircle(
            Brush.radialGradient(listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)), center, radius * scale),
            radius * scale,
            center,
        )
        // Soft light from the top and shade at the bottom give flat colors some depth, like a wallpaper.
        fun depth() {
            blob(Color.White, 0.22f, Offset(size.width * 0.15f, size.height * 0.05f), 0.8f)
            blob(Color.Black, 0.12f, Offset(size.width * 0.9f, size.height * 1.0f), 0.9f)
        }
        when (backdrop.behind) {
            GlassBehind.ONE_COLOR -> {
                drawRect(backdrop.color1)
                depth()
            }
            GlassBehind.TWO_COLORS -> {
                drawRect(Brush.linearGradient(listOf(backdrop.color1, backdrop.color2), Offset.Zero, Offset(size.width, size.height)))
                depth()
            }
            else -> {
                drawRect(
                    Brush.linearGradient(
                        listOf(colors.primaryContainer, colors.background, colors.tertiaryContainer),
                        Offset.Zero,
                        Offset(size.width, size.height),
                    ),
                )
                blob(colors.primary, 0.50f, Offset(size.width * 0.05f, size.height * 0.10f))
                blob(colors.tertiary, 0.42f, Offset(size.width * 1.0f, size.height * 0.45f))
                blob(colors.secondary, 0.38f, Offset(size.width * 0.15f, size.height * 0.95f))
            }
        }
    }
}

/**
 * Fades content out over its bottom [height], so lists melt away before they slide under the glass mini
 * player instead of showing half-hidden thumbnails through it.
 */
fun Modifier.fadeOutBottom(height: Dp): Modifier {
    if (height <= 0.dp) return this
    return graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
        drawContent()
        val fade = height.toPx()
        val top = size.height - fade - 24.dp.toPx()
        drawRect(
            Brush.verticalGradient(listOf(Color.Black, Color.Transparent), startY = top, endY = size.height - fade * 0.4f),
            topLeft = Offset(0f, top),
            size = Size(size.width, size.height - top),
            blendMode = BlendMode.DstIn,
        )
    }
}

/**
 * Material's own surfaces in the glass tint: screens and bars are clear over the backdrop, and every
 * container (dialogs, menus, sheets, cards) is the same pane of glass as Yoink's own panels.
 */
fun ColorScheme.glassy(glass: Glass): ColorScheme = copy(
    background = Color.Transparent,
    surface = Color.Transparent,
    surfaceContainerLowest = glass.tint,
    surfaceContainerLow = glass.tint,
    // Menus use this one.
    surfaceContainer = glass.menuTint,
    surfaceContainerHigh = glass.tint,
    surfaceContainerHighest = glass.tint,
)

/**
 * Whether windows can blur what's behind them right now (Android 12+, and not switched off, e.g. by
 * battery saver). Without it, see-through dialogs and sheets fall back to the thicker menu glass.
 */
fun windowBlurAvailable(context: Context): Boolean =
    Build.VERSION.SDK_INT >= 31 && context.getSystemService(WindowManager::class.java)?.isCrossWindowBlurEnabled == true

/**
 * One accent color across the app. Material gives some buttons the secondary or tertiary color, so a
 * palette could make one button blue and the next green; here everything uses the primary color.
 * (The glass backdrop keeps the other colors, where the variety looks good.)
 */
fun ColorScheme.oneAccent(): ColorScheme = copy(
    secondary = primary,
    onSecondary = onPrimary,
    secondaryContainer = primaryContainer,
    onSecondaryContainer = onPrimaryContainer,
    tertiary = primary,
    onTertiary = onPrimary,
    tertiaryContainer = primaryContainer,
    onTertiaryContainer = onPrimaryContainer,
)

/**
 * Inside a dialog or sheet with glass on: blurs what's behind its window (Android 12 and newer), so the
 * see-through pane stays readable, the same frosted feel as the panels.
 */
@Composable
fun GlassWindowBlur(strength: Dp? = null) {
    val glass = LocalGlass.current
    if (!glass.enabled || Build.VERSION.SDK_INT < 31) return
    val view = LocalView.current
    val radius = with(LocalDensity.current) { (strength ?: glass.blur).roundToPx() }
    DisposableEffect(view, radius) {
        runCatching {
            var parent = view.parent
            while (parent != null && parent !is DialogWindowProvider) parent = parent.parent
            (parent as? DialogWindowProvider)?.window?.let { window ->
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.attributes = window.attributes.also { it.blurBehindRadius = radius }
            }
        }
        onDispose {}
    }
}

/** Material's AlertDialog with the glass rim and window blur. Same parameters, so it drops in. */
@Composable
fun YoinkAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    OverlayGlass { GlassAlertDialog(onDismissRequest, confirmButton, modifier, dismissButton, title, text) }
}

@Composable
private fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier,
    dismissButton: @Composable (() -> Unit)?,
    title: @Composable (() -> Unit)?,
    text: @Composable (() -> Unit)?,
) {
    val glass = LocalGlass.current
    val shape = MaterialTheme.shapes.extraLarge
    val context = LocalContext.current
    val blur = remember { windowBlurAvailable(context) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            GlassWindowBlur()
            confirmButton()
        },
        modifier = if (glass.enabled) modifier.border(1.dp, glass.rim, shape) else modifier,
        dismissButton = dismissButton,
        title = title,
        text = text,
        shape = shape,
        containerColor = if (glass.enabled && !blur) glass.menuTint else AlertDialogDefaults.containerColor,
        tonalElevation = if (glass.enabled) 0.dp else AlertDialogDefaults.TonalElevation,
    )
}
