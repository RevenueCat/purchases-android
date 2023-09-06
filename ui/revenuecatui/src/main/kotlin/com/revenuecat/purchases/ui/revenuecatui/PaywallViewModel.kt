package com.revenuecat.purchases.ui.revenuecatui

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.purchaseWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
        Purchases.sharedInstance.purchaseWith(
            purchaseParams = PurchaseParams.Builder(activity, packageToPurchase).build(),
            onError = { error, _ ->
                Log.e("PaywallTester", "Error purchasing package: $error")
            },
            onSuccess = { purchase, _ ->
                Log.i("PaywallTester", "Purchased package: $purchase")
            },
        )
    }

    private fun updateOffering() {
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error ->
                _state.value = PaywallViewState.Error(error.toString())
            },
            onSuccess = { offerings ->
                val currentOffering = offerings.current
                if (currentOffering == null) {
                    _state.value = PaywallViewState.Error("No offering or current offering")
                } else {
                    _state.value = PaywallViewState.Loaded(currentOffering)
                }
            },
        )
    }
}
