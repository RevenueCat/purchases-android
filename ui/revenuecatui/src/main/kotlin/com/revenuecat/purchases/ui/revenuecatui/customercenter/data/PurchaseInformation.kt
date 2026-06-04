@file:Suppress("TooManyFunctions")

package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import android.net.Uri
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.TransactionDetails
import com.revenuecat.purchases.ui.revenuecatui.utils.DateFormatter
import com.revenuecat.purchases.ui.revenuecatui.utils.DefaultDateFormatter
import java.util.Locale

@Suppress("LongParameterList")
internal data class PurchaseInformation(
    val title: String?,
    val pricePaid: PriceDetails,
    val expirationOrRenewal: ExpirationOrRenewal?,
    val product: StoreProduct?,
    val store: Store,
    /**
     * Indicates whether the purchase is a subscription.
     * This is false for non-subscription products, such as one-time purchases.
     * This is false for promotionals, as they are not considered subscriptions.
     */
    var isSubscription: Boolean,
    val managementURL: Uri?,
    val isExpired: Boolean,
    val isTrial: Boolean,
    /**
     * Indicates whether the subscription has been cancelled.
     * This is true if the user has unsubscribed. This is always true for promotional products.
     * This does not mean that the subscription is expired yet, as the user may still have access
     * until the end of the billing period.
     */
    val isCancelled: Boolean,
    /**
     * Indicates whether this is a lifetime purchase.
     * This is true for promotional lifetime purchases or non-subscription purchases attached to an entitlement.
     */
    val isLifetime: Boolean,
) {

    constructor(
        entitlementInfo: EntitlementInfo? = null,
        subscribedProduct: StoreProduct? = null,
        transaction: TransactionDetails,
        dateFormatter: DateFormatter = DefaultDateFormatter(),
        locale: Locale,
        localization: CustomerCenterConfigData.Localization,
    ) : this(
        title = determineTitle(entitlementInfo, subscribedProduct, transaction, localization),
        expirationOrRenewal = determineExpirationOrRenewal(entitlementInfo, transaction, dateFormatter, locale),
        product = subscribedProduct,
        store = entitlementInfo?.store ?: transaction.store,
        pricePaid = determinePrice(subscribedProduct, transaction),
        isSubscription = transaction is TransactionDetails.Subscription && transaction.store != Store.PROMOTIONAL,
        managementURL = (transaction as? TransactionDetails.Subscription)?.managementURL,
        isExpired = entitlementInfo?.isActive?.let { !it }
            ?: when (transaction) {
                is TransactionDetails.Subscription -> !transaction.isActive
                is TransactionDetails.NonSubscription -> false
            },
        isTrial = determineTrialStatus(entitlementInfo, transaction),
        isCancelled = determineCancellationStatus(entitlementInfo, transaction),
        isLifetime = determineLifetimeStatus(entitlementInfo, transaction),
    )

    fun renewalString(
        renewalDate: String,
        localization: CustomerCenterConfigData.Localization,
    ): String {
        return when (pricePaid) {
            PriceDetails.Free, PriceDetails.Unknown -> localization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.RENEWS_ON_DATE,
            ).replace("{{ date }}", renewalDate)
            is PriceDetails.Paid -> {
                val lastChargeText = localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.LAST_CHARGE_WAS,
                ).replace("{{ price }}", pricePaid.price)
                val nextBillingText = localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.NEXT_BILLING_DATE_ON,
                ).replace("{{ date }}", renewalDate)
                "$lastChargeText\n$nextBillingText"
            }
        }
    }

    fun expirationString(
        expirationDate: String,
        localization: CustomerCenterConfigData.Localization,
    ): String {
        return if (isExpired) {
            localization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.PURCHASE_INFO_EXPIRED_ON_DATE,
            ).replace("{{ date }}", expirationDate)
        } else {
            localization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.PURCHASE_INFO_EXPIRES_ON_DATE,
            ).replace("{{ date }}", expirationDate)
        }
    }
}

private fun determinePrice(
    subscribedProduct: StoreProduct?,
    transaction: TransactionDetails,
): PriceDetails {
    return when {
        transaction.store == Store.PROMOTIONAL || transaction.price?.amountMicros == 0L -> PriceDetails.Free

        transaction.price?.amountMicros?.let { it > 0L } == true -> {
            transaction.price?.let { PriceDetails.Paid(it.formatted) } ?: PriceDetails.Unknown
        }

        subscribedProduct != null -> {
            if (subscribedProduct.price.amountMicros == 0L) {
                PriceDetails.Free
            } else {
                PriceDetails.Paid(subscribedProduct.price.formatted)
            }
        }

        else -> PriceDetails.Unknown
    }
}

