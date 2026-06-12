package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.HelpPath
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants
import com.revenuecat.purchases.ui.revenuecatui.customercenter.actions.CustomerCenterAction
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation
import com.revenuecat.purchases.ui.revenuecatui.customercenter.theme.CustomerCenterPreviewTheme

@Suppress("LongParameterList")
@Composable
internal fun SelectedPurchaseDetailView(
    contactEmail: String?,
    localization: CustomerCenterConfigData.Localization,
    purchaseInformation: PurchaseInformation,
    supportedPaths: List<HelpPath>,
    supportTickets: CustomerCenterConfigData.Support.SupportTickets,
    hasActiveSubscriptions: Boolean,
    modifier: Modifier = Modifier,
    onAction: (CustomerCenterAction) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = CustomerCenterConstants.Layout.TOP_PADDING_AFTER_TOP_BAR),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PurchaseInformationCardView(
            purchaseInformation = purchaseInformation,
            localization = localization,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CustomerCenterConstants.Layout.HORIZONTAL_PADDING),
            position = ButtonPosition.SINGLE,
            isDetailedView = true,
            onCardClick = null,
        )

        ManageSubscriptionsButtonsView(
            associatedPurchaseInformation = purchaseInformation,
            supportedPaths = supportedPaths,
            localization = localization,
            addCreateTicketButton = supportTickets.allowCreation &&
                if (hasActiveSubscriptions) {
                    supportTickets.allowsActiveCustomers()
                } else {
                    supportTickets.allowsNonActiveCustomers()
                },
            addContactButton = purchaseInformation.store != Store.PLAY_STORE,
            contactEmail = contactEmail,
            onAction = onAction,
        )
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=412dp,height=915dp",
    group = "scale = 1",
    fontScale = 1F,
)
@Composable
private fun SelectedPurchaseDetailViewPreview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    val managementScreen = testData.screens[CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT]!!
    val purchaseInfo = CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing
    CustomerCenterPreviewTheme {
        SelectedPurchaseDetailView(
            contactEmail = testData.support.email,
            localization = testData.localization,
            purchaseInformation = purchaseInfo,
            supportedPaths = managementScreen.paths,
            supportTickets = testData.support.supportTickets,
            hasActiveSubscriptions = true,
            onAction = {},
        )
    }
}
