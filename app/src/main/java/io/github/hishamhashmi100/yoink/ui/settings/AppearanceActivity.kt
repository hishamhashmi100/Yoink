package io.github.hishamhashmi100.yoink.ui.settings

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.data.Appearance
import io.github.hishamhashmi100.yoink.data.Contrast
import io.github.hishamhashmi100.yoink.data.CornerStyle
import io.github.hishamhashmi100.yoink.data.Download
import io.github.hishamhashmi100.yoink.data.DownloadStatus
import io.github.hishamhashmi100.yoink.data.FontStyle
import io.github.hishamhashmi100.yoink.data.GlassBehind
import io.github.hishamhashmi100.yoink.data.GlassStyle
import io.github.hishamhashmi100.yoink.data.PaletteStyle
import io.github.hishamhashmi100.yoink.data.SavedTheme
import io.github.hishamhashmi100.yoink.data.StyleLook
import io.github.hishamhashmi100.yoink.data.ThemeMode
import io.github.hishamhashmi100.yoink.data.ThemePreset
import io.github.hishamhashmi100.yoink.data.YOINK_RED
import io.github.hishamhashmi100.yoink.download.Quality
import io.github.hishamhashmi100.yoink.ui.home.LibraryFilter
import io.github.hishamhashmi100.yoink.ui.home.LibraryItem
import io.github.hishamhashmi100.yoink.ui.home.MiniPlayerBar
import io.github.hishamhashmi100.yoink.ui.player.NowPlaying
import io.github.hishamhashmi100.yoink.ui.player.PlaybackService
import io.github.hishamhashmi100.yoink.ui.theme.ButtonShape
import io.github.hishamhashmi100.yoink.ui.theme.Glass
import io.github.hishamhashmi100.yoink.ui.theme.LocalGlass
import io.github.hishamhashmi100.yoink.ui.theme.WallpaperScreens
import io.github.hishamhashmi100.yoink.ui.theme.YoinkAlertDialog
import io.github.hishamhashmi100.yoink.ui.theme.YoinkTheme
import io.github.hishamhashmi100.yoink.ui.theme.colorScheme
import io.github.hishamhashmi100.yoink.ui.theme.fontFamily
import io.github.hishamhashmi100.yoink.ui.theme.glassPanel
import io.github.hishamhashmi100.yoink.ui.theme.isDarkTheme
import io.github.hishamhashmi100.yoink.ui.theme.shapes
import io.github.hishamhashmi100.yoink.yoink
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppearanceActivity : ComponentActivity() {

    override fun finish() {
        WallpaperScreens.finishing(this)
        super.finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WallpaperScreens.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AppearanceViewModel = viewModel()
            val appearance by viewModel.appearance.collectAsStateWithLifecycle()
            YoinkTheme(appearance) {
                AppearanceScreen(viewModel, onBack = ::finish)
            }
        }
    }
}

class AppearanceViewModel(application: Application) : AndroidViewModel(application) {

    private val store = application.yoink.settings

    private val _appearance = MutableStateFlow(store.current().appearance)

    /** What the screen shows: changes appear right away, and are saved in the background. */
    val appearance: StateFlow<Appearance> = _appearance

    // Only the newest change waits to be saved, so dragging a slider doesn't queue up a write per frame.
    private val toSave = Channel<Appearance>(Channel.CONFLATED)

    // The last change asked for, until it's saved. Older saved values arriving meanwhile are ignored,
    // so the screen never jumps back to them.
    private var pending: Appearance? = null

    init {
        viewModelScope.launch {
            store.settings.map { it.appearance }.collect { saved ->
                if (pending == null || saved == pending) {
                    pending = null
                    _appearance.value = saved
                }
            }
        }
        viewModelScope.launch { for (next in toSave) store.setAppearance(next) }
    }

    val savedThemes: StateFlow<List<SavedTheme>> =
        store.savedThemes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun update(change: (Appearance) -> Appearance) {
        val next = change(_appearance.value)
        if (next == _appearance.value) return
        pending = next
        _appearance.value = next
        toSave.trySend(next)
    }

    // The look before the last random one, for Undo.
    private var beforeRandom: Appearance? = null

    /** Every option at random. The other surface styles keep their own backgrounds. */
    fun randomize() {
        beforeRandom = _appearance.value
        update { current -> Appearance.random().let { it.copy(looks = current.looks + it.looks) } }
    }

    fun undoRandom() {
        val before = beforeRandom ?: return
        beforeRandom = null
        update { before }
    }

    fun save(name: String) = viewModelScope.launch { store.saveTheme(name, appearance.value) }
    fun delete(name: String) = viewModelScope.launch { store.deleteTheme(name) }
}

