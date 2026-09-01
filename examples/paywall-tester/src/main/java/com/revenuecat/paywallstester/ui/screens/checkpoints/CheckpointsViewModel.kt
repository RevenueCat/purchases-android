package com.revenuecat.paywallstester.ui.screens.checkpoints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.revenuecat.paywallstester.data.RecentCheckpointsStore
import com.revenuecat.paywallstester.ui.screens.checkpoints.CheckpointsViewModel.CheckpointResultUi
import com.revenuecat.paywallstester.ui.screens.checkpoints.CheckpointsViewModel.UiState
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointPaywallOutcome
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.awaitCheckpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
        viewModelScope.launch {
            val resultUi = try {
                Purchases.sharedInstance.awaitCheckpoint(
                    checkpointIdentifier,
                    CheckpointParams { customVariables { "source" to "paywall-tester" } },
                ).toUi()
            } catch (e: PurchasesException) {
                CheckpointResultUi(
                    title = "Error",
                    detail = "${e.code}: ${e.message}",
                    isError = true,
                    raw = e.error.toString(),
                )
            }
            _state.update { it.copy(waitingFor = null, lastResult = resultUi) }
        }
    }

    @OptIn(InternalRevenueCatAPI::class)
    private fun CheckpointResult.toUi(): CheckpointResultUi = when (this) {
        is CheckpointResult.PaywallPresented -> CheckpointResultUi(
            title = "Paywall presented",
            detail = paywallOutcome.describe(),
            isError = false,
            raw = toString(),
        )
        is CheckpointResult.NoAction -> CheckpointResultUi(
            title = "No action",
            detail = "Reason: $reason",
            isError = false,
            raw = toString(),
        )
        else -> CheckpointResultUi(
            title = "Unknown result",
            detail = "",
            isError = false,
            raw = toString(),
        )
    }

    @OptIn(InternalRevenueCatAPI::class)
    private fun CheckpointPaywallOutcome.describe(): String = when (this) {
        is CheckpointPaywallOutcome.Purchased -> "Purchased"
        is CheckpointPaywallOutcome.Restored -> "Restored"
        is CheckpointPaywallOutcome.Error -> "Paywall error: ${error.message}"
        CheckpointPaywallOutcome.Dismissed -> "Dismissed"
        CheckpointPaywallOutcome.WebCheckoutOpened -> "Web checkout opened"
        else -> toString()
    }
}
