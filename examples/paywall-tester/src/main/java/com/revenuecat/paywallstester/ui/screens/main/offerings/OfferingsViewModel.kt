package com.revenuecat.paywallstester.ui.screens.main.offerings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.awaitSyncAttributesAndOfferingsIfNeeded
import com.revenuecat.purchases.getOfferingsWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class OfferingsViewModel : ViewModel() {

    abstract val offeringsState: StateFlow<OfferingsState>

    abstract fun refreshOfferings()

    abstract fun updateSearchQuery(query: String)
}

class OfferingsViewModelImpl : OfferingsViewModel() {

    override val offeringsState: StateFlow<OfferingsState>
        get() = _offeringsState.asStateFlow()
    private val _offeringsState = MutableStateFlow<OfferingsState>(OfferingsState.Loading)

    init {
        updateOfferings()
    }

    override fun refreshOfferings() {
        _offeringsState.update { OfferingsState.Loading }
        viewModelScope.launch {
            val offerings = Purchases.sharedInstance.awaitSyncAttributesAndOfferingsIfNeeded()
            val currentQuery = (_offeringsState.value as? OfferingsState.Loaded)?.searchQuery ?: ""
            _offeringsState.update { OfferingsState.Loaded(offerings, currentQuery) }
        }
    }

    override fun updateSearchQuery(query: String) {
        val currentState = _offeringsState.value
        if (currentState is OfferingsState.Loaded) {
            _offeringsState.update { currentState.copy(searchQuery = query) }
        }
    }

    private fun updateOfferings() {
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error ->
                Log.e("PaywallsTester", "Error fetching offerings: $error")
                _offeringsState.update { OfferingsState.Error(error) }
            },
            onSuccess = { offerings ->
                _offeringsState.update { OfferingsState.Loaded(offerings) }
            },
        )
    }
}