/** Seed colors offered as one-tap swatches. */
private val SWATCHES = listOf(
    YOINK_RED, 0xFF8B0000, 0xFFFF7043, 0xFFFF6D00, 0xFFFFAB00, 0xFFFFC83D,
    0xFFC0CA33, 0xFF7CB342, 0xFF2E7D32, 0xFF26A69A, 0xFF00897B, 0xFF00ACC1,
    0xFF039BE5, 0xFF1E88E5, 0xFF3D5AFE, 0xFF1A237E, 0xFF7C4DFF, 0xFF8E24AA,
    0xFFD500F9, 0xFFEC407A, 0xFFF8BBD0, 0xFF795548, 0xFFC8A97E, 0xFF607D8B,
    0xFF424242,
).map { it.toInt() }

@StringRes
private fun ThemePreset.label() = when (this) {
    ThemePreset.YOINK -> R.string.preset_yoink
    ThemePreset.MIDNIGHT -> R.string.preset_midnight
    ThemePreset.FOREST -> R.string.preset_forest
    ThemePreset.OCEAN -> R.string.preset_ocean
    ThemePreset.SUNSET -> R.string.preset_sunset
    ThemePreset.GRAPE -> R.string.preset_grape
    ThemePreset.CANDY -> R.string.preset_candy
    ThemePreset.RETRO -> R.string.preset_retro
    ThemePreset.NOIR -> R.string.preset_noir
    ThemePreset.COZY -> R.string.preset_cozy
    ThemePreset.ROYAL -> R.string.preset_royal
    ThemePreset.ARCADE -> R.string.preset_arcade
    ThemePreset.COMIC -> R.string.preset_comic
    ThemePreset.SPACE -> R.string.preset_space
}

@StringRes
private fun PaletteStyle.label() = when (this) {
    PaletteStyle.TONAL -> R.string.palette_tonal
    PaletteStyle.VIBRANT -> R.string.palette_vibrant
    PaletteStyle.EXPRESSIVE -> R.string.palette_expressive
    PaletteStyle.FIDELITY -> R.string.palette_fidelity
    PaletteStyle.NEUTRAL -> R.string.palette_neutral
    PaletteStyle.MONOCHROME -> R.string.palette_monochrome
    PaletteStyle.RAINBOW -> R.string.palette_rainbow
    PaletteStyle.FRUIT_SALAD -> R.string.palette_fruit_salad
}

@StringRes
private fun GlassStyle.label() = when (this) {
    GlassStyle.OFF -> R.string.glass_off
    GlassStyle.CLEAR -> R.string.glass_clear
    GlassStyle.FROSTED -> R.string.glass_frosted
}

@StringRes
private fun CornerStyle.label() = when (this) {
    CornerStyle.BUBBLY -> R.string.corners_bubbly
    CornerStyle.ROUND -> R.string.corners_round
    CornerStyle.SOFT -> R.string.corners_soft
    CornerStyle.SQUARE -> R.string.corners_square
}

