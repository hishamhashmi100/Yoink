package io.github.hishamhashmi100.yoink.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.hishamhashmi100.yoink.data.Download
import io.github.hishamhashmi100.yoink.data.DownloadStatus
import io.github.hishamhashmi100.yoink.download.LiveProgress
import io.github.hishamhashmi100.yoink.yoink
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActiveDownload(val download: Download, val progress: LiveProgress?)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = application.yoink.repository

    val active: StateFlow<List<ActiveDownload>> =
        combine(repository.downloads, repository.progress) { downloads, progress ->
            downloads
                .filter { it.status != DownloadStatus.COMPLETED }
                .sortedBy { it.createdAt }
                .map { ActiveDownload(it, progress[it.id]) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun cancel(id: Long) = viewModelScope.launch { repository.cancel(id) }

    fun retry(id: Long) = viewModelScope.launch { repository.retry(id) }
}
