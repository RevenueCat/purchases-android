package com.revenuecat.purchases.ui.revenuecatui

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal interface PaywallViewModel {
    val state: StateFlow<PaywallViewState>

    fun purchasePackage(activity: Activity, packageToPurchase: Package)
}

internal class PaywallViewModelImpl(offering: Offering?) : ViewModel(), PaywallViewModel {
    override val state: StateFlow<PaywallViewState>
        get() = _state.asStateFlow()
    private val initialState: PaywallViewState = offering?.let {
        PaywallViewState.Loaded(it)
    } ?: PaywallViewState.Loading
    private val _state = MutableStateFlow(initialState)

    init {
        if (offering == null) {
            updateOffering()
        }
    }

    override fun purchasePackage(activity: Activity, packageToPurchase: Package) {
        viewModelScope.launch {
            try {
                val purchaseResult = Purchases.sharedInstance.awaitPurchase(
                    PurchaseParams.Builder(activity, packageToPurchase).build(),
                )
                Log.i("PaywallTester", "Purchased package: ${purchaseResult.storeTransaction}")
            } catch (e: PurchasesException) {
                Log.e("PaywallTester", "Error purchasing package: $e")
            }
        }
    }

    private fun updateOffering() {
        viewModelScope.launch {
            try {
                val offerings = Purchases.sharedInstance.awaitOfferings()
                val currentOffering = offerings.current
                if (currentOffering == null) {
                    _state.value = PaywallViewState.Error("No offering or current offering")
                } else {
                    _state.value = PaywallViewState.Loaded(currentOffering)
                }
            } catch (e: PurchasesException) {
                _state.value = PaywallViewState.Error(e.toString())
            }
        }
    }
}
