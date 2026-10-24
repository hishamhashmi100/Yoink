package io.github.hishamhashmi100.yoink.data

import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** How the full color scheme is built from the one seed color. */
enum class PaletteStyle { TONAL, VIBRANT, EXPRESSIVE, FIDELITY, NEUTRAL, MONOCHROME, RAINBOW, FRUIT_SALAD }

enum class Contrast(val level: Double) { STANDARD(0.0), MEDIUM(0.5), HIGH(1.0) }

enum class CornerStyle { BUBBLY, ROUND, SOFT, SQUARE }

/** System font families, so nothing has to be bundled. */
enum class FontStyle(val family: String?) {
    DEFAULT(null),
    CONDENSED("sans-serif-condensed"),
    SERIF("serif"),
    MONO("monospace"),
    CASUAL("casual"),
    CURSIVE("cursive"),
}

/** The brand red from docs/logo.svg. */
const val YOINK_RED = 0xFFD90F1A.toInt()

/** Everything that changes how Yoink looks. */
data class Appearance(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Black instead of dark grey backgrounds in dark mode. */
    val pureBlack: Boolean = false,
    /** Take the seed color from the wallpaper (Android 12 and newer). */
    val dynamicColor: Boolean = true,
    val seedColor: Int = YOINK_RED,
    val palette: PaletteStyle = PaletteStyle.TONAL,
    val contrast: Contrast = Contrast.STANDARD,
    val corners: CornerStyle = CornerStyle.ROUND,
    val font: FontStyle = FontStyle.DEFAULT,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("theme", theme.name)
        .put("pureBlack", pureBlack)
        .put("dynamicColor", dynamicColor)
        .put("seedColor", seedColor)
        .put("palette", palette.name)
        .put("contrast", contrast.name)
        .put("corners", corners.name)
        .put("font", font.name)

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
            )
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
    NOIR(0xFF000000.toInt(), PaletteStyle.MONOCHROME, CornerStyle.SQUARE, FontStyle.MONO, pureBlack = true);

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
