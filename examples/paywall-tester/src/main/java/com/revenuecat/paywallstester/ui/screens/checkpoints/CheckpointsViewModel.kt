package com.revenuecat.paywallstester.ui.screens.checkpoints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.revenuecat.paywallstester.data.RecentCheckpointsStore
import com.revenuecat.paywallstester.ui.screens.checkpoints.CheckpointsViewModel.CheckpointResultUi
import com.revenuecat.paywallstester.ui.screens.checkpoints.CheckpointsViewModel.UiState
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.FlowResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface CheckpointsViewModel {
    data class CheckpointResultUi(
        val title: String,
        val detail: String,
        val isError: Boolean,
        val raw: String,
    )

    data class UiState(
        val recents: List<String> = emptyList(),
        val waitingFor: String? = null,
        val lastResult: CheckpointResultUi? = null,
    )

    val state: StateFlow<UiState>

    fun hit(identifier: String)
}

internal class CheckpointsViewModelImpl(
    private val recentCheckpointsStore: RecentCheckpointsStore,
) : ViewModel(), CheckpointsViewModel {

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val context = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                CheckpointsViewModelImpl(recentCheckpointsStore = RecentCheckpointsStore(context))
            }
        }
    }

    override val state: StateFlow<UiState>
        get() = _state.asStateFlow()

    private val _state = MutableStateFlow(UiState(recents = recentCheckpointsStore.recents()))

    @OptIn(InternalRevenueCatAPI::class)
    override fun hit(identifier: String) {
        val checkpointIdentifier = identifier.trim()
        if (checkpointIdentifier.isEmpty() || _state.value.waitingFor != null) return
        val updatedRecents = recentCheckpointsStore.recordUse(checkpointIdentifier)
        _state.update { it.copy(recents = updatedRecents, waitingFor = checkpointIdentifier) }
        Purchases.sharedInstance.checkpoint(
            checkpointIdentifier,
            CheckpointParams { customVariables { "source" to "paywall-tester" } },
        ) { result ->
            _state.update { it.copy(waitingFor = null, lastResult = result.toUi()) }
        }
    }

    // Why nothing was presented, and any failure, are in the SDK logs.
    @OptIn(InternalRevenueCatAPI::class)
    private fun FlowResult?.toUi(): CheckpointResultUi = if (this == null) {
        CheckpointResultUi(
            title = "Nothing presented",
            detail = "See the logs for the reason.",
            isError = false,
            raw = "null",
        )
    } else {
        CheckpointResultUi(
            title = "Flow presented",
            detail = if (obtainedEntitlements.isNotEmpty()) {
                "Obtained ${obtainedEntitlements.joinToString { it.entitlementInfo.identifier }}"
            } else {
                "Nothing obtained"
            },
            isError = false,
            raw = toString(),
        )
    }
}
