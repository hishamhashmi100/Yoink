package io.github.hishamhashmi100.yoink.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import io.github.hishamhashmi100.yoink.data.Download
import io.github.hishamhashmi100.yoink.data.DownloadStatus
import io.github.hishamhashmi100.yoink.download.DownloadLog
import io.github.hishamhashmi100.yoink.download.LiveProgress
import io.github.hishamhashmi100.yoink.download.Notifications
import io.github.hishamhashmi100.yoink.download.YouTubeUrl
import io.github.hishamhashmi100.yoink.ui.theme.YoinkAlertDialog
import io.github.hishamhashmi100.yoink.ui.theme.YoinkCard
import io.github.hishamhashmi100.yoink.ui.theme.fadeOutBottom
import java.io.File

@Composable
fun DownloadingScreen(viewModel: HomeViewModel, modifier: Modifier = Modifier, bottomInset: Dp = 0.dp) {
    val active = viewModel.active.collectAsStateWithLifecycle().value.orEmpty()
    if (active.isEmpty()) {
        Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.downloading_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    var cancellingAll by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .fadeOutBottom(bottomInset),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + bottomInset),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // A playlist can fill the list with hundreds of videos; one button clears them all.
        if (active.size > 1) {
            item(key = "header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        pluralStringResource(R.plurals.downloads_in_list, active.size, active.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { cancellingAll = true }) { Text(stringResource(R.string.cancel_all)) }
                }
            }
        }
        items(active, key = { it.download.id }) { item ->
            ActiveDownloadCard(
                item = item,
                onCancel = { viewModel.cancel(item.download.id) },
                onRetry = { viewModel.retry(item.download.id) },
            )
        }
    }
    if (cancellingAll) {
        YoinkAlertDialog(
            onDismissRequest = { cancellingAll = false },
            title = { Text(stringResource(R.string.cancel_all_title)) },
            text = { Text(pluralStringResource(R.plurals.cancel_all_text, active.size, active.size)) },
            confirmButton = {
                TextButton(onClick = {
                    cancellingAll = false
                    viewModel.cancelAll()
                }) { Text(stringResource(R.string.cancel_all)) }
            },
            dismissButton = { TextButton(onClick = { cancellingAll = false }) { Text(stringResource(R.string.keep)) } },
        )
    }
}

@Composable
private fun ActiveDownloadCard(item: ActiveDownload, onCancel: () -> Unit, onRetry: () -> Unit) {
    val download = item.download
    val progress = item.progress
    val failed = download.status == DownloadStatus.FAILED
    YoinkCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Thumbnail(download, Modifier.fillMaxWidth(0.3f).widthIn(max = 160.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    download.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                if (failed) {
                    Text(
                        download.error ?: stringResource(R.string.status_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                    CopyDetailsButton(download.id)
                } else if (download.status == DownloadStatus.WAITING) {
                    // A playlist video further down the line.
                    Text(
                        stringResource(R.string.status_in_line),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                } else {
                    val percent = progress?.percent ?: -1f
                    if (percent >= 0) {
                        LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        statusLine(progress),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            if (failed) {
                IconButton(onClick = onRetry) {
                    Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.action_retry))
                }
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_cancel))
            }
        }
    }
}

@Composable
private fun statusLine(progress: LiveProgress?): String {
    if (progress == null || progress.stage.isEmpty()) return stringResource(R.string.status_waiting)
    return buildList {
        add(progress.stage)
        if (progress.percent >= 0) add("${progress.percent.toInt()}%")
        progress.speed?.let(::add)
        if (progress.etaSeconds > 0) add(stringResource(R.string.eta_left, Notifications.formatDuration(progress.etaSeconds)))
    }.joinToString(" · ")
}

/** Copies what yt-dlp printed for a failed download, so it can be pasted into a bug report. */
@Composable
private fun CopyDetailsButton(downloadId: Long) {
    val context = LocalContext.current
    TextButton(
        onClick = {
            val details = DownloadLog.read(context, downloadId) ?: return@TextButton
            context.getSystemService(ClipboardManager::class.java)
                ?.setPrimaryClip(ClipData.newPlainText("Yoink details", details))
            Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
        },
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
    ) {
        Text(stringResource(R.string.copy_details), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun Thumbnail(download: Download, modifier: Modifier = Modifier) {
    Box(
        modifier
            .aspectRatio(16f / 9f)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        AsyncImage(
            model = download.thumbnailPath?.let { File(it) } ?: YouTubeUrl.thumbnailUrl(download.videoId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
    }
}
