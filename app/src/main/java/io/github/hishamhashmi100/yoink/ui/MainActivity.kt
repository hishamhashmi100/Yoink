package io.github.hishamhashmi100.yoink.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings as SettingsIcon
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.ui.home.HomeScreen
import io.github.hishamhashmi100.yoink.ui.home.HomeTab
import io.github.hishamhashmi100.yoink.ui.player.AudioController
import io.github.hishamhashmi100.yoink.ui.settings.SettingsActivity
import io.github.hishamhashmi100.yoink.ui.theme.YoinkTheme
import io.github.hishamhashmi100.yoink.yoink

class MainActivity : ComponentActivity() {

    private val requestedTab = mutableStateOf<HomeTab?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AudioController.connect(this)
        if (savedInstanceState == null) requestedTab.value = tabFrom(intent)
        setContent {
            val settings by yoink.settings.settings.collectAsStateWithLifecycle(remember { yoink.settings.current() })
            YoinkTheme(settings) {
                HomeScreen(
                    requestedTab = requestedTab.value,
                    onTabHandled = { requestedTab.value = null },
                    actions = {
                        IconButton(onClick = { startActivity(Intent(this, SettingsActivity::class.java)) }) {
                            Icon(Icons.Rounded.SettingsIcon, contentDescription = stringResource(R.string.settings))
                        }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        tabFrom(intent)?.let { requestedTab.value = it }
    }

    private fun tabFrom(intent: Intent?): HomeTab? =
        intent?.getStringExtra(EXTRA_TAB)?.let { name -> HomeTab.entries.firstOrNull { it.name == name } }

    companion object {
        const val EXTRA_TAB = "tab"
    }
}
