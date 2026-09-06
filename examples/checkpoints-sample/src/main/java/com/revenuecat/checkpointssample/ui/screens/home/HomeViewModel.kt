package com.revenuecat.checkpointssample.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.checkpointssample.Constants
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val activeEntitlements: List<String> = emptyList(),
        val message: String? = null,
        val gamesPlayed: Int = 0,
    ) {
        val gameUnlocked: Boolean
            get() = activeEntitlements.isNotEmpty()
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(loading = true, message = null) }
        viewModelScope.launch {
            val customerInfo = try {
                Purchases.sharedInstance.awaitCustomerInfo(CacheFetchPolicy.FETCH_CURRENT)
            } catch (e: PurchasesException) {
                _state.update { it.copy(loading = false, message = "${e.code}: ${e.message}") }
                return@launch
            }
            _state.update {
                it.copy(loading = false, activeEntitlements = customerInfo.entitlements.active.keys.sorted())
            }
        }
    }

    @OptIn(InternalRevenueCatAPI::class)
    fun play(onAccessGranted: () -> Unit) {
        val current = _state.value
        if (current.loading) return
        if (current.gameUnlocked) {
            startGame(onAccessGranted)
            return
        }
        Purchases.sharedInstance.checkpoint(
            Constants.PLAY_GAME_CHECKPOINT_ID,
            CheckpointParams { customVariables { "games_played" to current.gamesPlayed } },
        ) { result ->
            // Only called once the gate lets the user through, so the game starts whatever was served (a null
            // result means nothing was). The grants are merged in so the card reflects what the user obtained.
            val granted = result?.entitlements.orEmpty().map { it.identifier }
            _state.update {
                it.copy(activeEntitlements = (it.activeEntitlements + granted).distinct().sorted(), message = null)
            }
            startGame(onAccessGranted)
        }
    }

    private fun startGame(onAccessGranted: () -> Unit) {
        _state.update { it.copy(gamesPlayed = it.gamesPlayed + 1) }
        onAccessGranted()
    }
}
