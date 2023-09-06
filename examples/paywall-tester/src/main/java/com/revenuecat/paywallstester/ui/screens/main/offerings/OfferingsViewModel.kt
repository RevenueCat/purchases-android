package com.revenuecat.paywallstester.ui.screens.main.offerings

import android.util.Log
import androidx.lifecycle.ViewModel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getOfferingsWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

abstract class OfferingsViewModel : ViewModel() {

    abstract val offeringsState: StateFlow<OfferingsState>
}

class OfferingsViewModelImpl : OfferingsViewModel() {

    override val offeringsState: StateFlow<OfferingsState>
        get() = _offeringsState.asStateFlow()
    private val _offeringsState = MutableStateFlow<OfferingsState>(OfferingsState.Loading)

    init {
        updateOfferings()
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
