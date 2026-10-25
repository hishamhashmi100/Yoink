package io.github.hishamhashmi100.yoink.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.download.Notifications
import io.github.hishamhashmi100.yoink.download.Quality
import io.github.hishamhashmi100.yoink.ui.theme.ButtonShape
import io.github.hishamhashmi100.yoink.ui.theme.LocalGlass
import io.github.hishamhashmi100.yoink.ui.theme.OverlayGlass
import io.github.hishamhashmi100.yoink.ui.theme.windowBlurAvailable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(
    state: ShareUiState,
    videoQuality: Quality,
    onPick: (Quality) -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    /** Set when the video was shared from a playlist: offers the whole playlist instead. */
    onWholePlaylist: (() -> Unit)? = null,
) {
    // Over another app: dark glass with white text, readable whatever is behind.
    OverlayGlass { SheetContent(state, videoQuality, onPick, onMinimize, onClose, onWholePlaylist) }
}

/** A shared playlist: one quality for all its videos. */
@Composable
fun PlaylistSheet(state: PlaylistUiState, onPick: (Quality) -> Unit, onClose: () -> Unit) {
    OverlayGlass {
        GlassSheet(onDismiss = onClose) {
            PlaylistHeader(state, onClose)
            Spacer(Modifier.height(16.dp))
            if (state.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(
                    stringResource(R.string.playlist_checking),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
                )
            }
            state.error?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            // Each quality falls back to the nearest one a video has, so every button works for every video.
            val ready = !state.loading && state.entries.isNotEmpty()
            QualityButtons(if (ready) Quality.entries.toSet() else emptySet(), onPick)
            if (ready) {
                Text(
                    pluralStringResource(R.plurals.playlist_hint, state.entries.size, state.entries.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun SheetContent(
    state: ShareUiState,
    videoQuality: Quality,
    onPick: (Quality) -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    onWholePlaylist: (() -> Unit)?,
) {
    // Swipe down / Back / tap outside tucks the sheet away; the ✕ closes it for good.
    GlassSheet(onDismiss = onMinimize) {
        VideoHeader(state, onClose)
        Spacer(Modifier.height(16.dp))

        if (state.loadingFormats) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(
                stringResource(R.string.share_checking),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
            )
        }
        state.formatsError?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }

        if (state.quick) VideoOrAudioButtons(videoQuality, onPick) else QualityButtons(state.available, onPick)

        if (onWholePlaylist != null) {
            OutlinedButton(
                onClick = onWholePlaylist,
                shape = ButtonShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(48.dp),
            ) {
                Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.playlist_whole))
            }
        }
    }
}

/** The bottom sheet both share sheets sit in, in the app's look. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val glass = LocalGlass.current
    val context = LocalContext.current
    // Glass over another app needs the window blur to stay readable; without it, use the thicker glass.
    val blur = remember { windowBlurAvailable(context) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (glass.enabled && !blur) glass.menuTint else BottomSheetDefaults.ContainerColor,
        // At low opacity the glass sheet can all but vanish over the app behind; a white edge keeps it clear.
        modifier = if (glass.enabled) {
            Modifier.border(1.5.dp, Color.White.copy(alpha = 0.8f), BottomSheetDefaults.ExpandedShape)
        } else {
            Modifier
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
        ) {
            content()
        }
    }
}

@Composable
private fun VideoHeader(state: ShareUiState, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .width(132.dp)
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = state.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            state.durationSec?.let {
                Text(
                    Notifications.formatDuration(it),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                state.title ?: stringResource(R.string.share_loading_title),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            state.channel?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.share_close))
        }
    }
}

/** The first video's thumbnail with a "N videos" band, like YouTube shows playlists. */
@Composable
private fun PlaylistHeader(state: PlaylistUiState, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .width(132.dp)
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            state.thumbnailUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.VideoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                if (!state.loading && state.entries.isNotEmpty()) {
                    Text(
                        pluralStringResource(R.plurals.playlist_count, state.entries.size, state.entries.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                state.title ?: stringResource(R.string.playlist_loading_title),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            state.channel?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.share_close))
        }
    }
}

/** The small sheet: one tap for video in the chosen quality, one for MP3. Nothing to wait for. */
@Composable
private fun VideoOrAudioButtons(videoQuality: Quality, onPick: (Quality) -> Unit) {
    val resources = LocalContext.current.resources
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { onPick(videoQuality) }, shape = ButtonShape, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.share_video_with, videoQuality.label(resources)))
        }
        FilledTonalButton(onClick = { onPick(Quality.AUDIO) }, shape = ButtonShape, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(Quality.AUDIO.label(resources))
        }
    }
}

@Composable
private fun QualityButtons(available: Set<Quality>, onPick: (Quality) -> Unit) {
    val resources = LocalContext.current.resources
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { onPick(Quality.BEST) },
            enabled = Quality.BEST in available,
            shape = ButtonShape,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(Quality.BEST.label(resources))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Quality.P1080, Quality.P720, Quality.P480).forEach { quality ->
                OutlinedButton(
                    onClick = { onPick(quality) },
                    enabled = quality in available,
                    shape = ButtonShape,
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Text(quality.label(resources))
                }
            }
        }
        FilledTonalButton(
            onClick = { onPick(Quality.AUDIO) },
            enabled = Quality.AUDIO in available,
            shape = ButtonShape,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(Quality.AUDIO.label(resources))
        }
    }
}
