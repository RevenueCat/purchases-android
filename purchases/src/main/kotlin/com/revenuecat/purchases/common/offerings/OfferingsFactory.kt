package com.revenuecat.purchases.common.offerings

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.HTTPResponseOriginalSource
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.strings.OfferingStrings
import kotlinx.serialization.SerializationException
import org.json.JSONException
import org.json.JSONObject

@OptIn(InternalRevenueCatAPI::class)
internal class OfferingsFactory(
    private val billing: BillingAbstract,
    private val offeringParser: OfferingParser,
    private val dispatcher: Dispatcher,
    private val appConfig: AppConfig,
) {

    @SuppressWarnings("TooGenericExceptionCaught", "LongMethod")
    fun createOfferings(
        offeringsJSON: JSONObject,
        originalDataSource: HTTPResponseOriginalSource,
        loadedFromDiskCache: Boolean,
        onError: (PurchasesError) -> Unit,
        onSuccess: (OfferingsResultData) -> Unit,
    ) {
        try {
            val allRequestedProductIdentifiers = extractProductIdentifiers(offeringsJSON)
            if (allRequestedProductIdentifiers.isEmpty()) {
                onError(
                    PurchasesError(
                        PurchasesErrorCode.ConfigurationError,
                        OfferingStrings.getConfigurationErrorNoProductsForOfferings(
                            appConfig.apiKeyValidationResult,
                            appConfig.store,
                        ),
                    ),
                )
            } else {
                getStoreProductsById(
                    productIds = allRequestedProductIdentifiers,
                    onCompleted = { productsById ->
                        try {
                            val notFoundProductIds = allRequestedProductIdentifiers
                                .filterNot { productsById.containsKey(it) }
                                .toSet()
                            notFoundProductIds
                                .takeIf { it.isNotEmpty() }
                                ?.let { missingProducts ->
                                    log(LogIntent.GOOGLE_WARNING) {
                                        OfferingStrings.CANNOT_FIND_PRODUCT_CONFIGURATION_ERROR
                                            .format(missingProducts.joinToString(", "))
                                    }
                                }

                            val offerings = offeringParser.createOfferings(
                                offeringsJSON,
                                productsById,
                                originalDataSource,
                                loadedFromDiskCache,
                            )
                            if (offerings.all.isEmpty()) {
                                onError(
                                    PurchasesError(
                                        PurchasesErrorCode.ConfigurationError,
                                        OfferingStrings.CONFIGURATION_ERROR_PRODUCTS_NOT_FOUND,
                                    ),
                                )
                            } else {
                                verboseLog { OfferingStrings.CREATED_OFFERINGS.format(offerings.all.size) }
                                onSuccess(
                                    OfferingsResultData(offerings, allRequestedProductIdentifiers, notFoundProductIds),
                                )
                            }
                        } catch (error: Exception) {
                            when (error) {
                                is JSONException, is SerializationException -> {
                                    log(LogIntent.RC_ERROR) {
                                        OfferingStrings.JSON_EXCEPTION_ERROR.format(error.localizedMessage)
                                    }
                                    onError(
                                        PurchasesError(
                                            PurchasesErrorCode.UnexpectedBackendResponseError,
                                            error.localizedMessage,
                                        ),
                                    )
                                }

                                else -> throw error
                            }
                        }
                    },
                    onError = { error ->
                        onError(error)
                    },
                )
            }
        } catch (error: JSONException) {
            log(LogIntent.RC_ERROR) { OfferingStrings.JSON_EXCEPTION_ERROR.format(error.localizedMessage) }
            onError(
                PurchasesError(
                    PurchasesErrorCode.UnexpectedBackendResponseError,
                    error.localizedMessage,
                ),
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
        onError: (PurchasesError) -> Unit,
    ) {
        billing.queryProductDetailsAsync(
            productType = ProductType.SUBS,
            productIds = productIds,
            onReceive = { subscriptionProducts ->
                dispatcher.enqueue(command = {
                    val productsById = subscriptionProducts
                        .groupBy { subProduct -> subProduct.purchasingData.productId }
                        .toMutableMap()
                    val subscriptionIds = productsById.keys

                    val inAppProductIds = productIds - subscriptionIds
                    if (inAppProductIds.isNotEmpty()) {
                        billing.queryProductDetailsAsync(
                            productType = ProductType.INAPP,
                            productIds = inAppProductIds,
                            onReceive = { inAppProducts ->
                                dispatcher.enqueue(command = {
                                    productsById.putAll(inAppProducts.map { it.purchasingData.productId to listOf(it) })
                                    onCompleted(productsById)
                                })
                            },
                            onError = {
                                onError(it)
                            },
                        )
                    } else {
                        onCompleted(productsById)
                    }
                })
            },
            onError = {
                onError(it)
            },
        )
    }
}
