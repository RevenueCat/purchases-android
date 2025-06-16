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
    var isLifetime: Boolean,
    val managementURL: Uri?,
    val isActive: Boolean,
    val isTrial: Boolean,
    val isCancelled: Boolean,
) {

    constructor(
        entitlementInfo: EntitlementInfo? = null,
        subscribedProduct: StoreProduct? = null,
        transaction: TransactionDetails,
        managementURL: Uri?,
        dateFormatter: DateFormatter = DefaultDateFormatter(),
        locale: Locale,
    ) : this(
        title = subscribedProduct?.title ?: transaction.productIdentifier,
        expirationOrRenewal = determineExpirationOrRenewal(entitlementInfo, transaction, dateFormatter, locale),
        product = subscribedProduct,
        store = entitlementInfo?.store ?: transaction.store,
        pricePaid = entitlementInfo?.priceBestEffort(subscribedProduct) ?: if (transaction.store == Store.PROMOTIONAL) {
            PriceDetails.Free
        } else {
            subscribedProduct?.let { PriceDetails.Paid(it.price.formatted) } ?: PriceDetails.Unknown
        },
        isLifetime = entitlementInfo?.let {
            it.expirationDate == null
        } ?: when (transaction) {
            is TransactionDetails.Subscription -> {
                false
            }

            is TransactionDetails.NonSubscription ->
                true
        },
        managementURL = managementURL,
        isActive = entitlementInfo?.isActive ?: when (transaction) {
            is TransactionDetails.Subscription -> transaction.isActive
            is TransactionDetails.NonSubscription -> true
        },
        isTrial = determineTrialStatus(entitlementInfo, transaction),
        isCancelled = determineCancellationStatus(entitlementInfo, transaction),
    )

    fun renewalString(
        renewalDate: String,
        localization: CustomerCenterConfigData.Localization,
    ): String {
        return when (pricePaid) {
            PriceDetails.Free, PriceDetails.Unknown -> localization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.RENEWS_ON_DATE,
            ).replace("{{ date }}", renewalDate)
            is PriceDetails.Paid -> localization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.RENEWS_ON_DATE_FOR_PRICE,
            ).replace("{{ date }}", renewalDate).replace("{{ price }}", pricePaid.price)
        }
    }

    fun expirationString(
        expirationDate: String,
        localization: CustomerCenterConfigData.Localization,
    ): String {
        return if (isActive) {
            localization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.PURCHASE_INFO_EXPIRES_ON_DATE,
            ).replace("{{ date }}", expirationDate)
        } else {
            localization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.PURCHASE_INFO_EXPIRED_ON_DATE,
            ).replace("{{ date }}", expirationDate)
        }
    }
}

private fun EntitlementInfo.priceBestEffort(subscribedProduct: StoreProduct?): PriceDetails {
    return subscribedProduct?.let {
        PriceDetails.Paid(it.price.formatted)
    } ?: if (store == Store.PROMOTIONAL) {
        PriceDetails.Free
    } else {
        PriceDetails.Unknown
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
