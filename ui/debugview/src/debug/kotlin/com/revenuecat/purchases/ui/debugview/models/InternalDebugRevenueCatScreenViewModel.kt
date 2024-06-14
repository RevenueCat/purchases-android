package com.revenuecat.purchases.ui.debugview.models

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.ui.debugview.DebugRevenueCatViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class InternalDebugRevenueCatScreenViewModel(
    private val onPurchaseCompleted: (StoreTransaction) -> Unit,
    private val onPurchaseErrored: (PurchasesTransactionException) -> Unit,
) : ViewModel(), DebugRevenueCatViewModel {
    override val state: StateFlow<SettingScreenState>
        get() = _state

    private var _state: MutableStateFlow<SettingScreenState> = MutableStateFlow(
        SettingScreenState.NotConfigured(configurationGroup()),
    )

    init {
        if (Purchases.isConfigured) {
            refreshInfo()
        }
    }

    override fun toastDisplayed() {
        _state.update { currentState ->
            when (currentState) {
                is SettingScreenState.Configured -> { currentState.copy(toastMessage = null) }
                is SettingScreenState.NotConfigured -> { currentState.copy(toastMessage = null) }
            }
        }
    }

    override fun purchasePackage(activity: Activity, rcPackage: Package) {
        viewModelScope.launch {
            purchaseWithParams(PurchaseParams.Builder(activity, rcPackage).build())
        }
    }

    override fun purchaseProduct(activity: Activity, storeProduct: StoreProduct) {
        viewModelScope.launch {
            purchaseWithParams(PurchaseParams.Builder(activity, storeProduct).build())
        }
    }

    override fun purchaseSubscriptionOption(activity: Activity, subscriptionOption: SubscriptionOption) {
        viewModelScope.launch {
            purchaseWithParams(PurchaseParams.Builder(activity, subscriptionOption).build())
        }
    }

    private suspend fun purchaseWithParams(purchaseParams: PurchaseParams) {
        try {
            val purchaseResult = Purchases.sharedInstance.awaitPurchase(purchaseParams)
            _state.update { currentState ->
                if (currentState is SettingScreenState.Configured) {
                    currentState.copy(
                        toastMessage = "Purchase completed successfully",
                    )
                } else {
                    Log.e("RevenueCatDebugView", "Invalid state. Purchase completed but SDK is not configured.")
                    currentState
                }
            }
            onPurchaseCompleted(purchaseResult.storeTransaction)
        } catch (e: PurchasesTransactionException) {
            _state.update { currentState ->
                if (currentState is SettingScreenState.Configured) {
                    currentState.copy(
                        toastMessage = "Purchase error: ${e.message}",
                    )
                } else {
                    Log.e("RevenueCatDebugView", "Invalid state. Purchase error but SDK is not configured.")
                    currentState
                }
            }
            onPurchaseErrored(e)
        }
    }

    private fun refreshInfo() {
        viewModelScope.launch {
            try {
                val offerings = Purchases.sharedInstance.awaitOfferings()
                val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
                _state.update {
                    SettingScreenState.Configured(
                        configurationGroup(),
                        customerInfoGroup(customerInfo),
                        offeringsGroup(offerings),
                    )
                }
            } catch (e: PurchasesException) {
                Log.e("RevenueCatDebugView", "Error getting RevenueCat SDK info for debug view. Exception: $e")
            }
        }
    }

    private fun configurationGroup(): SettingGroupState {
        val storeName = if (Purchases.isConfigured) {
            Purchases.sharedInstance.store.name
        } else {
            "Not configured"
        }
        val purchasesAreCompletedBy = if (Purchases.isConfigured) {
            Purchases.sharedInstance.purchasesAreCompletedBy.name
        } else {
            "Not configured"
        }
        return SettingGroupState(
            "Configuration",
            listOf(
                SettingState.Text("SDK version", Purchases.frameworkVersion),
                SettingState.Text("Is configured", "${Purchases.isConfigured}"),
                SettingState.Text("Store", storeName),
                SettingState.Text("Purchases are completed by", purchasesAreCompletedBy),
            ),
        )
    }

    private fun customerInfoGroup(customerInfo: CustomerInfo): SettingGroupState {
        return SettingGroupState(
            title = "Customer info",
            settings = listOf(
                SettingState.Text("Current User ID", Purchases.sharedInstance.appUserID),
                SettingState.Text("Original User ID", customerInfo.originalAppUserId),
                SettingState.Text(
                    "Active entitlements",
                    customerInfo.entitlements.active
                        .map { "${it.key} until ${it.value.expirationDate}" }
                        .joinToString("\n")
                        .takeIf { it.isNotEmpty() } ?: "None",
                ),
                SettingState.Text("Verification result", customerInfo.entitlements.verification.name),
                SettingState.Text("Request date", customerInfo.requestDate.toString()),
            ),
        )
    }

    private fun offeringsGroup(offerings: Offerings): SettingGroupState {
        return SettingGroupState(
            title = "Offerings",
            settings = offerings.all.values.map { offering ->
                SettingState.OfferingSetting(offering)
            },
        )
    }
}
