package com.revenuecat.checkpointtester.ui.screens.usecases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.checkpointtester.checkpoints.CheckpointResultUi
import com.revenuecat.checkpointtester.checkpoints.CheckpointRunner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UseCasesViewModel : ViewModel() {

    data class UiState(
        val waitingFor: String? = null,
        val lastResult: CheckpointResultUi? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun hit(identifier: String) {
        if (_state.value.waitingFor != null) return
        _state.update { it.copy(waitingFor = identifier, lastResult = null) }
        viewModelScope.launch {
            val result = CheckpointRunner.run(identifier, "use_case" to "inline")
            _state.update { it.copy(waitingFor = null, lastResult = result) }
        }
    }
}
