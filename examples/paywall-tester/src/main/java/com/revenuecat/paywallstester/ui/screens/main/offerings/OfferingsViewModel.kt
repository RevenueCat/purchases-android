package com.revenuecat.paywallstester.ui.screens.main.offerings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.awaitSyncAttributesAndOfferingsIfNeeded
import com.revenuecat.purchases.getOfferingsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class OfferingsViewModel : ViewModel() {

    abstract val offeringsState: StateFlow<OfferingsState>

    abstract fun refreshOfferings()

    abstract fun updateSearchQuery(query: String)

    abstract fun markOfferingAsRecent(offeringId: String)
}

class OfferingsViewModelImpl : OfferingsViewModel() {

    override val offeringsState: StateFlow<OfferingsState>
        get() = _offeringsState.asStateFlow()
    private val _offeringsState = MutableStateFlow<OfferingsState>(OfferingsState.Loading)
    private var loadFlowsJob: Job? = null

    init {
        updateOfferings()
    }

    override fun refreshOfferings() {
        val previousState = _offeringsState.value as? OfferingsState.Loaded
        _offeringsState.update { OfferingsState.Loading }
        viewModelScope.launch {
            val offerings = Purchases.sharedInstance.awaitSyncAttributesAndOfferingsIfNeeded()
            val flows = loadFlows(offerings)
            _offeringsState.update {
                OfferingsState.Loaded(
                    offerings,
                    searchQuery = previousState?.searchQuery ?: "",
                    recentOfferingIds = previousState?.recentOfferingIds ?: emptyList(),
                    flows = flows,
                )
            }
        }
    }

    override fun updateSearchQuery(query: String) {
        val currentState = _offeringsState.value
        if (currentState is OfferingsState.Loaded) {
            _offeringsState.update { currentState.copy(searchQuery = query) }
        }
    }

    override fun markOfferingAsRecent(offeringId: String) {
        val currentState = _offeringsState.value
        if (currentState is OfferingsState.Loaded) {
            val updated = listOf(offeringId) + currentState.recentOfferingIds.filter { it != offeringId }
            _offeringsState.update { currentState.copy(recentOfferingIds = updated.take(MAX_RECENTS)) }
        }
    }

    companion object {
        private const val MAX_RECENTS = 5
    }

    private fun updateOfferings() {
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error ->
                Log.e("PaywallsTester", "Error fetching offerings: $error")
                _offeringsState.update { OfferingsState.Error(error) }
            },
            onSuccess = { offerings ->
                _offeringsState.update { OfferingsState.Loaded(offerings) }
                loadFlowsJob?.cancel()
                loadFlowsJob = viewModelScope.launch {
                    val flows = loadFlows(offerings)
                    _offeringsState.update { (it as? OfferingsState.Loaded)?.copy(flows = flows) ?: it }
                }
            },
        )
    }

    private suspend fun loadFlows(offerings: Offerings): List<FlowRow> =
        runCatching { FlowRow.loadAll(offerings) }
            .onFailure { if (it is CancellationException) throw it }
            .onFailure { Log.e("PaywallsTester", "Error loading flows: $it") }
            .getOrDefault(emptyList())
}
