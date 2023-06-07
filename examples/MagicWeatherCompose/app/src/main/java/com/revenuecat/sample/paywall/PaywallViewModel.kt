package com.revenuecat.sample.paywall

import android.app.Activity
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.purchaseWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PaywallViewModel : ViewModel() {

    private val _uiState: MutableStateFlow<PaywallState> = MutableStateFlow(PaywallState.Loading)
    val uiState: StateFlow<PaywallState> = _uiState.asStateFlow()

    init {
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error ->
                _uiState.update { PaywallState.Error(error.message) }
            },
            onSuccess = { offerings ->
                offerings.current?.let { currentOffering ->
                    _uiState.update { PaywallState.Success(currentOffering) }
                } ?: run {
                    _uiState.update { PaywallState.Error("No current offering") }
                }
            },
        )
    }

    fun purchasePackage(activity: Activity, packageToPurchase: Package) {
        Purchases.sharedInstance.purchaseWith(
            PurchaseParams.Builder(activity, packageToPurchase).build(),
            onError = { error, userCancelled ->
                if (userCancelled) {
                    Toast.makeText(activity, "User cancelled", Toast.LENGTH_SHORT).show()
                } else {
                    _uiState.update { PaywallState.Error(error.message) }
                }
            },
            onSuccess = { _, consumerInfo ->
                Toast.makeText(
                    activity,
                    "Purchase succeeded. Current entitlements: ${consumerInfo.entitlements.active.keys}",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
    }
}
