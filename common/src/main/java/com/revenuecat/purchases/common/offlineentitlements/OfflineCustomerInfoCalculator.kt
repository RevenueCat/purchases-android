package com.revenuecat.purchases.common.offlineentitlements

import com.google.gson.internal.bind.util.ISO8601Utils
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.CustomerInfoFactory
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.strings.CustomerInfoStrings.COMPUTING_OFFLINE_CUSTOMER_INFO_FAILED
import com.revenuecat.purchases.utils.Iso8601Utils
import org.json.JSONObject
import java.util.Date

class OfflineCustomerInfoCalculator(
    private val purchasedProductsFetcher: PurchasedProductsFetcher,
    private val appConfig: AppConfig,
    private val dateProvider: DateProvider = DefaultDateProvider()
) {

    fun computeOfflineCustomerInfo(
        appUserID: String,
        onSuccess: (CustomerInfo) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        purchasedProductsFetcher.queryPurchasedProducts(
            appUserID,
            onSuccess = { purchasedProducts ->
                val jsonObject = JSONObject()
                val requestDate = dateProvider.now
                jsonObject.apply {
                    val formattedDate = Iso8601Utils.format(requestDate)
                    put("request_date", formattedDate)
                    put("request_date_ms", requestDate.time)
                    put("subscriber", JSONObject().apply {
                        put("original_app_user_id", appUserID)
                        put("original_application_version", "1.0")
                        put("entitlements", generateEntitlementsResponse(purchasedProducts))
                        put("first_seen", formattedDate)
                        val originalPurchaseDate = calculateOriginalPurchaseDate(purchasedProducts)
                        put("original_purchase_date", originalPurchaseDate)
                        put("non_subscriptions", JSONObject()) // TODO in another PR
                        put("subscriptions", generateSubscriptions(purchasedProducts))
                        put("management_url", "https://play.google.com/store/account/subscriptions")
                    })
                }

                val customerInfo = CustomerInfoFactory.buildCustomerInfo(
                    jsonObject, requestDate, VerificationResult.NOT_REQUESTED
                )
                onSuccess.invoke(customerInfo)
            }, onError = { error ->
                errorLog(COMPUTING_OFFLINE_CUSTOMER_INFO_FAILED.format(error))
                onError(error)
            })
    }

    private fun calculateOriginalPurchaseDate(purchasedProducts: List<PurchasedProduct>): String? {
        val minPurchaseDate = purchasedProducts.minOfOrNull { it.storeTransaction.purchaseTime }
        return minPurchaseDate?.let { ISO8601Utils.format(Date(minPurchaseDate)) }
    }

    private fun generateSubscriptions(
        purchasedProducts: List<PurchasedProduct>,
    ): JSONObject {
        val subscriptions = JSONObject()

        purchasedProducts.filter { it.storeTransaction.type == ProductType.SUBS }.forEach { product ->
            subscriptions.put(product.productIdentifier, JSONObject().apply {
                put("billing_issues_detected_at", JSONObject.NULL)
                put("is_sandbox", false)
                put("original_purchase_date", false)
                val purchaseDate = Date(product.storeTransaction.purchaseTime)
                put("original_purchase_date", Iso8601Utils.format(purchaseDate))
                put("purchase_date", Iso8601Utils.format(purchaseDate))
                put("store", appConfig.store.name.lowercase())
                put("unsubscribe_detected_at", JSONObject.NULL)
                put("expires_date", Iso8601Utils.format(product.expiresDate))
                put("period_type", "normal") // Best guess, we don't know what period type was purchased
            })
        }
        return subscriptions
    }

    private fun generateEntitlementsResponse(purchasedProducts: List<PurchasedProduct>): JSONObject {
        val entitlements = JSONObject()

        purchasedProducts.forEach { product ->
            val entitlementDetails = JSONObject().apply {
                put("expires_date", product.expiresDate?.let { Iso8601Utils.format(it) })
                put("product_identifier", product.productIdentifier)
                val purchaseDate = Date(product.storeTransaction.purchaseTime)
                put("purchase_date", Iso8601Utils.format(purchaseDate))
            }
            product.entitlements.forEach { entitlement ->
                entitlements.put(entitlement, entitlementDetails)
            }
        }
        return entitlements
    }
}
