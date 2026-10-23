package io.github.hishamhashmi100.yoink.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.data.Settings
import io.github.hishamhashmi100.yoink.ui.home.DownloadingScreen
import io.github.hishamhashmi100.yoink.ui.theme.YoinkTheme
import io.github.hishamhashmi100.yoink.yoink

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by yoink.settings.settings.collectAsStateWithLifecycle(Settings())
            YoinkTheme(settings) {
                Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }) { padding ->
                    DownloadingScreen(Modifier.padding(padding))
                }
            }
        }
    }

    companion object {
        const val EXTRA_PLAY_DOWNLOAD = "play_download"
    }
}
