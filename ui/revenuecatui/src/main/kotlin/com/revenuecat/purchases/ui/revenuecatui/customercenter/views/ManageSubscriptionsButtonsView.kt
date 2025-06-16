package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.HelpPath
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ManagementViewHorizontalPadding
import com.revenuecat.purchases.ui.revenuecatui.customercenter.actions.CustomerCenterAction
import com.revenuecat.purchases.ui.revenuecatui.customercenter.composables.SettingsButton
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation

@Composable
internal fun ManageSubscriptionsButtonsView(
    associatedPurchaseInformation: PurchaseInformation?,
    supportedPaths: List<HelpPath>,
    localization: CustomerCenterConfigData.Localization,
    contactEmail: String?,
    addContactButton: Boolean = false,
    onAction: (CustomerCenterAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = ManagementViewHorizontalPadding)
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        supportedPaths.forEach { path ->
            val pathButtonPressed =
                CustomerCenterAction.PathButtonPressed(path, associatedPurchaseInformation)
            SettingsButton(
                onClick = { onAction(pathButtonPressed) },
                title = path.title,
            )
        }
        if (addContactButton && contactEmail != null) {
            SettingsButton(
                onClick = { onAction(CustomerCenterAction.ContactSupport(contactEmail)) },
                title = localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.CONTACT_SUPPORT,
                ),
            )
        }
    }
}
