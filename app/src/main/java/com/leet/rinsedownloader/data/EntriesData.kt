package com.leet.rinsedownloader.data

import kotlinx.serialization.Serializable

@Serializable
data class EpisodeList(
    val entries: List<EpisodeEntry>
)

@Serializable
data class EpisodeEntry(
    val id: String,
    val slug: String,
    val title: String,
    val artistTitle: String? = null,
    val displayTitle: String? = null,
    val subtitle: String? = null,
    val extract: String? = null,
    val episodeDate: String? = null,
    val episodeTime: String? = null,
    val episodeLength: Int? = null,
    val isRebroadcast: Boolean,
    val fileUrl: String? = null,
    val youtubeStreamUrl: String? = null,
    val hideFromSchedule: Boolean? = null,
    val isPermanentEpisode: Boolean? = null,
    val parentShow: List<ShowEntry> = emptyList(),
    val channel: List<ChannelEntry> = emptyList(),
    val genreTag: List<GenreTagEntry> = emptyList(),
    val originalEpisode: List<EpisodeEntry> = emptyList()
)

@Serializable
data class ShowEntry(
    val id: String,
    val url: String,
    val slug: String,
    val title: String,
    val showStatus: String,
    val extract: String? = null,
    val genreTag: List<GenreTagEntry> = emptyList(),
    val channel: List<ChannelEntry> = emptyList()
)

@Serializable
data class ChannelEntry(
    val id: String? = null,
    val title: String? = null,
    val slug: String
)

@Serializable
data class GenreTagEntry(
    val slug: String,
    val title: String
)