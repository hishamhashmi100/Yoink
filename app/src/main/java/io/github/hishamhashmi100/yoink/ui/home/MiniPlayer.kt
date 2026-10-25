package io.github.hishamhashmi100.yoink.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.ui.player.AudioController
import io.github.hishamhashmi100.yoink.ui.player.NowPlaying
import io.github.hishamhashmi100.yoink.ui.theme.LocalGlass
import io.github.hishamhashmi100.yoink.ui.theme.LocalHazeState
import io.github.hishamhashmi100.yoink.ui.theme.glassPanel
import kotlinx.coroutines.delay

/** The bar at the bottom of Home while an MP3 is playing: cover, title, 🔁 ⏮ ⏯ ⏭ and ✕. */
@Composable
fun MiniPlayer(
    onOpen: () -> Unit,
    onDrag: ((Float) -> Unit)? = null,
    onDragStopped: ((Float) -> Unit)? = null,
) {
    val nowPlaying by AudioController.nowPlaying.collectAsStateWithLifecycle()
    val playing = nowPlaying ?: return

    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playing.downloadId, playing.isPlaying) {
        while (true) {
            progress = AudioController.progress()
            if (!playing.isPlaying) break
            delay(500)
        }
    }
    MiniPlayerBar(playing, progress, onOpen, LocalHazeState.current, onDrag = onDrag, onDragStopped = onDragStopped)
}

/**
 * The bar itself. The Appearance preview shows it too, with sample content and its own button actions;
 * left out, the buttons drive real playback.
 */
@Composable
fun MiniPlayerBar(
    playing: NowPlaying,
    progress: Float,
    onOpen: () -> Unit,
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    insets: Boolean = true,
    onRepeat: (() -> Unit)? = null,
    onPrevious: (() -> Unit)? = null,
    onPlayPause: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    /** Pulling the bar up drags the full-screen player along with the finger. */
    onDrag: ((Float) -> Unit)? = null,
    onDragStopped: ((Float) -> Unit)? = null,
) {
    val context = LocalContext.current
    val glass = LocalGlass.current
    Surface(
        tonalElevation = if (glass.enabled) 0.dp else 3.dp,
        shadowElevation = if (glass.enabled) 0.dp else 8.dp,
        color = if (glass.enabled) Color.Transparent else MaterialTheme.colorScheme.surface,
        modifier = modifier.glassPanel(glass, RectangleShape, hazeState),
    ) {
        // The whole bar (arrow included) can be pulled up.
        Column(
            Modifier
                .draggable(
                    orientation = Orientation.Vertical,
                    enabled = onDrag != null,
                    state = rememberDraggableState { delta -> onDrag?.invoke(delta) },
                    onDragStopped = { velocity -> onDragStopped?.invoke(velocity) },
                )
                .then(if (insets) Modifier.navigationBarsPadding() else Modifier),
        ) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(2.dp))
            // A small arrow: the bar can be pulled up into the full-screen player.
            Icon(
                Icons.Rounded.KeyboardArrowUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(18.dp),
            )
            // Tap the cover or title to open the full-screen player.
            Row(
                Modifier
                    .clickable(onClick = onOpen)

                    .padding(start = 12.dp, end = 4.dp, top = 0.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = playing.artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(playing.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    playing.artist?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(onClick = { onRepeat?.invoke() ?: AudioController.cycleRepeat(context) }, modifier = Modifier.size(40.dp)) {
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
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
                IconButton(onClick = { onPrevious?.invoke() ?: AudioController.previous(context) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = stringResource(R.string.player_previous))
                }
                IconButton(onClick = { onPlayPause?.invoke() ?: AudioController.togglePlayPause(context) }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (playing.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(if (playing.isPlaying) R.string.player_pause else R.string.player_play),
                    )
                }
                IconButton(
                    onClick = { onNext?.invoke() ?: AudioController.next(context) },
                    enabled = playing.hasNext,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(R.string.player_next))
                }
                IconButton(onClick = { onClose?.invoke() ?: AudioController.stop(context) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.player_stop))
                }
            }
        }
    }
}
