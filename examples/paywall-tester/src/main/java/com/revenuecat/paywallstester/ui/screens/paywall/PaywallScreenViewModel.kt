package com.revenuecat.paywallstester.ui.screens.paywall

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.AtomicReference
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicResult
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

    fun performMyAppLogicPurchase(activity: Activity, rcPackage: Package, resume: (PurchaseLogicResult) -> Unit)
    fun onDialogDismissed()
}

class PaywallScreenViewModelImpl(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application), PaywallScreenViewModel, PurchasesUpdatedListener {

    val billingClient: BillingClient

    override val state: StateFlow<PaywallScreenState>
        get() = _state.asStateFlow()
    private val _state: MutableStateFlow<PaywallScreenState> = MutableStateFlow(PaywallScreenState.Loading)

    private val offeringId = savedStateHandle.get<String?>(PaywallScreenViewModel.OFFERING_ID_KEY)
    private val footerCondensed = savedStateHandle.get<Boolean?>(PaywallScreenViewModel.FOOTER_CONDENSED_KEY)
    private val placementId = savedStateHandle.get<String?>(PaywallScreenViewModel.PLACEMENT_ID_KEY)

    init {
        updateOffering()
        billingClient = BillingClient.newBuilder(application.applicationContext)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().enablePrepaidPlans().build())
            .setListener(this)
            .build()
        billingClient.startConnection(object: BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                Log.e("PaywallScreenViewModel", "Billing service disconnected")
                // TODO("Not yet implemented")
            }

            override fun onBillingSetupFinished(p0: BillingResult) {
                Log.i("PaywallScreenViewModel", "Billing service connected")
                // TODO("Not yet implemented")
            }
        })
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

    private var purchaseInProgress: AtomicReference<((PurchaseLogicResult) -> Unit)?> = AtomicReference(null)

    override fun performMyAppLogicPurchase(activity: Activity, rcPackage: Package, resume: (PurchaseLogicResult) -> Unit) {
        if (purchaseInProgress.get() != null) {
            // A purchase is already in progress
            resume(PurchaseLogicResult.Error(PurchasesError(PurchasesErrorCode.OperationAlreadyInProgressError, "Another purchase is already in progress.")))
            return
        }
        purchaseInProgress.set(resume)
        val productDetailsToPurchase = (rcPackage.product as GoogleStoreProduct).productDetails
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setOfferToken(productDetailsToPurchase.subscriptionOfferDetails?.firstOrNull()?.offerToken!!)
                        .setProductDetails(productDetailsToPurchase)
                        .build(),
                ),
            )
            .build()
        billingClient.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(result: BillingResult, p1: List<Purchase?>?) {
        Log.d("PaywallScreenViewModel", "onPurchasesUpdated: ${result.responseCode}")
        if (result.responseCode == BillingClient.BillingResponseCode.OK && p1 != null) {
            purchaseInProgress.get()?.let {
                it(PurchaseLogicResult.Success)
                purchaseInProgress.set(null)
            }
        } else {
            // Handle error cases
            purchaseInProgress.get()?.let {
                val error = if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                    PurchaseLogicResult.Cancellation
                } else {
                    PurchaseLogicResult.Error(PurchasesError(PurchasesErrorCode.UnknownError, "Purchase failed with response code: ${result.responseCode}"))
                }
                it(error)
                purchaseInProgress.set(null)
            }
        }
    }

    private fun updateOffering() {
        viewModelScope.launch {
            placementId?.let {
                try {
                    val offerings = Purchases.sharedInstance.awaitOfferings()
                    val offeringToLoad = offerings.getCurrentOfferingForPlacement(it)

                    if (offeringToLoad == null) {
                        _state.update {
                            PaywallScreenState.Error("Could not find offering for placement $it")
                        }
                    } else {
                        _state.update {
                            PaywallScreenState.Loaded(offeringToLoad, footerCondensed = footerCondensed ?: false)
                        }
                    }
                } catch (e: PurchasesException) {
                    _state.update { PaywallScreenState.Error(e.toString()) }
                }
            } ?: run {
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
}