@StringRes
private fun FontStyle.label() = when (this) {
    FontStyle.DEFAULT -> R.string.font_default
    FontStyle.CONDENSED -> R.string.font_condensed
    FontStyle.SERIF -> R.string.font_serif
    FontStyle.MONO -> R.string.font_mono
    FontStyle.CASUAL -> R.string.font_casual
    FontStyle.CURSIVE -> R.string.font_cursive
    FontStyle.ROUNDED -> R.string.font_rounded
    FontStyle.MODERN -> R.string.font_modern
    FontStyle.ELEGANT -> R.string.font_elegant
    FontStyle.COMFY -> R.string.font_comfy
    FontStyle.BUBBLY -> R.string.font_bubbly
    FontStyle.SCRIPT -> R.string.font_script
    FontStyle.RETRO -> R.string.font_retro
    FontStyle.GROOVY -> R.string.font_groovy
    FontStyle.FUTURE -> R.string.font_future
    FontStyle.PIXEL -> R.string.font_pixel
    FontStyle.COMIC -> R.string.font_comic
    FontStyle.TYPEWRITER -> R.string.font_typewriter
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AppearanceScreen(viewModel: AppearanceViewModel, onBack: () -> Unit) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val savedThemes by viewModel.savedThemes.collectAsStateWithLifecycle()
    var saving by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    var pickingColor by rememberSaveable { mutableStateOf(false) }
    // 1 or 2 while picking one of the backdrop colors.
    var pickingBackdrop by rememberSaveable { mutableStateOf<Int?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val randomApplied = stringResource(R.string.appearance_random_applied)
    val undo = stringResource(R.string.undo)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_appearance)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    // A whole new look at random; Undo goes back if it's not a keeper.
                    FilledTonalButton(
                        onClick = {
                            viewModel.randomize()
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                val result = snackbar.showSnackbar(randomApplied, undo, duration = SnackbarDuration.Short)
                                if (result == SnackbarResult.ActionPerformed) viewModel.undoRandom()
                            }
                        },
                        shape = ButtonShape,
                        contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Icon(Icons.Rounded.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.appearance_random), modifier = Modifier.padding(start = 8.dp))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // Stays on screen while the options scroll underneath, so every change is visible.
            LookPreview()
            HorizontalDivider()
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
            ) {
                Header(R.string.appearance_themes)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(ThemePreset.entries) { preset ->
                        ThemeCard(
                            name = stringResource(preset.label()),
                            look = preset.applyTo(appearance),
                            selected = preset.matches(appearance),
                            onClick = { viewModel.update(preset::applyTo) },
                        )
                    }
                    items(savedThemes, key = { "saved:" + it.name }) { theme ->
                        ThemeCard(
                            name = theme.name,
                            look = theme.appearance,
                            selected = theme.appearance == appearance,
                            onClick = { viewModel.update { theme.appearance } },
                            onDelete = { deleting = theme.name },
                        )
                    }
                    item {
                        Column(
                            Modifier
                                .width(76.dp)
                                .clickable { saving = true },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier
                                    .size(64.dp)
                                    .clip(MaterialTheme.shapes.large)
                                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                stringResource(R.string.appearance_save),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 2,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }

                Header(R.string.appearance_mode)
                val modes = listOf(
                    ThemeMode.SYSTEM to R.string.theme_system,
                    ThemeMode.LIGHT to R.string.theme_light,
                    ThemeMode.DARK to R.string.theme_dark,
                )
                SingleChoiceSegmentedButtonRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    modes.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            // Pure black is dark, so it shows as Dark; Light or System turns it off.
                            selected = if (appearance.pureBlack) mode == ThemeMode.DARK else appearance.theme == mode,
                            onClick = {
                                viewModel.update {
                                    it.copy(theme = mode, pureBlack = it.pureBlack && mode == ThemeMode.DARK)
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, modes.size, ButtonShape),
                        ) { OneLineLabel(stringResource(label)) }
                    }
                }
                if (appearance.customBackdrop && appearance.theme == ThemeMode.SYSTEM) {
                    Hint(R.string.appearance_mode_follows_glass)
                }
                ListItem(
                    headlineContent = { Text(stringResource(R.string.appearance_pure_black)) },
                    supportingContent = { Text(stringResource(R.string.appearance_pure_black_summary)) },
                    trailingContent = { Switch(checked = appearance.pureBlack, onCheckedChange = null) },
                    modifier = Modifier.clickable {
                        viewModel.update {
                            if (it.pureBlack) it.copy(pureBlack = false) else it.copy(pureBlack = true, theme = ThemeMode.DARK)
                        }
                    },
                )

                Header(R.string.appearance_glass)
                Hint(R.string.appearance_glass_hint)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GlassStyle.entries.forEach { style ->
                        GlassTile(
                            style = style,
                            appearance = appearance,
                            selected = appearance.glass == style,
                            onClick = { viewModel.update { it.copy(glass = style) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                StyleControls(
                    appearance = appearance,
                    onChange = viewModel::update,
                    onCustomColor = { pickingBackdrop = 1 },
                )

                Header(R.string.appearance_color)
                if (Build.VERSION.SDK_INT >= 31) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_dynamic_color)) },
                        supportingContent = { Text(stringResource(R.string.settings_dynamic_color_summary)) },
                        trailingContent = { Switch(checked = appearance.dynamicColor, onCheckedChange = null) },
                        modifier = Modifier.clickable { viewModel.update { it.copy(dynamicColor = !it.dynamicColor) } },
                    )
                }
                val wallpaper = appearance.dynamicColor && Build.VERSION.SDK_INT >= 31
                // With their own glass colors, the app's colors follow those, so nothing here is ticked;
                // picking a color goes back to theme colors behind the glass.
                val followsGlass = appearance.customBackdrop
                if (followsGlass) Hint(R.string.appearance_color_follows_glass)
                fun pick(color: Int) = viewModel.update {
                    it.copy(
                        dynamicColor = false,
                        seedColor = color,
                    ).let { picked -> if (it.customBackdrop) picked.withLook { look -> look.copy(behind = GlassBehind.THEME) } else picked }
                }
                FlowRow(
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .alpha(if (wallpaper || followsGlass) 0.45f else 1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SWATCHES.forEach { color ->
                        Swatch(
                            brush = Brush.linearGradient(listOf(Color(color), Color(color))),
                            selected = !wallpaper && !followsGlass && appearance.seedColor == color,
                            onClick = { pick(color) },
                        )
                    }
                    val custom = !wallpaper && !followsGlass && appearance.seedColor !in SWATCHES
                    Swatch(
                        brush = if (custom) {
                            Brush.linearGradient(listOf(Color(appearance.seedColor), Color(appearance.seedColor)))
                        } else {
                            Brush.sweepGradient(HUES)
                        },
                        selected = custom,
                        icon = !custom,
                        onClick = { pickingColor = true },
                    )
                }

                Header(R.string.appearance_palette)
                Hint(R.string.appearance_palette_hint)
                FlowRow(
                    Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PaletteStyle.entries.forEach { style ->
                        FilterChip(
                            selected = appearance.palette == style,
                            onClick = { viewModel.update { it.copy(palette = style) } },
                            label = { Text(stringResource(style.label())) },
                        )
                    }
                }

                Header(R.string.appearance_contrast)
                val contrasts = listOf(
                    Contrast.STANDARD to R.string.contrast_standard,
                    Contrast.MEDIUM to R.string.contrast_medium,
                    Contrast.HIGH to R.string.contrast_high,
                )
                SingleChoiceSegmentedButtonRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    contrasts.forEachIndexed { index, (contrast, label) ->
                        SegmentedButton(
                            selected = appearance.contrast == contrast,
                            onClick = { viewModel.update { it.copy(contrast = contrast) } },
                            shape = SegmentedButtonDefaults.itemShape(index, contrasts.size, ButtonShape),
                        ) { OneLineLabel(stringResource(label)) }
                    }
                }

                Header(R.string.appearance_shape)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    CornerStyle.entries.forEach { style ->
                        val selected = appearance.corners == style
                        val shape = shapes(style).large
                        Column(
                            Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { viewModel.update { it.copy(corners = style) } }
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier
                                    .size(56.dp)
                                    .clip(shape)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                            }
                            Text(
                                stringResource(style.label()),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }

                Header(R.string.appearance_font)
                FlowRow(
                    Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FontStyle.entries.forEach { font ->
                        FilterChip(
                            selected = appearance.font == font,
                            onClick = { viewModel.update { it.copy(font = font) } },
                            label = { Text(stringResource(font.label()), fontFamily = fontFamily(font)) },
                        )
                    }
                }

                TextButton(
                    onClick = { viewModel.update { Appearance() } },
                    enabled = appearance != Appearance(),
                    shape = ButtonShape,
                    modifier = Modifier.padding(start = 8.dp, top = 24.dp),
                ) { Text(stringResource(R.string.appearance_reset)) }
            }
        }
    }

    if (saving) {
        SaveThemeDialog(
            existing = savedThemes.map { it.name },
            onSave = { name ->
                viewModel.save(name)
                saving = false
            },
            onDismiss = { saving = false },
        )
    }
    deleting?.let { name ->
        YoinkAlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.appearance_delete_theme, name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(name)
                    deleting = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    pickingBackdrop?.let {
        ColorPickerDialog(
            initial = appearance.look.colors.last(),
            onPick = { color ->
                viewModel.update { it.tickColor(color) }
                pickingBackdrop = null
            },
            onDismiss = { pickingBackdrop = null },
        )
    }
    if (pickingColor) {
        ColorPickerDialog(
            initial = appearance.seedColor,
            onPick = { color ->
                viewModel.update { it.copy(dynamicColor = false, seedColor = color) }
                pickingColor = false
            },
            onDismiss = { pickingColor = false },
        )
    }
}

