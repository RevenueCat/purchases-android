package com.revenuecat.purchases.common.networking

import android.net.Uri
import com.revenuecat.purchases.common.AppConfig
import java.net.URI
import java.net.URL

internal sealed class Endpoint(
    val pathTemplate: String,
    val name: String,
    val fallbackPath: String? = null,
) {
    abstract fun getPath(useFallback: Boolean): String
    data class GetCustomerInfo(val userId: String) : Endpoint("/v1/subscribers/%s", "get_customer") {
        override fun getPath(useFallback: Boolean) = pathTemplate.format(Uri.encode(userId))
    }
    object PostReceipt : Endpoint("/v1/receipts", "post_receipt") {
        override fun getPath(useFallback: Boolean) = pathTemplate
    }
    data class GetOfferings(val userId: String) : Endpoint(
        "/v1/subscribers/%s/offerings",
        "get_offerings",
        fallbackPath = "v1/offerings",
    ) {
        override fun getPath(useFallback: Boolean): String {
            return if (useFallback && fallbackPath != null) {
                fallbackPath
            } else {
                pathTemplate.format(Uri.encode(userId))
            }
        }
    }
    object LogIn : Endpoint("/v1/subscribers/identify", "log_in") {
        override fun getPath(useFallback: Boolean) = pathTemplate
    }
    data class AliasUsers(val userId: String) : Endpoint("/v1/subscribers/%s/alias", "alias_users") {
        override fun getPath(useFallback: Boolean) = pathTemplate.format(Uri.encode(userId))
    }
    object PostDiagnostics : Endpoint("/v1/diagnostics", "post_diagnostics") {
        override fun getPath(useFallback: Boolean) = pathTemplate
    }
    object PostPaywallEvents : Endpoint("/v1/events", "post_paywall_events") {
        override fun getPath(useFallback: Boolean) = pathTemplate
    }
    data class PostAttributes(val userId: String) : Endpoint("/v1/subscribers/%s/attributes", "post_attributes") {
        override fun getPath(useFallback: Boolean) = pathTemplate.format(Uri.encode(userId))
    }
    data class GetAmazonReceipt(
        val userId: String,
        val receiptId: String,
    ) : Endpoint("/v1/receipts/amazon/%s/%s", "get_amazon_receipt") {
        override fun getPath(useFallback: Boolean) = pathTemplate.format(Uri.encode(userId), receiptId)
    }
    object GetProductEntitlementMapping : Endpoint(
        "/v1/product_entitlement_mapping",
        "get_product_entitlement_mapping",
        fallbackPath = "v1/product_entitlement_mapping",
    ) {
        override fun getPath(useFallback: Boolean): String {
            return if (useFallback && fallbackPath != null) {
                fallbackPath
            } else {
                pathTemplate
            }
        }
    }
    data class GetCustomerCenterConfig(val userId: String) : Endpoint(
        "/v1/customercenter/%s",
        "get_customer_center_config",
    ) {
        override fun getPath(useFallback: Boolean) = pathTemplate.format(Uri.encode(userId))
    }
    object PostRedeemWebPurchase : Endpoint(
        "/v1/subscribers/redeem_purchase",
        "post_redeem_web_purchase",
    ) {
        override fun getPath(useFallback: Boolean) = pathTemplate
    }
    data class GetVirtualCurrencies(val userId: String) : Endpoint(
        pathTemplate = "/v1/subscribers/%s/virtual_currencies",
        name = "get_virtual_currencies",
    ) {
        override fun getPath(useFallback: Boolean) = pathTemplate.format(Uri.encode(userId))
    }
    data class WebBillingGetProducts(val userId: String, val productIds: Set<String>) : Endpoint(
        pathTemplate = "/rcbilling/v1/subscribers/%s/products?id=%s",
        name = "web_billing_get_products",
    ) {
        override fun getPath(useFallback: Boolean): String {
            return pathTemplate.format(Uri.encode(userId), productIds.joinToString("&id=") { Uri.encode(it) })
        }
    }

    val supportsSignatureVerification: Boolean
        get() = when (this) {
            is GetCustomerInfo,
            LogIn,
            PostReceipt,
            is GetOfferings,
            GetProductEntitlementMapping,
            PostRedeemWebPurchase,
            is GetVirtualCurrencies,
            ->
                true
            is GetAmazonReceipt,
            is PostAttributes,
            PostDiagnostics,
            PostPaywallEvents,
            is GetCustomerCenterConfig,
            is WebBillingGetProducts,
            is AliasUsers,
            ->
                false
        }

    val needsNonceToPerformSigning: Boolean
        get() = when (this) {
            is GetCustomerInfo,
            LogIn,
            PostReceipt,
            PostRedeemWebPurchase,
            is GetVirtualCurrencies,
            ->
                true
            is GetAmazonReceipt,
            is GetOfferings,
            is PostAttributes,
            PostDiagnostics,
            PostPaywallEvents,
            GetProductEntitlementMapping,
            is GetCustomerCenterConfig,
            is WebBillingGetProducts,
            is AliasUsers,
            ->
                false
        }

    val supportsFallbackBaseURLs: Boolean
        get() = fallbackPath != null
}
