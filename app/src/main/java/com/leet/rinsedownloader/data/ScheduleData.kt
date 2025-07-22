package com.leet.rinsedownloader.data

import java.time.LocalDate
import java.util.UUID

enum class RinseChannel{
    RINSE_ALL,
    RINSE_UK,
    RINSE_FRANCE,
    KOOL,
    SWU;

    override fun toString(): String {
        return when(this){
            RINSE_ALL -> "All Channels"
            RINSE_UK -> "Rinse UK"
            RINSE_FRANCE -> "Rinse france"
            KOOL -> "Kool"
            SWU -> "SWU"
        }
    }
}

sealed class DownloadState{
    data object NotStarted : DownloadState()
    data class InProgress(val progress: Float) : DownloadState(){
        override fun toString() = "${(progress * 100).toInt()}%"
    }
    data object Completed : DownloadState(){
        override fun toString() = "Download completed"
    }
    data class Cancelled(val reason: String) : DownloadState() {
        override fun toString() = reason
    }
}

data class ScheduleData(
    val entries: List<Episode> = emptyList(),
)

data class Episode(
    val title: String,
    val subtitle: String? = null,
    val date: LocalDate,
    val channel: RinseChannel,
    val fileUrl: String,
    val id: UUID = UUID.nameUUIDFromBytes((title + fileUrl).toByteArray()),
    val downloadState : DownloadState = DownloadState.NotStarted,
)