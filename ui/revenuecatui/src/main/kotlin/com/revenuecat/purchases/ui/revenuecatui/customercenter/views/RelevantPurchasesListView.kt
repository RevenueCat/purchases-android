package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.HelpPath
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ContentUnavailableIconSize
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ContentUnavailableViewPadding
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ContentUnavailableViewPaddingTopDescription
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ContentUnavailableViewPaddingTopTitle
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ManagementViewHorizontalPadding
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ManagementViewSpacer
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ManagementViewTitleTopPadding
import com.revenuecat.purchases.ui.revenuecatui.customercenter.actions.CustomerCenterAction
import com.revenuecat.purchases.ui.revenuecatui.customercenter.composables.SettingsButton
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation
import com.revenuecat.purchases.ui.revenuecatui.customercenter.theme.CustomerCenterPreviewTheme

@Suppress("LongParameterList")
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
internal fun RelevantPurchasesListView(
    screenTitle: String,
    screenSubtitle: String?,
    screenType: CustomerCenterConfigData.Screen.ScreenType,
    supportedPaths: List<HelpPath>,
    localization: CustomerCenterConfigData.Localization,
    modifier: Modifier = Modifier,
    purchaseInformation: List<PurchaseInformation> = emptyList(),
    onPurchaseSelected: (PurchaseInformation) -> Unit,
    onAction: (CustomerCenterAction) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (screenType == CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT && purchaseInformation.isNotEmpty()) {
            val restorePath = supportedPaths.firstOrNull { it.type == HelpPath.PathType.MISSING_PURCHASE }

            ActiveUserManagementView(
                screenTitle,
                localization,
                purchaseInformation,
                onPurchaseSelected,
            )

            if (purchaseInformation.size > 1 && restorePath != null) {
                ManageSubscriptionsButtonsView(
                    supportedPaths = listOf(restorePath),
                    onButtonPress = { onAction(CustomerCenterAction.PathButtonPressed(it, product = null)) },
                )
            }
        } else {
            NoActiveUserManagementView(
                screenTitle,
                screenSubtitle,
                supportedPaths,
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
    screenTitle: String,
    localization: CustomerCenterConfigData.Localization,
    purchaseInformation: List<PurchaseInformation>,
    onPurchaseSelected: (PurchaseInformation) -> Unit,
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

        // Split purchases into subscriptions and non-subscriptions
        val subscriptions = purchaseInformation.filter { it.product?.period != null }
        val nonSubscriptions = purchaseInformation.filter { it.product?.period == null }

        if (subscriptions.isNotEmpty()) {
            subscriptions.forEachIndexed { index, info ->
                if (index > 0) {
                    Spacer(modifier = Modifier.size(2.dp))
                }

                val position = when {
                    subscriptions.size == 1 -> ButtonPosition.SINGLE
                    index == 0 -> ButtonPosition.FIRST
                    index == subscriptions.size - 1 -> ButtonPosition.LAST
                    else -> ButtonPosition.MIDDLE
                }
                PurchaseInformationCardView(
                    purchaseInformation = info,
                    localization = localization,
                    position = position,
                    onCardClick = { onPurchaseSelected(info) },
                    isDetailedView = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        if (nonSubscriptions.isNotEmpty()) {
            Spacer(modifier = Modifier.size(24.dp))

            if (subscriptions.isNotEmpty()) {
                Text(
                    text = "Purchases",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        start = ManagementViewHorizontalPadding,
                        bottom = 8.dp,
                    ),
                )
            }

            nonSubscriptions.forEachIndexed { index, info ->
                if (index > 0) {
                    Spacer(modifier = Modifier.size(2.dp))
                }

                val position = when {
                    nonSubscriptions.size == 1 -> ButtonPosition.SINGLE
                    index == 0 -> ButtonPosition.FIRST
                    index == nonSubscriptions.size - 1 -> ButtonPosition.LAST
                    else -> ButtonPosition.MIDDLE
                }
                PurchaseInformationCardView(
                    purchaseInformation = info,
                    localization = localization,
                    position = position,
                    onCardClick = { onPurchaseSelected(info) },
                    isDetailedView = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
private fun NoActiveUserManagementView(
    screenTitle: String,
    screenSubtitle: String?,
    supportedPaths: List<HelpPath>,
    onButtonPress: (CustomerCenterConfigData.HelpPath) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ContentUnavailableView(
            title = screenTitle,
            description = screenSubtitle,
            modifier = Modifier.padding(ManagementViewHorizontalPadding),
        )

        ManageSubscriptionsButtonsView(
            supportedPaths,
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

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
internal fun ManageSubscriptionsButtonsView(
    supportedPaths: List<HelpPath>,
    onButtonPress: (CustomerCenterConfigData.HelpPath) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = ManagementViewHorizontalPadding)
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        supportedPaths.forEachIndexed { index, path ->
            SettingsButton(
                onClick = { onButtonPress(path) },
                title = path.title,
            )
        }
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
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

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Preview(showBackground = true, device = "spec:width=412dp,height=915dp", group = "scale = 1", fontScale = 1F)
@Composable
private fun RelevantPurchasesListViewPreview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    val managementScreen = testData.screens[CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT]!!
    CustomerCenterPreviewTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            RelevantPurchasesListView(
                screenTitle = managementScreen.title,
                screenSubtitle = managementScreen.subtitle,
                screenType = managementScreen.type,
                supportedPaths = managementScreen.supportedPaths,
                localization = testData.localization,
                purchaseInformation = listOf(CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing),
                onPurchaseSelected = {},
                onAction = {},
            )
        }
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun NoActiveSubscriptionsViewPreview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    val noActiveScreen = testData.screens[CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE]!!
    CustomerCenterPreviewTheme {
        Column {
            RelevantPurchasesListView(
                screenTitle = noActiveScreen.title,
                screenSubtitle = noActiveScreen.subtitle,
                screenType = noActiveScreen.type,
                supportedPaths = noActiveScreen.supportedPaths,
                localization = testData.localization,
                purchaseInformation = emptyList(),
                onPurchaseSelected = {},
                onAction = {},
            )
        }
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun RelevantPurchasesListViewWithLifetimePurchasePreview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    val managementScreen = testData.screens[CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT]!!
    CustomerCenterPreviewTheme {
        Column {
            RelevantPurchasesListView(
                screenTitle = managementScreen.title,
                screenSubtitle = managementScreen.subtitle,
                screenType = managementScreen.type,
                supportedPaths = managementScreen.supportedPaths,
                localization = testData.localization,
                purchaseInformation = listOf(
                    CustomerCenterConfigTestData.purchaseInformationLifetime,
                ),
                onPurchaseSelected = {},
                onAction = {},
            )
        }
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun RelevantPurchasesListViewWithSubscriptionsAndLifetimePurchasePreview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    val managementScreen = testData.screens[CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT]!!
    CustomerCenterPreviewTheme {
        Column {
            RelevantPurchasesListView(
                screenTitle = managementScreen.title,
                screenSubtitle = managementScreen.subtitle,
                screenType = managementScreen.type,
                supportedPaths = managementScreen.supportedPaths,
                localization = testData.localization,
                purchaseInformation = listOf(
                    CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing,
                    CustomerCenterConfigTestData.purchaseInformationYearlyExpiring,
                    CustomerCenterConfigTestData.purchaseInformationLifetime,
                ),
                onPurchaseSelected = {},
                onAction = {},
            )
        }
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun NoActiveSubscriptionsViewNoDescription_Preview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    val noActiveScreen = testData.screens[CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE]!!.copy(subtitle = null)

    CustomerCenterPreviewTheme {
        Column {
            RelevantPurchasesListView(
                screenTitle = noActiveScreen.title,
                screenSubtitle = noActiveScreen.subtitle,
                screenType = noActiveScreen.type,
                supportedPaths = noActiveScreen.supportedPaths,
                localization = testData.localization,
                purchaseInformation = emptyList(),
                onPurchaseSelected = {},
                onAction = {},
            )
        }
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Preview(showBackground = true, device = "spec:width=412dp,height=915dp", group = "scale = 1", fontScale = 1F)
@Composable
private fun RelevantPurchasesListViewWithMultiplePurchasesPreview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    val managementScreen = testData.screens[CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT]!!
    CustomerCenterPreviewTheme {
        Column {
            RelevantPurchasesListView(
                screenTitle = managementScreen.title,
                screenSubtitle = managementScreen.subtitle,
                screenType = managementScreen.type,
                supportedPaths = managementScreen.supportedPaths,
                localization = testData.localization,
                purchaseInformation = listOf(
                    CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing,
                    CustomerCenterConfigTestData.purchaseInformationYearlyExpiring,
                ),
                onPurchaseSelected = {},
                onAction = {},
            )
        }
    }
}