/**
 * Tick a background color for the surface style in use. With One color, a tap picks it. With Two colors,
 * up to two are ticked (in order): tapping a third replaces the older one, and tapping a ticked one unticks
 * it, leaving the other.
 */
private fun Appearance.tickColor(color: Int): Appearance = withLook { look ->
    when (look.behind) {
        GlassBehind.TWO_COLORS -> {
            val ticked = look.colors.take(2)
            look.copy(
                colors = when {
                    color in ticked -> if (ticked.size == 2) ticked - color else ticked
                    ticked.size < 2 -> ticked + color
                    else -> listOf(ticked[1], color)
                },
            )
        }
        // One color: remember the previous one as the second, for switching to Two colors.
        else -> look.copy(colors = listOf(color) + look.colors.filter { it != color }.take(1))
    }
}

/**
 * The background of the surface style in use (each has its own): theme colors, one or two colors ticked
 * from a palette, or (glass only) the wallpaper; plus, for glass, its opacity.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleControls(
    appearance: Appearance,
    onChange: ((Appearance) -> Appearance) -> Unit,
    onCustomColor: () -> Unit,
) {
    val glassOn = appearance.glass != GlassStyle.OFF
    Column(Modifier.padding(horizontal = 16.dp)) {
        if (glassOn) {
            Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.appearance_glass_opacity), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.appearance_percent, (appearance.effectiveGlassOpacity * 100).roundToInt()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            SmoothSlider(
                value = appearance.effectiveGlassOpacity,
                onValueChange = { value -> onChange { a -> a.withLook { it.copy(opacity = value) } } },
                valueRange = 0.05f..0.95f,
            )
            Text(
                stringResource(R.string.appearance_glass_opacity_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (appearance.glassOpacity != null) {
                TextButton(onClick = { onChange { a -> a.withLook { it.copy(opacity = null) } } }, shape = ButtonShape) {
                    Text(stringResource(R.string.appearance_glass_opacity_reset))
                }
            }

            // How light or dark the full-screen player's glass is.
            Text(
                stringResource(R.string.appearance_player_shade),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.appearance_shade_light), style = MaterialTheme.typography.labelMedium)
                SmoothSlider(
                    value = appearance.playerShade,
                    onValueChange = { value -> onChange { it.copy(playerShade = value) } },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                )
                Text(stringResource(R.string.appearance_shade_dark), style = MaterialTheme.typography.labelMedium)
            }
            Text(
                stringResource(R.string.appearance_player_shade_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            stringResource(if (glassOn) R.string.appearance_glass_behind else R.string.appearance_background),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOfNotNull(
                GlassBehind.THEME to R.string.glass_behind_theme,
                GlassBehind.ONE_COLOR to R.string.glass_behind_one,
                GlassBehind.TWO_COLORS to R.string.glass_behind_two,
                (GlassBehind.WALLPAPER to R.string.glass_behind_wallpaper).takeIf { glassOn },
            ).forEach { (behind, label) ->
                FilterChip(
                    selected = appearance.glassBehind == behind,
                    onClick = { onChange { a -> a.withLook { it.copy(behind = behind) } } },
                    label = { Text(stringResource(label)) },
                )
            }
        }

        val behind = appearance.glassBehind
        if (behind == GlassBehind.ONE_COLOR || behind == GlassBehind.TWO_COLORS) {
            val two = behind == GlassBehind.TWO_COLORS
            val ticked = if (two) appearance.look.colors.take(2) else appearance.look.colors.take(1)
            Text(
                stringResource(if (two) R.string.appearance_tick_two else R.string.appearance_tick_one),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // With two colors, the ticks are numbered: the blend runs from 1 to 2.
                fun badge(color: Int) = ticked.indexOf(color).takeIf { it >= 0 }?.let { if (two) "${it + 1}" else "" }
                SWATCHES.forEach { color ->
                    Swatch(
                        brush = Brush.linearGradient(listOf(Color(color), Color(color))),
                        selected = color in ticked,
                        badge = badge(color),
                        onClick = { onChange { it.tickColor(color) } },
                    )
                }
                val custom = ticked.firstOrNull { it !in SWATCHES }
                Swatch(
                    brush = custom?.let { Brush.linearGradient(listOf(Color(it), Color(it))) } ?: Brush.sweepGradient(HUES),
                    selected = custom != null,
                    badge = custom?.let(::badge),
                    icon = custom == null,
                    onClick = onCustomColor,
                )
            }
        }
        if (behind == GlassBehind.WALLPAPER) {
            Text(
                stringResource(R.string.glass_behind_wallpaper_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private val HUES = listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { Color.hsv(it % 360f, 1f, 1f) }

/** A button label that stays on one line, shrinking a little when a wide font (like Pixel) doesn't fit. */
@Composable
private fun OneLineLabel(text: String) {
    val style = LocalTextStyle.current
    val size = style.fontSize.takeOrElse { 14.sp }
    BasicText(
        text,
        style = style.copy(color = style.color.takeOrElse { LocalContentColor.current }),
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = size, stepSize = 0.5.sp),
    )
}

