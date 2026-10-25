package io.github.hishamhashmi100.yoink.data

import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** How the full color scheme is built from the one seed color. */
enum class PaletteStyle { TONAL, VIBRANT, EXPRESSIVE, FIDELITY, NEUTRAL, MONOCHROME, RAINBOW, FRUIT_SALAD }

enum class Contrast(val level: Double) { STANDARD(0.0), MEDIUM(0.5), HIGH(1.0) }

/** Solid surfaces, or see-through glass panels over a soft color backdrop (clear or frosted). */
enum class GlassStyle {
    OFF, CLEAR, FROSTED;

    /** How solid the glass is unless the user picks their own (0 = invisible, 1 = solid). */
    val defaultOpacity: Float get() = if (this == FROSTED) 0.55f else 0.30f
}

/**
 * What's behind the glass (or behind solid screens): soft blobs of the theme's colors, one color or a blend of
 * two colors the user picks, or a picture of their own. (Looks saved with the old "Your wallpaper" option load
 * as theme colors.)
 */
enum class GlassBehind { THEME, ONE_COLOR, TWO_COLORS, PICTURE }

enum class CornerStyle { BUBBLY, ROUND, SOFT, SQUARE }

/** Fonts for the whole app. The first few are built into Android; the rest ship with Yoink (res/font). */
enum class FontStyle {
    DEFAULT, CONDENSED, SERIF, MONO, CASUAL, CURSIVE,
    ROUNDED, MODERN, ELEGANT, COMFY, BUBBLY, SCRIPT, RETRO, GROOVY, FUTURE, PIXEL, COMIC, TYPEWRITER,
}

/** The brand red from docs/logo.svg. */
const val YOINK_RED = 0xFFD90F1A.toInt()

/**
 * One surface style's own background. Solid, Glass and Frosted glass each keep their own, so picking
 * colors for one doesn't change the others.
 */
data class StyleLook(
    val behind: GlassBehind = GlassBehind.THEME,
    /** The ticked colors, first to last (one or two of them). */
    val colors: List<Int> = DEFAULT_LOOK_COLORS,
    /** Glass opacity, or null for the style's default. */
    val opacity: Float? = null,
    /** The picture behind the app: Yoink's own copy of a photo the user picked (see Pictures). */
    val picture: String? = null,
    /** The picture's average color, which the app's colors are built from. */
    val pictureColor: Int? = null,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("behind", behind.name)
        .put("colors", JSONArray(colors))
        .put("opacity", opacity?.toDouble() ?: -1.0)
        .apply {
            picture?.let { put("picture", it) }
            pictureColor?.let { put("pictureColor", it) }
        }

    companion object {
        fun fromJson(json: JSONObject?): StyleLook {
            if (json == null) return StyleLook()
            val colors = json.optJSONArray("colors")?.let { array -> (0 until array.length()).map { array.optInt(it) } }
            return StyleLook(
                behind = enumOf(json.optString("behind"), GlassBehind.THEME),
                colors = colors?.takeIf { it.isNotEmpty() }?.take(2) ?: DEFAULT_LOOK_COLORS,
                opacity = json.optDouble("opacity", -1.0).toFloat().takeIf { it in 0f..1f },
                picture = json.optString("picture").ifBlank { null },
                pictureColor = if (json.has("pictureColor")) json.optInt("pictureColor") else null,
            )
        }

        fun mapToJson(looks: Map<GlassStyle, StyleLook>): JSONObject = JSONObject().apply {
            looks.forEach { (style, look) -> put(style.name, look.toJson()) }
        }

        fun mapFromJson(json: JSONObject?): Map<GlassStyle, StyleLook> =
            GlassStyle.entries.mapNotNull { style -> json?.optJSONObject(style.name)?.let { style to fromJson(it) } }.toMap()
    }
}

val DEFAULT_LOOK_COLORS = listOf(0xFF3D5AFE.toInt(), 0xFFEC407A.toInt())

