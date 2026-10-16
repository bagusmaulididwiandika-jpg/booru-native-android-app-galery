package com.example.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.downloader.DownloadManager
import com.example.data.local.entity.DownloadTaskEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DownloadsUiState(
    val selectedTab: Int = 0, // 0 = Active Queue, 1 = Completed, 2 = Failed
    val tasks: List<DownloadTaskEntity> = emptyList()
)

class DownloadsViewModel(
    private val downloadManager: DownloadManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            downloadManager.allTasks.collect { taskList ->
                _uiState.update { it.copy(tasks = taskList) }
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun cancelDownload(downloadId: String) {
        downloadManager.cancelDownload(downloadId)
    }

    fun retryDownload(task: DownloadTaskEntity) {
        downloadManager.retryDownload(task)
    }

    fun deleteTask(downloadId: String) {
        downloadManager.deleteTask(downloadId)
    }

    fun clearCompleted() {
        downloadManager.clearCompleted()
    }
}