@Composable
private fun Header(@StringRes title: Int) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun Hint(@StringRes text: Int) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
    )
}

/** Sample items for the preview. Their thumbnails are drawn on the phone (see [PreviewArt]), no internet needed. */
private class PreviewSample(
    val download: Download,
    val heading: String,
    val caption: String,
    val from: Int,
    val to: Int,
)

private val PREVIEW_VIDEO = PreviewSample(
    Download(
        id = -1,
        videoId = "",
        title = "A walk through Madinah at sunset",
        channel = "Travel Diaries",
        durationSec = 760,
        mimeType = "video/mp4",
        quality = Quality.P1080,
        sizeBytes = 182_000_000,
        status = DownloadStatus.COMPLETED,
        createdAt = System.currentTimeMillis() - 3 * 3_600_000L,
        resolution = 1080,
    ),
    heading = "Madinah",
    caption = "A walk at sunset",
    from = 0xFF6D4C41.toInt(),
    to = 0xFFE0A96D.toInt(),
)

private val PREVIEW_AUDIO = listOf(
    PreviewSample(
        Download(
            id = -2,
            videoId = "",
            title = "Surah Ar-Rahman | سورة الرحمن",
            channel = "Abdul Basit Abdus Samad",
            durationSec = 1_371,
            mimeType = "audio/mpeg",
            quality = Quality.AUDIO,
            sizeBytes = 21_000_000,
            status = DownloadStatus.COMPLETED,
            createdAt = System.currentTimeMillis() - 26 * 3_600_000L,
        ),
        heading = "سورة الرحمن",
        caption = "Surah Ar-Rahman",
        from = 0xFF0F4C5C.toInt(),
        to = 0xFF1B998B.toInt(),
    ),
    PreviewSample(
        Download(
            id = -3,
            videoId = "",
            title = "Surah Al-Kahf | سورة الكهف",
            channel = "Mishary Rashid Alafasy",
            durationSec = 2_536,
            mimeType = "audio/mpeg",
            quality = Quality.AUDIO,
            sizeBytes = 39_000_000,
            status = DownloadStatus.COMPLETED,
            createdAt = System.currentTimeMillis() - 50 * 3_600_000L,
        ),
        heading = "سورة الكهف",
        caption = "Surah Al-Kahf",
        from = 0xFF1D3557.toInt(),
        to = 0xFF457B9D.toInt(),
    ),
)

