package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.Explanation
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PriceDetails
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation

internal enum class ButtonPosition {
    SINGLE,
    FIRST,
    MIDDLE,
    LAST,
}

private val roundedCornerShapeSize = 24.dp

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
internal fun PurchaseInformationCardView(
    purchaseInformation: PurchaseInformation,
    localization: CustomerCenterConfigData.Localization,
    modifier: Modifier = Modifier,
    position: ButtonPosition = ButtonPosition.SINGLE,
) {
    val shape = when (position) {
        ButtonPosition.SINGLE -> RoundedCornerShape(roundedCornerShapeSize)
        ButtonPosition.FIRST -> RoundedCornerShape(
            topStart = roundedCornerShapeSize,
            topEnd = roundedCornerShapeSize,
            bottomStart = 4.dp,
            bottomEnd = 4.dp,
        )
        ButtonPosition.LAST -> RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 4.dp,
            bottomStart = roundedCornerShapeSize,
            bottomEnd = roundedCornerShapeSize,
        )
        ButtonPosition.MIDDLE -> RoundedCornerShape(4.dp)
    }

    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
            ) {
                Text(
                    text = purchaseInformation.title ?: purchaseInformation.product?.title ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                when {
                    purchaseInformation.isCancelled -> StatusBadge(
                        text = localization.commonLocalizedString(
                            CustomerCenterConfigData.Localization.CommonLocalizedString.BADGE_CANCELLED,
                        ),
                        backgroundColor = Color(0x33F2545B),
                    )
                    purchaseInformation.isTrial -> StatusBadge(
                        text = localization.commonLocalizedString(
                            CustomerCenterConfigData.Localization.CommonLocalizedString.BADGE_FREE_TRIAL,
                        ),
                        backgroundColor = Color(0x5BF5CA5C),
                    )
                    purchaseInformation.isActive -> StatusBadge(
                        text = localization.commonLocalizedString(
                            CustomerCenterConfigData.Localization.CommonLocalizedString.ACTIVE,
                        ),
                        backgroundColor = Color(0x9911D483),
                    )
                }
            }

            getSubtitle(
                purchaseInformation,
                localization,
            )?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }

            Text(
                text = getStoreText(purchaseInformation.store),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
private fun getSubtitle(
    purchaseInformation: PurchaseInformation,
    localization: CustomerCenterConfigData.Localization,
): String? {
    if (purchaseInformation.renewalDate != null) {
        return purchaseInformation.renewalString(purchaseInformation.renewalDate, localization)
    } else if (purchaseInformation.expirationDate != null) {
        return purchaseInformation.expirationString(purchaseInformation.expirationDate, localization)
    } else {
        return getPrice(purchaseInformation, localization)
    }
}

@Composable
private fun StatusBadge(
    text: String,
    backgroundColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

private fun getStoreText(store: Store): String {
    return when (store) {
        Store.APP_STORE -> "Apple App Store"
        Store.PLAY_STORE -> "Google Play Store"
        Store.STRIPE -> "Web"
        Store.PROMOTIONAL -> "Promotional"
        Store.AMAZON -> "Amazon Store"
        Store.RC_BILLING -> "RevenueCat"
        Store.MAC_APP_STORE -> "Mac App Store"
        Store.UNKNOWN_STORE -> "Unknown Store"
        Store.EXTERNAL -> "External purchase"
        Store.PADDLE -> "Paddle"
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
private fun getPrice(
    purchaseInformation: PurchaseInformation,
    localization: CustomerCenterConfigData.Localization,
): String? {
    return when (purchaseInformation.pricePaid) {
        is PriceDetails.Paid ->
            purchaseInformation.pricePaid.price
        PriceDetails.Free ->
            localization.commonLocalizedString(CustomerCenterConfigData.Localization.CommonLocalizedString.FREE)
        PriceDetails.Unknown -> null
    }
}

private const val MANAGEMENT_URL = "https://play.google.com/store/account/subscriptions"

@Suppress("MagicNumber")
private class PurchaseInformationProvider : PreviewParameterProvider<PurchaseInformation> {
    override val values: Sequence<PurchaseInformation> = sequenceOf(
        PurchaseInformation(
            title = "Basic",
            durationTitle = "Monthly",
            pricePaid = PriceDetails.Paid("$4.99"),
            explanation = Explanation.EARLIEST_RENEWAL,
            expirationDate = null,
            renewalDate = "June 1st, 2024",
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
            isLifetime = false,
            isActive = true,
            isTrial = false,
            isCancelled = false,
        ),
        PurchaseInformation(
            title = "Basic",
            durationTitle = "Yearly",
            pricePaid = PriceDetails.Paid("$49.99"),
            explanation = Explanation.EARLIEST_EXPIRATION,
            expirationDate = "June 1st, 2024",
            renewalDate = null,
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
            isLifetime = false,
            isActive = false,
            isTrial = false,
            isCancelled = true,
        ),
        PurchaseInformation(
            title = "Basic",
            durationTitle = "Weekly",
            pricePaid = PriceDetails.Paid("$1.99"),
            explanation = Explanation.EXPIRED,
            expirationDate = "June 1st, 2024",
            renewalDate = null,
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
            isLifetime = false,
            isActive = false,
            isTrial = false,
            isCancelled = false,
        ),
        PurchaseInformation(
            title = "Monthly long subscription name that overflows",
            durationTitle = "Weekly",
            pricePaid = PriceDetails.Paid("$1.99"),
            explanation = Explanation.EXPIRED,
            expirationDate = "June 1st, 2024",
            renewalDate = null,
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
            isLifetime = false,
            isActive = false,
            isTrial = false,
            isCancelled = false,
        ),
    )
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Preview(group = "scale = 1", fontScale = 1F)
@Composable
private fun PurchaseInformationCardView_Preview(
    @PreviewParameter(PurchaseInformationProvider::class) details: PurchaseInformation,
) {
    Surface(
        color = Color.White,
    ) {
        PurchaseInformationCardView(
            purchaseInformation = details,
            localization = CustomerCenterConfigTestData.customerCenterData(
                shouldWarnCustomerToUpdate = false,
            ).localization,
        )
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Preview(group = "scale = 2", fontScale = 2F)
@Composable
private fun PurchaseInformationCardView_Preview_Scale2(
    @PreviewParameter(PurchaseInformationProvider::class) details: PurchaseInformation,
) {
    Surface(
        color = Color.White,
    ) {
        PurchaseInformationCardView(
            purchaseInformation = details,
            localization = CustomerCenterConfigTestData.customerCenterData(
                shouldWarnCustomerToUpdate = false,
            ).localization,
        )
    }
}
