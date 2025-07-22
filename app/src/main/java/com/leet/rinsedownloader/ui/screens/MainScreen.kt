package com.leet.rinsedownloader.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leet.rinsedownloader.R
import com.leet.rinsedownloader.data.DownloadState
import com.leet.rinsedownloader.data.Episode
import com.leet.rinsedownloader.data.RinseChannel
import com.leet.rinsedownloader.data.RinseSelectableDates
import com.leet.rinsedownloader.ui.components.DefaultCard
import com.leet.rinsedownloader.ui.components.DefaultColumn
import com.leet.rinsedownloader.ui.components.DefaultRow
import com.leet.rinsedownloader.ui.components.DefaultSubtitleText
import com.leet.rinsedownloader.ui.components.DefaultTextButton
import com.leet.rinsedownloader.ui.components.DefaultTitleText
import com.leet.rinsedownloader.ui.components.StartAlignedColumn
import com.leet.rinsedownloader.ui.model.DownloaderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloaderViewModel = viewModel(),
) {
    val uiState = viewModel.uiState.collectAsState()
    if (uiState.value.isShowingDatePicker) ScheduleDateRangePicker(
        onDismissRequest = { viewModel.showDatePicker(false) },
        onDateRangeSelected = viewModel::getSchedulesFor
    )
    val filteredEpisodes by remember(
        uiState.value.scheduleData.entries,
        uiState.value.scheduleFilter
    ) {
        derivedStateOf {
            when (uiState.value.scheduleFilter) {
                RinseChannel.RINSE_ALL -> uiState.value.scheduleData.entries
                else -> uiState.value.scheduleData.entries.filter { it.channel == uiState.value.scheduleFilter }
            }
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.app_title))
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        }
    ) {
        if(uiState.value.snackbarMessage != null) {
            ShowSnackbar(
                hostState = snackbarHostState,
                message = uiState.value.snackbarMessage!!,
                onDismiss = viewModel::dismissSnackbar
            )
        }
        DefaultColumn(
            modifier = Modifier.padding(it),
        ) {
            DateSelectionText(
                selectedDate = uiState.value.selectedDates,
                showDatePicker = { viewModel.showDatePicker(true) }
            )
            if (uiState.value.scheduleData.entries.isNotEmpty())
                ChannelFilter( onChannelSelected = viewModel::filterChannels)

            EpisodeList(
                modifier = Modifier.weight(1f),
                episodes = filteredEpisodes,
                downloadFile = viewModel::downloadFile,
                cancelDownload = viewModel::cancelDownload,
            )

            if (uiState.value.isLoadingSchedules) LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ShowSnackbar(
    hostState: SnackbarHostState,
    message: String,
    onDismiss: () -> Unit,
){
  LaunchedEffect(message) {
      val result = hostState.showSnackbar(
          message = message,
      )
      if (result == SnackbarResult.Dismissed) {
          onDismiss()
      }
  }
}

@Composable
fun DateSelectionText(
    modifier: Modifier = Modifier,
    selectedDate: String = "",
    showDatePicker: () -> Unit = {},
) {
    OutlinedTextField(
        value = selectedDate,
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable(onClick = showDatePicker),
        enabled = false,
        readOnly = true,
        onValueChange = {},
        label = {
            Text(text = stringResource(R.string.select_dates_label))
        }
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDateRangePicker(
    modifier: Modifier = Modifier,
    state: DateRangePickerState = rememberDateRangePickerState(
        selectableDates = RinseSelectableDates()
    ),
    onDateRangeSelected: (Pair<Long?, Long?>) -> Unit = {},
    onDismissRequest: () -> Unit,
) {
    DatePickerDialog(
        modifier = modifier,
        onDismissRequest = {},
        confirmButton = {
            DefaultTextButton(
                label = stringResource(R.string.confirm_button_label)
            ) {
                onDateRangeSelected(
                    Pair(state.selectedStartDateMillis, state.selectedEndDateMillis)
                )
                onDismissRequest()
            }
        },
        dismissButton = {
            DefaultTextButton(
                label = stringResource(R.string.dismiss_button_label),
                onClick = onDismissRequest
            )
        }
    ) {
        DateRangePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelFilter(
    modifier: Modifier = Modifier,
    onChannelSelected: (RinseChannel) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    val textFieldState = rememberTextFieldState(RinseChannel.RINSE_ALL.toString())
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        OutlinedTextField(
            value = textFieldState.text.toString(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(
                    type = MenuAnchorType.PrimaryNotEditable,
                    enabled = true,
                ),
            readOnly = true,
            onValueChange = { },
            label = { Text(text = "Channel") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            RinseChannel.entries.forEach { channel ->
                DropdownMenuItem(
                    text = { Text(text = channel.toString()) },
                    onClick = {
                        textFieldState.setTextAndPlaceCursorAtEnd(channel.toString())
                        onChannelSelected(channel)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun EpisodeList(
    modifier: Modifier = Modifier,
    episodes: List<Episode>,
    downloadFile: (Episode) -> Unit,
    cancelDownload: (Episode) -> Unit,
){
    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(episodes.size) { i ->
            EpisodeCard(
                episode = episodes[i],
                downloadFile = downloadFile,
                cancelDownload = cancelDownload,
            )
        }
    }
}

@Composable
fun EpisodeCard(
    episode: Episode,
    downloadFile: (Episode) -> Unit,
    cancelDownload: (Episode) -> Unit,
) {
    val logoRes = remember(episode.channel) {
        when (episode.channel) {
            RinseChannel.RINSE_ALL -> R.drawable.rinse
            RinseChannel.RINSE_UK -> R.drawable.rinse
            RinseChannel.RINSE_FRANCE -> R.drawable.rinse
            RinseChannel.KOOL -> R.drawable.kool
            RinseChannel.SWU -> R.drawable.swu
        }
    }
    DefaultCard(
        onClick = { downloadFile(episode) }
    ){
        DefaultRow {
            Image(
                painter = painterResource(id = logoRes),
                contentDescription = stringResource(R.string.logo_content_description)
            )
            StartAlignedColumn(
                modifier = Modifier.weight(1f),
            ) {
                DefaultTitleText(text = episode.title)
                episode.subtitle?.let {
                    DefaultSubtitleText(text = it)
                }
                Text(text = episode.channel.toString())
                DownloadState(episode = episode)

            }
            if (episode.downloadState is DownloadState.InProgress) {
                CancelButton(onClick = { cancelDownload(episode) })
            }
        }
    }
}

@Composable
fun DownloadState(episode: Episode) =
    when (val state = episode.downloadState) {
        is DownloadState.NotStarted -> {}
        is DownloadState.InProgress -> {
            ProgressBar(progress = state.progress)
            Text(text = state.toString())
        }
        else -> { Text(text = state.toString()) }
    }


@Composable
fun ProgressBar(
    modifier: Modifier = Modifier,
    progress: Float = 1f,
) {
    if (progress != -1f) {
        LinearProgressIndicator(
            progress = {
                progress
            },
            modifier = modifier,
        )
    } else {
        LinearProgressIndicator(
            modifier = modifier,
        )
    }
}

@Composable
fun CancelButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.width(30.dp),
    ) {
        Icon(imageVector = Icons.Filled.Close, contentDescription = stringResource(R.string.cancel_icon_content_description))
    }
}