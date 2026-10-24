package io.github.hishamhashmi100.yoink.ui.home

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.ui.share.ShareActivity

enum class HomeTab { DOWNLOADING, LIBRARY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    requestedTab: HomeTab?,
    onTabHandled: () -> Unit,
    actions: @Composable () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val context = LocalContext.current
    val active by viewModel.active.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf<HomeTab?>(null) }
    var addingLink by rememberSaveable { mutableStateOf(false) }

    // Open on Downloading when something is in progress, otherwise on the Library.
    LaunchedEffect(active == null) {
        if (tab == null && active != null) {
            tab = if (active.orEmpty().isNotEmpty()) HomeTab.DOWNLOADING else HomeTab.LIBRARY
        }
    }
    LaunchedEffect(requestedTab) {
        if (requestedTab != null) {
            tab = requestedTab
            onTabHandled()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = { actions() },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { addingLink = true }) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_link))
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            UpdateCard()
            val selected = tab ?: HomeTab.LIBRARY
            PrimaryTabRow(selectedTabIndex = selected.ordinal) {
                Tab(
                    selected = selected == HomeTab.DOWNLOADING,
                    onClick = { tab = HomeTab.DOWNLOADING },
                    text = {
                        val count = active.orEmpty().size
                        BadgedBox(badge = { if (count > 0) Badge { Text("$count") } }) {
                            Text(stringResource(R.string.tab_downloading))
                        }
                    },
                )
                Tab(
                    selected = selected == HomeTab.LIBRARY,
                    onClick = { tab = HomeTab.LIBRARY },
                    text = { Text(stringResource(R.string.tab_library)) },
                )
            }
            when (selected) {
                HomeTab.DOWNLOADING -> DownloadingScreen(viewModel)
                HomeTab.LIBRARY -> LibraryScreen(viewModel)
            }
        }
    }

    if (addingLink) {
        AddLinkDialog(
            onDismiss = { addingLink = false },
            onSubmit = { link ->
                addingLink = false
                // Same quality sheet as the Share menu.
                context.startActivity(
                    Intent(context, ShareActivity::class.java)
                        .setAction(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, link)
                )
            },
        )
    }
}
