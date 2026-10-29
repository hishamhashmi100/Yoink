package io.github.hishamhashmi100.yoink.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.data.DownloadStatus
import io.github.hishamhashmi100.yoink.data.Playlist
import io.github.hishamhashmi100.yoink.data.PlaylistSummary
import io.github.hishamhashmi100.yoink.download.YouTubeUrl
import io.github.hishamhashmi100.yoink.ui.player.AudioController
import io.github.hishamhashmi100.yoink.ui.theme.OverlayGlass
import io.github.hishamhashmi100.yoink.ui.theme.YoinkAlertDialog
import io.github.hishamhashmi100.yoink.ui.theme.fadeOutBottom
import io.github.hishamhashmi100.yoink.yoink
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Library → Playlists: "New playlist", then the user's playlists. [query] is the Library's search. */
@Composable
fun MyPlaylistsList(query: String, bottomInset: Dp, onOpen: (Long) -> Unit) {
    val context = LocalContext.current
    val app = context.yoink
    val all by remember { app.playlists.observeSummaries(DownloadStatus.COMPLETED) }.collectAsStateWithLifecycle(null)
    var creating by rememberSaveable { mutableStateOf(false) }
    val shown = all.orEmpty().filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp + bottomInset),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .fadeOutBottom(bottomInset),
    ) {
        item(key = "new") {
            PlaylistRow(
                name = stringResource(R.string.my_playlist_new),
                detail = null,
                cover = null,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = { creating = true },
            )
        }
        if (all?.isEmpty() == true) {
            item(key = "hint") { Hint(stringResource(R.string.my_playlists_empty)) }
        }
        items(shown, key = { it.id }) { playlist ->
            PlaylistRow(
                name = playlist.name,
                detail = pluralStringResource(R.plurals.my_playlist_songs, playlist.count, playlist.count),
                cover = playlist.coverModel,
                onClick = { onOpen(playlist.id) },
            )
        }
    }

    if (creating) {
        PlaylistNameDialog(
            title = stringResource(R.string.my_playlist_new),
            confirm = stringResource(R.string.my_playlist_create),
            onSave = { name ->
                creating = false
                app.appScope.launch {
                    val id = app.playlists.insert(Playlist(name = name))
                    withContext(Dispatchers.Main) { onOpen(id) }
                }
            },
            onDismiss = { creating = false },
        )
    }
}

