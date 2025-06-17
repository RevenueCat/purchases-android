package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

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
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.ExpirationOrRenewal
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PriceDetails
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation

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
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = CustomerCenterConstants.Card.TITLE_ROW_BOTTOM_PADDING),
            ) {
                Text(
                    text = purchaseInformation.title ?: purchaseInformation.product?.title ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                PurchaseStatusBadge(purchaseInformation, localization)
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

private fun getSubtitle(
    purchaseInformation: PurchaseInformation,
    localization: CustomerCenterConfigData.Localization,
): String? {
    return when (purchaseInformation.expirationOrRenewal) {
        is ExpirationOrRenewal.Expiration ->
            purchaseInformation.expirationString(purchaseInformation.expirationOrRenewal.date, localization)
        is ExpirationOrRenewal.Renewal ->
            purchaseInformation.renewalString(purchaseInformation.expirationOrRenewal.date, localization)
        null -> getPrice(purchaseInformation, localization) // Can return null if price is unknown
    }
}

private fun getStoreText(store: Store): String {
    return when (store) {
        Store.APP_STORE -> "Apple App Store"
        Store.PLAY_STORE -> "Google Play Store"
        Store.STRIPE -> "Web"
        Store.PROMOTIONAL -> "Via Support"
        Store.AMAZON -> "Amazon Store"
        Store.RC_BILLING -> "RevenueCat"
        Store.MAC_APP_STORE -> "Mac App Store"
        Store.UNKNOWN_STORE -> "Unknown Store"
        Store.EXTERNAL -> "External purchase"
        Store.PADDLE -> "Paddle"
    }
}

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
        CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing,
        CustomerCenterConfigTestData.purchaseInformationYearlyExpiring,
        CustomerCenterConfigTestData.purchaseInformationYearlyExpired,
        CustomerCenterConfigTestData.purchaseInformationPromotional,
        CustomerCenterConfigTestData.purchaseInformationLifetime,
        CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing.copy(
            title = "Monthly long subscription name that overflows",
        ),
    )
}

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
