package io.github.hishamhashmi100.yoink.ui.share

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.download.Notifications
import io.github.hishamhashmi100.yoink.download.Quality

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(state: ShareUiState, onPick: (Quality) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
        ) {
            VideoHeader(state)
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

            QualityButtons(state.available, onPick)
        }
    }
}

@Composable
private fun VideoHeader(state: ShareUiState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(132.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
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
    }
}

@Composable
private fun QualityButtons(available: Set<Quality>, onPick: (Quality) -> Unit) {
    val resources = LocalContext.current.resources
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { onPick(Quality.BEST) },
            enabled = Quality.BEST in available,
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
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Text(quality.label(resources))
                }
            }
        }
        FilledTonalButton(
            onClick = { onPick(Quality.AUDIO) },
            enabled = Quality.AUDIO in available,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(Quality.AUDIO.label(resources))
        }
    }
}
