package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import android.net.Uri
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
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

@SuppressWarnings("LongParameterList")
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
internal fun ManageSubscriptionsView(
    screen: CustomerCenterConfigData.Screen,
    localization: CustomerCenterConfigData.Localization,
    support: CustomerCenterConfigData.Support,
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
        if (screen.type == CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT && purchaseInformation != null) {
            ActiveUserManagementView(
                screen,
                localization,
                purchaseInformation,
                support,
                onAction,
            )
        } else {
            NoActiveUserManagementView(
                screen,
                null,
                onButtonPress = {
                    onAction(CustomerCenterAction.PathButtonPressed(it, product = null))
                },
            )
        }
    }
}

@SuppressWarnings("LongParameterList")
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
private fun ActiveUserManagementView(
    screen: CustomerCenterConfigData.Screen,
    localization: CustomerCenterConfigData.Localization,
    purchaseInformation: PurchaseInformation,
    support: CustomerCenterConfigData.Support,
    onAction: (CustomerCenterAction) -> Unit,
) {
    Column {
        Text(
            text = screen.title,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(
                start = ManagementViewHorizontalPadding,
                end = ManagementViewHorizontalPadding,
                top = ManagementViewTitleTopPadding,
            ),
        )

        Spacer(modifier = Modifier.size(ManagementViewSpacer))

        SubscriptionDetailsView(details = purchaseInformation, localization = localization)

        if (purchaseInformation.store == Store.PLAY_STORE) {
            ManageSubscriptionsButtonsView(
                screen,
                purchaseInformation,
                onButtonPress = {
                    onAction(CustomerCenterAction.PathButtonPressed(it, purchaseInformation.product))
                },
            )
        } else {
            OtherPlatformSubscriptionButtonsView(
                localization = localization,
                support = support,
                managementURL = purchaseInformation.managementURL,
                onAction = onAction,
            )
        }
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
private fun NoActiveUserManagementView(
    screen: CustomerCenterConfigData.Screen,
    purchaseInformation: PurchaseInformation?,
    onButtonPress: (CustomerCenterConfigData.HelpPath) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ContentUnavailableView(
            title = screen.title,
            description = screen.subtitle,
            modifier = Modifier.padding(ManagementViewHorizontalPadding),
        )

        ManageSubscriptionsButtonsView(
            screen,
            purchaseInformation,
            onButtonPress,
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
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
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

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
private fun ManageSubscriptionsButtonsView(
    screen: CustomerCenterConfigData.Screen,
    purchaseInformation: PurchaseInformation?,
    onButtonPress: (CustomerCenterConfigData.HelpPath) -> Unit,
) {
    val filteredPaths = if (purchaseInformation?.isLifetime == true) {
        screen.supportedPaths.filter { it.type != CustomerCenterConfigData.HelpPath.PathType.CANCEL }
    } else {
        screen.supportedPaths
    }

    Column {
        HorizontalDivider(Modifier.padding(horizontal = ManagementViewHorizontalPadding))
        filteredPaths.forEach { path ->
            SettingsButton(
                onClick = { onButtonPress(path) },
                title = path.title,
            )
        }
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
private fun OtherPlatformSubscriptionButtonsView(
    localization: CustomerCenterConfigData.Localization,
    support: CustomerCenterConfigData.Support,
    managementURL: Uri?,
    onAction: (CustomerCenterAction) -> Unit,
) {
    Column {
        HorizontalDivider(Modifier.padding(horizontal = ManagementViewHorizontalPadding))

        managementURL?.let {
            SettingsButton(
                onClick = { onAction(CustomerCenterAction.OpenURL(it.toString())) },
                title = localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.MANAGE_SUBSCRIPTION,
                ),
            )
        }
        support.email?.let {
            SettingsButton(
                onClick = { onAction(CustomerCenterAction.ContactSupport(it)) },
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
        screen = managementScreen,
        localization = testData.localization,
        support = testData.support,
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
        screen = noActiveScreen,
        localization = testData.localization,
        support = testData.support,
        purchaseInformation = null,
        onAction = {},
    )
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun NoActiveSubscriptionsViewNoDescription_Preview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    val noActiveScreen = testData.screens[CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE]!!

    ManageSubscriptionsView(
        screen = noActiveScreen.copy(subtitle = null),
        localization = testData.localization,
        support = testData.support,
        purchaseInformation = null,
        onAction = {},
    )
}
