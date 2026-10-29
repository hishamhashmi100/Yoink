package io.github.hishamhashmi100.yoink.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.stats.Stats
import io.github.hishamhashmi100.yoink.ui.theme.YoinkAlertDialog
import kotlinx.coroutines.launch

/** The question asked the first time the app opens, saying exactly what's collected. It needs an answer. */
@Composable
fun StatsConsentDialog(onAnswer: (Boolean) -> Unit) {
    YoinkAlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.stats_title)) },
        text = { WhatsCollected() },
        confirmButton = { TextButton(onClick = { onAnswer(true) }) { Text(stringResource(R.string.stats_yes)) } },
        dismissButton = { TextButton(onClick = { onAnswer(false) }) { Text(stringResource(R.string.stats_no)) } },
    )
}

@Composable
private fun WhatsCollected() {
    Text(stringResource(R.string.stats_what), modifier = Modifier.verticalScroll(rememberScrollState()))
}

/** Settings → Usage data: the same choice, what's collected, and deleting what was sent. */
@Composable
fun UsageDataItems() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sharing by remember { mutableStateOf(Stats.consent(context) == true) }
    var showing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(stringResource(R.string.stats_share)) },
        supportingContent = { Text(stringResource(if (sharing) R.string.stats_share_on else R.string.stats_share_off)) },
        trailingContent = { Switch(checked = sharing, onCheckedChange = null) },
        modifier = Modifier.clickable {
            sharing = !sharing
            Stats.setConsent(context, sharing)
        },
    )
    ListItem(
        headlineContent = { Text(stringResource(R.string.stats_what_title)) },
        modifier = Modifier.clickable { showing = true },
    )
    ListItem(
        headlineContent = { Text(stringResource(R.string.stats_delete)) },
        supportingContent = { Text(stringResource(R.string.stats_delete_summary)) },
        trailingContent = { if (deleting) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) },
        modifier = Modifier.clickable(enabled = !deleting) {
            deleting = true
            scope.launch {
                val done = Stats.forget(context)
                deleting = false
                Toast.makeText(context, if (done) R.string.stats_deleted else R.string.stats_delete_failed, Toast.LENGTH_LONG).show()
            }
        },
    )

    if (showing) {
        YoinkAlertDialog(
            onDismissRequest = { showing = false },
            title = { Text(stringResource(R.string.stats_what_title)) },
            text = { WhatsCollected() },
            confirmButton = { TextButton(onClick = { showing = false }) { Text(stringResource(R.string.stats_got_it)) } },
        )
    }
}