/** Everything that changes how Yoink looks. */
data class Appearance(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Dark mode with a pure black background, in every surface style. */
    val pureBlack: Boolean = false,
    /** Take the seed color from the wallpaper (Android 12 and newer). */
    val dynamicColor: Boolean = true,
    val seedColor: Int = YOINK_RED,
    val palette: PaletteStyle = PaletteStyle.TONAL,
    val contrast: Contrast = Contrast.STANDARD,
    val corners: CornerStyle = CornerStyle.ROUND,
    val font: FontStyle = FontStyle.DEFAULT,
    val glass: GlassStyle = GlassStyle.OFF,
    /** Each surface style's own background (see [look]). */
    val looks: Map<GlassStyle, StyleLook> = emptyMap(),
    /** How light or dark the full-screen player's glass is (0 = white, 1 = black). */
    val playerShade: Float = 0.3f,
) {
    /** The background of the surface style in use. */
    val look: StyleLook get() = looks[glass] ?: StyleLook()

    fun withLook(change: (StyleLook) -> StyleLook): Appearance = copy(looks = looks + (glass to change(look)))

    /** What's behind (theme colors until a picture has been picked for "Your picture"). */
    val glassBehind: GlassBehind
        get() = if (look.behind == GlassBehind.PICTURE && look.picture == null) GlassBehind.THEME else look.behind

    /** The picture behind the app, when that's what's chosen. */
    val picture: String? get() = look.picture.takeIf { glassBehind == GlassBehind.PICTURE }

    val glassOpacity: Float? get() = look.opacity
    val effectiveGlassOpacity: Float get() = look.opacity ?: glass.defaultOpacity
    // With a picture, its average color stands in for the background colors.
    val backdropColor1: Int get() = picture?.let { look.pictureColor } ?: look.colors.first()
    val backdropColor2: Int get() = picture?.let { look.pictureColor } ?: look.colors.getOrElse(1) { look.colors.first() }

    /** The user picked the background: color(s) or a picture (pure black, which overrides them, is off). */
    val customBackdrop: Boolean
        get() = !pureBlack && glassBehind in setOf(GlassBehind.ONE_COLOR, GlassBehind.TWO_COLORS, GlassBehind.PICTURE)

    /**
     * The settings the color scheme is built from. With their own background colors, the accent follows the
     * first of them, so buttons match the background instead of clashing with it.
     */
    val colorSource: Appearance
        get() = if (customBackdrop) copy(seedColor = backdropColor1, dynamicColor = false) else this

    fun toJson(): JSONObject = JSONObject()
        .put("theme", theme.name)
        .put("pureBlack", pureBlack)
        .put("dynamicColor", dynamicColor)
        .put("seedColor", seedColor)
        .put("palette", palette.name)
        .put("contrast", contrast.name)
        .put("corners", corners.name)
        .put("font", font.name)
        .put("glass", glass.name)
        .put("looks", StyleLook.mapToJson(looks))
        .put("playerShade", playerShade.toDouble())

    companion object {
        fun fromJson(json: JSONObject): Appearance {
            val default = Appearance()
            return Appearance(
                theme = enumOf(json.optString("theme"), default.theme),
                pureBlack = json.optBoolean("pureBlack", default.pureBlack),
                dynamicColor = json.optBoolean("dynamicColor", default.dynamicColor),
                seedColor = json.optInt("seedColor", default.seedColor),
                palette = enumOf(json.optString("palette"), default.palette),
                contrast = enumOf(json.optString("contrast"), default.contrast),
                corners = enumOf(json.optString("corners"), default.corners),
                font = enumOf(json.optString("font"), default.font),
                glass = enumOf(json.optString("glass"), default.glass),
                playerShade = json.optDouble("playerShade", default.playerShade.toDouble()).toFloat().coerceIn(0f, 1f),
                looks = json.optJSONObject("looks")?.let(StyleLook::mapFromJson) ?: legacyLooks(
                    behind = enumOf(json.optString("glassBehind"), GlassBehind.THEME),
                    color1 = json.optInt("backdropColor1", DEFAULT_LOOK_COLORS[0]),
                    color2 = json.optInt("backdropColor2", DEFAULT_LOOK_COLORS[1]),
                    opacity = json.optDouble("glassOpacity", -1.0).toFloat().takeIf { it in 0f..1f },
                ),
            )
        }

        /**
         * A look with every option picked at random: light or dark, colors, palette, contrast, corners, font,
         * surface style, what's behind it, its opacity and the player's shade.
         */
        fun random(random: Random = Random): Appearance {
            // Lively colors: any hue, never gray or muddy.
            fun color() = Color.HSVToColor(
                floatArrayOf(random.nextFloat() * 360f, 0.45f + random.nextFloat() * 0.55f, 0.55f + random.nextFloat() * 0.45f),
            )
            val glass = GlassStyle.entries.random(random)
            // Not "Your picture": there may be none, and it's the user's to pick.
            val behind = GlassBehind.entries
                .filter { it != GlassBehind.PICTURE }
                .random(random)
            val look = StyleLook(
                behind = behind,
                colors = listOf(color(), color()),
                // Kept between "barely there" and "nearly solid", so the glass still reads as glass.
                opacity = if (glass == GlassStyle.OFF) null else 0.2f + random.nextFloat() * 0.6f,
            )
            return Appearance(
                theme = ThemeMode.entries.random(random),
                pureBlack = random.nextInt(5) == 0,
                // Now and then the wallpaper's colors.
                dynamicColor = random.nextInt(6) == 0,
                seedColor = color(),
                palette = PaletteStyle.entries.random(random),
                contrast = Contrast.entries.random(random),
                corners = CornerStyle.entries.random(random),
                font = FontStyle.entries.random(random),
                glass = glass,
                looks = mapOf(glass to look),
                playerShade = random.nextFloat(),
            )
        }

        /** Before each style had its own background there was one, shared by both glass styles. */
        fun legacyLooks(behind: GlassBehind, color1: Int, color2: Int, opacity: Float?): Map<GlassStyle, StyleLook> {
            val look = StyleLook(behind, listOf(color1, color2), opacity)
            return mapOf(GlassStyle.CLEAR to look, GlassStyle.FROSTED to look)
        }
    }
}

