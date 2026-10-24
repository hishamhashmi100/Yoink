package io.github.hishamhashmi100.yoink.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.update.AppUpdater
import io.github.hishamhashmi100.yoink.update.Release
import io.github.hishamhashmi100.yoink.update.UpdateState

/** Shown at the top of Home when a newer Yoink is out: Update → download → Android's installer. */
@Composable
fun UpdateCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    if (!AppUpdater.canSelfUpdate(context)) return
    val state by AppUpdater.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { AppUpdater.check(context) }

    val release: Release = when (val s = state) {
        is UpdateState.Available -> s.release
        is UpdateState.Downloading -> s.release
        is UpdateState.ReadyToInstall -> s.release
        is UpdateState.Failed -> s.release ?: return
        else -> return
    }

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.update_card_title, release.version), style = MaterialTheme.typography.titleMedium)
            when (val s = state) {
                is UpdateState.Downloading -> {
                    LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.update_downloading, (s.progress * 100).toInt()), style = MaterialTheme.typography.bodySmall)
                }
                is UpdateState.ReadyToInstall -> {
                    Text(stringResource(R.string.update_ready), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.update_allow_install), style = MaterialTheme.typography.bodySmall)
                }
                is UpdateState.Failed -> Text(s.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                else -> Text(stringResource(R.string.update_card_body), style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (state) {
                    is UpdateState.Available -> Button(onClick = { AppUpdater.download(context) }) { Text(stringResource(R.string.update_action)) }
                    is UpdateState.Failed -> Button(onClick = { AppUpdater.download(context) }) { Text(stringResource(R.string.update_retry)) }
                    is UpdateState.ReadyToInstall -> Button(onClick = { AppUpdater.install(context) }) { Text(stringResource(R.string.update_install)) }
                    else -> Unit
                }
                if (release.pageUrl.isNotEmpty()) {
                    TextButton(onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.pageUrl))) }
                    }) { Text(stringResource(R.string.update_whats_new)) }
                }
            }
        }
    }
}
