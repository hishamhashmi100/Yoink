package io.github.hishamhashmi100.yoink.ui.home

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.ui.player.AudioController
import io.github.hishamhashmi100.yoink.ui.player.NowPlayingOverlay
import io.github.hishamhashmi100.yoink.ui.share.ShareActivity
import io.github.hishamhashmi100.yoink.ui.theme.GlassBackdrop
import io.github.hishamhashmi100.yoink.ui.theme.LocalBackdropColors
import io.github.hishamhashmi100.yoink.ui.theme.LocalGlass
import io.github.hishamhashmi100.yoink.ui.theme.LocalHazeState
import io.github.hishamhashmi100.yoink.ui.theme.glassPanel
import kotlinx.coroutines.launch

enum class HomeTab { DOWNLOADING, LIBRARY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    requestedTab: HomeTab?,
    onTabHandled: () -> Unit,
    openPlayer: Boolean = false,
    onPlayerHandled: () -> Unit = {},
    actions: @Composable () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val context = LocalContext.current
    val active by viewModel.active.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf<HomeTab?>(null) }
    var addingLink by rememberSaveable { mutableStateOf(false) }
    // The full-screen player: 0 = tucked behind the mini player, 1 = open. It follows the finger while
    // dragged (from the mini player or the player itself), then springs open or closed.
    val player = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var screenHeight by remember { mutableFloatStateOf(1f) }
    val nowPlaying by AudioController.nowPlaying.collectAsStateWithLifecycle()
    fun settlePlayer(open: Boolean) {
        scope.launch { player.animateTo(if (open) 1f else 0f, spring(stiffness = Spring.StiffnessMediumLow)) }
    }
    val dragPlayer: (Float) -> Unit = { delta ->
        scope.launch { player.snapTo((player.value - delta / screenHeight).coerceIn(0f, 1f)) }
    }
    val releasePlayer: (Float) -> Unit = { velocity ->
        // A flick decides on its own; otherwise it opens once pulled past a third of the way.
        settlePlayer(
            when {
                velocity < -1_000f -> true
                velocity > 1_000f -> false
                else -> player.value > 0.33f
            },
        )
    }
    LaunchedEffect(nowPlaying == null) {
        if (nowPlaying == null) player.snapTo(0f)
    }
    BackHandler(enabled = player.value > 0f) { settlePlayer(false) }
    val glass = LocalGlass.current
    val hazeState = rememberHazeState()
    // The theme's color backdrop can be blurred; the phone's wallpaper can't (it's not part of the app).
    val blurBackdrop = glass.enabled && !glass.wallpaper

    // Open on Downloading when something is in progress, otherwise on the Library.
    LaunchedEffect(active == null) {
        if (tab == null && active != null) {
            tab = if (active.orEmpty().isNotEmpty()) HomeTab.DOWNLOADING else HomeTab.LIBRARY
        }
    }
    LaunchedEffect(openPlayer) {
        if (openPlayer) {
            settlePlayer(true)
            onPlayerHandled()
        }
    }
    LaunchedEffect(requestedTab) {
        if (requestedTab != null) {
            tab = requestedTab
            onTabHandled()
        }
    }

    CompositionLocalProvider(LocalHazeState provides hazeState.takeIf { blurBackdrop }) {
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { screenHeight = it.height.toFloat().coerceAtLeast(1f) },
    ) {
    // With glass, the backdrop is part of what the mini player and + button blur, under the lists. Without
    // it they'd show a solid color wherever no list item is behind them.
    if (blurBackdrop) {
        GlassBackdrop(
            LocalBackdropColors.current ?: MaterialTheme.colorScheme,
            Modifier.hazeSource(hazeState, zIndex = 0f),
        )
    }
    Scaffold(
        // Over the wallpaper the player has no backdrop of its own, so the screen fades away under it.
        modifier = if (glass.wallpaper && glass.enabled) Modifier.graphicsLayer { alpha = 1f - player.value } else Modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = { actions() },
            )
        },
        bottomBar = {
            // Fades as it's pulled up, so it turns into the full-screen player.
            Box(Modifier.graphicsLayer { alpha = 1f - player.value }) {
                MiniPlayer(onOpen = { settlePlayer(true) }, onDrag = dragPlayer, onDragStopped = releasePlayer)
            }
        },
        floatingActionButton = {
            val fabShape = MaterialTheme.shapes.large
            FloatingActionButton(
                onClick = { addingLink = true },
                shape = fabShape,
                containerColor = if (glass.enabled) Color.Transparent else FloatingActionButtonDefaults.containerColor,
                elevation = if (glass.enabled) {
                    FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                } else {
                    FloatingActionButtonDefaults.elevation()
                },
                modifier = Modifier.glassPanel(glass, fabShape, hazeState.takeIf { blurBackdrop }),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_link))
            }
        },
    ) { padding ->
        // With glass, the lists run on under the mini player and the + button, which blur them.
        val direction = LocalLayoutDirection.current
        val bottomInset = if (glass.enabled) padding.calculateBottomPadding() else 0.dp
        Column(
            Modifier
                .padding(
                    start = padding.calculateStartPadding(direction),
                    top = padding.calculateTopPadding(),
                    end = padding.calculateEndPadding(direction),
                    bottom = padding.calculateBottomPadding() - bottomInset,
                )
                .fillMaxSize()
                .then(if (blurBackdrop) Modifier.hazeSource(hazeState, zIndex = 1f) else Modifier),
        ) {
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
                HomeTab.DOWNLOADING -> DownloadingScreen(viewModel, bottomInset = bottomInset)
                HomeTab.LIBRARY -> LibraryScreen(viewModel, bottomInset = bottomInset)
            }
        }
    }
    // On top of everything on Home (bars, the + button, the mini player), so the full-screen player covers it all.
    if (player.value > 0f) {
        NowPlayingOverlay(
            progress = player.value,
            onDrag = dragPlayer,
            onDragStopped = releasePlayer,
            onClose = { settlePlayer(false) },
        )
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
