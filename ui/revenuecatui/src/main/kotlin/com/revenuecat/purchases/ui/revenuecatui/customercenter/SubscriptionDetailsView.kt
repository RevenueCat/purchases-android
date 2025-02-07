package com.revenuecat.purchases.ui.revenuecatui.customercenter

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.Localization.CommonLocalizedString
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.PADDING_LARGE
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.PADDING_MEDIUM
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.PADDING_SMALL
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.SUBSCRIPTION_DETAILS_ROW_HEIGHT
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.TEXT_LIKE_SETTINGS_MAIN_SIZE
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.TEXT_LIKE_SETTINGS_SUPPORTING_SIZE
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
    Column(
        modifier = modifier,
    ) {
        val explanation = remember { getSubscriptionExplanation(details, localization) }
        SubscriptionDetailsRow(
            title = details.title ?: details.product?.title ?: "",
            subtitle = explanation,
            prominentSubtitle = false,
        )

        details.durationTitle?.let {
            SubscriptionDetailsRow(
                title = localization.commonLocalizedString(CommonLocalizedString.BILLING_CYCLE),
                subtitle = it,
                icon = CurrencyExchange,
            )
        }

        val price = remember { getPrice(details, localization) }
        price?.let {
            SubscriptionDetailsRow(
                title = localization.commonLocalizedString(CommonLocalizedString.CURRENT_PRICE),
                subtitle = it,
                icon = UniversalCurrencyAlt,
            )
        }
        details.expirationOrRenewal?.let { expirationOrRenewal ->
            val expirationValue = remember { getExpirationValue(expirationOrRenewal, localization) }
            val expirationOverline = remember { labelForExpirationOrRenewal(expirationOrRenewal, localization) }

            SubscriptionDetailsRow(
                icon = Icons.Rounded.DateRange,
                title = expirationOverline,
                subtitle = expirationValue,
            )
        }
    }
}

@Composable
@JvmSynthetic
internal fun SubscriptionDetailsRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    prominentSubtitle: Boolean = true,
) {
    val titleTextStyle = TextStyle(
        fontSize = if (prominentSubtitle) TEXT_LIKE_SETTINGS_SUPPORTING_SIZE else TEXT_LIKE_SETTINGS_MAIN_SIZE,
        fontWeight = FontWeight.Normal,
    )

    val subtitleTextStyle = TextStyle(
        fontSize = if (prominentSubtitle) TEXT_LIKE_SETTINGS_MAIN_SIZE else TEXT_LIKE_SETTINGS_SUPPORTING_SIZE,
        fontWeight = FontWeight.Normal,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .defaultMinSize(minHeight = SUBSCRIPTION_DETAILS_ROW_HEIGHT)
            .fillMaxWidth()
            .padding(PADDING_MEDIUM),
        horizontalArrangement = Arrangement.spacedBy(PADDING_SMALL),
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(PADDING_LARGE),
            )
        }
        Column {
            Text(
                text = title,
                style = titleTextStyle,
                maxLines = 1,
                modifier = Modifier.alpha(if (prominentSubtitle) 0.6f else 1.0f),
            )
            Text(
                text = subtitle,
                style = subtitleTextStyle,
                maxLines = 2,
                modifier = Modifier.alpha(if (prominentSubtitle) 1.0f else 0.6f),
            )
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

private const val MANAGEMENT_URL = "https://play.google.com/store/account/subscriptions"

@Suppress("MagicNumber")
private class SubscriptionInformationProvider : PreviewParameterProvider<PurchaseInformation> {
    override val values: Sequence<PurchaseInformation> = sequenceOf(
        PurchaseInformation(
            title = "Basic",
            durationTitle = "Monthly",
            price = PriceDetails.Paid("$4.99"),
            explanation = Explanation.EARLIEST_RENEWAL,
            expirationOrRenewal =
            ExpirationOrRenewal(
                ExpirationOrRenewal.Label.NEXT_BILLING_DATE,
                ExpirationOrRenewal.Date.DateString("June 1st, 2024"),
            ),
            store = Store.PLAY_STORE,
            managementURL = Uri.parse(MANAGEMENT_URL),
            product = TestStoreProduct(
                "basic_monthly",
                "name",
                "title",
                "description",
                Price("$1.99", 1_990_000, "US"),
                Period(1, Period.Unit.MONTH, "P1M"),
            ),
        ),
        PurchaseInformation(
            title = "Basic",
            durationTitle = "Yearly",
            price = PriceDetails.Paid("$49.99"),
            explanation = Explanation.EARLIEST_EXPIRATION,
            expirationOrRenewal =
            ExpirationOrRenewal(
                ExpirationOrRenewal.Label.EXPIRED,
                ExpirationOrRenewal.Date.DateString("June 1st, 2024"),
            ),
            store = Store.PLAY_STORE,
            managementURL = Uri.parse(MANAGEMENT_URL),
            product = TestStoreProduct(
                "basic_yearly",
                "name",
                "title",
                "description",
                Price("$1.99", 1_990_000, "US"),
                Period(1, Period.Unit.YEAR, "P1Y"),
            ),
        ),
        PurchaseInformation(
            title = "Basic",
            durationTitle = "Weekly",
            price = PriceDetails.Paid("$1.99"),
            explanation = Explanation.EXPIRED,
            expirationOrRenewal =
            ExpirationOrRenewal(
                ExpirationOrRenewal.Label.EXPIRED,
                ExpirationOrRenewal.Date.DateString("June 1st, 2024"),
            ),
            store = Store.PLAY_STORE,
            managementURL = Uri.parse(MANAGEMENT_URL),
            product = TestStoreProduct(
                "basic_weekly",
                "name",
                "title",
                "description",
                Price("$0.99", 990_000, "US"),
                Period(1, Period.Unit.WEEK, "P1W"),
            ),
        ),
    )
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Preview(group = "scale = 1", fontScale = 1F, device = "spec:width=1080px,height=2720px,dpi=440")
// Unrealistically long device to make the Column fit. Can be removed once Emerge Snapshot Test supports
// @PreviewParameter.
@Preview(group = "scale = 2", fontScale = 2F, device = "spec:width=1080px,height=2720px,dpi=440")
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

@Preview
@Composable
private fun SubscriptionDetailsRowIcon_Preview() {
    SubscriptionDetailsRow(
        "Next Billing Date",
        "June 1st, 2024",
        Modifier,
        CalendarMonth,
    )
}

@Preview
@Composable
private fun SubscriptionDetailsRow_Preview() {
    SubscriptionDetailsRow(
        title = "Basic",
        subtitle = "This is your subscription with the earliest expiration date",
        modifier = Modifier,
    )
}
