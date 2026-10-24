package io.github.hishamhashmi100.yoink.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.data.Settings
import io.github.hishamhashmi100.yoink.data.ShareMode
import io.github.hishamhashmi100.yoink.download.Engine
import io.github.hishamhashmi100.yoink.download.Quality
import io.github.hishamhashmi100.yoink.yoink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = application.yoink.settings

    val settings: StateFlow<Settings> =
        store.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), store.current())

    private val _ytDlpVersion = MutableStateFlow<String?>(null)
    val ytDlpVersion = _ytDlpVersion.asStateFlow()

    private val _updating = MutableStateFlow(false)
    val updating = _updating.asStateFlow()

    /** One-shot messages for the snackbar. */
    val messages = MutableStateFlow<Int?>(null)

    init {
        viewModelScope.launch { _ytDlpVersion.value = runCatching { Engine.version(application) }.getOrNull() }
    }

    fun setShareMode(mode: ShareMode) = viewModelScope.launch { store.setShareMode(mode) }
    fun setVideoQuality(quality: Quality) = viewModelScope.launch { store.setVideoQuality(quality) }
    fun setSaveToGallery(value: Boolean) = viewModelScope.launch { store.setSaveToGallery(value) }

    fun updateDownloader() {
        if (_updating.value) return
        _updating.value = true
        viewModelScope.launch {
            val app = getApplication<Application>()
            messages.value = try {
                if (Engine.update(app)) R.string.settings_updated else R.string.settings_up_to_date
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                R.string.settings_update_failed
            }
            _ytDlpVersion.value = runCatching { Engine.version(app) }.getOrNull()
            _updating.value = false
        }
    }
}
