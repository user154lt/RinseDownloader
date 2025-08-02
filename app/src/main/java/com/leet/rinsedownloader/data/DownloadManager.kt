package com.leet.rinsedownloader.data

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.prepareGet
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.exhausted
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.coroutineScope
import kotlinx.io.asSink

class DownloadManager(
    private val client: HttpClient,
    private val contentResolver: ContentResolver,
    private val downloadCollectionUri: Uri,
) {

    private val isSDKQOrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun insertFileEntry(fileName: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "audio/mpeg")
            if (isSDKQOrAbove) put(MediaStore.Downloads.IS_PENDING, 1)
        }
        return contentResolver.insert(downloadCollectionUri, contentValues)
    }

    suspend fun downloadFile(
        url: String,
        localFileUri: Uri,
        updateProgress: suspend (Float) -> Unit
    ) {
        val stream = contentResolver.openOutputStream(localFileUri)?.asSink()
        coroutineScope {
            client.prepareGet(url).execute { response ->
                var count = 0L
                val channel: ByteReadChannel = response.body()
                stream?.use { fileStream ->
                    while (!channel.exhausted()) {
                        val chunk = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                        count += chunk.remaining
                        updateDownloadProgress(count, response.contentLength(), updateProgress)
                        chunk.transferTo(fileStream)
                    }
                }
            }
        }
    }

    private suspend fun updateDownloadProgress(
        count: Long,
        size: Long?,
        updateProgress: suspend (Float) -> Unit
    ) {
        val progress = if (size != null) count / size.toFloat() else -1f
        updateProgress(progress)
    }

    fun clearPending(fileUri: Uri) {
        if (isSDKQOrAbove) {
            val clearPending = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            contentResolver.update(fileUri, clearPending, null, null)
        }
    }

    fun deleteFile(fileUri: Uri){
        contentResolver.delete(fileUri, null, null)
    }

}