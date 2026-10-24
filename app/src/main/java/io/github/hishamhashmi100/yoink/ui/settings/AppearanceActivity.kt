package io.github.hishamhashmi100.yoink.ui.settings

import android.app.Application
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.data.Appearance
import io.github.hishamhashmi100.yoink.data.Contrast
import io.github.hishamhashmi100.yoink.data.CornerStyle
import io.github.hishamhashmi100.yoink.data.FontStyle
import io.github.hishamhashmi100.yoink.data.GlassStyle
import io.github.hishamhashmi100.yoink.data.PaletteStyle
import io.github.hishamhashmi100.yoink.data.SavedTheme
import io.github.hishamhashmi100.yoink.data.ThemeMode
import io.github.hishamhashmi100.yoink.data.ThemePreset
import io.github.hishamhashmi100.yoink.data.YOINK_RED
import io.github.hishamhashmi100.yoink.ui.theme.ButtonShape
import io.github.hishamhashmi100.yoink.ui.theme.Glass
import io.github.hishamhashmi100.yoink.ui.theme.LocalGlass
import io.github.hishamhashmi100.yoink.ui.theme.glassPanel
import io.github.hishamhashmi100.yoink.ui.theme.YoinkTheme
import io.github.hishamhashmi100.yoink.ui.theme.colorScheme
import io.github.hishamhashmi100.yoink.ui.theme.fontFamily
import io.github.hishamhashmi100.yoink.ui.theme.isDarkTheme
import io.github.hishamhashmi100.yoink.ui.theme.shapes
import io.github.hishamhashmi100.yoink.yoink
import io.github.hishamhashmi100.yoink.ui.theme.YoinkAlertDialog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import io.github.hishamhashmi100.yoink.data.Download
import io.github.hishamhashmi100.yoink.data.DownloadStatus
import io.github.hishamhashmi100.yoink.download.Quality
import io.github.hishamhashmi100.yoink.ui.home.LibraryItem
import io.github.hishamhashmi100.yoink.ui.home.MiniPlayerBar
import io.github.hishamhashmi100.yoink.ui.player.NowPlaying
import androidx.media3.common.Player
import android.graphics.Color as AndroidColor

class AppearanceActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
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

    val appearance: StateFlow<Appearance> = store.settings.map { it.appearance }
        .stateIn(viewModelScope, SharingStarted.Eagerly, store.current().appearance)

    val savedThemes: StateFlow<List<SavedTheme>> =
        store.savedThemes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun update(change: (Appearance) -> Appearance) {
        viewModelScope.launch { store.setAppearance(change(appearance.value)) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_appearance)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
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
                            selected = appearance.theme == mode,
                            onClick = { viewModel.update { it.copy(theme = mode) } },
                            shape = SegmentedButtonDefaults.itemShape(index, modes.size, ButtonShape),
                        ) { Text(stringResource(label)) }
                    }
                }
                ListItem(
                    headlineContent = { Text(stringResource(R.string.appearance_pure_black)) },
                    supportingContent = { Text(stringResource(R.string.appearance_pure_black_summary)) },
                    trailingContent = { Switch(checked = appearance.pureBlack, onCheckedChange = null) },
                    modifier = Modifier.clickable { viewModel.update { it.copy(pureBlack = !it.pureBlack) } },
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
                            selected = appearance.glass == style,
                            onClick = { viewModel.update { it.copy(glass = style) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

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
                FlowRow(
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .alpha(if (wallpaper) 0.45f else 1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SWATCHES.forEach { color ->
                        Swatch(
                            brush = Brush.linearGradient(listOf(Color(color), Color(color))),
                            selected = !wallpaper && appearance.seedColor == color,
                            onClick = { viewModel.update { it.copy(dynamicColor = false, seedColor = color) } },
                        )
                    }
                    val custom = !wallpaper && appearance.seedColor !in SWATCHES
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
                        ) { Text(stringResource(label)) }
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

private val HUES = listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { Color.hsv(it % 360f, 1f, 1f) }

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

/**
 * A slice of the real Home screen (the Library filters, a Library item and the mini player), drawn with
 * the very same components, so the preview looks exactly like the app.
 */
@Composable
private fun LookPreview() {
    val glass = LocalGlass.current
    val title = stringResource(R.string.preview_title)
    val channel = stringResource(R.string.preview_channel)
    val sample = remember(title, channel) {
        Download(
            videoId = "",
            title = title,
            channel = channel,
            durationSec = 36_000,
            mimeType = "video/mp4",
            quality = Quality.P1080,
            sizeBytes = 452_000_000,
            status = DownloadStatus.COMPLETED,
            createdAt = System.currentTimeMillis() - 3 * 3_600_000L,
            resolution = 1080,
        )
    }
    val playing = NowPlaying(
        downloadId = null,
        title = title,
        artist = channel,
        artworkUri = null,
        isPlaying = true,
        hasNext = true,
        repeatMode = Player.REPEAT_MODE_ALL,
    )
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
            FilterChip(selected = true, onClick = {}, label = { Text(stringResource(R.string.filter_all)) })
            FilterChip(selected = false, onClick = {}, label = { Text(stringResource(R.string.filter_video)) })
            FilterChip(selected = false, onClick = {}, label = { Text(stringResource(R.string.filter_audio)) })
        }
        Box(Modifier.padding(start = 12.dp, end = 4.dp, bottom = 8.dp)) {
            LibraryItem(sample, grid = false, onPlay = {}, onShare = {}, onOpenWith = {}, onDelete = {})
        }
        MiniPlayerBar(playing, progress = 0.62f, onOpen = {}, hazeState = null, interactive = false)
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

/** A little picture of each surface style: a panel over the color backdrop. */
@Composable
private fun GlassTile(style: GlassStyle, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.onSurface.luminance() > 0.5f
    val tile = Glass(style, dark, Color.Unspecified, colors.primary)
    val shape = MaterialTheme.shapes.medium
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
                .then(if (selected) Modifier.border(3.dp, colors.primary, shape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawRect(if (dark) Color(0xFF101014) else Color(0xFFF6F6F8))
                drawCircle(colors.primary, size.minDimension * 0.42f, Offset(size.width * 0.28f, size.height * 0.35f))
                drawCircle(colors.tertiary, size.minDimension * 0.36f, Offset(size.width * 0.72f, size.height * 0.75f))
            }
            val panel = Modifier
                .fillMaxWidth(0.7f)
                .height(36.dp)
            val panelShape = MaterialTheme.shapes.small
            when (style) {
                GlassStyle.OFF -> Box(panel.background(if (dark) Color(0xFF26262C) else Color.White, panelShape))
                // A real blur of the circles behind, so the tile shows what frosted means.
                GlassStyle.FROSTED -> Box(panel.clip(panelShape)) {
                    Canvas(
                        Modifier
                            .matchParentSize()
                            .blur(10.dp),
                    ) {
                        drawCircle(colors.primary, size.height * 1.1f, Offset(size.width * 0.1f, 0f))
                        drawCircle(colors.tertiary, size.height * 0.9f, Offset(size.width * 0.95f, size.height * 1.2f))
                    }
                    Box(Modifier.matchParentSize().glassPanel(tile, panelShape))
                }
                GlassStyle.CLEAR -> Box(panel.glassPanel(tile, panelShape))
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
private fun Swatch(brush: Brush, selected: Boolean, onClick: () -> Unit, icon: Boolean = false) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(brush)
            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when {
            selected -> Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White)
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
