package com.revenuecat.purchases.common.offerings

import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.strings.OfferingStrings
import org.json.JSONException
import org.json.JSONObject

class OfferingsManager(
    private val deviceCache: DeviceCache,
    private val backend: Backend,
    private val billing: BillingAbstract,
    private val offeringParser: OfferingParser,
    // This is nullable due to: https://github.com/RevenueCat/purchases-flutter/issues/408
    private val mainHandler: Handler? = Handler(Looper.getMainLooper())
) {
    fun isOfferingsCacheStale(appInBackground: Boolean) = deviceCache.isOfferingsCacheStale(appInBackground)

    fun getOfferings(
        appUserID: String,
        appInBackground: Boolean,
        onError: ((PurchasesError) -> Unit)? = null,
        onSuccess: ((Offerings) -> Unit)? = null
    ) {
        val cachedOfferings = deviceCache.cachedOfferings
        if (cachedOfferings == null) {
            log(LogIntent.DEBUG, OfferingStrings.NO_CACHED_OFFERINGS_FETCHING_NETWORK)
            fetchAndCacheOfferings(appUserID, appInBackground, onError, onSuccess)
        } else {
            log(LogIntent.DEBUG, OfferingStrings.VENDING_OFFERINGS_CACHE)
            dispatch {
                onSuccess?.invoke(cachedOfferings)
            }
            if (deviceCache.isOfferingsCacheStale(appInBackground)) {
                log(
                    LogIntent.DEBUG,
                    if (appInBackground) OfferingStrings.OFFERINGS_STALE_UPDATING_IN_BACKGROUND
                    else OfferingStrings.OFFERINGS_STALE_UPDATING_IN_FOREGROUND
                )
                fetchAndCacheOfferings(appUserID, appInBackground)
                log(LogIntent.RC_SUCCESS, OfferingStrings.OFFERINGS_UPDATED_FROM_NETWORK)
            }
        }
    }

    fun fetchAndCacheOfferings(
        appUserID: String,
        appInBackground: Boolean,
        onError: ((PurchasesError) -> Unit)? = null,
        onSuccess: ((Offerings) -> Unit)? = null
    ) {
        deviceCache.setOfferingsCacheTimestampToNow()
        backend.getOfferings(
            appUserID,
            appInBackground,
            { offeringsJSON ->
                try {
                    val allRequestedProductIdentifiers = extractProductIdentifiers(offeringsJSON)
                    if (allRequestedProductIdentifiers.isEmpty()) {
                        handleErrorFetchingOfferings(
                            PurchasesError(
                                PurchasesErrorCode.ConfigurationError,
                                OfferingStrings.CONFIGURATION_ERROR_NO_PRODUCTS_FOR_OFFERINGS
                            ),
                            onError
                        )
                    } else {
                        getStoreProductsById(allRequestedProductIdentifiers, { productsById ->
                            logMissingProducts(allRequestedProductIdentifiers, productsById)

                            val offerings = offeringParser.createOfferings(offeringsJSON, productsById)
                            if (offerings.all.isEmpty()) {
                                handleErrorFetchingOfferings(
                                    PurchasesError(
                                        PurchasesErrorCode.ConfigurationError,
                                        OfferingStrings.CONFIGURATION_ERROR_PRODUCTS_NOT_FOUND
                                    ),
                                    onError
                                )
                            } else {
                                synchronized(this@OfferingsManager) {
                                    deviceCache.cacheOfferings(offerings)
                                }
                                dispatch {
                                    onSuccess?.invoke(offerings)
                                }
                            }
                        }, { error ->
                            handleErrorFetchingOfferings(error, onError)
                        })
                    }
                } catch (error: JSONException) {
                    log(LogIntent.RC_ERROR, OfferingStrings.JSON_EXCEPTION_ERROR.format(error.localizedMessage))
                    handleErrorFetchingOfferings(
                        PurchasesError(
                            PurchasesErrorCode.UnexpectedBackendResponseError,
                            error.localizedMessage
                        ),
                        onError
                    )
                }
            }, { error ->
                handleErrorFetchingOfferings(error, onError)
            })
    }

    private fun extractProductIdentifiers(offeringsJSON: JSONObject): Set<String> {
        val jsonOfferingsArray = offeringsJSON.getJSONArray("offerings")
        val productIds = mutableSetOf<String>()
        for (i in 0 until jsonOfferingsArray.length()) {
            val jsonPackagesArray =
                jsonOfferingsArray.getJSONObject(i).getJSONArray("packages")
            for (j in 0 until jsonPackagesArray.length()) {
                jsonPackagesArray.getJSONObject(j)
                    .optString("platform_product_identifier").takeIf { it.isNotBlank() }?.let {
                        productIds.add(it)
                    }
            }
        }
        return productIds
    }

    private fun handleErrorFetchingOfferings(
        error: PurchasesError,
        onError: ((PurchasesError) -> Unit)?
    ) {
        val errorCausedByPurchases = setOf(
            PurchasesErrorCode.ConfigurationError,
            PurchasesErrorCode.UnexpectedBackendResponseError
        )
            .contains(error.code)

        log(
            if (errorCausedByPurchases) LogIntent.RC_ERROR else LogIntent.GOOGLE_ERROR,
            OfferingStrings.FETCHING_OFFERINGS_ERROR.format(error)
        )

        deviceCache.clearOfferingsCacheTimestamp()
        dispatch {
            onError?.invoke(error)
        }
    }

    private fun getStoreProductsById(
        productIds: Set<String>,
        onCompleted: (Map<String, List<StoreProduct>>) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        billing.queryProductDetailsAsync(
            ProductType.SUBS,
            productIds,
            { subscriptionProducts ->
                val productsById = subscriptionProducts
                    .groupBy { subProduct -> subProduct.purchasingData.productId }
                    .toMutableMap()
                val subscriptionIds = productsById.keys

                val inAppProductIds = productIds - subscriptionIds
                if (inAppProductIds.isNotEmpty()) {
                    billing.queryProductDetailsAsync(
                        ProductType.INAPP,
                        inAppProductIds,
                        { inAppProducts ->
                            productsById.putAll(inAppProducts.map { it.purchasingData.productId to listOf(it) })
                            onCompleted(productsById)
                        }, {
                            onError(it)
                        }
                    )
                } else {
                    onCompleted(productsById)
                }
            }, {
                onError(it)
            })
    }

    private fun logMissingProducts(
        allProductIdsInOfferings: Set<String>,
        storeProductByID: Map<String, List<StoreProduct>>
    ) = allProductIdsInOfferings
        .filterNot { storeProductByID.containsKey(it) }
        .takeIf { it.isNotEmpty() }
        ?.let { missingProducts ->
            log(
                LogIntent.GOOGLE_WARNING, OfferingStrings.CANNOT_FIND_PRODUCT_CONFIGURATION_ERROR
                    .format(missingProducts.joinToString(", "))
            )
        }

    private fun dispatch(action: () -> Unit) {
        if (Thread.currentThread() != Looper.getMainLooper().thread) {
            val handler = mainHandler ?: Handler(Looper.getMainLooper())
            handler.post(action)
        } else {
            action()
        }
    }
}
