package com.revenuecat.purchases.ui.revenuecatui.data

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.SubscriptionInfo
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreReplacementMode
import com.revenuecat.purchases.paywalls.components.common.ProductChangeConfig
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

internal data class ProductChangeInfo(
    val oldProductId: String,
    val replacementMode: StoreReplacementMode,
)

private sealed interface ProductLookupResult {
    data class Success(val product: StoreProduct?) : ProductLookupResult
    object Error : ProductLookupResult
}

/**
 * Calculates product change information for subscription upgrades/downgrades.
 *
 * Limitations:
 * - Only supports users with a single active Play Store subscription. If a user has multiple
 *   active subscriptions (e.g., subscribed to both Product A and Product B), the behavior is
 *   undefined as we simply pick the first active subscription found.
 */
internal class ProductChangeCalculator(
    private val purchases: PurchasesType,
) {
    suspend fun calculateProductChangeInfo(
        packageToPurchase: Package,
        productChangeConfig: ProductChangeConfig,
    ): ProductChangeInfo? {
        return if (packageToPurchase.product.type != ProductType.SUBS) {
            null
        } else {
            val customerInfo = try {
                purchases.awaitCustomerInfo()
            } catch (e: PurchasesException) {
                Logger.e("Error determining product change info: ${e.message}")
                null
            }

            customerInfo?.subscriptionsByProductIdentifier?.values
                ?.firstOrNull { subscriptionInfo ->
                    subscriptionInfo.isActive && subscriptionInfo.store == Store.PLAY_STORE
                }?.let { activePlayStoreSubscription ->
                    calculateProductChange(
                        activePlayStoreSubscription = activePlayStoreSubscription,
                        packageToPurchase = packageToPurchase,
                        productChangeConfig = productChangeConfig,
                    )
                }
        }
    }

    private suspend fun calculateProductChange(
        activePlayStoreSubscription: SubscriptionInfo,
        packageToPurchase: Package,
        productChangeConfig: ProductChangeConfig,
    ): ProductChangeInfo? {
        val oldSubscriptionId = activePlayStoreSubscription.productIdentifier
        val oldBasePlanId = activePlayStoreSubscription.productPlanIdentifier

        val (newSubscriptionId, newBasePlanId) = packageToPurchase.product.subscriptionIdentifiers()

        return if (oldSubscriptionId == newSubscriptionId) {
            if (oldBasePlanId == newBasePlanId) {
                // The user already actively owns this exact subscription (same product and base
                // plan). Launching the billing flow would fail with ITEM_ALREADY_OWNED, so we
                // surface the error to the developer instead.
                Logger.d("Already subscribed to $newSubscriptionId ($newBasePlanId)")
                throw PurchasesException(
                    PurchasesError(
                        PurchasesErrorCode.ProductAlreadyPurchasedError,
                        "This product is already active for the user.",
                    ),
                )
            }
            // Same product, different base plan: Google handles the base plan change automatically.
            Logger.d("Same product ($newSubscriptionId), Google handles base plan change automatically")
            null
        } else {
            val oldProductLookup = try {
                ProductLookupResult.Success(purchases.awaitGetProduct(oldSubscriptionId, oldBasePlanId))
            } catch (e: PurchasesException) {
                Logger.e("Error determining product change info: ${e.message}")
                ProductLookupResult.Error
            }
            val isSandbox = activePlayStoreSubscription.isSandbox

            when (oldProductLookup) {
                ProductLookupResult.Error -> null
                is ProductLookupResult.Success -> {
                    val oldNormalizedPrice = oldProductLookup.product?.getNormalizedPrice(isSandbox)
                    val newNormalizedPrice = packageToPurchase.product.getNormalizedPrice(isSandbox)

                    val isUpgrade = oldNormalizedPrice != null &&
                        newNormalizedPrice != null &&
                        newNormalizedPrice > oldNormalizedPrice

                    val replacementMode = if (isUpgrade) {
                        Logger.d(
                            "Detected upgrade: $oldSubscriptionId -> $newSubscriptionId " +
                                "(old: $oldNormalizedPrice, new: $newNormalizedPrice, sandbox: $isSandbox)",
                        )
                        productChangeConfig.upgradeReplacementMode
                    } else {
                        Logger.d(
                            "Detected downgrade: $oldSubscriptionId -> $newSubscriptionId " +
                                "(old: $oldNormalizedPrice, new: $newNormalizedPrice, sandbox: $isSandbox)",
                        )
                        productChangeConfig.downgradeReplacementMode
                    }

                    ProductChangeInfo(
                        oldProductId = oldSubscriptionId,
                        replacementMode = replacementMode,
                    )
                }
            }
        }
    }

    companion object {
        private const val MONTHS_IN_YEAR = 12
        private const val MONTHS_IN_HALF_YEAR = 6
        private const val MONTHS_IN_QUARTER = 3
        private const val SANDBOX_YEARLY_MINUTES = 30L
        private const val SANDBOX_HALF_YEAR_MINUTES = 15L
        private const val SANDBOX_QUARTER_MINUTES = 10L
        private const val SANDBOX_MONTHLY_MINUTES = 5L

        internal fun parseProductIdentifier(productIdentifier: String): Pair<String, String?> {
            val subscriptionId = productIdentifier.substringBefore(":")
            val basePlanId = productIdentifier.substringAfter(":", "")
                .takeIf { it.isNotEmpty() }
            return subscriptionId to basePlanId
        }

        internal fun StoreProduct.getNormalizedPrice(isSandbox: Boolean): Long? {
            val period = this.period ?: return null
            val priceAmountMicros = this.price.amountMicros
            return if (isSandbox) {
                val sandboxMinutes = getSandboxRenewalMinutes(period)
                priceAmountMicros / sandboxMinutes
            } else {
                pricePerMonth()?.amountMicros
            }
        }

        @Suppress("MagicNumber")
        internal fun getSandboxRenewalMinutes(period: Period): Long {
            val totalMonths = period.valueInMonths
            return when {
                totalMonths >= MONTHS_IN_YEAR -> SANDBOX_YEARLY_MINUTES
                totalMonths >= MONTHS_IN_HALF_YEAR -> SANDBOX_HALF_YEAR_MINUTES
                totalMonths >= MONTHS_IN_QUARTER -> SANDBOX_QUARTER_MINUTES
                else -> SANDBOX_MONTHLY_MINUTES
            }
        }

        internal fun StoreProduct.subscriptionIdentifiers(): Pair<String, String?> {
            return when (this) {
                is GoogleStoreProduct -> this.productId to this.basePlanId
                else -> parseProductIdentifier(this.id)
            }
        }
    }
}
