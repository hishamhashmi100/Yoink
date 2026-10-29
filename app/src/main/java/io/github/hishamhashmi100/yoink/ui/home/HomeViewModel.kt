package io.github.hishamhashmi100.yoink.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.hishamhashmi100.yoink.data.Download
import io.github.hishamhashmi100.yoink.data.DownloadStatus
import io.github.hishamhashmi100.yoink.download.LiveProgress
import io.github.hishamhashmi100.yoink.yoink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActiveDownload(val download: Download, val progress: LiveProgress?)

/** PLAYLISTS shows the user's own playlists instead of downloads. */
enum class LibraryFilter { ALL, VIDEO, AUDIO, PLAYLISTS }

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = application.yoink.repository

    val query = MutableStateFlow("")
    val filter = MutableStateFlow(LibraryFilter.ALL)
    val grid = MutableStateFlow(false)

    /** The playlist open under Library → Playlists, or null for the list of them. */
    val openPlaylist = MutableStateFlow<Long?>(null)

    /** Null until the database has answered once. */
    val active: StateFlow<List<ActiveDownload>?> =
        combine(repository.downloads, repository.progress) { downloads, progress ->
            downloads
                .filter { it.status != DownloadStatus.COMPLETED }
                .sortedBy { it.createdAt }
                .map { ActiveDownload(it, progress[it.id]) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val hasLibraryItems: StateFlow<Boolean> =
        repository.downloads.map { downloads ->
            downloads.any { it.status == DownloadStatus.COMPLETED }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val library: StateFlow<List<Download>> =
        combine(repository.downloads, query, filter) { downloads, query, filter ->
            downloads.filter { d ->
                d.status == DownloadStatus.COMPLETED &&
                    when (filter) {
                        LibraryFilter.ALL -> true
                        LibraryFilter.VIDEO -> !d.quality.isAudio
                        LibraryFilter.AUDIO -> d.quality.isAudio
                        LibraryFilter.PLAYLISTS -> false
                    } &&
                    (query.isBlank() || d.title.contains(query.trim(), ignoreCase = true) ||
                        d.channel?.contains(query.trim(), ignoreCase = true) == true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun cancel(id: Long) = viewModelScope.launch { repository.cancel(id) }

    fun retry(id: Long) = viewModelScope.launch { repository.retry(id) }

    fun cancelAll() = viewModelScope.launch { repository.cancelAll() }

    fun delete(download: Download) = viewModelScope.launch { repository.delete(download) }
}
