package com.leet.rinsedownloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.ui.Modifier
import com.example.compose.AppTheme
import com.leet.rinsedownloader.ui.model.DownloaderViewModel
import com.leet.rinsedownloader.ui.screens.MainScreen

class MainActivity : ComponentActivity() {

    private val viewModel: DownloaderViewModel by viewModels { DownloaderViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                MainScreen(
                    modifier = Modifier,
                    viewModel = viewModel,
                )
            }
        }
    }
}

