package com.revenuecat.purchases.common.networking

import android.net.Uri

sealed class Endpoint(val pathTemplate: String, val name: String) {
    abstract fun getPath(): String
    data class GetCustomerInfo(val userId: String) : Endpoint("/subscribers/%s", "get_customer") {
        override fun getPath() = pathTemplate.format(Uri.encode(userId))
    }
    object PostReceipt : Endpoint("/receipts", "post_receipt") {
        override fun getPath() = pathTemplate
    }
    data class GetOfferings(val userId: String) : Endpoint("/subscribers/%s/offerings", "get_offerings") {
        override fun getPath() = pathTemplate.format(Uri.encode(userId))
    }
    object LogIn : Endpoint("/subscribers/identify", "log_in") {
        override fun getPath() = pathTemplate
    }
    object PostDiagnostics : Endpoint("/diagnostics", "post_diagnostics") {
        override fun getPath() = pathTemplate
    }
    data class PostAttributes(val userId: String) : Endpoint("/subscribers/%s/attributes", "post_attributes") {
        override fun getPath() = pathTemplate.format(Uri.encode(userId))
    }
    data class GetAmazonReceipt(
        val userId: String,
        val receiptId: String,
    ) : Endpoint("/receipts/amazon/%s/%s", "get_amazon_receipt") {
        override fun getPath() = pathTemplate.format(Uri.encode(userId), receiptId)
    }
    object GetProductEntitlementMapping : Endpoint("/product_entitlement_mapping", "get_product_entitlement_mapping") {
        override fun getPath() = pathTemplate
    }

    val supportsSignatureValidation: Boolean
        get() = when (this) {
            is GetCustomerInfo,
            LogIn,
            PostReceipt,
            is GetOfferings,
            GetProductEntitlementMapping,
            ->
                true
            is GetAmazonReceipt,
            is PostAttributes,
            PostDiagnostics,
            ->
                false
        }

    val needsNonceToPerformSigning: Boolean
        get() = when (this) {
            is GetCustomerInfo,
            LogIn,
            PostReceipt,
            ->
                true
            is GetAmazonReceipt,
            is GetOfferings,
            is PostAttributes,
            PostDiagnostics,
            GetProductEntitlementMapping,
            ->
                false
        }
}
