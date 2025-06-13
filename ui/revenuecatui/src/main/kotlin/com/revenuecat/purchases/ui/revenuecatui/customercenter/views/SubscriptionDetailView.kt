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
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.HelpPath
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants
import com.revenuecat.purchases.ui.revenuecatui.customercenter.actions.CustomerCenterAction
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Suppress("LongParameterList")
@Composable
internal fun SubscriptionDetailView(
    contactEmail: String?,
    localization: CustomerCenterConfigData.Localization,
    purchaseInformation: PurchaseInformation,
    supportedPaths: List<HelpPath>,
    modifier: Modifier = Modifier,
    onAction: (CustomerCenterAction) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
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

        val pathsForPurchase = if (purchaseInformation.isLifetime) {
            supportedPaths.filter { it.type != HelpPath.PathType.CANCEL }
        } else {
            supportedPaths
        }

        if (purchaseInformation.store == Store.PLAY_STORE) {
            ManageSubscriptionsButtonsView(pathsForPurchase) { path ->
                onAction(CustomerCenterAction.PathButtonPressed(path, purchaseInformation))
            }
        } else {
            OtherPlatformSubscriptionButtonsView(
                localization = localization,
                contactEmail = contactEmail,
                managementURL = purchaseInformation.managementURL,
                onAction = onAction,
            )
        }
    }
}
