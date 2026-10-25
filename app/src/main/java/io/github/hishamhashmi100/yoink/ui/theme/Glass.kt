package io.github.hishamhashmi100.yoink.ui.theme

import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import io.github.hishamhashmi100.yoink.data.GlassBehind
import io.github.hishamhashmi100.yoink.data.GlassStyle
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
    /** The phone's wallpaper shows through instead of the theme's color blobs. */
    val wallpaper: Boolean = false,
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
        get() {
            if (!enabled || colors.isEmpty()) return SolidColor(tint)
            val neutral = if (dark) Color(0xFF17171C) else Color.White
            val stops = colors.map { lerp(neutral, it, 0.5f).copy(alpha = opacity) }
            return Brush.linearGradient(if (stops.size == 1) stops + stops else stops)
        }

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
class GlassOverlay(val colors: ColorScheme, val glass: Glass)

val LocalGlassOverlay = staticCompositionLocalOf<GlassOverlay?> { null }

/** Shows [content] in the dark overlay glass when glass is on; unchanged otherwise. */
@Composable
fun OverlayGlass(content: @Composable () -> Unit) {
    val overlay = LocalGlassOverlay.current
    if (overlay == null || !LocalGlass.current.enabled) {
        content()
        return
    }
    MaterialTheme(colorScheme = overlay.colors, shapes = MaterialTheme.shapes, typography = MaterialTheme.typography) {
        CompositionLocalProvider(LocalGlass provides overlay.glass, content = content)
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
        .then(if (hazeState != null) Modifier.hazeEffect(hazeState, glass.hazeStyle) else Modifier.background(glass.tintBrush))
        .border(1.dp, glass.rim, shape)
}

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

/** What's drawn behind the glass when it isn't the wallpaper (see [GlassBehind]). */
@Immutable
data class Backdrop(
    val behind: GlassBehind = GlassBehind.THEME,
    val color1: Color = Color.Unspecified,
    val color2: Color = Color.Unspecified,
)

val LocalBackdrop = staticCompositionLocalOf { Backdrop() }

/**
 * Glass needs something colorful behind it: the user's color, or blend of two colors, or else soft blobs
 * of the theme's colors, like a phone wallpaper.
 */
@Composable
fun GlassBackdrop(colors: ColorScheme, modifier: Modifier = Modifier, backdrop: Backdrop = LocalBackdrop.current) {
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
