package com.leet.rinsedownloader.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leet.rinsedownloader.MainApplication
import com.leet.rinsedownloader.data.DownloadManager
import com.leet.rinsedownloader.data.DownloadState
import com.leet.rinsedownloader.data.Episode
import com.leet.rinsedownloader.data.EpisodeResult
import com.leet.rinsedownloader.data.RinseChannel
import com.leet.rinsedownloader.data.ScheduleData
import com.leet.rinsedownloader.data.ScheduleManager
import com.leet.rinsedownloader.data.toLocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class DownloaderViewModel(
    private val scheduleManager: ScheduleManager,
    private val downloadManager: DownloadManager
) : ViewModel() {

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MainApplication)
                DownloaderViewModel(
                    application.scheduleManager,
                    application.downloadManager
                )
            }
        }
    }

    private val _uiState = MutableStateFlow(DownloaderUIState())
    val uiState: StateFlow<DownloaderUIState> = _uiState.asStateFlow()

    private val episodeJobMap = mutableMapOf<UUID, Job>()
    private var scheduleJob: Job? = null


    fun showDatePicker(isShowing: Boolean) = viewModelScope.launch {
        _uiState.update {
            it.copy(
                isShowingDatePicker = isShowing
            )
        }
    }

    fun dismissSnackbar() = viewModelScope.launch {
        _uiState.update {
            it.copy(
                snackbarMessage = null
            )
        }
    }

    fun getSchedulesFor(range: Pair<Long?, Long?>) = viewModelScope.launch {
        setUiSelectedDates(range)
        cancelAllJobs()
        if (range.first == null && range.second == null) return@launch
        scheduleJob = launch {
            if (range.first != null && range.second == null) {
                loadSingleScheduleFor(range.first!!)
            } else {
                loadSchedulesFor(Pair(range.first!!, range.second!!))
            }
        }
    }

    private fun setUiSelectedDates(range: Pair<Long?, Long?>) {
        val firstDate = if (range.first != null) "${range.first!!.toLocalDate()}" else ""
        val secondDate = if (range.second != null) " - ${range.second!!.toLocalDate()}" else ""
        _uiState.update {
            it.copy(
                selectedDates = "$firstDate$secondDate",
                isLoadingSchedules = true
            )
        }
    }

    private suspend fun cancelAllJobs(){
        scheduleJob?.cancelAndJoin()
        episodeJobMap.entries.forEach { it.value.cancelAndJoin() }
        episodeJobMap.clear()
    }

    private suspend fun loadSingleScheduleFor(timestamp: Long) = withContext(Dispatchers.IO) {
        when (val result = scheduleManager.entriesForDay(timestamp)) {
            is EpisodeResult.Success -> {
                withContext(Dispatchers.Main) { updateSingleSchedule(result.episodes) }
            }

            is EpisodeResult.Error -> {
                updateIsLoading(false)
                if (result.e is CancellationException) throw result.e
                result.e.printStackTrace()
            }
        }
    }

    private suspend fun updateSingleSchedule(episodes: List<Episode>) = withContext(Dispatchers.Main) {
        _uiState.update {
            it.copy(
                scheduleData = ScheduleData(episodes),
                isLoadingSchedules = false,
            )
        }
    }

    private suspend fun loadSchedulesFor(range: Pair<Long, Long>) = withContext(Dispatchers.IO) {
        val accumulatedEpisodes = mutableListOf<Episode>()
        val failedTimestamps = mutableListOf<Long>()
        scheduleManager.entriesForRange(Pair(range.first, range.second))
            .onStart {
                updateIsLoading(true)
            }
            .onCompletion {
                updateIsLoading(false)
                if (failedTimestamps.isNotEmpty()) updateErrorMessage(failedTimestamps)
            }
            .collect { result ->
                when (result) {
                    is EpisodeResult.Success -> {
                        accumulatedEpisodes += result.episodes
                        _uiState.update { currentState ->
                            currentState.copy(
                                scheduleData = ScheduleData(accumulatedEpisodes.sortedBy { it.date })
                            )
                        }
                    }

                    is EpisodeResult.Error -> {
                        if (result.e is CancellationException) throw result.e
                        failedTimestamps.add(result.timestamp)
                        result.e.printStackTrace()
                    }
                }

            }
    }

    private suspend fun updateErrorMessage(failedTimestamps: List<Long>) = withContext(Dispatchers.Main) {
        val message = if(failedTimestamps.size == 1){
            "Failed to get schedule for ${failedTimestamps.first().toLocalDate()}"
        } else {
            "Failed to get ${failedTimestamps.size} schedules"
        }
        _uiState.update {
            it.copy(
                snackbarMessage = message
            )
        }
    }

    private suspend fun updateIsLoading(isLoading: Boolean) = withContext(Dispatchers.Main) {
        _uiState.update {
            it.copy(
                isLoadingSchedules = isLoading
            )
        }
    }




    fun filterChannels(channel: RinseChannel) = viewModelScope.launch {
        _uiState.update {
            it.copy(
                scheduleFilter = channel
            )
        }
    }

    fun downloadFile(episode: Episode) = viewModelScope.launch {
        if (episode.downloadState is DownloadState.InProgress) return@launch
        updateDownloadState(episode, DownloadState.InProgress(0f))
        val job = launch(Dispatchers.IO) {
            try {
                downloadManager.downloadFile(episode.fileUrl) {
                    withContext(Dispatchers.Main) {
                        updateDownloadProgress(episode, it)
                    }
                }
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    throw e
                } else {
                    onDownloadFailed(episode)
                }
            }
        }
        episodeJobMap[episode.id] = job
    }

    private fun updateDownloadProgress(episode: Episode, progress: Float) {
        if (progress == 1f) {
            updateDownloadState(episode, DownloadState.Completed)
            episodeJobMap.remove(episode.id)
        } else {
            updateDownloadState(episode, DownloadState.InProgress(progress))
        }
    }

    private fun onDownloadFailed(episode: Episode) {
        cancelEpisodeJob(episode)
        updateDownloadState(episode, DownloadState.Cancelled("Download failed"))
    }

    fun cancelDownload(episode: Episode) {
        cancelEpisodeJob(episode)
        updateDownloadState(episode, DownloadState.Cancelled("Download cancelled"))
    }

    private fun cancelEpisodeJob(episode: Episode) = viewModelScope.launch {
        episodeJobMap[episode.id]?.cancelAndJoin()
        episodeJobMap.remove(episode.id)
    }


    private fun updateDownloadState(episode: Episode, state: DownloadState) {
        val updatedEntries = uiState.value.scheduleData.entries.map {
            if (it.id == episode.id) {
                it.copy(downloadState = state)
            } else {
                it
            }
        }
        _uiState.update {
            it.copy(
                scheduleData = ScheduleData(updatedEntries)
            )
        }
    }
}