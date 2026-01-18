package com.revenuecat.purchasetester.ui.screens.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.revenuecat.purchasetester.MainApplication
import com.revenuecat.purchasetester.TesterLogHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

interface LogsViewModel {
    val state: StateFlow<LogsScreenState>
    fun loadLogs()
}

class LogsViewModelImpl(
    private val logHandler: TesterLogHandler
) : ViewModel(), LogsViewModel {

    companion object {
        private const val SUBSCRIPTION_TIMEOUT_MILLIS = 5000L

        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as MainApplication
                LogsViewModelImpl(application.logHandler)
            }
        }
    }

    private val _state = MutableStateFlow<LogsScreenState>(LogsScreenState.Loading)

    override val state: StateFlow<LogsScreenState> = _state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
        initialValue = LogsScreenState.Loading
    )

    override fun loadLogs() {
        _state.value = LogsScreenState.LogsData(
            logs = logHandler.storedLogs
        )
    }
}
