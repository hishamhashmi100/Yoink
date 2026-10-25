package io.github.hishamhashmi100.yoink.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.data.GlassBehind
import io.github.hishamhashmi100.yoink.ui.theme.ButtonShape
import io.github.hishamhashmi100.yoink.ui.theme.GlassBackdrop
import io.github.hishamhashmi100.yoink.ui.theme.LocalBackdrop
import io.github.hishamhashmi100.yoink.ui.theme.LocalBackdropColors
import io.github.hishamhashmi100.yoink.ui.theme.LocalGlass
import io.github.hishamhashmi100.yoink.ui.theme.LocalHazeState
import io.github.hishamhashmi100.yoink.ui.theme.OverlayGlass
import io.github.hishamhashmi100.yoink.ui.theme.glassPanel
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * The full-screen "Now playing" view for MP3s, pulled up from the mini player. [progress] runs from 0
 * (tucked away) to 1 (open): while a finger drags (here or on the mini player) it follows the finger, and
 * [onDragStopped] settles it open or closed. Drawn entirely from the Appearance settings.
 */
@Composable
fun NowPlayingOverlay(
    progress: Float,
    onDrag: (Float) -> Unit,
    onDragStopped: (Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // With glass: smoky glass with white text, like the pop-ups, so it reads well over Home at any opacity.
    OverlayGlass { PlayerPane(progress, onDrag, onDragStopped, onClose, modifier) }
}

@Composable
private fun PlayerPane(
    progress: Float,
    onDrag: (Float) -> Unit,
    onDragStopped: (Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier,
) {
    val nowPlaying by AudioController.nowPlaying.collectAsStateWithLifecycle()
    val playing = nowPlaying ?: return
    val glass = LocalGlass.current
    // Glass: the whole player is a pane of glass over Home, blurring it like the mini player does (Home's
    // blur state, from the screen below). Solid: an opaque background, the user's colors if they picked some.
    val homeHaze = LocalHazeState.current
    val backdrop = LocalBackdrop.current
    val customColors = backdrop.behind == GlassBehind.ONE_COLOR || backdrop.behind == GlassBehind.TWO_COLORS
    if (progress >= 1f) HideSystemBars()

    val drag = Modifier.draggable(
        orientation = Orientation.Vertical,
        state = rememberDraggableState(onDrag),
        onDragStopped = { velocity -> onDragStopped(velocity) },
    )
    BoxWithConstraints(modifier.fillMaxSize()) {
        val height = constraints.maxHeight
        val pane = when {
            glass.enabled && homeHaze != null -> Modifier
                .hazeEffect(
                    homeHaze,
                    // At least a little blur, so the screen behind doesn't fight with the player's text.
                    glass.hazeStyle.copy(blurRadius = maxOf(glass.blur, 16.dp)),
                )
                .background(SMOKE)
            glass.enabled -> Modifier.background(glass.tintBrush).background(SMOKE)
            else -> Modifier
        }
        Box(
            Modifier
                .offset { IntOffset(0, ((1f - progress) * height).roundToInt()) }
                .fillMaxSize()
                .then(pane)
                // Taps on empty space shouldn't reach the screen underneath.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        ) {
            if (!glass.enabled) {
                if (customColors) GlassBackdrop(LocalBackdropColors.current ?: MaterialTheme.colorScheme)
                else Box(Modifier.fillMaxSize().background(LocalBackdropColors.current?.surface ?: MaterialTheme.colorScheme.surface))
            }
            var showQueue by rememberSaveable { mutableStateOf(false) }
            Column(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 24.dp),
            ) {
                Row(
                    drag
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.close_player))
                    }
                    Text(
                        stringResource(if (showQueue) R.string.now_playing_queue else R.string.now_playing),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { showQueue = !showQueue }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = stringResource(R.string.now_playing_queue),
                            tint = if (showQueue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                AnimatedContent(
                    targetState = showQueue,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    // The cover can be dragged down to close; the queue scrolls instead.
                    modifier = Modifier
                        .weight(1f)
                        .then(if (showQueue) Modifier else drag),
                    label = "cover or queue",
                ) { queue ->
                    if (queue) QueueList(playing.index) else Cover(playing)
                }

                Controls(playing, null)
            }
        }
    }
}

@Composable
private fun Cover(playing: NowPlaying) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(56.dp),
            )
            AsyncImage(
                model = playing.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            playing.title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        playing.artist?.let {
            Text(
                it,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun QueueList(current: Int) {
    val context = LocalContext.current
    val queue by AudioController.queue.collectAsStateWithLifecycle()
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (current - 2).coerceAtLeast(0))
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(queue, key = { it.index }) { entry ->
            val active = entry.index == current
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent)
                    .clickable { AudioController.skipTo(context, entry.index) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = entry.artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 64.dp, height = 36.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                ) {
                    Text(
                        entry.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    entry.artist?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (active) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/**
 * Seek bar and buttons, on a pane of glass when glass is on: the same glass (and opacity) as everywhere
 * else, blurring the backdrop behind it when [hazeState] is given.
 */
@Composable
private fun Controls(playing: NowPlaying, hazeState: HazeState?) {
    val context = LocalContext.current
    val glass = LocalGlass.current
    val shape = MaterialTheme.shapes.extraLarge

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playing.downloadId, playing.isPlaying) {
        while (true) {
            AudioController.position().let { (p, d) ->
                position = p
                duration = d
            }
            if (!playing.isPlaying) break
            delay(250)
        }
    }

    Surface(
        color = if (glass.enabled) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 16.dp)
            .glassPanel(glass, shape, hazeState),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            val fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
            Slider(
                value = if (dragging) dragValue else fraction,
                onValueChange = {
                    dragging = true
                    dragValue = it
                },
                onValueChangeFinished = {
                    val target = (dragValue * duration).toLong()
                    AudioController.seekTo(context, target)
                    position = target
                    dragging = false
                },
                enabled = duration > 0,
            )
            Row(Modifier.fillMaxWidth()) {
                Text(
                    time(if (dragging) (dragValue * duration).toLong() else position),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    time(duration),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { AudioController.toggleShuffle(context) }) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = stringResource(if (playing.shuffle) R.string.player_shuffle_on else R.string.player_shuffle_off),
                        tint = if (playing.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { AudioController.previous(context) }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = stringResource(R.string.player_previous), modifier = Modifier.size(36.dp))
                }
                FilledIconButton(
                    onClick = { AudioController.togglePlayPause(context) },
                    shape = ButtonShape,
                    colors = IconButtonDefaults.filledIconButtonColors(),
                    modifier = Modifier.size(72.dp),
                ) {
                    Icon(
                        if (playing.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(if (playing.isPlaying) R.string.player_pause else R.string.player_play),
                        modifier = Modifier.size(40.dp),
                    )
                }
                IconButton(
                    onClick = { AudioController.next(context) },
                    enabled = playing.hasNext,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(R.string.player_next), modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { AudioController.cycleRepeat(context) }) {
                    Icon(
                        if (playing.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = stringResource(
                            when (playing.repeatMode) {
                                Player.REPEAT_MODE_ONE -> R.string.player_repeat_one
                                Player.REPEAT_MODE_ALL -> R.string.player_repeat_all
                                else -> R.string.player_repeat_off
                            },
                        ),
                        tint = if (playing.repeatMode == Player.REPEAT_MODE_OFF) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }
        }
    }
}

/** A light shade under the white text, so it stays readable even when the glass is nearly clear. */
private val SMOKE = Color.Black.copy(alpha = 0.22f)

/** 3:07, or 1:02:45 for long ones. */
private fun time(ms: Long): String {
    val total = ms / 1000
    val hours = total / 3600
    val minutes = total % 3600 / 60
    val seconds = total % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

/**
 * Full screen while the player is open: hides the status bar (clock, icons) and the navigation bar, on the
 * sheet's own window and the screen under it. A swipe from the edge brings them back for a moment;
 * closing the player restores them.
 */
@Composable
private fun HideSystemBars() {
    val view = LocalView.current
    DisposableEffect(view) {
        val windows = buildList {
            var parent = view.parent
            while (parent != null && parent !is DialogWindowProvider) parent = parent.parent
            (parent as? DialogWindowProvider)?.window?.let(::add)
            view.context.findActivity()?.window?.let(::add)
        }
        windows.forEach { window ->
            WindowCompat.getInsetsController(window, window.decorView).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            windows.forEach { window ->
                WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

