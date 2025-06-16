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
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PriceDetails
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation

@SuppressWarnings("LongParameterList", "LongMethod")
@Composable
internal fun PurchaseInformationCardView(
    purchaseInformation: PurchaseInformation,
    localization: CustomerCenterConfigData.Localization,
    modifier: Modifier = Modifier,
    position: ButtonPosition = ButtonPosition.SINGLE,
) {
    val shape = when (position) {
        ButtonPosition.SINGLE -> RoundedCornerShape(CustomerCenterConstants.Card.ROUNDED_CORNER_SIZE)
        ButtonPosition.FIRST -> RoundedCornerShape(
            topStart = CustomerCenterConstants.Card.ROUNDED_CORNER_SIZE,
            topEnd = CustomerCenterConstants.Card.ROUNDED_CORNER_SIZE,
            bottomStart = CustomerCenterConstants.Card.MIDDLE_CORNER_SIZE,
            bottomEnd = CustomerCenterConstants.Card.MIDDLE_CORNER_SIZE,
        )
        ButtonPosition.LAST -> RoundedCornerShape(
            topStart = CustomerCenterConstants.Card.MIDDLE_CORNER_SIZE,
            topEnd = CustomerCenterConstants.Card.MIDDLE_CORNER_SIZE,
            bottomStart = CustomerCenterConstants.Card.ROUNDED_CORNER_SIZE,
            bottomEnd = CustomerCenterConstants.Card.ROUNDED_CORNER_SIZE,
        )
        ButtonPosition.MIDDLE -> RoundedCornerShape(CustomerCenterConstants.Card.MIDDLE_CORNER_SIZE)
    }

    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(CustomerCenterConstants.Card.CARD_PADDING),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = CustomerCenterConstants.Card.TITLE_ROW_BOTTOM_PADDING),
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
                        backgroundColor = Color(CustomerCenterConstants.Card.COLOR_BADGE_CANCELLED),
                    )
                    purchaseInformation.isTrial -> StatusBadge(
                        text = localization.commonLocalizedString(
                            CustomerCenterConfigData.Localization.CommonLocalizedString.BADGE_FREE_TRIAL,
                        ),
                        backgroundColor = Color(CustomerCenterConstants.Card.COLOR_BADGE_FREE_TRIAL),
                    )
                    purchaseInformation.isActive -> StatusBadge(
                        text = localization.commonLocalizedString(
                            CustomerCenterConfigData.Localization.CommonLocalizedString.ACTIVE,
                        ),
                        backgroundColor = Color(CustomerCenterConstants.Card.COLOR_BADGE_ACTIVE),
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
    return if (purchaseInformation.renewalDate != null) {
        purchaseInformation.renewalString(purchaseInformation.renewalDate, localization)
    } else if (purchaseInformation.expirationDate != null) {
        purchaseInformation.expirationString(purchaseInformation.expirationDate, localization)
    } else {
        getPrice(purchaseInformation, localization)
    }
}

@Composable
private fun StatusBadge(
    text: String,
    backgroundColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(CustomerCenterConstants.Card.BADGE_CORNER_SIZE),
        color = backgroundColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(
                horizontal = CustomerCenterConstants.Card.BADGE_HORIZONTAL_PADDING,
                vertical = CustomerCenterConstants.Card.BADGE_VERTICAL_PADDING,
            ),
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
internal enum class ButtonPosition {
    SINGLE,
    FIRST,
    MIDDLE,
    LAST,
}

@Suppress("MagicNumber")
private class PurchaseInformationProvider : PreviewParameterProvider<PurchaseInformation> {
    override val values: Sequence<PurchaseInformation> = sequenceOf(
        PurchaseInformation(
            title = "Basic",
            pricePaid = PriceDetails.Paid("$4.99"),
            expirationDate = null,
            renewalDate = "June 1st, 2024",
            store = Store.PLAY_STORE,
            managementURL = Uri.parse(CustomerCenterConstants.Management.MANAGEMENT_URL),
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
            pricePaid = PriceDetails.Paid("$49.99"),
            expirationDate = "June 1st, 2024",
            renewalDate = null,
            store = Store.PLAY_STORE,
            managementURL = Uri.parse(CustomerCenterConstants.Management.MANAGEMENT_URL),
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
            pricePaid = PriceDetails.Paid("$1.99"),
            expirationDate = "June 1st, 2024",
            renewalDate = null,
            store = Store.PLAY_STORE,
            managementURL = Uri.parse(CustomerCenterConstants.Management.MANAGEMENT_URL),
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
            pricePaid = PriceDetails.Paid("$1.99"),
            expirationDate = "June 1st, 2024",
            renewalDate = null,
            store = Store.PLAY_STORE,
            managementURL = Uri.parse(CustomerCenterConstants.Management.MANAGEMENT_URL),
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
