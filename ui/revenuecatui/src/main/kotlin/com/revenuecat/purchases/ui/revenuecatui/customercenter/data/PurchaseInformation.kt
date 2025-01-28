package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import android.net.Uri
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.Store
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
    val price: PriceDetails,
    val expirationOrRenewal: ExpirationOrRenewal?,
    val product: StoreProduct,
    val store: Store,
    val managementURL: Uri?,
) {

    constructor(
        entitlementInfo: EntitlementInfo? = null,
        subscribedProduct: StoreProduct? = null,
        transaction: TransactionDetails,
        managementURL: Uri?,
        dateFormatter: DateFormatter = DefaultDateFormatter(),
        locale: Locale,
    ) : this(
        title = subscribedProduct?.title,
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
        expirationOrRenewal = entitlementInfo?.expirationOrRenewal(dateFormatter, locale) ?: when (transaction) {
            is TransactionDetails.Subscription -> {
                transaction.expiresDate?.let { date ->
                    val dateString = dateFormatter.format(date, locale)
                    val label = if (transaction.isActive) {
                        if (transaction.willRenew) {
                            ExpirationOrRenewal.Label.NEXT_BILLING_DATE
                        } else {
                            ExpirationOrRenewal.Label.EXPIRES
                        }
                    } else {
                        ExpirationOrRenewal.Label.EXPIRED
                    }
                    ExpirationOrRenewal(label, ExpirationOrRenewal.Date.DateString(dateString))
                }
            }

            is TransactionDetails.NonSubscription ->
                ExpirationOrRenewal(ExpirationOrRenewal.Label.EXPIRES, ExpirationOrRenewal.Date.Never)
        },
        productIdentifier = entitlementInfo?.productIdentifier ?: transaction.productIdentifier,
        store = entitlementInfo?.store ?: transaction.store,
        price = entitlementInfo?.priceBestEffort(subscribedProduct) ?: if (transaction.store == Store.PROMOTIONAL) {
            PriceDetails.Free
        } else {
            subscribedProduct?.let { PriceDetails.Paid(it.price.formatted) } ?: PriceDetails.Unknown
        },
        managementURL = managementURL,
    )
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
        Store.STRIPE, Store.RC_BILLING -> Explanation.WEB
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

private fun EntitlementInfo.expirationOrRenewal(dateFormatter: DateFormatter, locale: Locale): ExpirationOrRenewal? {
    val date = expirationDateBestEffort(dateFormatter, locale)
    val label = if (isActive) {
        if (willRenew) ExpirationOrRenewal.Label.NEXT_BILLING_DATE else ExpirationOrRenewal.Label.EXPIRES
    } else {
        ExpirationOrRenewal.Label.EXPIRED
    }
    return ExpirationOrRenewal(label, date)
}

private fun EntitlementInfo.expirationDateBestEffort(
    dateFormatter: DateFormatter,
    locale: Locale,
): ExpirationOrRenewal.Date {
    return expirationDate?.let { expirationDate ->
        if (store == Store.PROMOTIONAL && productIdentifier.isPromotionalLifetime(store)) {
            ExpirationOrRenewal.Date.Never
        } else {
            ExpirationOrRenewal.Date.DateString(dateFormatter.format(expirationDate, locale))
        }
    } ?: ExpirationOrRenewal.Date.Never
}

private fun String.isPromotionalLifetime(store: Store): Boolean {
    return store == Store.PROMOTIONAL && this.endsWith("_lifetime")
}

internal data class ExpirationOrRenewal(
    val label: Label,
    val date: Date,
) {
    enum class Label {
        NEXT_BILLING_DATE,
        EXPIRES,
        EXPIRED,
    }

    sealed class Date {
        object Never : Date()
        data class DateString(val date: String) : Date()
    }
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
