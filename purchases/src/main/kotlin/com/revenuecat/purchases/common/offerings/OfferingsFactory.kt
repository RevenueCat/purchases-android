package com.revenuecat.purchases.common.offerings

import com.revenuecat.purchases.PackageType
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
import com.revenuecat.purchases.utils.objects
import kotlinx.serialization.SerializationException
import org.json.JSONException
import org.json.JSONObject

internal class OfferingsFactory(
    private val billing: BillingAbstract,
    private val offeringParser: OfferingParser,
    private val dispatcher: Dispatcher,
    private val appConfig: AppConfig,
) {

    private val packageTypeByIdentifier: Map<String, PackageType> =
        PackageType.values()
            .mapNotNull { it.identifier?.let { id -> id to it } }
            .toMap()

    // The default OfferingParser matches products using base plan IDs (Google Play logic),
    // which doesn't apply to mock products. In preview mode we key products purely by
    // platform_product_identifier, so we override findMatchingProduct accordingly.
    private val previewOfferingParser = object : OfferingParser() {
        override fun findMatchingProduct(
            productsById: Map<String, List<StoreProduct>>,
            packageJson: JSONObject,
        ): StoreProduct? {
            val productIdentifier = packageJson.getString("platform_product_identifier")
            return productsById[productIdentifier]?.firstOrNull()
        }
    }

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
                    offeringsJSON = offeringsJSON,
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

                            val parser = if (appConfig.uiPreviewMode) previewOfferingParser else offeringParser
                            val offerings = parser.createOfferings(
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
                    .nonBlankString("platform_product_identifier")?.let {
                        productIds.add(it)
                    }
            }
        }
        return productIds
    }

    private fun getStoreProductsById(
        productIds: Set<String>,
        offeringsJSON: JSONObject,
        onCompleted: (Map<String, List<StoreProduct>>) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        if (appConfig.uiPreviewMode) {
            val productsById = createPreviewProducts(productIds, offeringsJSON)
            onCompleted(productsById)
            return
        }
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

    private fun createPreviewProducts(
        productIds: Set<String>,
        offeringsJSON: JSONObject,
    ): Map<String, List<StoreProduct>> {
        val packageTypeByProductId = extractPackageTypeByProductId(offeringsJSON)
        return productIds.associateWith { productId ->
            val packageType = packageTypeByProductId[productId]
                ?: PreviewProductSpec.inferFromProductId(productId)
            listOf(PreviewProductSpec.fromPackageType(packageType).toTestStoreProduct(productId))
        }
    }

    // Walks the offerings JSON to map each product ID to its PackageType using the package
    // identifier (e.g. $rc_monthly). This lets us assign the correct mock price per package
    // type rather than falling back to inference from the product ID string alone.
    private fun extractPackageTypeByProductId(offeringsJson: JSONObject): Map<String, PackageType> {
        val offerings = offeringsJson.optJSONArray("offerings") ?: return emptyMap()

        return offerings.objects()
            .mapNotNull { it.optJSONArray("packages") }
            .flatMap { it.objects() }
            .mapNotNull { pkg ->
                val productId = pkg.nonBlankString("platform_product_identifier") ?: return@mapNotNull null
                val identifier = pkg.nonBlankString("identifier") ?: return@mapNotNull null
                val type = packageTypeByIdentifier[identifier] ?: return@mapNotNull null
                productId to type
            }
            .toMap()
    }

    private fun JSONObject.nonBlankString(key: String): String? =
        optString(key).trim().takeIf { it.isNotEmpty() }
}
