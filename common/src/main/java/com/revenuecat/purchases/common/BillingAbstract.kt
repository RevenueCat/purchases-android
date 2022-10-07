package com.revenuecat.purchases.common

import android.app.Activity
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PackageTemplate
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
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
        offerings: Offerings,
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

    open fun mapStoreProducts(offeringsIn: Offerings, products: List<StoreProduct>): Offerings {
        return mapStoreProducts(offeringsIn, products, { product -> product.sku }, {template -> template.product_identifier})
    }

    fun mapStoreProducts(
        offeringsIn: Offerings,
        products: List<StoreProduct>,
        storeProductKeyFunc: (StoreProduct) -> String,
        packageKeyFunc: (PackageTemplate) -> String
    ): Offerings {
        val subscriptionProductsById = products.groupBy(storeProductKeyFunc)
        log(LogIntent.DEBUG, subscriptionProductsById.toString())
        val offerings =
            offeringsIn.all.values.map { offering -> setPackagesInOffering(offering, subscriptionProductsById, packageKeyFunc) }
        return Offerings(offeringsIn.current, offerings.filterNotNull().associateBy { it.identifier })
    }


    private fun setPackagesInOffering(offering: Offering, subscriptionProductsById: Map<String, List<StoreProduct>>, packageKeyFunc: (PackageTemplate) -> String): Offering? {
        val packages = offering.packageTemplates.flatMap { template ->
            val products = subscriptionProductsById.get(packageKeyFunc(template))
            if (products != null)
                return@flatMap products.map { template.makePackage(it) }
            else
                return@flatMap emptyList()
        }

        if (packages.isEmpty())
            return null
        else
            return Offering(offering.identifier, offering.serverDescription, packages, offering.packageTemplates)
    }

    interface PurchasesUpdatedListener {
        fun onPurchasesUpdated(purchases: List<StoreTransaction>)
        fun onPurchasesFailedToUpdate(purchasesError: PurchasesError)
    }
}
