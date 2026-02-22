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
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.HelpPath
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ManagementViewHorizontalPadding
import com.revenuecat.purchases.ui.revenuecatui.customercenter.actions.CustomerCenterAction
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation
import com.revenuecat.purchases.ui.revenuecatui.customercenter.theme.CustomerCenterPreviewTheme
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies

@Suppress("LongParameterList", "LongMethod")
@Composable
internal fun RelevantPurchasesListView(
    supportedPaths: List<HelpPath>,
    contactEmail: String?,
    virtualCurrencies: VirtualCurrencies?,
    appearance: CustomerCenterConfigData.Appearance,
    localization: CustomerCenterConfigData.Localization,
    supportTickets: CustomerCenterConfigData.Support.SupportTickets,
    onPurchaseSelect: (PurchaseInformation) -> Unit,
    onAction: (CustomerCenterAction) -> Unit,
    modifier: Modifier = Modifier,
    purchases: List<PurchaseInformation> = emptyList(),
    purchasesWithActions: Set<PurchaseInformation> = emptySet(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
    ) {
        val subscriptions = purchases.filter {
            it.isSubscription || (it.store == Store.PROMOTIONAL && !it.isLifetime)
        }.toSet()
        val nonSubscriptions = (purchases - subscriptions).toSet()

        PurchaseListSection(
            purchases = subscriptions,
            localization = localization,
            totalPurchaseCount = purchases.size,
            onPurchaseSelect = onPurchaseSelect,
            purchasesWithActions = purchasesWithActions,
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
                totalPurchaseCount = purchases.size,
                onPurchaseSelect = onPurchaseSelect,
                purchasesWithActions = purchasesWithActions,
            )
        }

        virtualCurrencies?.let {
            if (virtualCurrencies.all.isNotEmpty()) {
                Spacer(modifier = Modifier.size(CustomerCenterConstants.Layout.SECTION_SPACING))

                VirtualCurrenciesListView(
                    virtualCurrencies = virtualCurrencies,
                    appearance = appearance,
                    localization = localization,
                    onAction = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CustomerCenterConstants.Layout.HORIZONTAL_PADDING),
                )
            }
        }

        ManageSubscriptionsButtonsView(
            associatedPurchaseInformation = if (purchases.size == 1) {
                purchases.first()
            } else {
                null
            },
            supportedPaths = supportedPaths,
            localization = localization,
            contactEmail = contactEmail,
            addCreateTicketButton = supportTickets.allowCreation && supportTickets.allowsActiveCustomers(),
            addContactButton = supportTickets.allowsActiveCustomers(),
            onAction = onAction,
        )
    }
}

@Composable
private fun PurchaseListSection(
    purchases: Set<PurchaseInformation>,
    localization: CustomerCenterConfigData.Localization,
    totalPurchaseCount: Int,
    onPurchaseSelect: (PurchaseInformation) -> Unit,
    purchasesWithActions: Set<PurchaseInformation>,
) {
    if (purchases.isNotEmpty()) {
        purchases.forEachIndexed { index, purchase ->
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
                purchaseInformation = purchase,
                localization = localization,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CustomerCenterConstants.Layout.HORIZONTAL_PADDING),
                position = position,
                isDetailedView = totalPurchaseCount == 1,
                onCardClick = if (totalPurchaseCount > 1 && purchase in purchasesWithActions) {
                    { onPurchaseSelect(purchase) }
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
                supportedPaths = managementScreen.paths,
                contactEmail = "support@revenuecat.com",
                virtualCurrencies = null,
                appearance = testData.appearance,
                localization = testData.localization,
                supportTickets = testData.support.supportTickets,
                onPurchaseSelect = {},
                onAction = {},
                purchases = listOf(CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing),
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
                supportedPaths = noActiveScreen.paths,
                contactEmail = "support@revenuecat.com",
                virtualCurrencies = null,
                appearance = testData.appearance,
                localization = testData.localization,
                supportTickets = testData.support.supportTickets,
                onPurchaseSelect = {},
                onAction = {},
                purchases = emptyList(),
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
                supportedPaths = managementScreen.paths,
                appearance = testData.appearance,
                localization = testData.localization,
                supportTickets = testData.support.supportTickets,
                contactEmail = "support@revenuecat.com",
                virtualCurrencies = null,
                onPurchaseSelect = {},
                onAction = {},
                purchases = listOf(
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
                supportedPaths = managementScreen.paths,
                appearance = testData.appearance,
                localization = testData.localization,
                supportTickets = testData.support.supportTickets,
                contactEmail = "support@revenuecat.com",
                virtualCurrencies = null,
                onPurchaseSelect = {},
                onAction = {},
                purchases = listOf(
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
                supportedPaths = noActiveScreen.paths,
                appearance = testData.appearance,
                localization = testData.localization,
                supportTickets = testData.support.supportTickets,
                contactEmail = "support@revenuecat.com",
                virtualCurrencies = null,
                onPurchaseSelect = {},
                onAction = {},
                purchases = emptyList(),
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
                supportedPaths = managementScreen.paths,
                appearance = testData.appearance,
                localization = testData.localization,
                supportTickets = testData.support.supportTickets,
                contactEmail = "support@revenuecat.com",
                virtualCurrencies = null,
                onPurchaseSelect = {},
                onAction = {},
                purchases = listOf(
                    CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing,
                    CustomerCenterConfigTestData.purchaseInformationYearlyExpiring,
                ),
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
private fun RelevantPurchasesListViewWithMultiplePurchasesAndVirtualCurrenciesPreview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    val managementScreen = testData.screens[CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT]!!
    CustomerCenterPreviewTheme {
        Column {
            RelevantPurchasesListView(
                supportedPaths = managementScreen.paths,
                appearance = testData.appearance,
                localization = testData.localization,
                supportTickets = testData.support.supportTickets,
                contactEmail = "support@revenuecat.com",
                virtualCurrencies = CustomerCenterConfigTestData.fiveVirtualCurrencies,
                onPurchaseSelect = {},
                onAction = {},
                purchases = listOf(
                    CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing,
                    CustomerCenterConfigTestData.purchaseInformationLifetime,
                ),
            )
        }
    }
}
