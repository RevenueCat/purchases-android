package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.HelpPath
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ContentUnavailableIconSize
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ContentUnavailableViewPadding
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ContentUnavailableViewPaddingTopDescription
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ContentUnavailableViewPaddingTopTitle
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ManagementViewHorizontalPadding
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ManagementViewSpacer
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ManagementViewTitleTopPadding
import com.revenuecat.purchases.ui.revenuecatui.customercenter.SubscriptionDetailsView
import com.revenuecat.purchases.ui.revenuecatui.customercenter.actions.CustomerCenterAction
import com.revenuecat.purchases.ui.revenuecatui.customercenter.composables.SettingsButton
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation

@Suppress("LongParameterList")
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
internal fun ManageSubscriptionsView(
    screenTitle: String,
    screenSubtitle: String?,
    screenType: CustomerCenterConfigData.Screen.ScreenType,
    supportedPaths: List<HelpPath>,
    contactEmail: String?,
    localization: CustomerCenterConfigData.Localization,
    modifier: Modifier = Modifier,
    purchaseInformation: PurchaseInformation? = null,
    onAction: (CustomerCenterAction) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (screenType == CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT && purchaseInformation != null) {
            ActiveUserManagementView(
                screenTitle,
                contactEmail,
                localization,
                purchaseInformation,
                supportedPaths,
                onAction,
            )
        } else {
            NoActiveUserManagementView(
                screenTitle,
                screenSubtitle,
                contactEmail,
                localization,
                purchaseInformation,
                supportedPaths,
                onAction,
            )
        }
    }
}

@SuppressWarnings("LongParameterList")
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
private fun ActiveUserManagementView(
    screenTitle: String,
    contactEmail: String?,
    localization: CustomerCenterConfigData.Localization,
    purchaseInformation: PurchaseInformation,
    supportedPaths: List<HelpPath>,
    onAction: (CustomerCenterAction) -> Unit,
) {
    Column {
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

        SubscriptionDetailsView(details = purchaseInformation, localization = localization)

        ManageSubscriptionsButtonsView(
            purchaseInformation = purchaseInformation,
            supportedPaths = supportedPaths,
            localization = localization,
            addContactButton = purchaseInformation.store != Store.PLAY_STORE,
            contactEmail = contactEmail,
            onAction = onAction,
        )
    }
}

@Suppress("LongParameterList")
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
private fun NoActiveUserManagementView(
    screenTitle: String,
    screenSubtitle: String?,
    contactEmail: String?,
    localization: CustomerCenterConfigData.Localization,
    purchaseInformation: PurchaseInformation?,
    supportedPaths: List<HelpPath>,
    onAction: (CustomerCenterAction) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ContentUnavailableView(
            title = screenTitle,
            description = screenSubtitle,
            modifier = Modifier.padding(ManagementViewHorizontalPadding),
        )

        ManageSubscriptionsButtonsView(
            purchaseInformation = purchaseInformation,
            supportedPaths = supportedPaths,
            localization = localization,
            addContactButton = purchaseInformation?.store != Store.PLAY_STORE,
            contactEmail = contactEmail,
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(ContentUnavailableViewPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = null,
            modifier = Modifier.size(ContentUnavailableIconSize),
        )

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = ContentUnavailableViewPaddingTopTitle),
        )

        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = ContentUnavailableViewPaddingTopDescription),
            )
        }
    }
}

@Suppress("LongParameterList")
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
private fun ManageSubscriptionsButtonsView(
    purchaseInformation: PurchaseInformation?,
    supportedPaths: List<HelpPath>,
    localization: CustomerCenterConfigData.Localization,
    contactEmail: String?,
    addContactButton: Boolean = false,
    onAction: (CustomerCenterAction) -> Unit,
) {
    Column {
        HorizontalDivider(Modifier.padding(horizontal = ManagementViewHorizontalPadding))
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

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Preview(showBackground = true, device = "spec:width=412dp,height=915dp", group = "scale = 1", fontScale = 1F)
@Composable
private fun ManageSubscriptionsViewPreview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    val managementScreen = testData.screens[CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT]!!
    ManageSubscriptionsView(
        screenTitle = managementScreen.title,
        screenSubtitle = managementScreen.subtitle,
        screenType = managementScreen.type,
        supportedPaths = managementScreen.paths,
        contactEmail = testData.support.email,
        localization = testData.localization,
        purchaseInformation = CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing,
        onAction = {},
    )
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun NoActiveSubscriptionsViewPreview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    val noActiveScreen = testData.screens[CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE]!!

    ManageSubscriptionsView(
        screenTitle = noActiveScreen.title,
        screenSubtitle = noActiveScreen.subtitle,
        screenType = noActiveScreen.type,
        supportedPaths = noActiveScreen.paths,
        contactEmail = testData.support.email,
        localization = testData.localization,
        purchaseInformation = null,
        onAction = {},
    )
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun NoActiveSubscriptionsViewNoDescription_Preview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    val noActiveScreen = testData.screens[CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE]!!.copy(subtitle = null)

    ManageSubscriptionsView(
        screenTitle = noActiveScreen.title,
        screenSubtitle = noActiveScreen.subtitle,
        screenType = noActiveScreen.type,
        supportedPaths = noActiveScreen.paths,
        contactEmail = testData.support.email,
        localization = testData.localization,
        purchaseInformation = null,
        onAction = {},
    )
}
