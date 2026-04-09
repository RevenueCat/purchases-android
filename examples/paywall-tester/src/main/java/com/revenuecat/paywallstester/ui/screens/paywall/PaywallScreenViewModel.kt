package com.revenuecat.paywallstester.ui.screens.paywall

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitSyncAttributesAndOfferingsIfNeeded
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.utils.Resumable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface PaywallScreenViewModel : PaywallListener {
    companion object {
        const val OFFERING_ID_KEY = "offering_id"
        const val FOOTER_CONDENSED_KEY = "footer_condensed"
        const val PLACEMENT_ID_KEY = "placement_id"
    }
    val state: StateFlow<PaywallScreenState>
    val isRefreshing: StateFlow<Boolean>

    fun onDialogDismissed()
    fun refreshOffering()
}

class PaywallScreenViewModelImpl(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application), PaywallScreenViewModel {

    override val state: StateFlow<PaywallScreenState>
        get() = _state.asStateFlow()
    private val _state: MutableStateFlow<PaywallScreenState> = MutableStateFlow(PaywallScreenState.Loading)

    override val isRefreshing: StateFlow<Boolean>
        get() = _isRefreshing.asStateFlow()
    private val _isRefreshing = MutableStateFlow(false)

    private val offeringId = savedStateHandle.get<String?>(PaywallScreenViewModel.OFFERING_ID_KEY)
    private val footerCondensed = savedStateHandle.get<Boolean?>(PaywallScreenViewModel.FOOTER_CONDENSED_KEY)
    private val placementId = savedStateHandle.get<String?>(PaywallScreenViewModel.PLACEMENT_ID_KEY)

    private var refreshCounter = 0

    init {
        updateOffering()
    }

    @Suppress("MagicNumber")
    override fun onPurchasePackageInitiated(rcPackage: Package, resume: Resumable) {
        resume()
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

    override fun refreshOffering() {
        refreshCounter++
        _isRefreshing.value = true
        updateOffering(refreshCounter)
    }

    private fun updateOffering(refreshCount: Int = 0) {
        viewModelScope.launch {
            try {
                val offeringToLoad = fetchOffering(forceRefresh = refreshCount > 0)
                _state.update {
                    PaywallScreenState.Loaded(
                        offeringToLoad,
                        footerCondensed = footerCondensed ?: false,
                        refreshCount = refreshCount,
                    )
                }
            } catch (e: PurchasesException) {
                _state.update { PaywallScreenState.Error(e.toString()) }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun fetchOffering(forceRefresh: Boolean): Offering {
        val offerings = if (forceRefresh) {
            Purchases.sharedInstance.awaitSyncAttributesAndOfferingsIfNeeded()
        } else {
            Purchases.sharedInstance.awaitOfferings()
        }
        return placementId?.let { offerings.getCurrentOfferingForPlacement(it) }
            ?: offeringId?.let { offerings.all[it] }
            ?: offerings.current
            ?: error("Could not find offering")
    }
}