private fun determineTitle(
    entitlementInfo: EntitlementInfo?,
    subscribedProduct: StoreProduct?,
    transaction: TransactionDetails,
    localization: CustomerCenterConfigData.Localization,
): String {
    if (transaction.store == Store.PROMOTIONAL && entitlementInfo != null) {
        return entitlementInfo.identifier
    }

    return subscribedProduct?.title
        ?: when (transaction) {
            is TransactionDetails.Subscription ->
                localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.TYPE_SUBSCRIPTION,
                )
            is TransactionDetails.NonSubscription ->
                localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.TYPE_ONE_TIME_PURCHASE,
                )
        }
}

private fun EntitlementInfo.expirationDate(dateFormatter: DateFormatter, locale: Locale): String? {
    if (!isPromotionalLifetime() && (!willRenew || !isActive)) {
        expirationDate?.let { expirationDate ->
            return dateFormatter.format(expirationDate, locale)
        }
    }
    return null
}

private fun TransactionDetails.expirationDate(dateFormatter: DateFormatter, locale: Locale): String? {
    val hasAnExpiration = this is TransactionDetails.Subscription && expiresDate != null
    val isExpiringOrExpired = hasAnExpiration &&
        (this as TransactionDetails.Subscription).isExpiringOrExpired()

    return if (isExpiringOrExpired) {
        dateFormatter.format((this as TransactionDetails.Subscription).expiresDate!!, locale)
    } else {
        null
    }
}

private fun TransactionDetails.Subscription.isExpiringOrExpired(): Boolean {
    return !willRenew || !isActive
}

private fun EntitlementInfo.renewalDate(dateFormatter: DateFormatter, locale: Locale): String? {
    if (willRenew && !isPromotionalLifetime()) {
        expirationDate?.let { expirationDate ->
            return dateFormatter.format(expirationDate, locale)
        }
    }
    return null
}

private fun TransactionDetails.renewalDate(dateFormatter: DateFormatter, locale: Locale): String? {
    if (this is TransactionDetails.Subscription && willRenew && expiresDate != null) {
        return dateFormatter.format(expiresDate, locale)
    }
    return null
}

private fun EntitlementInfo.isPromotionalLifetime(): Boolean {
    return store == Store.PROMOTIONAL && productIdentifier.endsWith("_lifetime")
}

private fun determineTrialStatus(
    entitlementInfo: EntitlementInfo?,
    transaction: TransactionDetails,
): Boolean {
    return entitlementInfo?.periodType == PeriodType.TRIAL ||
        (transaction as? TransactionDetails.Subscription)?.isTrial == true
}

private fun determineCancellationStatus(
    entitlementInfo: EntitlementInfo?,
    transaction: TransactionDetails,
): Boolean {
    val entitlementCancelled = entitlementInfo?.let {
        it.unsubscribeDetectedAt != null && !it.willRenew
    } ?: false

    val transactionCancelled = (transaction as? TransactionDetails.Subscription)?.let {
        !it.willRenew
    } ?: false

    return entitlementCancelled || transactionCancelled
}

private fun determineLifetimeStatus(
    entitlementInfo: EntitlementInfo?,
    transaction: TransactionDetails,
): Boolean {
    val isPromotionalLifetime = entitlementInfo?.isPromotionalLifetime() == true

    val isNonSubscriptionWithEntitlement = transaction !is TransactionDetails.Subscription &&
        transaction.store != Store.PROMOTIONAL &&
        entitlementInfo != null

    return isPromotionalLifetime || isNonSubscriptionWithEntitlement
}

internal sealed class PriceDetails {
    object Free : PriceDetails()
    data class Paid(val price: String) : PriceDetails()
    object Unknown : PriceDetails()
}

internal sealed class ExpirationOrRenewal {
    data class Expiration(val date: String) : ExpirationOrRenewal()
    data class Renewal(val date: String) : ExpirationOrRenewal()
}

private fun determineExpirationOrRenewal(
    entitlementInfo: EntitlementInfo?,
    transaction: TransactionDetails,
    dateFormatter: DateFormatter,
    locale: Locale,
): ExpirationOrRenewal? {
    // First try to get a date from entitlement info
    val entitlementDate =
        entitlementInfo?.renewalDate(dateFormatter, locale)?.let { ExpirationOrRenewal.Renewal(it) }
            ?: entitlementInfo?.expirationDate(dateFormatter, locale)?.let { ExpirationOrRenewal.Expiration(it) }

    // If no entitlement date, try to get a date from transaction
    val transactionDate = transaction.let { tx ->
        tx.renewalDate(dateFormatter, locale)?.let { ExpirationOrRenewal.Renewal(it) }
            ?: tx.expirationDate(dateFormatter, locale)?.let { ExpirationOrRenewal.Expiration(it) }
    }

    return entitlementDate ?: transactionDate
}
