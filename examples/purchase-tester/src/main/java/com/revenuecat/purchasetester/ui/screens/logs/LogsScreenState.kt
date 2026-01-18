package com.revenuecat.purchasetester.ui.screens.logs

import com.revenuecat.purchasetester.LogMessage

sealed class LogsScreenState {

    object Loading : LogsScreenState()

    data class LogsData(
        val logs: List<LogMessage> = emptyList()
    ) : LogsScreenState()

}
