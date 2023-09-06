package com.revenuecat.paywallstester.ui.screens.paywall

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getOfferingsWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface PaywallScreenViewModel {
    companion object {
        const val OFFERING_ID_KEY = "offering_id"
    }
    val state: StateFlow<PaywallScreenState>
}

class PaywallScreenViewModelImpl(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application), PaywallScreenViewModel {

    override val state: StateFlow<PaywallScreenState>
        get() = _state.asStateFlow()
    private val _state: MutableStateFlow<PaywallScreenState> = MutableStateFlow(PaywallScreenState.Loading)

    private val offeringId = savedStateHandle.get<String?>(PaywallScreenViewModel.OFFERING_ID_KEY)

    init {
        updateOffering()
    }

    private fun updateOffering() {
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error ->
                _state.update { PaywallScreenState.Error(error.toString()) }
            },
            onSuccess = { offerings ->
                val offeringToLoad = offeringId?.let {
                    offerings.all[it]
                } ?: offerings.current
                if (offeringToLoad == null) {
                    _state.update {
                        PaywallScreenState.Error("Could not find offering or current offering")
                    }
                } else {
                    _state.update {
                        PaywallScreenState.Loaded(offeringToLoad)
                    }
                }
            },
        )
    }
}
