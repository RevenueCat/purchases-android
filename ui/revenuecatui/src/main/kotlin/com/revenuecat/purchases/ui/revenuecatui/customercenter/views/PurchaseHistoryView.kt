package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ManagementViewHorizontalPadding
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.ExpirationOrRenewal
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PriceDetails
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation
import com.revenuecat.purchases.ui.revenuecatui.customercenter.theme.CustomerCenterPreviewTheme

@Suppress("LongMethod")
@Composable
internal fun PurchaseHistoryView(
    activeSubscriptions: List<PurchaseInformation>,
    inactiveSubscriptions: List<PurchaseInformation>,
    nonSubscriptions: List<PurchaseInformation>,
    localization: CustomerCenterConfigData.Localization,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
    ) {
        if (activeSubscriptions.isNotEmpty()) {
            Spacer(modifier = Modifier.size(CustomerCenterConstants.Layout.SECTION_SPACING))
            Text(
                text = localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.SUBSCRIPTIONS_SECTION_TITLE,
                ),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = ManagementViewHorizontalPadding,
                    bottom = CustomerCenterConstants.Layout.SECTION_TITLE_BOTTOM_PADDING,
                ),
            )
            PurchaseHistorySection(
                purchases = activeSubscriptions,
                localization = localization,
            )
        }

        if (inactiveSubscriptions.isNotEmpty()) {
            Spacer(modifier = Modifier.size(CustomerCenterConstants.Layout.SECTION_SPACING))
            Text(
                text = localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.INACTIVE,
                ),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = ManagementViewHorizontalPadding,
                    bottom = CustomerCenterConstants.Layout.SECTION_TITLE_BOTTOM_PADDING,
                ),
            )
            PurchaseHistorySection(
                purchases = inactiveSubscriptions,
                localization = localization,
            )
        }

        if (nonSubscriptions.isNotEmpty()) {
            Spacer(modifier = Modifier.size(CustomerCenterConstants.Layout.SECTION_SPACING))
            Text(
                text = localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.PURCHASES_SECTION_TITLE,
                ),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = ManagementViewHorizontalPadding,
                    bottom = CustomerCenterConstants.Layout.SECTION_TITLE_BOTTOM_PADDING,
                ),
            )
            PurchaseHistorySection(
                purchases = nonSubscriptions,
                localization = localization,
            )
        }

        Spacer(modifier = Modifier.size(CustomerCenterConstants.Layout.SECTION_SPACING))
    }
}

@Composable
private fun PurchaseHistorySection(
    purchases: List<PurchaseInformation>,
    localization: CustomerCenterConfigData.Localization,
) {
    purchases.forEachIndexed { index, info ->
        if (index > 0) {
            Spacer(modifier = Modifier.size(CustomerCenterConstants.Layout.ITEMS_SPACING))
        }

        val position = when {
            purchases.size == 1 -> ButtonPosition.SINGLE
            index == 0 -> ButtonPosition.FIRST
            index == purchases.size - 1 -> ButtonPosition.LAST
            else -> ButtonPosition.MIDDLE
        }
        PurchaseInformationCardView(
            purchaseInformation = info,
            localization = localization,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CustomerCenterConstants.Layout.HORIZONTAL_PADDING),
            position = position,
            isDetailedView = false,
            onCardClick = null,
        )
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp")
@Composable
private fun PurchaseHistoryViewPreview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    CustomerCenterPreviewTheme {
        PurchaseHistoryView(
            activeSubscriptions = listOf(CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing),
            inactiveSubscriptions = listOf(CustomerCenterConfigTestData.purchaseInformationYearlyExpiring),
            nonSubscriptions = listOf(
                PurchaseInformation(
                    title = "Lifetime Access",
                    pricePaid = PriceDetails.Paid("$9.99"),
                    expirationOrRenewal = null,
                    product = null,
                    store = Store.PLAY_STORE,
                    isSubscription = false,
                    managementURL = null,
                    isExpired = false,
                    isTrial = false,
                    isCancelled = false,
                    isLifetime = true,
                ),
            ),
            localization = testData.localization,
        )
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp")
@Composable
private fun PurchaseHistoryViewOnlyExpiredPreview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    CustomerCenterPreviewTheme {
        PurchaseHistoryView(
            activeSubscriptions = emptyList(),
            inactiveSubscriptions = listOf(
                CustomerCenterConfigTestData.purchaseInformationYearlyExpiring,
                PurchaseInformation(
                    title = "Monthly Plan",
                    pricePaid = PriceDetails.Paid("$4.99"),
                    expirationOrRenewal = ExpirationOrRenewal.Expiration("Jan 1, 2024"),
                    product = null,
                    store = Store.PLAY_STORE,
                    isSubscription = true,
                    managementURL = null,
                    isExpired = true,
                    isTrial = false,
                    isCancelled = true,
                    isLifetime = false,
                ),
            ),
            nonSubscriptions = emptyList(),
            localization = testData.localization,
        )
    }
}
