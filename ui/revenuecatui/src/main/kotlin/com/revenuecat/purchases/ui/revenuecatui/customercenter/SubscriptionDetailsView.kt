package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.Localization.CommonLocalizedString
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.ExpirationOrRenewal
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.Explanation
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PriceDetails
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation
import com.revenuecat.purchases.ui.revenuecatui.icons.CalendarMonth
import com.revenuecat.purchases.ui.revenuecatui.icons.CurrencyExchange
import com.revenuecat.purchases.ui.revenuecatui.icons.UniversalCurrencyAlt

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
internal fun SubscriptionDetailsView(
    details: PurchaseInformation,
    localization: CustomerCenterConfigData.Localization,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .padding(all = PaddingContent),
        ) {
            details.title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            val explanation = remember { getSubscriptionExplanation(details, localization) }

            Text(
                text = explanation,
                color = LocalContentColor.current.copy(alpha = AlphaSecondaryText),
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(modifier = Modifier.size(PaddingVertical))

            HorizontalDivider()

            details.durationTitle?.let {
                Spacer(modifier = Modifier.size(PaddingVertical))

                SubscriptionDetailRow(
                    icon = CurrencyExchange,
                    overline = localization.commonLocalizedString(CommonLocalizedString.BILLING_CYCLE),
                    text = it,
                )
            }

            Spacer(modifier = Modifier.size(PaddingVertical))

            val price = remember { getPrice(details, localization) }

            price?.let {
                SubscriptionDetailRow(
                    icon = UniversalCurrencyAlt,
                    overline = localization.commonLocalizedString(CommonLocalizedString.CURRENT_PRICE),
                    text = it,
                )
            }

            details.expirationOrRenewal?.let { expirationOrRenewal ->
                val expirationValue = remember { getExpirationValue(expirationOrRenewal, localization) }
                val expirationOverline = remember { labelForExpirationOrRenewal(expirationOrRenewal, localization) }

                Spacer(modifier = Modifier.size(PaddingVertical))

                SubscriptionDetailRow(
                    icon = CalendarMonth,
                    overline = expirationOverline,
                    text = expirationValue,
                )
            }
        }
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
private fun getExpirationValue(
    expirationOrRenewal: ExpirationOrRenewal,
    localization: CustomerCenterConfigData.Localization,
): String {
    val expirationValue =
        when (expirationOrRenewal.date) {
            is ExpirationOrRenewal.Date.DateString -> expirationOrRenewal.date.date
            ExpirationOrRenewal.Date.Never -> localization.commonLocalizedString(
                CommonLocalizedString.NEVER,
            )
        }
    return expirationValue
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
private fun labelForExpirationOrRenewal(
    expirationOrRenewal: ExpirationOrRenewal,
    localization: CustomerCenterConfigData.Localization,
): String {
    val stringKey = when (expirationOrRenewal.label) {
        ExpirationOrRenewal.Label.EXPIRES -> CommonLocalizedString.EXPIRES
        ExpirationOrRenewal.Label.EXPIRED -> CommonLocalizedString.EXPIRED
        ExpirationOrRenewal.Label.NEXT_BILLING_DATE -> CommonLocalizedString.NEXT_BILLING_DATE
    }
    return localization.commonLocalizedString(stringKey)
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
private fun getPrice(
    purchaseInformation: PurchaseInformation,
    localization: CustomerCenterConfigData.Localization,
): String? {
    return when (purchaseInformation.price) {
        PriceDetails.Free -> localization.commonLocalizedString(CommonLocalizedString.FREE)
        is PriceDetails.Paid -> purchaseInformation.price.price
        PriceDetails.Unknown -> null
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
private fun getSubscriptionExplanation(
    purchaseInformation: PurchaseInformation,
    localization: CustomerCenterConfigData.Localization,
): String {
    val stringKey = when (purchaseInformation.explanation) {
        Explanation.APPLE -> CommonLocalizedString.APPLE_SUBSCRIPTION_MANAGE
        Explanation.PROMOTIONAL -> CommonLocalizedString.YOU_HAVE_PROMO
        Explanation.WEB -> CommonLocalizedString.WEB_SUBSCRIPTION_MANAGE
        Explanation.OTHER_STORE_PURCHASE -> CommonLocalizedString.PLEASE_CONTACT_SUPPORT
        Explanation.AMAZON -> CommonLocalizedString.AMAZON_SUBSCRIPTION_MANAGE
        Explanation.EARLIEST_RENEWAL -> CommonLocalizedString.SUB_EARLIEST_RENEWAL
        Explanation.EARLIEST_EXPIRATION -> CommonLocalizedString.SUB_EARLIEST_EXPIRATION
        Explanation.EXPIRED -> CommonLocalizedString.SUB_EXPIRED
        Explanation.LIFETIME -> CommonLocalizedString.YOU_HAVE_LIFETIME
    }
    return localization.commonLocalizedString(stringKey)
}

@Composable
private fun SubscriptionDetailRow(
    icon: ImageVector,
    overline: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size((LocalDensity.current.fontScale * SizeIconDp).dp),
        )

        Spacer(modifier = Modifier.size(PaddingHorizontal))

        Column {
            Text(
                text = overline,
                color = LocalContentColor.current.copy(alpha = AlphaSecondaryText),
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private const val AlphaSecondaryText = 0.6f
private val PaddingContent = 16.dp
private val PaddingHorizontal = 8.dp
private val PaddingVertical = 8.dp
private const val SizeIconDp = 22

private class SubscriptionInformationProvider : PreviewParameterProvider<PurchaseInformation> {
    override val values: Sequence<PurchaseInformation> = sequenceOf(
        PurchaseInformation(
            title = "Basic",
            durationTitle = "Monthly",
            productIdentifier = "basic_monthly",
            price = PriceDetails.Paid("$4.99"),
            explanation = Explanation.EARLIEST_RENEWAL,
            expirationOrRenewal =
            ExpirationOrRenewal(
                ExpirationOrRenewal.Label.NEXT_BILLING_DATE,
                ExpirationOrRenewal.Date.DateString("June 1st, 2024"),
            ),
            store = Store.PLAY_STORE,
        ),
        PurchaseInformation(
            title = "Basic",
            durationTitle = "Yearly",
            productIdentifier = "basic_yearly",
            price = PriceDetails.Paid("$49.99"),
            explanation = Explanation.EARLIEST_EXPIRATION,
            expirationOrRenewal =
            ExpirationOrRenewal(
                ExpirationOrRenewal.Label.EXPIRED,
                ExpirationOrRenewal.Date.DateString("June 1st, 2024"),
            ),
            store = Store.PLAY_STORE,
        ),
        PurchaseInformation(
            title = "Basic",
            durationTitle = "Weekly",
            productIdentifier = "basic_weekly",
            price = PriceDetails.Paid("$1.99"),
            explanation = Explanation.EXPIRED,
            expirationOrRenewal =
            ExpirationOrRenewal(
                ExpirationOrRenewal.Label.EXPIRED,
                ExpirationOrRenewal.Date.DateString("June 1st, 2024"),
            ),
            store = Store.PLAY_STORE,
        ),
    )
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Preview(group = "scale = 1", fontScale = 1F)
// Unrealistically long device to make the Column fit. Can be removed once Emerge Snapshot Test supports
// @PreviewParameter.
@Preview(group = "scale = 2", fontScale = 2F, device = "spec:width=1080px,height=4720px,dpi=440")
@Composable
internal fun SubscriptionDetailsView_Preview() {
    Column(
        modifier = Modifier.height(2000.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Bit of a roundabout way of using the PreviewParameterProvider, because Emerge Snapshot Test doesn't support
        // it yet.
        SubscriptionInformationProvider().values.forEach { details ->
            SubscriptionDetailsView(
                details = details,
                localization =
                CustomerCenterConfigTestData.customerCenterData(shouldWarnCustomerToUpdate = false).localization,
            )
        }
    }
}
