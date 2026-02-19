package com.revenuecat.purchases.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.PresentedOfferingContextSerializer
import com.revenuecat.purchases.ReplacementMode
import com.revenuecat.purchases.ReplacementModeSerializer
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.PeriodSerializer
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PricingPhaseSerializer
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.SubscriptionOption
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.serialization.Serializable

@Serializable
internal data class ReceiptInfo(
    val productIDs: List<String>,
    val purchaseTime: Long? = null,
    @Serializable(with = PresentedOfferingContextSerializer::class)
    val presentedOfferingContext: PresentedOfferingContext? = null,
    val price: Double? = null,
    val formattedPrice: String? = null,
    val currency: String? = null,
    @Serializable(with = PeriodSerializer::class)
    val period: Period? = null,
    val pricingPhases: List<
        @Serializable(with = PricingPhaseSerializer::class)
        PricingPhase,
        >? = null,
    @Serializable(with = ReplacementModeSerializer::class)
    val replacementMode: ReplacementMode? = null,
    val platformProductIds: List<Map<String, String?>> = emptyList(),
    val sdkOriginated: Boolean = false,
    // Amazon-only fields
    val storeUserID: String? = null,
    val marketplace: String? = null,
) {
    companion object {
        @OptIn(InternalRevenueCatAPI::class)
        fun from(
            storeTransaction: StoreTransaction,
            storeProduct: StoreProduct?,
            subscriptionOptionsForProductIDs: Map<String, SubscriptionOption>?,
            sdkOriginated: Boolean = false,
        ): ReceiptInfo {
            val subscriptionOption = storeProduct?.subscriptionOptions
                ?.firstOrNull { it.id == storeTransaction.subscriptionOptionId }

            val storeProductPlatformProductId =
                subscriptionOption?.platformProductId() ?: storeProduct?.platformProductId()

            // ReceiptInfo can be created with only productIDs but it might also be created with
            // a StoreProduct
            // We want the PlatformProductID with most info (like GooglePlatformProductId from a SubscriptionOption)
            // so this logic prevents duplicate productIds (PlatformProductID) from being returned
            //
            // To simplify backend processing when handling a subscription purchase with add-ons,
            // we want to use the same order as the products returned from purchase, so that the base item
            // is first in the list.
            val platformProductIds = storeTransaction.productIds
                .map { productId ->
                    if (productId == storeProductPlatformProductId?.productId) {
                        storeProductPlatformProductId.asMap
                    } else {
                        subscriptionOptionsForProductIDs
                            ?.get(productId)?.platformProductId()?.asMap
                            ?: PlatformProductId(productId).asMap
                    }
                }

            return ReceiptInfo(
                productIDs = storeTransaction.productIds,
                purchaseTime = storeTransaction.purchaseTime,
                presentedOfferingContext = storeTransaction.presentedOfferingContext,
                price = storeProduct?.price?.amountMicros?.div(SharedConstants.MICRO_MULTIPLIER),
                formattedPrice = storeProduct?.price?.formatted,
                currency = storeProduct?.price?.currencyCode,
                period = storeProduct?.period,
                pricingPhases = subscriptionOption?.pricingPhases,
                replacementMode = storeTransaction.replacementMode,
                platformProductIds = platformProductIds,
                sdkOriginated = sdkOriginated,
                storeUserID = storeTransaction.storeUserID,
                marketplace = storeTransaction.marketplace,
            )
        }
    }

    @IgnoredOnParcel
    val duration: String? = period?.iso8601?.takeUnless { it.isEmpty() }
}

private fun StoreProduct.platformProductId(): PlatformProductId {
    return PlatformProductId(id)
}

private fun SubscriptionOption.platformProductId(): PlatformProductId? {
    return when (this) {
        is GoogleSubscriptionOption -> GooglePlatformProductId(
            productId,
            basePlanId,
            offerId,
        )
        else -> null
    }
}

private open class PlatformProductId(open val productId: String) {
    open val asMap: Map<String, String?>
        get() = mapOf(
            "product_id" to productId,
        )
}

private class GooglePlatformProductId(
    override val productId: String,
    val basePlanId: String? = null,
    val offerId: String? = null,
) : PlatformProductId(productId) {
    override val asMap: Map<String, String?>
        get() = mapOf(
            "product_id" to productId,
            "base_plan_id" to basePlanId,
            "offer_id" to offerId,
        )
}
