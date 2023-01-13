package com.revenuecat.purchases.models

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType

sealed class GooglePurchaseInfo: PurchaseInfo {
    data class NotSubscription(
        override val productId: String,
        val type: ProductType,
        val productDetails: ProductDetails
    ) : GooglePurchaseInfo()

    data class Subscription(
        override val productId: String,
        val type: ProductType,
        val productDetails: ProductDetails,

        val optionId: String?,
        val token: String?
    ) : GooglePurchaseInfo()

    override val productType: ProductType
        get() = when (this) {
            is NotSubscription -> {
                ProductType.INAPP
            }
            is Subscription -> {
                ProductType.SUBS
            }
        }

//    fun getProductId(): String {
//        return when (this) {
//            is NotSubscription -> {
//                this.productId
//            }
//            is Subscription -> {
//                this.productId
//            }
//        }
//    }
//
//    fun getPurchaseOptionId(): String? {
//        return when (this) {
//            is NotSubscription -> {
//                null
//            }
//            is Subscription -> {
//                this.optionId
//            }
//        }
//    }
//
//    fun getType(): ProductType {
//        return when (this) {
//            is NotSubscription -> {
//                ProductType.INAPP
//            }
//            is Subscription -> {
//                ProductType.SUBS
//            }
//        }
//    }
}