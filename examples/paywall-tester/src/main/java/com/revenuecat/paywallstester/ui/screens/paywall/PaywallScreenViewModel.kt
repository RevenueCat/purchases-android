package com.revenuecat.paywallstester.ui.screens.paywall

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface PaywallScreenViewModel : PaywallViewListener {
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

    override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
        val value = _state.value
        if (value is PaywallScreenState.Loaded) {
            _state.update {
                value.copy(
                    displayCompletedPurchaseMessage = true,
                    displayErrorPurchasingMessage = false,
                )
            }
        }
    }

    override fun onPurchaseError(error: PurchasesError) {
        val value = _state.value
        if (value is PaywallScreenState.Loaded) {
            _state.update {
                value.copy(
                    displayCompletedPurchaseMessage = false,
                    displayErrorPurchasingMessage = true,
                )
            }
        }
    }

    override fun onDismissed() {
        val value = _state.value
        if (value is PaywallScreenState.Loaded) {
            _state.update {
                value.copy(
                    displayCompletedPurchaseMessage = false,
                    displayErrorPurchasingMessage = false,
                )
            }
        }
    }

    private fun updateOffering() {
        viewModelScope.launch {
            try {
                val offerings = Purchases.sharedInstance.awaitOfferings()
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
            } catch (e: PurchasesException) {
                _state.update { PaywallScreenState.Error(e.toString()) }
            }
        }
    }
}
