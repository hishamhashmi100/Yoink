package io.github.hishamhashmi100.yoink.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.data.ShareMode
import io.github.hishamhashmi100.yoink.download.Quality
import io.github.hishamhashmi100.yoink.ui.theme.WallpaperScreens
import io.github.hishamhashmi100.yoink.ui.theme.YoinkAlertDialog
import io.github.hishamhashmi100.yoink.ui.theme.YoinkTheme
import io.github.hishamhashmi100.yoink.update.AppUpdater
import io.github.hishamhashmi100.yoink.update.UpdateState
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {

    override fun startActivity(intent: Intent, options: Bundle?) {
        WallpaperScreens.starting(this, intent)
        super.startActivity(intent, options)
    }

    override fun finish() {
        WallpaperScreens.finishing(this)
        super.finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WallpaperScreens.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SettingsViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            YoinkTheme(settings) {
                SettingsScreen(viewModel, onBack = ::finish)
            }
        }
    }
}

private const val REPO_URL = "https://github.com/hishamhashmi100/Yoink"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val version by viewModel.ytDlpVersion.collectAsStateWithLifecycle()
    val updating by viewModel.updating.collectAsStateWithLifecycle()
    val message by viewModel.messages.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var pickingMode by rememberSaveable { mutableStateOf(false) }
    var pickingVideoQuality by rememberSaveable { mutableStateOf(false) }
    val shareModes = listOf(
        ShareMode.ASK to stringResource(R.string.share_mode_ask),
        ShareMode.VIDEO_OR_AUDIO to stringResource(R.string.share_mode_quick),
        ShareMode.VIDEO to stringResource(R.string.share_mode_video),
        ShareMode.AUDIO to stringResource(R.string.share_mode_audio),
    )
    val videoQualities = Quality.entries.filterNot { it.isAudio }.map { it to it.label(context.resources) }

    LaunchedEffect(message) {
        message?.let {
            viewModel.messages.value = null
            snackbar.showSnackbar(context.getString(it))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(R.string.settings_downloads)
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_share_mode)) },
                supportingContent = { Text(shareModes.first { it.first == settings.shareMode }.second) },
                modifier = Modifier.clickable { pickingMode = true },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_video_quality)) },
                supportingContent = { Text(settings.videoQuality.label(context.resources)) },
                modifier = Modifier.clickable { pickingVideoQuality = true },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_gallery)) },
                supportingContent = {
                    Text(stringResource(if (settings.saveToGallery) R.string.settings_gallery_on else R.string.settings_gallery_off))
                },
                trailingContent = {
                    Switch(checked = settings.saveToGallery, onCheckedChange = null)
                },
                modifier = Modifier.clickable { viewModel.setSaveToGallery(!settings.saveToGallery) },
            )

            SectionHeader(R.string.settings_downloader)
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_update)) },
                supportingContent = {
                    Column {
                        Text(stringResource(R.string.settings_update_summary))
                        Text(
                            version?.let { stringResource(R.string.settings_version, it.removePrefix("yt-dlp").trim()) } ?: stringResource(R.string.settings_version_missing),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                trailingContent = {
                    if (updating) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                },
                modifier = Modifier.clickable(enabled = !updating) { viewModel.updateDownloader() },
            )

            SectionHeader(R.string.settings_appearance)
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_customize)) },
                supportingContent = { Text(stringResource(R.string.settings_customize_summary)) },
                leadingContent = { Icon(Icons.Rounded.Palette, contentDescription = null) },
                modifier = Modifier.clickable { context.startActivity(Intent(context, AppearanceActivity::class.java)) },
            )

            SectionHeader(R.string.settings_about)
            val appVersion = remember {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
            }
            AppUpdatesItem()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_source)) },
                supportingContent = { Text(stringResource(R.string.settings_app_version, appVersion)) },
                modifier = Modifier.clickable {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL))) }
                },
            )
            Text(
                stringResource(R.string.settings_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }

    if (pickingMode) {
        ChoiceDialog(
            title = stringResource(R.string.settings_share_mode),
            hint = stringResource(R.string.settings_share_mode_hint),
            options = shareModes,
            selected = settings.shareMode,
            onSelect = {
                viewModel.setShareMode(it)
                pickingMode = false
            },
            onDismiss = { pickingMode = false },
        )
    }
    if (pickingVideoQuality) {
        ChoiceDialog(
            title = stringResource(R.string.settings_video_quality),
            hint = stringResource(R.string.settings_video_quality_hint),
            options = videoQualities,
            selected = settings.videoQuality,
            onSelect = {
                viewModel.setVideoQuality(it)
                pickingVideoQuality = false
            },
            onDismiss = { pickingVideoQuality = false },
        )
    }
}

/** Manual "check for updates"; the same flow as the card on Home. */
@Composable
private fun AppUpdatesItem() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by AppUpdater.state.collectAsStateWithLifecycle()
    val current = remember { AppUpdater.currentVersion(context) }
    val selfUpdate = remember { AppUpdater.canSelfUpdate(context) }

    val summary = when (val s = state) {
        UpdateState.Idle -> stringResource(R.string.settings_check_updates, current)
        UpdateState.Checking -> stringResource(R.string.settings_checking)
        UpdateState.UpToDate -> stringResource(R.string.settings_latest, current)
        is UpdateState.Available -> stringResource(
            if (selfUpdate) R.string.settings_update_available else R.string.settings_update_debug,
            s.release.version,
        )
        is UpdateState.Downloading -> stringResource(R.string.update_downloading, (s.progress * 100).toInt())
        is UpdateState.ReadyToInstall -> stringResource(R.string.update_ready)
        is UpdateState.Failed -> s.message
    }

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_app_updates)) },
        supportingContent = { Text(summary) },
        trailingContent = {
            if (state is UpdateState.Checking) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        },
        modifier = Modifier.clickable(enabled = state !is UpdateState.Checking && state !is UpdateState.Downloading) {
            when (val s = state) {
                is UpdateState.Available -> if (selfUpdate) AppUpdater.download(context)
                is UpdateState.Failed -> if (s.release != null && selfUpdate) AppUpdater.download(context) else scope.launch { AppUpdater.check(context, force = true) }
                is UpdateState.ReadyToInstall -> AppUpdater.install(context)
                else -> scope.launch { AppUpdater.check(context, force = true) }
            }
        },
    )
}

@Composable
private fun SectionHeader(title: Int) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    hint: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    YoinkAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(hint, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
                options.forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = value == selected, role = Role.RadioButton) { onSelect(value) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
