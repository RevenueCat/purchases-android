package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.HelpPath
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants
import com.revenuecat.purchases.ui.revenuecatui.customercenter.actions.CustomerCenterAction
import com.revenuecat.purchases.ui.revenuecatui.customercenter.composables.SettingsButton
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation

@Suppress("LongParameterList")
@Composable
internal fun ManageSubscriptionsButtonsView(
    purchaseInformation: PurchaseInformation?,
    supportedPaths: List<HelpPath>,
    localization: CustomerCenterConfigData.Localization,
    contactEmail: String?,
    addContactButton: Boolean = false,
    onAction: (CustomerCenterAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(
                top = CustomerCenterConstants.Layout.BUTTONS_TOP_PADDING,
                bottom = CustomerCenterConstants.Layout.BUTTONS_BOTTOM_PADDING,
            ),
        verticalArrangement = Arrangement.spacedBy(CustomerCenterConstants.Layout.BUTTONS_SPACING),
    ) {
        supportedPaths.forEach { path ->
            val pathButtonPressed =
                CustomerCenterAction.PathButtonPressed(path, purchaseInformation)
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