/** One playlist: its name, songs, Play and Shuffle. Back (the arrow or the system's) returns to the list. */
@Composable
fun MyPlaylistScreen(playlistId: Long, bottomInset: Dp, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.yoink
    val playlist by remember(playlistId) { app.playlists.observe(playlistId) }.collectAsStateWithLifecycle(null)
    val tracks by remember(playlistId) { app.playlists.observeTracks(playlistId, DownloadStatus.COMPLETED) }
        .collectAsStateWithLifecycle(emptyList())
    val nowPlaying by AudioController.nowPlaying.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    val name = playlist?.name.orEmpty()

    Column(modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    pluralStringResource(R.plurals.my_playlist_songs, tracks.size, tracks.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more_options))
                }
                OverlayGlass {
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.my_playlist_rename)) },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = { menuOpen = false; renaming = true },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.my_playlist_delete)) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                            onClick = { menuOpen = false; deleting = true },
                        )
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { AudioController.play(context, tracks, 0, shuffle = false) },
                enabled = tracks.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.my_playlist_play), modifier = Modifier.padding(start = 8.dp), maxLines = 1)
            }
            OutlinedButton(
                onClick = { AudioController.play(context, tracks, tracks.indices.random(), shuffle = true) },
                enabled = tracks.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.my_playlist_shuffle), modifier = Modifier.padding(start = 8.dp), maxLines = 1)
            }
        }
        if (tracks.isEmpty()) {
            Hint(stringResource(R.string.my_playlist_empty), Modifier.padding(horizontal = 16.dp))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 96.dp + bottomInset),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .fadeOutBottom(bottomInset),
            ) {
                items(tracks, key = { it.id }) { track ->
                    val playing = nowPlaying?.downloadId == track.id
                    var trackMenu by remember { mutableStateOf(false) }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { AudioController.play(context, tracks, tracks.indexOf(track)) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Thumbnail(track, Modifier.fillMaxWidth(0.3f).widthIn(max = 160.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                track.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            track.channel?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (playing) Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Box {
                            IconButton(onClick = { trackMenu = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more_options))
                            }
                            if (trackMenu) OverlayGlass {
                                DropdownMenu(expanded = true, onDismissRequest = { trackMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.my_playlist_remove)) },
                                        leadingIcon = { Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = null) },
                                        onClick = {
                                            trackMenu = false
                                            app.appScope.launch { app.playlists.removeItem(playlistId, track.id) }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (renaming) {
        PlaylistNameDialog(
            title = stringResource(R.string.my_playlist_rename),
            confirm = stringResource(R.string.save),
            initial = name,
            onSave = { newName ->
                renaming = false
                app.appScope.launch { app.playlists.rename(playlistId, newName) }
            },
            onDismiss = { renaming = false },
        )
    }
    if (deleting) {
        YoinkAlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text(stringResource(R.string.my_playlist_delete_title, name)) },
            text = { Text(stringResource(R.string.my_playlist_delete_text)) },
            confirmButton = {
                TextButton(onClick = {
                    deleting = false
                    onBack()
                    app.appScope.launch { app.playlists.delete(playlistId) }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** "Add to playlist": the user's playlists, or a new one, for the song [downloadId]. Says where it went. */
@Composable
fun AddToPlaylistDialog(downloadId: Long, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val app = context.yoink
    val all by remember { app.playlists.observeSummaries(DownloadStatus.COMPLETED) }.collectAsStateWithLifecycle(emptyList())
    var creating by rememberSaveable { mutableStateOf(false) }

    // The database work runs in the app's scope: this dialog is gone before it finishes.
    fun addTo(id: Long, name: String) {
        app.appScope.launch {
            val added = app.playlists.add(id, downloadId)
            withContext(Dispatchers.Main) {
                val message = if (added) R.string.my_playlist_added else R.string.my_playlist_already
                Toast.makeText(app, app.getString(message, name), Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (creating) {
        PlaylistNameDialog(
            title = stringResource(R.string.my_playlist_new),
            confirm = stringResource(R.string.my_playlist_create),
            onSave = { name ->
                onDismiss()
                app.appScope.launch {
                    val id = app.playlists.insert(Playlist(name = name))
                    withContext(Dispatchers.Main) { addTo(id, name) }
                }
            },
            onDismiss = onDismiss,
        )
        return
    }
    YoinkAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlist_add_to)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                PlaylistRow(
                    name = stringResource(R.string.my_playlist_new),
                    detail = null,
                    cover = null,
                    small = true,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { creating = true },
                )
                all.forEach { playlist ->
                    PlaylistRow(
                        name = playlist.name,
                        detail = pluralStringResource(R.plurals.my_playlist_songs, playlist.count, playlist.count),
                        cover = playlist.coverModel,
                        small = true,
                        onClick = {
                            onDismiss()
                            addTo(playlist.id, playlist.name)
                        },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Asks for a playlist's name: a new one, or a new name for one. */
@Composable
private fun PlaylistNameDialog(
    title: String,
    confirm: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    initial: String = "",
) {
    var name by rememberSaveable { mutableStateOf(initial) }
    YoinkAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                label = { Text(stringResource(R.string.my_playlist_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** The first song's thumbnail: Yoink's saved copy, or YouTube's while there's none. */
private val PlaylistSummary.coverModel: Any?
    get() = cover?.let { File(it) } ?: coverVideoId?.let(YouTubeUrl::thumbnailUrl)

@Composable
private fun PlaylistRow(
    name: String,
    detail: String?,
    cover: Any?,
    onClick: () -> Unit,
    small: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = if (small) 6.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .then(if (small) Modifier.width(64.dp) else Modifier.fillMaxWidth(0.3f).widthIn(max = 160.dp))
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                icon()
            } else {
                Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                if (cover != null) {
                    AsyncImage(
                        model = cover,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            detail?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
    )
}
