package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.foundation.layout.Box
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
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.HelpPath
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ManagementViewHorizontalPadding
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ManagementViewSpacer
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ManagementViewTitleTopPadding
import com.revenuecat.purchases.ui.revenuecatui.customercenter.actions.CustomerCenterAction
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation
import com.revenuecat.purchases.ui.revenuecatui.customercenter.theme.CustomerCenterPreviewTheme

@Suppress("LongParameterList", "LongMethod")
@Composable
internal fun RelevantPurchasesListView(
    screenTitle: String,
    supportedPaths: List<HelpPath>,
    contactEmail: String?,
    localization: CustomerCenterConfigData.Localization,
    onPurchaseSelect: (PurchaseInformation) -> Unit,
    onAction: (CustomerCenterAction) -> Unit,
    modifier: Modifier = Modifier,
    purchaseInformation: List<PurchaseInformation> = emptyList(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = screenTitle,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(
                start = ManagementViewHorizontalPadding,
                end = ManagementViewHorizontalPadding,
                top = ManagementViewTitleTopPadding,
            ),
        )

        Spacer(modifier = Modifier.size(ManagementViewSpacer))

        // Split purchases into subscriptions and non-subscriptions
        val subscriptions = purchaseInformation.filter { it.product?.period != null }
        val nonSubscriptions = purchaseInformation.filter { it.product?.period == null }

        PurchaseListSection(
            purchases = subscriptions,
            localization = localization,
            totalPurchaseCount = purchaseInformation.size,
            onPurchaseSelect = onPurchaseSelect,
        )

        if (nonSubscriptions.isNotEmpty()) {
            Spacer(modifier = Modifier.size(CustomerCenterConstants.Layout.SECTION_SPACING))

            if (subscriptions.isNotEmpty()) {
                Text(
                    text = "Purchases",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        start = ManagementViewHorizontalPadding,
                        bottom = CustomerCenterConstants.Layout.SECTION_TITLE_BOTTOM_PADDING,
                    ),
                )
            }

            PurchaseListSection(
                purchases = nonSubscriptions,
                localization = localization,
                totalPurchaseCount = purchaseInformation.size,
                onPurchaseSelect = onPurchaseSelect,
            )
        }

        ManageSubscriptionsButtonsView(
            associatedPurchaseInformation = if (purchaseInformation.size == 1) {
                purchaseInformation.first()
            } else {
                null
            },
            supportedPaths = supportedPaths,
            localization = localization,
            contactEmail = contactEmail,
            onAction = onAction,
        )
    }
}

@Composable
private fun PurchaseListSection(
    purchases: List<PurchaseInformation>,
    localization: CustomerCenterConfigData.Localization,
    totalPurchaseCount: Int,
    onPurchaseSelect: (PurchaseInformation) -> Unit,
) {
    if (purchases.isNotEmpty()) {
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
                isDetailedView = totalPurchaseCount == 1,
                onCardClick = if (totalPurchaseCount > 1) {
                    { onPurchaseSelect(info) }
                } else {
                    null
                },
            )
        }
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=412dp,height=915dp",
    group = "scale = 1",
    fontScale = 1F,
)
@Composable
private fun RelevantPurchasesListViewPreview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    val managementScreen = testData.screens[CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT]!!
    CustomerCenterPreviewTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            RelevantPurchasesListView(
                screenTitle = managementScreen.title,
                supportedPaths = managementScreen.paths,
                contactEmail = "support@revenuecat.com",
                localization = testData.localization,
                onPurchaseSelect = {},
                onAction = {},
                purchaseInformation = listOf(CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing),
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun NoActiveSubscriptionsViewPreview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    val noActiveScreen = testData.screens[CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE]!!
    CustomerCenterPreviewTheme {
        Column {
            RelevantPurchasesListView(
                screenTitle = noActiveScreen.title,
                supportedPaths = noActiveScreen.paths,
                contactEmail = "support@revenuecat.com",
                localization = testData.localization,
                onPurchaseSelect = {},
                onAction = {},
                purchaseInformation = emptyList(),
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun RelevantPurchasesListViewWithLifetimePurchasePreview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    val managementScreen = testData.screens[CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT]!!
    CustomerCenterPreviewTheme {
        Column {
            RelevantPurchasesListView(
                screenTitle = managementScreen.title,
                supportedPaths = managementScreen.paths,
                localization = testData.localization,
                contactEmail = "support@revenuecat.com",
                onPurchaseSelect = {},
                onAction = {},
                purchaseInformation = listOf(
                    CustomerCenterConfigTestData.purchaseInformationLifetime,
                ),
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun RelevantPurchasesListViewWithSubscriptionsAndLifetimePurchasePreview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    val managementScreen = testData.screens[CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT]!!
    CustomerCenterPreviewTheme {
        Column {
            RelevantPurchasesListView(
                screenTitle = managementScreen.title,
                supportedPaths = managementScreen.paths,
                localization = testData.localization,
                contactEmail = "support@revenuecat.com",
                onPurchaseSelect = {},
                onAction = {},
                purchaseInformation = listOf(
                    CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing,
                    CustomerCenterConfigTestData.purchaseInformationYearlyExpiring,
                    CustomerCenterConfigTestData.purchaseInformationLifetime,
                ),
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun NoActiveSubscriptionsViewNoDescription_Preview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    val noActiveScreen = testData.screens[CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE]!!.copy(subtitle = null)

    CustomerCenterPreviewTheme {
        Column {
            RelevantPurchasesListView(
                screenTitle = noActiveScreen.title,
                supportedPaths = noActiveScreen.paths,
                localization = testData.localization,
                contactEmail = "support@revenuecat.com",
                onPurchaseSelect = {},
                onAction = {},
                purchaseInformation = emptyList(),
            )
        }
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=412dp,height=915dp",
    group = "scale = 1",
    fontScale = 1F,
)
@Composable
private fun RelevantPurchasesListViewWithMultiplePurchasesPreview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    val managementScreen = testData.screens[CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT]!!
    CustomerCenterPreviewTheme {
        Column {
            RelevantPurchasesListView(
                screenTitle = managementScreen.title,
                supportedPaths = managementScreen.paths,
                localization = testData.localization,
                contactEmail = "support@revenuecat.com",
                onPurchaseSelect = {},
                onAction = {},
                purchaseInformation = listOf(
                    CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing,
                    CustomerCenterConfigTestData.purchaseInformationYearlyExpiring,
                ),
            )
        }
    }
}