/** Draws the preview's thumbnails once (a gradient with the title in big letters) and caches them. */
private object PreviewArt {
    fun path(context: Context, sample: PreviewSample): String? = runCatching {
        val file = File(context.cacheDir, "preview/thumb${-sample.download.id}.png")
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            val width = 640
            val height = 360
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = AndroidCanvas(bitmap)
            canvas.drawRect(
                0f, 0f, width.toFloat(), height.toFloat(),
                Paint().apply {
                    shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), sample.from, sample.to, Shader.TileMode.CLAMP)
                },
            )
            val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = 84f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            val caption = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.argb(220, 255, 255, 255)
                textSize = 34f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(sample.heading, width / 2f, height / 2f + 10f, heading)
            canvas.drawText(sample.caption, width / 2f, height / 2f + 70f, caption)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        file.absolutePath
    }.getOrNull()
}

/**
 * A working slice of the real Home screen, drawn with the very same components: the Library filters, Library
 * items (a video and two Quran recitations) and the mini player. Everything can be tapped to try it, with sample content
 * that never touches real downloads or playback.
 */
@Composable
private fun LookPreview() {
    val glass = LocalGlass.current
    var filter by rememberSaveable { mutableStateOf(LibraryFilter.ALL) }
    var track by rememberSaveable { mutableStateOf<Int?>(0) } // Index into PREVIEW_AUDIO, null when closed.
    var playing by rememberSaveable { mutableStateOf(true) }
    var repeat by rememberSaveable { mutableIntStateOf(Player.REPEAT_MODE_ALL) }
    var progress by remember { mutableFloatStateOf(0.35f) }
    val context = LocalContext.current
    // Thumbnails drawn in the background; until then the items show their placeholder.
    var thumbs by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    LaunchedEffect(Unit) {
        thumbs = withContext(Dispatchers.IO) {
            (PREVIEW_AUDIO + PREVIEW_VIDEO).mapNotNull { sample ->
                PreviewArt.path(context, sample)?.let { sample.download.id to it }
            }.toMap()
        }
    }
    fun withThumb(sample: PreviewSample) = sample.download.copy(thumbnailPath = thumbs[sample.download.id])
    LaunchedEffect(track, playing) {
        while (playing && track != null) {
            delay(200)
            progress = (progress + 0.004f).let { if (it > 1f) 0f else it }
        }
    }
    val items = when (filter) {
        LibraryFilter.ALL -> listOf(PREVIEW_VIDEO, PREVIEW_AUDIO[0])
        LibraryFilter.VIDEO -> listOf(PREVIEW_VIDEO)
        LibraryFilter.AUDIO -> PREVIEW_AUDIO
    }
    fun play(index: Int) {
        track = index
        playing = true
        progress = 0f
    }

    val shape = MaterialTheme.shapes.large
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(shape)
            .then(
                if (glass.enabled) {
                    Modifier.border(1.dp, glass.rim, shape)
                } else {
                    Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                },
            ),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                LibraryFilter.ALL to R.string.filter_all,
                LibraryFilter.VIDEO to R.string.filter_video,
                LibraryFilter.AUDIO to R.string.filter_audio,
            ).forEach { (option, label) ->
                FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(stringResource(label)) })
            }
        }
        Column(
            Modifier.padding(start = 12.dp, end = 4.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.forEach { sample ->
                LibraryItem(
                    withThumb(sample),
                    grid = false,
                    onPlay = { PREVIEW_AUDIO.indexOf(sample).takeIf { it >= 0 }?.let(::play) },
                    onShare = {},
                    onOpenWith = {},
                    onDelete = {},
                    onCopyDetails = {},
                )
            }
        }
        track?.let { index ->
            val audio = withThumb(PREVIEW_AUDIO[index])
            MiniPlayerBar(
                playing = NowPlaying(
                    downloadId = audio.id,
                    title = audio.title,
                    artist = audio.channel,
                    artworkUri = audio.thumbnailPath?.let { Uri.fromFile(File(it)) },
                    isPlaying = playing,
                    hasNext = true,
                    repeatMode = repeat,
                ),
                progress = progress,
                onOpen = {},
                hazeState = null,
                insets = false,
                onRepeat = { repeat = PlaybackService.nextRepeatMode(repeat) },
                onPrevious = { play((index + PREVIEW_AUDIO.size - 1) % PREVIEW_AUDIO.size) },
                onPlayPause = { playing = !playing },
                onNext = { play((index + 1) % PREVIEW_AUDIO.size) },
                onClose = { track = null },
                onSeek = { progress = it },
            )
        }
    }
}

