package com.leet.rinsedownloader

import android.app.Application
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import com.leet.rinsedownloader.data.DownloadManager
import com.leet.rinsedownloader.data.ScheduleManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry


class MainApplication : Application() {

    lateinit var scheduleManager: ScheduleManager
    lateinit var downloadManager: DownloadManager

    private val client = HttpClient(OkHttp){
        install(HttpRequestRetry){
            retryOnServerErrors(5)
            exponentialDelay()
        }
        expectSuccess = true
    }


    private val downloadsCollectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        "content://downloads/public_downloads".toUri()
    }

    override fun onCreate() {
        super.onCreate()
        scheduleManager = ScheduleManager(
            client = client,
            filesDir = filesDir
        )
        downloadManager = DownloadManager(
            client = client,
            contentResolver = contentResolver,
            downloadCollectionUri = downloadsCollectionUri,
        )
    }

    override fun onTerminate(){
        super.onTerminate()
        client.close()
    }
}