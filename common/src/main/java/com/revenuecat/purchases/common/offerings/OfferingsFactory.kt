package com.revenuecat.purchases.common.offerings

import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.strings.OfferingStrings
import org.json.JSONException
import org.json.JSONObject

class OfferingsFactory(
    private val billing: BillingAbstract,
    private val offeringParser: OfferingParser
) {

    fun createOfferings(
        offeringsJSON: JSONObject,
        onError: (PurchasesError) -> Unit,
        onSuccess: (Offerings) -> Unit
    ) {
        try {
            val allRequestedProductIdentifiers = extractProductIdentifiers(offeringsJSON)
            if (allRequestedProductIdentifiers.isEmpty()) {
                onError(
                    PurchasesError(
                        PurchasesErrorCode.ConfigurationError,
                        OfferingStrings.CONFIGURATION_ERROR_NO_PRODUCTS_FOR_OFFERINGS
                    )
                )
            } else {
                getStoreProductsById(allRequestedProductIdentifiers, { productsById ->
                    logMissingProducts(allRequestedProductIdentifiers, productsById)

                    val offerings = offeringParser.createOfferings(offeringsJSON, productsById)
                    if (offerings.all.isEmpty()) {
                        onError(
                            PurchasesError(
                                PurchasesErrorCode.ConfigurationError,
                                OfferingStrings.CONFIGURATION_ERROR_PRODUCTS_NOT_FOUND
                            )
                        )
                    } else {
                        onSuccess(offerings)
                    }
                }, { error ->
                    onError(error)
                })
            }
        } catch (error: JSONException) {
            log(LogIntent.RC_ERROR, OfferingStrings.JSON_EXCEPTION_ERROR.format(error.localizedMessage))
            onError(
                PurchasesError(
                    PurchasesErrorCode.UnexpectedBackendResponseError,
                    error.localizedMessage
                )
            )
        }
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
}