/** A theme's colors as a round swatch, the way Android shows wallpaper palettes. */
@Composable
private fun ThemeCard(
    name: String,
    look: Appearance,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val dark = isDarkTheme(look)
    val scheme = remember(look, dark) { colorScheme(context, look, dark) }
    val shape = shapes(look.corners).large
    Column(
        Modifier
            .width(76.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(shape)
                    .background(scheme.surfaceContainerHigh)
                    .then(
                        if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape) else Modifier,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(40.dp)) {
                    // The colors the app actually uses: its one accent, the lighter fills, the background.
                    drawArc(scheme.primary, 180f, 180f, useCenter = true)
                    drawArc(scheme.primaryContainer, 90f, 90f, useCenter = true)
                    drawArc(scheme.surfaceContainerHighest, 0f, 90f, useCenter = true)
                }
                if (selected) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(20.dp))
                }
            }
            if (onDelete != null) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.inverseSurface)
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Text(
            name,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = fontFamily(look.font),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/**
 * A little picture of each surface style, in the user's own colors: the glass tiles show what's chosen
 * behind the glass (theme colors, one or two colors, or a wallpaper) with a pane at the chosen opacity;
 * the solid tile shows the theme's colors with a solid pane.
 */
@Composable
private fun GlassTile(
    style: GlassStyle,
    appearance: Appearance,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val dark = isDarkTheme(appearance)
    // The theme's own palette (the Color section's), whatever is behind the glass.
    val palette = remember(appearance, dark) { colorScheme(context, appearance, dark) }
    // Each style's own background, as set when that style is chosen.
    val look = appearance.looks[style] ?: StyleLook()
    val tile = Glass(style, dark, Color.Unspecified, palette.primary, look.opacity ?: style.defaultOpacity)
    val black = appearance.pureBlack && style != GlassStyle.OFF
    val behind = when {
        black -> GlassBehind.ONE_COLOR
        style == GlassStyle.OFF && look.behind == GlassBehind.WALLPAPER -> GlassBehind.THEME
        else -> look.behind
    }
    val color1 = if (black) Color.Black else Color(look.colors.first())
    val color2 = when {
        black -> Color.Black
        behind == GlassBehind.TWO_COLORS -> Color(look.colors.getOrElse(1) { look.colors.first() })
        else -> color1
    }
    // Tinted with the style's own colors, like the real glass.
    val pane = if (!black && (behind == GlassBehind.ONE_COLOR || behind == GlassBehind.TWO_COLORS)) {
        tile.copy(colors = listOfNotNull(color1, color2.takeIf { it != color1 }))
    } else {
        tile
    }
    val shape = MaterialTheme.shapes.medium
    val drawBackdrop: DrawScope.() -> Unit = {
        when (behind) {
            GlassBehind.ONE_COLOR -> drawRect(color1)
            GlassBehind.TWO_COLORS -> drawRect(Brush.linearGradient(listOf(color1, color2), Offset.Zero, Offset(size.width, size.height)))
            // A little landscape stands for "your wallpaper".
            GlassBehind.WALLPAPER -> {
                drawRect(Brush.verticalGradient(listOf(palette.primaryContainer, palette.surface)))
                drawCircle(palette.tertiary, size.height * 0.16f, Offset(size.width * 0.78f, size.height * 0.28f))
                drawCircle(palette.primary, size.width * 0.45f, Offset(size.width * 0.22f, size.height * 1.25f))
                drawCircle(palette.secondary, size.width * 0.5f, Offset(size.width * 0.85f, size.height * 1.4f))
            }
            GlassBehind.THEME -> {
                drawRect(palette.background)
                drawCircle(palette.primary, size.minDimension * 0.42f, Offset(size.width * 0.28f, size.height * 0.35f))
                drawCircle(palette.tertiary, size.minDimension * 0.36f, Offset(size.width * 0.72f, size.height * 0.75f))
            }
        }
    }
    Column(
        modifier
            .clip(shape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(shape)
                .border(if (selected) 3.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant, shape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.matchParentSize(), onDraw = drawBackdrop)
            val panel = Modifier
                .fillMaxWidth(0.7f)
                .height(36.dp)
            val panelShape = MaterialTheme.shapes.small
            when (style) {
                GlassStyle.OFF -> Box(panel.background(palette.surfaceContainerHighest, panelShape))
                // A real blur of what's behind, so the tile shows what frosted means.
                GlassStyle.FROSTED -> Box(panel.clip(panelShape)) {
                    Canvas(
                        Modifier
                            .matchParentSize()
                            .blur(10.dp),
                        onDraw = drawBackdrop,
                    )
                    Box(Modifier.matchParentSize().glassPanel(pane, panelShape))
                }
                GlassStyle.CLEAR -> Box(panel.glassPanel(pane, panelShape))
            }
        }
        Text(
            stringResource(style.label()),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun Swatch(brush: Brush, selected: Boolean, onClick: () -> Unit, icon: Boolean = false, badge: String? = null) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(brush)
            // Always outlined, so a swatch never disappears into a background of the same color.
            .border(
                if (selected) 3.dp else 1.5.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = if (selected) 1f else 0.45f),
                CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when {
            // On a dark disc, so the mark shows on light colors too.
            selected -> Box(
                Modifier
                    .size(26.dp)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (badge.isNullOrEmpty()) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                } else {
                    Text(badge, color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
            icon -> Icon(
                Icons.Rounded.Colorize,
                contentDescription = stringResource(R.string.appearance_custom_color),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun SaveThemeDialog(existing: List<String>, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    val replaces = existing.any { it.equals(name.trim(), ignoreCase = true) }
    YoinkAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.appearance_save)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.appearance_save_hint))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    label = { Text(stringResource(R.string.appearance_theme_name)) },
                    supportingText = if (replaces) {
                        { Text(stringResource(R.string.appearance_theme_exists)) }
                    } else {
                        null
                    },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ColorPickerDialog(initial: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val start = remember { FloatArray(3).also { AndroidColor.colorToHSV(initial, it) } }
    var hue by rememberSaveable { mutableFloatStateOf(start[0]) }
    var saturation by rememberSaveable { mutableFloatStateOf(start[1]) }
    var brightness by rememberSaveable { mutableFloatStateOf(start[2]) }
    val color = Color.hsv(hue, saturation, brightness)
    var hex by rememberSaveable { mutableStateOf(color.hex()) }

    fun fromSliders(h: Float = hue, s: Float = saturation, v: Float = brightness) {
        hue = h
        saturation = s
        brightness = v
        hex = Color.hsv(h, s, v).hex()
    }

    YoinkAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.appearance_custom_color)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(color),
                )
                GradientSlider(R.string.appearance_hue, hue, 0f..360f, Brush.horizontalGradient(HUES)) { fromSliders(h = it) }
                GradientSlider(
                    R.string.appearance_saturation,
                    saturation,
                    0f..1f,
                    Brush.horizontalGradient(listOf(Color.hsv(hue, 0f, brightness), Color.hsv(hue, 1f, brightness))),
                ) { fromSliders(s = it) }
                GradientSlider(
                    R.string.appearance_brightness,
                    brightness,
                    0f..1f,
                    Brush.horizontalGradient(listOf(Color.Black, Color.hsv(hue, saturation, 1f))),
                ) { fromSliders(v = it) }
                OutlinedTextField(
                    value = hex,
                    onValueChange = { text ->
                        hex = text.trim().take(7)
                        parseHex(hex)?.let { parsed ->
                            val hsv = FloatArray(3).also { AndroidColor.colorToHSV(parsed, it) }
                            hue = hsv[0]
                            saturation = hsv[1]
                            brightness = hsv[2]
                        }
                    },
                    label = { Text(stringResource(R.string.appearance_hex)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(color.toArgb()) }) { Text(stringResource(R.string.appearance_use_color)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/**
 * A slider that follows the finger exactly. While it's held, it shows the finger's position rather than
 * the value coming back from the settings, which can trail behind and made the thumb jump around.
 */
@Composable
private fun SmoothSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    var held by remember { mutableStateOf<Float?>(null) }
    Slider(
        value = held ?: value,
        onValueChange = {
            held = it
            onValueChange(it)
        },
        onValueChangeFinished = { held = null },
        valueRange = valueRange,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GradientSlider(
    @StringRes label: Int,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    brush: Brush,
    onChange: (Float) -> Unit,
) {
    Column {
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            thumb = {
                Box(
                    Modifier
                        .size(24.dp)
                        .border(1.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
                        .padding(1.dp)
                        .border(3.dp, Color.White, CircleShape),
                )
            },
            track = {
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(14.dp),
                ) {
                    drawRoundRect(brush, Offset.Zero, Size(size.width, size.height), CornerRadius(size.height / 2))
                }
            },
        )
    }
}

private fun Color.hex() = "#%06X".format(toArgb() and 0xFFFFFF)

private fun parseHex(text: String): Int? {
    val digits = text.removePrefix("#")
    if (digits.length != 6) return null
    return digits.toIntOrNull(16)?.let { it or 0xFF000000.toInt() }
}
