package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.HelpPath
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ManagementViewHorizontalPadding
import com.revenuecat.purchases.ui.revenuecatui.customercenter.actions.CustomerCenterAction
import com.revenuecat.purchases.ui.revenuecatui.customercenter.composables.SettingsButton
import com.revenuecat.purchases.ui.revenuecatui.customercenter.composables.SettingsButtonConfig
import com.revenuecat.purchases.ui.revenuecatui.customercenter.composables.SettingsButtonStyle
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PathUtils
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation

@Suppress("LongParameterList")
@Composable
internal fun ManageSubscriptionsButtonsView(
    associatedPurchaseInformation: PurchaseInformation?,
    supportedPaths: List<HelpPath>,
    localization: CustomerCenterConfigData.Localization,
    contactEmail: String?,
    addContactButton: Boolean = false,
    addCreateTicketButton: Boolean = false,
    onAction: (CustomerCenterAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = ManagementViewHorizontalPadding)
            .padding(
                top = CustomerCenterConstants.Layout.BUTTONS_TOP_PADDING,
                bottom = CustomerCenterConstants.Layout.BUTTONS_BOTTOM_PADDING,
            ),
        verticalArrangement = Arrangement.spacedBy(CustomerCenterConstants.Layout.BUTTONS_SPACING),
    ) {
        PathUtils.sortPathsByButtonPriority(supportedPaths).forEach { path ->
            val pathButtonPressed =
                CustomerCenterAction.PathButtonPressed(path, associatedPurchaseInformation)
            SettingsButton(
                onClick = { onAction(pathButtonPressed) },
                title = path.title,
                style = PathUtils.getButtonStyleForPath(path),
                config = SettingsButtonConfig(),
            )
        }

        if (addCreateTicketButton) {
            SettingsButton(
                onClick = { onAction(CustomerCenterAction.ShowSupportTicketCreation) },
                title = localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.CONTACT_SUPPORT,
                ),
                style = SettingsButtonStyle.OUTLINED,
                config = SettingsButtonConfig(),
            )
        } else if (addContactButton && contactEmail != null) {
            SettingsButton(
                onClick = { onAction(CustomerCenterAction.ContactSupport(contactEmail)) },
                title = localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.CONTACT_SUPPORT,
                ),
                style = SettingsButtonStyle.OUTLINED,
                config = SettingsButtonConfig(),
            )
        }
    }
}
