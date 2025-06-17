package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.HelpPath
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ManagementViewHorizontalPadding
import com.revenuecat.purchases.ui.revenuecatui.customercenter.actions.CustomerCenterAction
import com.revenuecat.purchases.ui.revenuecatui.customercenter.composables.SettingsButton
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation

@Suppress("LongParameterList")
@Composable
internal fun SelectedPurchaseDetailView(
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
            ManageSubscriptionsButtonsView(
                associatedPurchaseInformation = purchaseInformation,
                supportedPaths = pathsForPurchase,
                localization = localization,
                contactEmail = contactEmail,
                addContactButton = false,
                onAction = onAction,
            )
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

@Composable
internal fun OtherPlatformSubscriptionButtonsView(
    localization: CustomerCenterConfigData.Localization,
    contactEmail: String?,
    managementURL: Uri?,
    onAction: (CustomerCenterAction) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = ManagementViewHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        managementURL?.let {
            SettingsButton(
                onClick = { onAction(CustomerCenterAction.OpenURL(it.toString())) },
                title = localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.MANAGE_SUBSCRIPTION,
                ),
            )
        }
        contactEmail?.let {
            SettingsButton(
                onClick = { onAction(CustomerCenterAction.ContactSupport(it)) },
                title = localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.CONTACT_SUPPORT,
                ),
            )
        }
    }
}
