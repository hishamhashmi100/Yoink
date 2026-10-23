package io.github.hishamhashmi100.yoink.ui.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.data.Download
import io.github.hishamhashmi100.yoink.data.DownloadStatus
import io.github.hishamhashmi100.yoink.download.LiveProgress
import io.github.hishamhashmi100.yoink.download.Notifications
import io.github.hishamhashmi100.yoink.download.YouTubeUrl
import java.io.File

@Composable
fun DownloadingScreen(modifier: Modifier = Modifier, viewModel: HomeViewModel = viewModel()) {
    val active by viewModel.active.collectAsStateWithLifecycle()
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
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(active, key = { it.download.id }) { item ->
            ActiveDownloadCard(
                item = item,
                onCancel = { viewModel.cancel(item.download.id) },
                onRetry = { viewModel.retry(item.download.id) },
            )
        }
    }
}

@Composable
private fun ActiveDownloadCard(item: ActiveDownload, onCancel: () -> Unit, onRetry: () -> Unit) {
    val download = item.download
    val progress = item.progress
    val failed = download.status == DownloadStatus.FAILED
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Thumbnail(download, Modifier.width(112.dp))
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
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
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

@Composable
fun Thumbnail(download: Download, modifier: Modifier = Modifier) {
    Box(
        modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(10.dp))
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
