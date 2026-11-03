package com.revenuecat.samplecat.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.samplecat.R
import com.revenuecat.samplecat.config.Constants
import com.revenuecat.samplecat.model.LocalizedMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel that manages RevenueCat offerings, products, and purchases.
 *
 * This follows RevenueCat's best practices for Android integration
 * and can serve as a reference for your own app's implementation.
 */
class UserViewModel : ViewModel() {

    private val _customerInfo = MutableStateFlow<CustomerInfo?>(null)
    val customerInfo: StateFlow<CustomerInfo?> = _customerInfo.asStateFlow()

    private val _offeringsState = MutableStateFlow<OfferingsUiState>(OfferingsUiState.Loading)
    val offeringsState: StateFlow<OfferingsUiState> = _offeringsState
        .onStart { fetchOfferings() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOPFLOW_TIMEOUT_MS),
            initialValue = OfferingsUiState.Loading,
        )

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    private val _purchasingProductId = MutableStateFlow<String?>(null)
    val purchasingProductId: StateFlow<String?> = _purchasingProductId.asStateFlow()

    private val _subscriptionActive = MutableStateFlow(false)
    val subscriptionActive: StateFlow<Boolean> = _subscriptionActive.asStateFlow()

    private val _purchaseError = MutableStateFlow<LocalizedMessage?>(null)
    val purchaseError: StateFlow<LocalizedMessage?> = _purchaseError.asStateFlow()

    private val customerInfoListener = UpdatedCustomerInfoListener { customerInfo ->
        _customerInfo.value = customerInfo
        updateSubscriptionStatus(customerInfo)
    }

    init {
        // Register listener for CustomerInfo updates
        Purchases.sharedInstance.updatedCustomerInfoListener = customerInfoListener

        // Fetch initial customer info
        viewModelScope.launch {
            try {
                val info = Purchases.sharedInstance.awaitCustomerInfo()
                _customerInfo.value = info
                updateSubscriptionStatus(info)
            } catch (_: PurchasesException) {
                // Initial fetch failed, will be updated via listener
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up listener when ViewModel is destroyed
        Purchases.sharedInstance.updatedCustomerInfoListener = null
    }

    private fun updateSubscriptionStatus(customerInfo: CustomerInfo?) {
        val entitlementId = Constants.entitlementId ?: return
        _subscriptionActive.value = customerInfo?.entitlements?.get(entitlementId)?.isActive == true
    }

    /**
     * Fetches the available offerings from RevenueCat.
     */
    fun fetchOfferings() {
        viewModelScope.launch {
            val currentState = _offeringsState.value

            // Set appropriate loading state based on whether we have existing data
            when (currentState) {
                is OfferingsUiState.Success -> {
                    // We have data, this is a refresh - keep showing data with refreshing indicator
                    _offeringsState.update { currentState.copy(isRefreshing = true, refreshError = null) }
                }
                else -> {
                    // No data yet, show loading state
                    _offeringsState.value = OfferingsUiState.Loading
                }
            }

            Log.d(TAG, "Fetching offerings...")

            try {
                val fetchedOfferings = Purchases.sharedInstance.awaitOfferings()

                Log.d(TAG, "Fetched offerings: ${fetchedOfferings.all.size} offerings")
                fetchedOfferings.all.forEach { (id, offering) ->
                    Log.d(TAG, "  Offering: $id with ${offering.availablePackages.size} packages")
                }

                if (fetchedOfferings.all.isEmpty()) {
                    Log.w(TAG, "No offerings returned from RevenueCat. Check your dashboard configuration.")
                }

                _offeringsState.value = OfferingsUiState.Success(offerings = fetchedOfferings)
            } catch (e: PurchasesException) {
                Log.e(TAG, "Failed to fetch offerings: ${e.message}", e)
                val localizedError = LocalizedMessage(
                    resId = R.string.error_fetch_offerings,
                    formatArgs = listOf(e.message ?: ""),
                )

                when (currentState) {
                    is OfferingsUiState.Success -> {
                        // Refresh failed, keep showing existing data with error
                        _offeringsState.value = currentState.copy(
                            isRefreshing = false,
                            refreshError = localizedError,
                        )
                    }
                    else -> {
                        // Initial load failed
                        _offeringsState.value = OfferingsUiState.Error(error = localizedError)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "UserViewModel"
        private const val STOPFLOW_TIMEOUT_MS = 5_000L
    }

    /**
     * Purchases a package.
     *
     * @param activity The activity to use for the purchase flow
     * @param packageToPurchase The package to purchase
     */
    fun purchase(activity: Activity, packageToPurchase: Package) {
        if (_isPurchasing.value) return

        viewModelScope.launch {
            _isPurchasing.value = true
            _purchasingProductId.value = packageToPurchase.product.id
            _purchaseError.value = null

            try {
                val purchaseParams = PurchaseParams.Builder(activity, packageToPurchase).build()
                val result = Purchases.sharedInstance.awaitPurchase(purchaseParams)
                _customerInfo.value = result.customerInfo
                updateSubscriptionStatus(result.customerInfo)
            } catch (e: PurchasesException) {
                // User cancelled is not an error
                if (e.error.code != PurchasesErrorCode.PurchaseCancelledError) {
                    _purchaseError.value = LocalizedMessage(
                        resId = R.string.error_purchase_failed,
                        formatArgs = listOf(e.message ?: ""),
                    )
                }
            } finally {
                _isPurchasing.value = false
                _purchasingProductId.value = null
            }
        }
    }

    /**
     * Purchases a store product directly.
     *
     * @param activity The activity to use for the purchase flow
     * @param product The store product to purchase
     */
    fun purchase(activity: Activity, product: StoreProduct) {
        if (_isPurchasing.value) return

        viewModelScope.launch {
            _isPurchasing.value = true
            _purchasingProductId.value = product.id
            _purchaseError.value = null

            try {
                val purchaseParams = PurchaseParams.Builder(activity, product).build()
                val result = Purchases.sharedInstance.awaitPurchase(purchaseParams)
                _customerInfo.value = result.customerInfo
                updateSubscriptionStatus(result.customerInfo)
            } catch (e: PurchasesException) {
                // User cancelled is not an error
                if (e.error.code != PurchasesErrorCode.PurchaseCancelledError) {
                    _purchaseError.value = LocalizedMessage(
                        resId = R.string.error_purchase_failed,
                        formatArgs = listOf(e.message ?: ""),
                    )
                }
            } finally {
                _isPurchasing.value = false
                _purchasingProductId.value = null
            }
        }
    }

    /**
     * Checks if a product has been purchased.
     *
     * @param productId The product identifier to check
     * @return True if the product has been purchased
     */
    fun isProductPurchased(productId: String): Boolean {
        val customerInfo = _customerInfo.value ?: return false
        return customerInfo.allPurchasedProductIds.contains(productId)
    }

    /**
     * Gets all offerings as a list.
     */
    fun getOfferingsList(): List<Offering> {
        val state = _offeringsState.value
        return if (state is OfferingsUiState.Success) {
            state.offerings.all.values.toList()
        } else {
            emptyList()
        }
    }

    /**
     * Gets all unique products from all offerings.
     */
    fun getAllProducts(): List<StoreProduct> {
        val state = _offeringsState.value
        if (state !is OfferingsUiState.Success) return emptyList()
        return state.offerings.all.values
            .flatMap { offering -> offering.availablePackages }
            .map { pkg -> pkg.product }
            .distinctBy { it.id }
    }

    /**
     * Clears the refresh error if in Success state.
     */
    fun clearRefreshError() {
        _offeringsState.update { currentState ->
            if (currentState is OfferingsUiState.Success) {
                currentState.copy(refreshError = null)
            } else {
                currentState
            }
        }
    }

    /**
     * Clears the purchase error.
     */
    fun clearPurchaseError() {
        _purchaseError.value = null
    }
}
