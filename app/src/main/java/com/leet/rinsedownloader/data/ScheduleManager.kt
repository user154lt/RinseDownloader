package com.leet.rinsedownloader.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException

fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

sealed class EpisodeResult {
    data class Success(val timestamp: Long, val episodes: List<Episode>) : EpisodeResult()
    data class Error(val timestamp: Long, val e: Throwable) : EpisodeResult()
}

class ScheduleManager(
    private val client: HttpClient,
    filesDir: File
) {

    private val scheduleDir = File(filesDir, "schedule")
    private val json = Json {
        ignoreUnknownKeys = true
    }

    init {
        if (!scheduleDir.exists()) scheduleDir.mkdirs()
    }

    fun entriesForRange(range: Pair<Long, Long>): Flow<EpisodeResult> = channelFlow {
        val timestamps = (range.first..range.second step Duration.ofDays(1).toMillis()).toList()
        coroutineScope {
            for (timestamp in timestamps) {
                launch(Dispatchers.IO) {
                    val result = entriesForDay(timestamp)
                    send(result)
                }
            }
        }
    }

    suspend fun entriesForDay(timestamp: Long) = withContext(Dispatchers.IO) {
        try {
            ensureActive()
            val date = timestamp.toLocalDate()
            val scheduleJson = getScheduleFor(date)
            ensureActive()
            val episodeList = json.decodeFromString<EpisodeList>(scheduleJson)
                .entries
                .filter {
                    it.fileUrl != null && it.fileUrl.split(".").last() == "mp3"
                }
                .map {
                    episodeFrom(it)
                }
            EpisodeResult.Success(timestamp, episodeList)
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            EpisodeResult.Error(timestamp, e)
        }
    }


    private suspend fun getScheduleFor(date: LocalDate) = withContext(Dispatchers.IO) {
        val month = "${date.monthValue}".padStart(2, '0')
        val day = "${date.dayOfMonth}".padStart(2, '0')
        val dateString = "${date.year}-$month-$day"
        if (!isSchedulePresent(dateString)) downloadSchedule(dateString)
        getScheduleJsonFromFile(dateString)
    }

    private fun isSchedulePresent(dateString: String) =
        File(scheduleDir, "$dateString.json").exists()

    private suspend fun downloadSchedule(dateString: String) = withContext(Dispatchers.IO) {
        val response = client.get("https://www.rinse.fm/api/query/v1/schedule/$dateString/")
        File(scheduleDir, "$dateString.json").writeText(response.body())
    }

    private fun getScheduleJsonFromFile(dateString: String) =
        File(scheduleDir, "$dateString.json").readText()


    private fun episodeFrom(entry: EpisodeEntry) =
        Episode(
            title = entry.title,
            subtitle = entry.displayTitle,
            date = LocalDate.parse(entry.episodeDate?.substring(0, 10)),
            channel = when (entry.channel.first { channelEntry -> channelEntry.slug != "" }.slug) {
                "uk" -> RinseChannel.RINSE_UK
                "france" -> RinseChannel.RINSE_FRANCE
                "kool" -> RinseChannel.KOOL
                "swu" -> RinseChannel.SWU
                else -> RinseChannel.RINSE_UK
            },
            fileUrl = entry.fileUrl ?: ""
        )
}