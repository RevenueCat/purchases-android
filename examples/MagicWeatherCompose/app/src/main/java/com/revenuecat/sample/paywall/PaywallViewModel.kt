package com.revenuecat.sample.paywall

import android.app.Activity
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.sample.R
import com.revenuecat.sample.data.paywallDrawableByName
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
                    val imageResource = paywallDrawableByName[
                        currentOffering.getMetadataString("imageName", "rainy_cat"),
                    ] ?: R.drawable.rainy_cat
                    _uiState.update {
                        PaywallState.Success(
                            currentOffering,
                            currentOffering.getMetadataString("title", "RevenueCat sample paywall"),
                            currentOffering.getMetadataString("subtitle", "Upgrade to premium and change the weather!"),
                            imageResource,
                        )
                    }
                } ?: run {
                    _uiState.update { PaywallState.Error("No current offering") }
                }
            },
        )
    }

    @Suppress("LongParameterList")
    fun purchasePackage(
        activity: Activity,
        packageToPurchase: Package,
        onPurchaseStarted: ((Package) -> Unit)? = null,
        onPurchaseCompleted: ((CustomerInfo) -> Unit)? = null,
        onPurchaseCancelled: (() -> Unit)? = null,
        onPurchaseErrored: ((PurchasesError) -> Unit)? = null,
    ) {
        onPurchaseStarted?.invoke(packageToPurchase)
        Purchases.sharedInstance.purchaseWith(
            PurchaseParams.Builder(activity, packageToPurchase).build(),
            onError = { error, userCancelled ->
                if (userCancelled) {
                    Toast.makeText(activity, "User cancelled", Toast.LENGTH_SHORT).show()
                    onPurchaseCancelled?.invoke()
                } else {
                    _uiState.update { PaywallState.Error(error.message) }
                    onPurchaseErrored?.invoke(error)
                }
            },
            onSuccess = { _, customerInfo ->
                Toast.makeText(
                    activity,
                    "Purchase succeeded. Current entitlements: ${customerInfo.entitlements.active.keys}",
                    Toast.LENGTH_SHORT,
                ).show()
                onPurchaseCompleted?.invoke(customerInfo)
            },
        )
    }
}
