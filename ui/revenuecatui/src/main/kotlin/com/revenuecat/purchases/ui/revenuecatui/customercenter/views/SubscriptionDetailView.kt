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
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.HelpPath
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ManagementViewHorizontalPadding
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ManagementViewSpacer
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ManagementViewTitleTopPadding
import com.revenuecat.purchases.ui.revenuecatui.customercenter.SubscriptionDetailsView
import com.revenuecat.purchases.ui.revenuecatui.customercenter.actions.CustomerCenterAction
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Suppress("LongParameterList")
@Composable
internal fun SubscriptionDetailView(
    screenTitle: String,
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

        SubscriptionDetailsView(
            details = purchaseInformation,
            localization = localization,
            modifier = Modifier.fillMaxWidth(),
        )

        val pathsForPurchase = if (purchaseInformation.isLifetime) {
            supportedPaths.filter { it.type != HelpPath.PathType.CANCEL }
        } else {
            supportedPaths
        }

        if (purchaseInformation.store == Store.PLAY_STORE) {
            ManageSubscriptionsButtonsView(pathsForPurchase) { path ->
                onAction(CustomerCenterAction.PathButtonPressed(path, purchaseInformation.product))
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
