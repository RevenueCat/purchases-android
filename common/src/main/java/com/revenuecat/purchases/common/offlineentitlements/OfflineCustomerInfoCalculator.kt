package com.revenuecat.purchases.common.offlineentitlements

import com.google.gson.internal.bind.util.ISO8601Utils
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Constants
import com.revenuecat.purchases.common.CustomerInfoFactory
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.responses.CustomerInfoResponseJsonKeys
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
                val customerInfo = buildCustomerInfoUsingListOfPurchases(appUserID, purchasedProducts)
                onSuccess.invoke(customerInfo)
            }, onError = { error ->
                errorLog(COMPUTING_OFFLINE_CUSTOMER_INFO_FAILED.format(error))
                onError(error)
            })
    }

    private fun buildCustomerInfoUsingListOfPurchases(
        appUserID: String,
        purchasedProducts: List<PurchasedProduct>
    ): CustomerInfo {
        val jsonObject = JSONObject()
        val requestDate = dateProvider.now
        jsonObject.apply {
            val formattedDate = Iso8601Utils.format(requestDate)
            put(CustomerInfoResponseJsonKeys.REQUEST_DATE, formattedDate)
            put(CustomerInfoResponseJsonKeys.REQUEST_DATE_MS, requestDate.time)
            put(CustomerInfoResponseJsonKeys.SUBSCRIBER, JSONObject().apply {
                put(CustomerInfoResponseJsonKeys.ORIGINAL_APP_USER_ID, appUserID)
                put(CustomerInfoResponseJsonKeys.ORIGINAL_APPLICATION_VERSION, "1.0")
                put(CustomerInfoResponseJsonKeys.ENTITLEMENTS, generateEntitlementsResponse(purchasedProducts))
                put(CustomerInfoResponseJsonKeys.FIRST_SEEN, formattedDate)
                val originalPurchaseDate = calculateOriginalPurchaseDate(purchasedProducts)
                put(CustomerInfoResponseJsonKeys.ORIGINAL_PURCHASE_DATE, originalPurchaseDate)
                put(CustomerInfoResponseJsonKeys.NON_SUBSCRIPTIONS, JSONObject()) // TODO in another PR
                put(CustomerInfoResponseJsonKeys.SUBSCRIPTIONS, generateSubscriptions(purchasedProducts))
                put(CustomerInfoResponseJsonKeys.MANAGEMENT_URL, determineManagementURL())
            })
        }

        return CustomerInfoFactory.buildCustomerInfo(
            jsonObject, requestDate, VerificationResult.NOT_REQUESTED
        )
    }

    private fun determineManagementURL() =
        if (appConfig.store == Store.PLAY_STORE) Constants.GOOGLE_PLAY_MANAGEMENT_URL else null

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
                put(CustomerInfoResponseJsonKeys.BILLING_ISSUES_DETECTED_AT, JSONObject.NULL)
                put(CustomerInfoResponseJsonKeys.IS_SANDBOX, false)
                val purchaseDate = Date(product.storeTransaction.purchaseTime)
                put(CustomerInfoResponseJsonKeys.ORIGINAL_PURCHASE_DATE, Iso8601Utils.format(purchaseDate))
                put(CustomerInfoResponseJsonKeys.PURCHASE_DATE, Iso8601Utils.format(purchaseDate))
                put(CustomerInfoResponseJsonKeys.STORE, appConfig.store.name.lowercase())
                put(CustomerInfoResponseJsonKeys.UNSUBSCRIBE_DETECTED_AT, JSONObject.NULL)
                put(
                    CustomerInfoResponseJsonKeys.EXPIRES_DATE,
                    product.expiresDate?.let { Iso8601Utils.format(it) } ?: JSONObject.NULL)
                put(
                    CustomerInfoResponseJsonKeys.PERIOD_TYPE,
                    PeriodType.NORMAL.name.lowercase()
                ) // Best guess, we don't know what period type was purchased
            })
        }
        return subscriptions
    }

    private fun generateEntitlementsResponse(purchasedProducts: List<PurchasedProduct>): JSONObject {
        val mapOfEntitlementsToProducts: Map<String, PurchasedProduct> = purchasedProducts
            // transform into a list of pairs of entitlement to product
            .flatMap { product -> product.entitlements.map { it to product } }
            // group by the entitlement name
            .groupBy({ it.first }, { it.second })
            // only take the product with the max date
            .mapValues { (_, products) ->
                products.maxByOrNull { it.expiresDate?.time ?: Long.MAX_VALUE } ?: products.first()
            }

        val entitlements = JSONObject()

        mapOfEntitlementsToProducts.forEach { (entitlement, product) ->
            val entitlementDetails = JSONObject().apply {
                put("expires_date", product.expiresDate?.let { Iso8601Utils.format(it) })
                put("product_identifier", product.productIdentifier)
                val purchaseDate = Date(product.storeTransaction.purchaseTime)
                put("purchase_date", Iso8601Utils.format(purchaseDate))
            }
            entitlements.put(entitlement, entitlementDetails)
        }
        return entitlements
    }
}
