@file:OptIn(InternalRevenueCatAPI::class)

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
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.PaywallPresenter
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
        val presentWithAppPaywall: Boolean = false,
    )

    val state: StateFlow<UiState>

    val paywallRequest: StateFlow<AppPaywallPresenter.Request?>

    fun hit(identifier: String)

    fun setPresentWithAppPaywall(enabled: Boolean)
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

    private val appPaywallPresenter = AppPaywallPresenter(onFinished = ::onAppPaywallFinished)

    override val paywallRequest: StateFlow<AppPaywallPresenter.Request?>
        get() = appPaywallPresenter.request

    // Never blocks on the previous callback: the SDK skips it when the user backs out of a paywall or when another
    // checkpoint flow is already on screen, so waiting for it would leave the screen stuck.
    override fun hit(identifier: String) {
        val checkpointIdentifier = identifier.trim()
        if (checkpointIdentifier.isEmpty()) return
        val updatedRecents = recentCheckpointsStore.recordUse(checkpointIdentifier)
        _state.update { it.copy(recents = updatedRecents, waitingFor = checkpointIdentifier) }
        val params = CheckpointParams {
            customVariables { "source" to "paywall-tester" }
            if (_state.value.presentWithAppPaywall) paywallPresenter(appPaywallPresenter)
        }
        Purchases.sharedInstance.checkpoint(checkpointIdentifier, params) { result ->
            _state.update { it.copy(waitingFor = null, lastResult = result.toUi()) }
        }
    }

    override fun setPresentWithAppPaywall(enabled: Boolean) {
        _state.update { it.copy(presentWithAppPaywall = enabled) }
    }

    private fun onAppPaywallFinished(result: PaywallPresenter.Completion.Result) {
        if (result != PaywallPresenter.Completion.Result.NavigatedBack) return
        _state.update {
            it.copy(
                waitingFor = null,
                lastResult = CheckpointResultUi(
                    title = "Backed out",
                    detail = "The user navigated back, so the checkpoint callback is not invoked.",
                    isError = false,
                    raw = "NavigatedBack",
                ),
            )
        }
    }

    // Why nothing was presented, and any failure, are in the SDK logs.
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
