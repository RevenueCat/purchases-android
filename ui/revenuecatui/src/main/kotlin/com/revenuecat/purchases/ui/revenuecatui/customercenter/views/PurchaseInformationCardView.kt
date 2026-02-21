package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.ExpirationOrRenewal
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PriceDetails
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation
import com.revenuecat.purchases.ui.revenuecatui.customercenter.theme.CustomerCenterPreviewTheme
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull
import com.revenuecat.purchases.ui.revenuecatui.icons.KeyboardArrowRight

@SuppressWarnings("LongParameterList", "LongMethod")
@Composable
internal fun PurchaseInformationCardView(
    purchaseInformation: PurchaseInformation,
    localization: CustomerCenterConfigData.Localization,
    modifier: Modifier = Modifier,
    position: ButtonPosition = ButtonPosition.SINGLE,
    isDetailedView: Boolean = false,
    onCardClick: (() -> Unit)?,
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
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .applyIfNotNull(onCardClick) { clickable { onCardClick?.invoke() } }
                .padding(CustomerCenterConstants.Card.CARD_PADDING),
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
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                when {
                    onCardClick != null && !isDetailedView -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(
                                CustomerCenterConstants.Card.BADGE_HORIZONTAL_PADDING,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PurchaseStatusBadge(purchaseInformation, localization)
                            Icon(
                                imageVector = KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    else -> PurchaseStatusBadge(purchaseInformation, localization)
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
                text = getStoreText(purchaseInformation.store, localization),
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

private fun getStoreText(store: Store, localization: CustomerCenterConfigData.Localization): String {
    val key = when (store) {
        Store.APP_STORE -> CustomerCenterConfigData.Localization.CommonLocalizedString.APP_STORE
        Store.MAC_APP_STORE -> CustomerCenterConfigData.Localization.CommonLocalizedString.MAC_APP_STORE
        Store.PLAY_STORE -> CustomerCenterConfigData.Localization.CommonLocalizedString.GOOGLE_PLAY_STORE
        Store.AMAZON -> CustomerCenterConfigData.Localization.CommonLocalizedString.AMAZON_STORE
        Store.PROMOTIONAL -> CustomerCenterConfigData.Localization.CommonLocalizedString.CARD_STORE_PROMOTIONAL
        Store.STRIPE,
        Store.EXTERNAL,
        Store.PADDLE,
        Store.RC_BILLING,
        -> CustomerCenterConfigData.Localization.CommonLocalizedString.WEB_STORE
        Store.UNKNOWN_STORE,
        -> CustomerCenterConfigData.Localization.CommonLocalizedString.UNKNOWN_STORE
        Store.TEST_STORE -> CustomerCenterConfigData.Localization.CommonLocalizedString.TEST_STORE
    }
    return localization.commonLocalizedString(key)
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
        CustomerCenterConfigTestData.purchaseInformationFreeTrial,
        CustomerCenterConfigTestData.purchaseInformationPromotional,
        CustomerCenterConfigTestData.purchaseInformationLifetime,
        CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing.copy(
            title = "Monthly long subscription name that overflows",
        ),
    )
}

@Preview(group = "scale = 1", fontScale = 1F)
@Preview(group = "scale = 1", fontScale = 1F, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PurchaseInformationCardView_Preview(
    @PreviewParameter(PurchaseInformationProvider::class) details: PurchaseInformation,
) {
    CustomerCenterPreviewTheme {
        PurchaseInformationCardView(
            purchaseInformation = details,
            localization = CustomerCenterConfigTestData.customerCenterData(
                shouldWarnCustomerToUpdate = false,
            ).localization,
            onCardClick = { },
        )
    }
}

@Preview(group = "scale = 2", fontScale = 2F)
@Preview(group = "scale = 2", fontScale = 2F, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PurchaseInformationCardView_Preview_Scale2(
    @PreviewParameter(PurchaseInformationProvider::class) details: PurchaseInformation,
) {
    CustomerCenterPreviewTheme {
        PurchaseInformationCardView(
            purchaseInformation = details,
            localization = CustomerCenterConfigTestData.customerCenterData(
                shouldWarnCustomerToUpdate = false,
            ).localization,
            onCardClick = { },
        )
    }
}
