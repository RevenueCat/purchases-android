package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import android.net.Uri
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.TransactionDetails
import com.revenuecat.purchases.ui.revenuecatui.extensions.localizedUnitPeriod
import com.revenuecat.purchases.ui.revenuecatui.utils.DateFormatter
import com.revenuecat.purchases.ui.revenuecatui.utils.DefaultDateFormatter
import java.util.Locale

@SuppressWarnings("LongParameterList")
internal class PurchaseInformation(
    val title: String?,
    val durationTitle: String?,
    val explanation: Explanation,
    val pricePaid: PriceDetails,
    val renewalDate: String?,
    val expirationDate: String?,
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
        durationTitle = subscribedProduct?.period?.localizedUnitPeriod(locale)?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
        },
        explanation = entitlementInfo?.explanation() ?: when (transaction) {
            is TransactionDetails.Subscription -> {
                if (transaction.expiresDate != null) {
                    if (transaction.isActive) {
                        if (transaction.willRenew) Explanation.EARLIEST_RENEWAL else Explanation.EARLIEST_EXPIRATION
                    } else {
                        Explanation.EXPIRED
                    }
                } else {
                    Explanation.LIFETIME
                }
            }

            is TransactionDetails.NonSubscription -> Explanation.LIFETIME
        },
        renewalDate =
        entitlementInfo?.renewalDate(dateFormatter, locale) ?: transaction.renewalDate(dateFormatter, locale),
        expirationDate = entitlementInfo?.expirationDate(dateFormatter, locale) ?: transaction.expirationDate(dateFormatter, locale),
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
            is TransactionDetails.NonSubscription -> false
        },
        isTrial = entitlementInfo?.periodType == PeriodType.TRIAL ||
            (transaction as? TransactionDetails.Subscription)?.isTrial == true,
        isCancelled = (entitlementInfo?.unsubscribeDetectedAt != null && !entitlementInfo.willRenew) ||
            (transaction as? TransactionDetails.Subscription)?.let { !it.willRenew } == true,
    )

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
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

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
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

private fun EntitlementInfo.explanation(): Explanation {
    return when (store) {
        Store.APP_STORE, Store.MAC_APP_STORE -> Explanation.APPLE
        Store.PLAY_STORE -> explanationForPlayStore()
        Store.STRIPE, Store.RC_BILLING, Store.PADDLE -> Explanation.WEB
        Store.PROMOTIONAL -> Explanation.PROMOTIONAL
        Store.EXTERNAL, Store.UNKNOWN_STORE -> Explanation.OTHER_STORE_PURCHASE
        Store.AMAZON -> Explanation.AMAZON
    }
}

private fun EntitlementInfo.explanationForPlayStore(): Explanation {
    return when {
        expirationDate == null -> Explanation.LIFETIME
        isActive -> if (willRenew) Explanation.EARLIEST_RENEWAL else Explanation.EARLIEST_EXPIRATION
        else -> Explanation.EXPIRED
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
    if (this is TransactionDetails.Subscription && expiresDate != null && (!willRenew || !isActive)) {
        return dateFormatter.format(expiresDate, locale)
    }
    return null
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

internal sealed class PriceDetails {
    object Free : PriceDetails()
    data class Paid(val price: String) : PriceDetails()
    object Unknown : PriceDetails()
}

internal enum class Explanation {
    APPLE,
    PROMOTIONAL,
    WEB,
    OTHER_STORE_PURCHASE,
    AMAZON,
    EARLIEST_RENEWAL,
    EARLIEST_EXPIRATION,
    EXPIRED,
    LIFETIME,
}
