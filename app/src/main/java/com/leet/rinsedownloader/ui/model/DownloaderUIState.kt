package com.leet.rinsedownloader.ui.model

import com.leet.rinsedownloader.data.RinseChannel
import com.leet.rinsedownloader.data.ScheduleData

data class DownloaderUIState(
    val selectedDates: String = "",
    val isShowingDatePicker: Boolean = false,
    val scheduleData: ScheduleData = ScheduleData(),
    val scheduleFilter: RinseChannel = RinseChannel.RINSE_ALL,
    val isLoadingSchedules: Boolean = false,
    val snackbarMessage: String? = null,
)
