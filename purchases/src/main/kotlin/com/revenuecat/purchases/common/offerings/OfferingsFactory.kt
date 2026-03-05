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
    // which doesn't apply to mock products. In preview mode we key products by a composite
    // "productId::planId" key (or just productId when no plan is present, e.g. Amazon/INAPP),
    // so we override findMatchingProduct accordingly.
    private val previewOfferingParser = object : OfferingParser() {
        override fun findMatchingProduct(
            productsById: Map<String, List<StoreProduct>>,
            packageJson: JSONObject,
        ): StoreProduct? {
            val productIdentifier = packageJson.getString("platform_product_identifier")
            val planIdentifier = packageJson.optString("platform_product_plan_identifier").takeIf { it.isNotEmpty() }
            val key = planIdentifier?.let { "$productIdentifier::$it" } ?: productIdentifier
            return productsById[key]?.firstOrNull()
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
                            // In preview mode the map is keyed by composite "productId::planId" keys,
                            // so plain product ID lookups would all appear missing — skip the check.
                            val notFoundProductIds = if (appConfig.uiPreviewMode) {
                                emptySet()
                            } else {
                                allRequestedProductIdentifiers
                                    .filterNot { productsById.containsKey(it) }
                                    .toSet()
                            }
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
            val productsById = createPreviewProducts(offeringsJSON)
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

    // Builds a map of mock products keyed by "productId::planId" (or just "productId" when no
    // plan is present, e.g. Amazon or INAPP). This ensures that multiple packages sharing the
    // same platform_product_identifier but differing by platform_product_plan_identifier each
    // get their own mock product with the correct pricing for their package type.
    //
    // Package type resolution priority:
    //   1. RC package identifier (e.g. $rc_monthly, $rc_annual) — most reliable
    //   2. Plan ID keywords (e.g. "monthly-plan", "annual-base") — helps custom packages on Google Play
    //   3. Product ID keywords (e.g. "com.app.monthly_sub") — last resort fallback
    private fun createPreviewProducts(offeringsJSON: JSONObject): Map<String, List<StoreProduct>> {
        val offerings = offeringsJSON.optJSONArray("offerings") ?: return emptyMap()
        return buildMap {
            offerings.objects()
                .mapNotNull { it.optJSONArray("packages") }
                .flatMap { it.objects() }
                .forEach { pkg ->
                    val productId = pkg.nonBlankString("platform_product_identifier") ?: return@forEach
                    val planId = pkg.nonBlankString("platform_product_plan_identifier")
                    val key = planId?.let { "$productId::$it" } ?: productId
                    if (containsKey(key)) return@forEach
                    val identifier = pkg.nonBlankString("identifier")
                    val packageType = identifier?.let { packageTypeByIdentifier[it] }
                        ?: planId?.let { PackageType.inferFromIdentifier(it) }
                        ?: PackageType.inferFromIdentifier(productId)
                    put(key, listOf(PreviewProductSpec.fromPackageType(packageType).toTestStoreProduct(productId)))
                }
        }
    }

    private fun JSONObject.nonBlankString(key: String): String? =
        optString(key).trim().takeIf { it.isNotEmpty() }
}