/** A look the user saved under a name of their own. */
data class SavedTheme(val name: String, val appearance: Appearance) {
    companion object {
        fun listToJson(themes: List<SavedTheme>): String = JSONArray().apply {
            themes.forEach { put(JSONObject().put("name", it.name).put("appearance", it.appearance.toJson())) }
        }.toString()

        fun listFromJson(text: String?): List<SavedTheme> {
            if (text.isNullOrBlank()) return emptyList()
            val array = runCatching { JSONArray(text) }.getOrNull() ?: return emptyList()
            return (0 until array.length()).mapNotNull { i ->
                val item = array.optJSONObject(i) ?: return@mapNotNull null
                val name = item.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                SavedTheme(name, Appearance.fromJson(item.optJSONObject("appearance") ?: JSONObject()))
            }
        }
    }
}

/** Ready-made looks. They keep the user's light/dark choice and contrast. */
enum class ThemePreset(
    val seedColor: Int,
    val palette: PaletteStyle,
    val corners: CornerStyle,
    val font: FontStyle,
    val pureBlack: Boolean = false,
) {
    YOINK(YOINK_RED, PaletteStyle.TONAL, CornerStyle.ROUND, FontStyle.DEFAULT),
    MIDNIGHT(0xFF3D5AFE.toInt(), PaletteStyle.TONAL, CornerStyle.SOFT, FontStyle.DEFAULT, pureBlack = true),
    FOREST(0xFF2E7D32.toInt(), PaletteStyle.TONAL, CornerStyle.ROUND, FontStyle.SERIF),
    OCEAN(0xFF00897B.toInt(), PaletteStyle.VIBRANT, CornerStyle.BUBBLY, FontStyle.DEFAULT),
    SUNSET(0xFFFF6D00.toInt(), PaletteStyle.EXPRESSIVE, CornerStyle.BUBBLY, FontStyle.DEFAULT),
    GRAPE(0xFF8E24AA.toInt(), PaletteStyle.VIBRANT, CornerStyle.ROUND, FontStyle.DEFAULT),
    CANDY(0xFFEC407A.toInt(), PaletteStyle.FRUIT_SALAD, CornerStyle.BUBBLY, FontStyle.CASUAL),
    RETRO(0xFFFFC83D.toInt(), PaletteStyle.FIDELITY, CornerStyle.SQUARE, FontStyle.SERIF),
    NOIR(0xFF000000.toInt(), PaletteStyle.MONOCHROME, CornerStyle.SQUARE, FontStyle.MONO, pureBlack = true),
    COZY(0xFFC8A97E.toInt(), PaletteStyle.TONAL, CornerStyle.BUBBLY, FontStyle.COMFY),
    ROYAL(0xFF1A237E.toInt(), PaletteStyle.FIDELITY, CornerStyle.SOFT, FontStyle.ELEGANT),
    ARCADE(0xFFD500F9.toInt(), PaletteStyle.VIBRANT, CornerStyle.SQUARE, FontStyle.PIXEL, pureBlack = true),
    COMIC(0xFFFFAB00.toInt(), PaletteStyle.FRUIT_SALAD, CornerStyle.ROUND, FontStyle.COMIC),
    SPACE(0xFF00ACC1.toInt(), PaletteStyle.EXPRESSIVE, CornerStyle.SOFT, FontStyle.FUTURE, pureBlack = true);

    fun applyTo(current: Appearance) = current.copy(
        dynamicColor = false,
        seedColor = seedColor,
        palette = palette,
        corners = corners,
        font = font,
        pureBlack = pureBlack,
    )

    fun matches(appearance: Appearance) = applyTo(appearance) == appearance
}

inline fun <reified T : Enum<T>> enumOf(name: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default
