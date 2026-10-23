package io.github.hishamhashmi100.yoink.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hishamhashmi100.yoink.data.Settings
import io.github.hishamhashmi100.yoink.ui.home.HomeScreen
import io.github.hishamhashmi100.yoink.ui.home.HomeTab
import io.github.hishamhashmi100.yoink.ui.theme.YoinkTheme
import io.github.hishamhashmi100.yoink.yoink

class MainActivity : ComponentActivity() {

    private val requestedTab = mutableStateOf<HomeTab?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) requestedTab.value = tabFrom(intent)
        setContent {
            val settings by yoink.settings.settings.collectAsStateWithLifecycle(Settings())
            YoinkTheme(settings) {
                HomeScreen(
                    requestedTab = requestedTab.value,
                    onTabHandled = { requestedTab.value = null },
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
