package com.revenuecat.purchases.ui.revenuecatui.data

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ProductType
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

internal sealed interface ProductChangeResult {
    data class Change(val info: ProductChangeInfo) : ProductChangeResult

    object NoChange : ProductChangeResult

    object AlreadySubscribed : ProductChangeResult
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
    @Suppress("ReturnCount")
    suspend fun calculateProductChangeInfo(
        packageToPurchase: Package,
        productChangeConfig: ProductChangeConfig,
    ): ProductChangeResult {
        if (packageToPurchase.product.type != ProductType.SUBS) {
            return ProductChangeResult.NoChange
        }

        val customerInfo = try {
            purchases.awaitCustomerInfo()
        } catch (e: PurchasesException) {
            Logger.e("Error fetching customer info to determine product change: ${e.message}")
            return ProductChangeResult.NoChange
        }

        val activePlayStoreSubscription = customerInfo.subscriptionsByProductIdentifier.values
            .firstOrNull { subscriptionInfo ->
                subscriptionInfo.isActive && subscriptionInfo.store == Store.PLAY_STORE
            } ?: return ProductChangeResult.NoChange

        return calculateProductChange(
            activePlayStoreSubscription = activePlayStoreSubscription,
            packageToPurchase = packageToPurchase,
            productChangeConfig = productChangeConfig,
        )
    }

    @Suppress("ReturnCount")
    private suspend fun calculateProductChange(
        activePlayStoreSubscription: SubscriptionInfo,
        packageToPurchase: Package,
        productChangeConfig: ProductChangeConfig,
    ): ProductChangeResult {
        val oldSubscriptionId = activePlayStoreSubscription.productIdentifier
        val oldBasePlanId = activePlayStoreSubscription.productPlanIdentifier

        val (newSubscriptionId, newBasePlanId) = packageToPurchase.product.subscriptionIdentifiers()

        if (oldSubscriptionId == newSubscriptionId) {
            if (oldBasePlanId == newBasePlanId) {
                // The user already actively owns this exact subscription (same product and base
                // plan). Launching the billing flow would fail with ITEM_ALREADY_OWNED.
                Logger.d("Already subscribed to $newSubscriptionId ($newBasePlanId)")
                return ProductChangeResult.AlreadySubscribed
            }
            // Same product, different base plan: Google handles the base plan change automatically.
            Logger.d("Same product ($newSubscriptionId), Google handles base plan change automatically")
            return ProductChangeResult.NoChange
        }

        val oldProduct = try {
            purchases.awaitGetProduct(oldSubscriptionId, oldBasePlanId)
        } catch (e: PurchasesException) {
            Logger.e("Error fetching currently subscribed product to determine product change: ${e.message}")
            return ProductChangeResult.NoChange
        }
        val isSandbox = activePlayStoreSubscription.isSandbox

        val oldNormalizedPrice = oldProduct?.getNormalizedPrice(isSandbox)
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

        return ProductChangeResult.Change(
            ProductChangeInfo(
                oldProductId = oldSubscriptionId,
                replacementMode = replacementMode,
            ),
        )
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
