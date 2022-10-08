package com.revenuecat.purchases.common

import android.app.Activity
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageTemplate
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.OfferingStrings
import org.json.JSONObject

typealias StoreProductsCallback = (List<StoreProduct>) -> Unit

@SuppressWarnings("TooManyFunctions")
abstract class BillingAbstract {

    @get:Synchronized
    @set:Synchronized
    @Volatile
    var stateListener: StateListener? = null

    @get:Synchronized
    @Volatile
    var purchasesUpdatedListener: PurchasesUpdatedListener? = null
        set(value) {
            synchronized(this@BillingAbstract) {
                field = value
            }
            if (value != null) {
                startConnectionOnMainThread()
            } else {
                endConnection()
            }
        }

    interface StateListener {
        fun onConnected()
    }

    abstract fun startConnectionOnMainThread(delayMilliseconds: Long = 0)

    abstract fun startConnection()

    protected abstract fun endConnection()

    fun close() {
        purchasesUpdatedListener = null
        endConnection()
    }

    abstract fun queryAllPurchases(
        appUserID: String,
        onReceivePurchaseHistory: (List<StoreTransaction>) -> Unit,
        onReceivePurchaseHistoryError: PurchasesErrorCallback
    )

    abstract fun querySkuDetailsAsync(
        productType: ProductType,
        skus: Set<String>,
        onReceive: StoreProductsCallback,
        onError: PurchasesErrorCallback
    )

    abstract fun consumeAndSave(
        shouldTryToConsume: Boolean,
        purchase: StoreTransaction
    )

    abstract fun findPurchaseInPurchaseHistory(
        appUserID: String,
        productType: ProductType,
        sku: String,
        onCompletion: (StoreTransaction) -> Unit,
        onError: (PurchasesError) -> Unit
    )

    abstract fun makePurchaseAsync(
        activity: Activity,
        appUserID: String,
        storeProduct: StoreProduct,
        replaceSkuInfo: ReplaceSkuInfo?,
        presentedOfferingIdentifier: String?
    )

    abstract fun isConnected(): Boolean

    abstract fun queryPurchases(
        appUserID: String,
        onSuccess: (Map<String, StoreTransaction>) -> Unit,
        onError: (PurchasesError) -> Unit
    )

    /**
     * Amazon has the concept of term and parent product ID. This function will return
     * the correct product ID the RevenueCat backend expects for a specific purchase.
     * Google doesn't need normalization so we return the productID by default
     */
    open fun normalizePurchaseData(
        productID: String,
        purchaseToken: String,
        storeUserID: String,
        onSuccess: (normalizedProductID: String) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        onSuccess(productID)
    }

    /**
     * Note: this may return an empty Offerings.
     */
    fun createOfferings(offeringsJson: JSONObject, productsById: Map<String, StoreProduct>): Offerings {
        with (offeringsJson) {
            val jsonOfferings = getJSONArray("offerings")
            val currentOfferingID = getString("current_offering_id")

            val offerings = mutableMapOf<String, Offering>()
            for (i in 0 until jsonOfferings.length()) {
                val offeringJson = jsonOfferings.getJSONObject(i)
                createOffering(offeringJson, productsById)?.let {
                    offerings[it.identifier] = it

                    if (it.availablePackages.isEmpty()) {
                        warnLog(OfferingStrings.OFFERING_EMPTY.format(it.identifier))
                    }
                }
            }

            return Offerings(offerings[currentOfferingID], offerings)
        }
    }

    private fun createOffering(offeringJson: JSONObject, productsById: Map<String, StoreProduct>): Offering? {
        with (offeringJson) {
            val offeringIdentifier = getString("identifier")
            val jsonPackages = getJSONArray("packages")

            val availablePackages = mutableListOf<Package>()
            for (i in 0 until jsonPackages.length()) {
                val packageJson = jsonPackages.getJSONObject(i)
                createPackage(packageJson, productsById, offeringIdentifier)?.let {
                    availablePackages.add(it)
                }
            }

            return if (availablePackages.isNotEmpty()) {
                Offering(offeringIdentifier, getString("description"), availablePackages)
            } else {
                null
            }
        }
    }

    private fun createPackage(
        packageJson: JSONObject,
        productsById: Map<String, StoreProduct>,
        offeringIdentifier: String
    ): Package? {
        with (packageJson) {
            val productId = getString("platform_product_identifier")
            return productsById[productId]?.let { product ->
                val identifier = getString("identifier")

                // don't need to check if bc5, can just be null in Package
                val groupIdentifier = optionalString("platform_product_group_identifier")
                val duration = optionalString("product_duration")
                val packageType = identifier.toPackageType()
                return Package(
                    identifier,
                    packageType,
                    product,
                    offeringIdentifier,
                    groupIdentifier,
                    duration
                )
            }
        }
    }

    // TODO pull out common
    fun JSONObject.optionalString(id: String): String? {
        return if (this.has(id)) getString(id) else null
    }

    // TODO pull out common
    fun String.toPackageType(): PackageType =
        PackageType.values().firstOrNull { it.identifier == this }
            ?: if (this.startsWith("\$rc_")) PackageType.UNKNOWN else PackageType.CUSTOM


    interface PurchasesUpdatedListener {
        fun onPurchasesUpdated(purchases: List<StoreTransaction>)
        fun onPurchasesFailedToUpdate(purchasesError: PurchasesError)
    }
}
