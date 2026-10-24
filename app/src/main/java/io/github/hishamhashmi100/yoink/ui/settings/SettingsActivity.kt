package io.github.hishamhashmi100.yoink.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import io.github.hishamhashmi100.yoink.data.ThemeMode
import io.github.hishamhashmi100.yoink.download.Quality
import io.github.hishamhashmi100.yoink.ui.theme.YoinkTheme
import io.github.hishamhashmi100.yoink.update.AppUpdater
import io.github.hishamhashmi100.yoink.update.UpdateState
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
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
    var pickingQuality by rememberSaveable { mutableStateOf(false) }

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
                headlineContent = { Text(stringResource(R.string.settings_default_quality)) },
                supportingContent = {
                    Text(settings.defaultQuality?.label(context.resources) ?: stringResource(R.string.settings_ask_every_time))
                },
                modifier = Modifier.clickable { pickingQuality = true },
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
                        version?.let { Text(stringResource(R.string.settings_version, it), style = MaterialTheme.typography.bodySmall) }
                    }
                },
                trailingContent = {
                    if (updating) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                },
                modifier = Modifier.clickable(enabled = !updating) { viewModel.updateDownloader() },
            )

            SectionHeader(R.string.settings_appearance)
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_theme)) },
                supportingContent = {
                    val options = listOf(
                        ThemeMode.SYSTEM to R.string.theme_system,
                        ThemeMode.LIGHT to R.string.theme_light,
                        ThemeMode.DARK to R.string.theme_dark,
                    )
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        options.forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                selected = settings.theme == mode,
                                onClick = { viewModel.setTheme(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                            ) { Text(stringResource(label)) }
                        }
                    }
                },
            )
            if (Build.VERSION.SDK_INT >= 31) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_dynamic_color)) },
                    supportingContent = { Text(stringResource(R.string.settings_dynamic_color_summary)) },
                    trailingContent = { Switch(checked = settings.dynamicColor, onCheckedChange = null) },
                    modifier = Modifier.clickable { viewModel.setDynamicColor(!settings.dynamicColor) },
                )
            }

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

    if (pickingQuality) {
        QualityDialog(
            selected = settings.defaultQuality,
            onSelect = {
                viewModel.setDefaultQuality(it)
                pickingQuality = false
            },
            onDismiss = { pickingQuality = false },
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
private fun QualityDialog(selected: Quality?, onSelect: (Quality?) -> Unit, onDismiss: () -> Unit) {
    val resources = LocalContext.current.resources
    val options: List<Pair<Quality?, String>> =
        listOf(null to stringResource(R.string.settings_ask_every_time)) + Quality.entries.map { it to it.label(resources) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_default_quality)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.settings_default_quality_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                options.forEach { (quality, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = quality == selected, role = Role.RadioButton) { onSelect(quality) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadioButton(selected = quality == selected, onClick = null)
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
