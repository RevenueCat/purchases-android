package com.revenuecat.paywallstester.ui.screens.main.offerings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.paywallstester.MainApplication
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitSyncAttributesAndOfferingsIfNeeded
import com.revenuecat.purchases.awaitSyncPurchases
import com.revenuecat.purchases.getOfferingsWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class OfferingsViewModel : ViewModel() {

    abstract val offeringsState: StateFlow<OfferingsState>

    abstract fun refreshOfferings()
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
            _offeringsState.update { OfferingsState.Loaded(offerings) }
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
