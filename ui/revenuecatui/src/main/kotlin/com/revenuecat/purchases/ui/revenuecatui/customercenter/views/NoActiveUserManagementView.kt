package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.HelpPath
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ContentUnavailableIconSize
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ContentUnavailableViewPaddingHorizontal
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ContentUnavailableViewPaddingText
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ContentUnavailableViewPaddingVertical
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ManagementViewHorizontalPadding
import com.revenuecat.purchases.ui.revenuecatui.customercenter.actions.CustomerCenterAction
import com.revenuecat.purchases.ui.revenuecatui.customercenter.composables.SettingsButton
import com.revenuecat.purchases.ui.revenuecatui.customercenter.composables.SettingsButtonStyle
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.theme.CustomerCenterPreviewTheme

@Suppress("LongParameterList")
@Composable
internal fun NoActiveUserManagementView(
    screenTitle: String,
    screenSubtitle: String?,
    contactEmail: String?,
    localization: CustomerCenterConfigData.Localization,
    supportedPaths: List<HelpPath>,
    offering: Offering?,
    onAction: (CustomerCenterAction) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ContentUnavailableView(
            title = screenTitle,
            description = screenSubtitle,
            modifier = Modifier.padding(
                top = ManagementViewHorizontalPadding,
                start = ManagementViewHorizontalPadding,
                end = ManagementViewHorizontalPadding,
            ),
        )

        // Subscribe button if offering is available
        offering?.let {
            SettingsButton(
                onClick = { onAction(CustomerCenterAction.ShowPaywall) },
                title = localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.SUBSCRIBE,
                ),
                style = SettingsButtonStyle.FILLED,
                modifier = Modifier.padding(
                    top = ManagementViewHorizontalPadding,
                    start = ManagementViewHorizontalPadding,
                    end = ManagementViewHorizontalPadding,
                ),
            )
        }

        ManageSubscriptionsButtonsView(
            associatedPurchaseInformation = null,
            supportedPaths = supportedPaths,
            localization = localization,
            contactEmail = contactEmail,
            addContactButton = true,
            onAction = onAction,
        )
    }
}

@Composable
private fun ContentUnavailableView(
    title: String,
    description: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(CustomerCenterConstants.Card.ROUNDED_CORNER_SIZE),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = ContentUnavailableViewPaddingVertical,
                    horizontal = ContentUnavailableViewPaddingHorizontal,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                modifier = Modifier.size(ContentUnavailableIconSize),
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = ContentUnavailableViewPaddingText),
            )

            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = ContentUnavailableViewPaddingText),
                )
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun NoActiveUserManagementView_Preview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    val noActiveScreen =
        testData.screens[CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE]!!
    CustomerCenterPreviewTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            NoActiveUserManagementView(
                screenTitle = noActiveScreen.title,
                screenSubtitle = noActiveScreen.subtitle,
                contactEmail = "support@example.com",
                localization = testData.localization,
                supportedPaths = noActiveScreen.paths,
                offering = null, // No offering in preview
                onAction = { },
            )
        }
    }
}
