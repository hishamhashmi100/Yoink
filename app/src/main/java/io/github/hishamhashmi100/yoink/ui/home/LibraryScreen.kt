package io.github.hishamhashmi100.yoink.ui.home

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.data.Download
import io.github.hishamhashmi100.yoink.download.Notifications
import io.github.hishamhashmi100.yoink.ui.player.AudioController
import io.github.hishamhashmi100.yoink.ui.player.PlayerActivity

@Composable
fun LibraryScreen(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    val items by viewModel.library.collectAsStateWithLifecycle()
    val hasAny by viewModel.hasLibraryItems.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val grid by viewModel.grid.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var toDelete by remember { mutableStateOf<Download?>(null) }

    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.query.value = it },
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.query.value = "" }) {
                        Icon(Icons.Rounded.Clear, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Row(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LibraryFilter.entries.forEach { option ->
                FilterChip(
                    selected = filter == option,
                    onClick = { viewModel.filter.value = option },
                    label = {
                        Text(
                            stringResource(
                                when (option) {
                                    LibraryFilter.ALL -> R.string.filter_all
                                    LibraryFilter.VIDEO -> R.string.filter_video
                                    LibraryFilter.AUDIO -> R.string.filter_audio
                                }
                            )
                        )
                    },
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { viewModel.grid.value = !grid }) {
                if (grid) {
                    Icon(Icons.AutoMirrored.Rounded.ViewList, contentDescription = stringResource(R.string.layout_list))
                } else {
                    Icon(Icons.Rounded.GridView, contentDescription = stringResource(R.string.layout_grid))
                }
            }
        }

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(if (hasAny) R.string.library_no_results else R.string.library_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = if (grid) GridCells.Adaptive(minSize = 160.dp) else GridCells.Fixed(1),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items, key = { it.id }) { download ->
                    LibraryItem(
                        download = download,
                        grid = grid,
                        onPlay = {
                            if (download.quality.isAudio) {
                                // Like a music app: the library's MP3s, in order, starting with this one.
                                val queue = items.filter { it.quality.isAudio }
                                AudioController.play(context, queue, queue.indexOf(download))
                            } else {
                                context.startActivity(PlayerActivity.intent(context, download.id))
                            }
                        },
                        onShare = { FileActions.share(context, download) },
                        onOpenWith = { FileActions.openWith(context, download) },
                        onDelete = { toDelete = download },
                    )
                }
            }
        }
    }

    toDelete?.let { download ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_message, download.title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(download)
                    toDelete = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryItem(
    download: Download,
    grid: Boolean,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onOpenWith: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val clickable = Modifier
        .clip(RoundedCornerShape(12.dp))
        .combinedClickable(onClick = onPlay, onLongClick = { menuOpen = true })
    // The ⋮ button and long-press open the same menu, anchored to the button.
    val more: @Composable () -> Unit = {
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more_options))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_share)) },
                    leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                    onClick = { menuOpen = false; onShare() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_open_with)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null) },
                    onClick = { menuOpen = false; onOpenWith() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_delete)) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                    onClick = { menuOpen = false; onDelete() },
                )
            }
        }
    }
    if (grid) {
        Column(clickable.fillMaxWidth()) {
            ThumbnailWithBadges(download, Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) { ItemText(download) }
                more()
            }
        }
    } else {
        Row(clickable.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ThumbnailWithBadges(download, Modifier.width(144.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { ItemText(download) }
            more()
        }
    }
}

@Composable
private fun ThumbnailWithBadges(download: Download, modifier: Modifier) {
    Box(modifier) {
        Thumbnail(download, Modifier.fillMaxWidth())
        download.durationSec?.let {
            Text(
                Notifications.formatDuration(it),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
        if (download.quality.isAudio) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                    .padding(3.dp),
            )
        }
    }
}

@Composable
private fun ItemText(download: Download) {
    val context = LocalContext.current
    Text(
        download.title,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    val details = listOfNotNull(
        download.channel,
        download.sizeBytes?.let { Formatter.formatShortFileSize(context, it) },
        DateUtils.getRelativeTimeSpanString(
            download.createdAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString(),
    ).joinToString(" · ")
    Text(
        details,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 2.dp),
    )
}
