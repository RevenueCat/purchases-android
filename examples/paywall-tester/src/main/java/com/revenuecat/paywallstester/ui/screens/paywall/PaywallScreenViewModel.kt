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
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
interface PaywallScreenViewModel : PaywallListener {
    companion object {
        const val OFFERING_ID_KEY = "offering_id"
        const val FOOTER_CONDENSED_KEY = "footer_condensed"
    }
    val state: StateFlow<PaywallScreenState>

    fun onDialogDismissed()
}

class PaywallScreenViewModelImpl(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application), PaywallScreenViewModel {

    override val state: StateFlow<PaywallScreenState>
        get() = _state.asStateFlow()
    private val _state: MutableStateFlow<PaywallScreenState> = MutableStateFlow(PaywallScreenState.Loading)

    private val offeringId = savedStateHandle.get<String?>(PaywallScreenViewModel.OFFERING_ID_KEY)
    private val footerCondensed = savedStateHandle.get<Boolean?>(PaywallScreenViewModel.FOOTER_CONDENSED_KEY)

    init {
        updateOffering()
    }

    override fun onRestoreStarted() {
        val value = _state.value
        if (value is PaywallScreenState.Loaded) {
            _state.update {
                value.copy(
                    dialogText = "Restoring purchases...",
                )
            }
        }
    }

    override fun onRestoreCompleted(customerInfo: CustomerInfo) {
        val value = _state.value
        if (value is PaywallScreenState.Loaded) {
            _state.update {
                value.copy(
                    dialogText = "Restore completed",
                )
            }
        }
    }

    override fun onRestoreError(error: PurchasesError) = Unit

    override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
        val value = _state.value
        if (value is PaywallScreenState.Loaded) {
            _state.update {
                value.copy(
                    dialogText = "Purchase was successful",
                )
            }
        }
    }

    override fun onPurchaseError(error: PurchasesError) = Unit

    override fun onDialogDismissed() {
        val value = _state.value
        if (value is PaywallScreenState.Loaded) {
            _state.update {
                value.copy(
                    dialogText = null,
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
                        PaywallScreenState.Loaded(offeringToLoad, footerCondensed = footerCondensed ?: false)
                    }
                }
            } catch (e: PurchasesException) {
                _state.update { PaywallScreenState.Error(e.toString()) }
            }
        }
    }
}
