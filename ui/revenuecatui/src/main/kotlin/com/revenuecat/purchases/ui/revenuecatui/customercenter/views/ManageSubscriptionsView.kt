package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.customercenter.SubscriptionDetailsView
import com.revenuecat.purchases.ui.revenuecatui.customercenter.actions.CustomerCenterAction
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            purchaseInformation?.let { purchaseInformation ->
                ActiveUserManagementView(
                    screen = screen,
                    localization = localization,
                    purchaseInformation = purchaseInformation,
                    support = support,
                    onAction = onAction,
                )
            } ?: NoActiveUserManagementView(
                screen = screen,
                onPathButtonPress = {
                    onAction(CustomerCenterAction.PathButtonPressed(it))
                },
            )
        }
    }
}

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
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(end = 16.dp, top = 32.dp),
        )

        screen.subtitle?.let { subtitle ->
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Spacer(modifier = Modifier.size(32.dp))

        SubscriptionDetailsView(details = purchaseInformation, localization = localization)

        Spacer(modifier = Modifier.size(32.dp))

        Surface(
            shape = MaterialTheme.shapes.medium,
        ) {
            if (purchaseInformation.store == Store.PLAY_STORE) {
                ManageSubscriptionsButtonsView(screen = screen, onButtonPress = {
                    onAction(CustomerCenterAction.PathButtonPressed(it))
                })
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
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
private fun NoActiveUserManagementView(
    screen: CustomerCenterConfigData.Screen,
    onPathButtonPress: (CustomerCenterConfigData.HelpPath) -> Unit,
) {
    Column {
        CompatibilityContentUnavailableView(
            title = screen.title,
            drawableResId = R.drawable.warning,
            description = screen.subtitle,
        )

        ManageSubscriptionsButtonsView(
            screen = screen,
            onButtonPress = onPathButtonPress,
            useOutlinedButton = true,
        )
    }
}

@Composable
fun CompatibilityContentUnavailableView(
    title: String,
    drawableResId: Int,
    description: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = drawableResId),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .padding(bottom = 8.dp),
        )

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp),
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
@Composable
private fun ManageSubscriptionsButtonsView(
    screen: CustomerCenterConfigData.Screen,
    onButtonPress: (CustomerCenterConfigData.HelpPath) -> Unit,
    useOutlinedButton: Boolean = false,
) {
    Column {
        screen.supportedPaths.forEach { path ->
            ManageSubscriptionButton(
                path = path,
                onButtonPress = onButtonPress,
                useOutlinedButton = useOutlinedButton,
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
        managementURL?.let {
            CustomerCenterButton(
                onClick = { onAction(CustomerCenterAction.OpenURL(it)) },
                buttonContent = { modifier ->
                    Text(
                        text = localization.commonLocalizedString(
                            CustomerCenterConfigData.Localization.CommonLocalizedString.MANAGE_SUBSCRIPTION,
                        ),
                        modifier = modifier,
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
            )
        }
        support.email?.let {
            CustomerCenterButton(
                onClick = { onAction(CustomerCenterAction.ContactSupport(it)) },
                buttonContent = { modifier ->
                    Text(
                        text = localization.commonLocalizedString(
                            CustomerCenterConfigData.Localization.CommonLocalizedString.CONTACT_SUPPORT,
                        ),
                        modifier = modifier,
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
private fun ManageSubscriptionButton(
    path: CustomerCenterConfigData.HelpPath,
    onButtonPress: (CustomerCenterConfigData.HelpPath) -> Unit,
    useOutlinedButton: Boolean,
) {
    CustomerCenterButton(
        onClick = { onButtonPress(path) },
        useOutlinedButton = useOutlinedButton,
        buttonContent = { modifier ->
            Text(
                text = path.title,
                modifier = modifier,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
    )
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
private fun CustomerCenterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    useOutlinedButton: Boolean = false,
    buttonContent: @Composable (Modifier) -> Unit,
) {
    val buttonModifier = modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)

    if (useOutlinedButton) {
        OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        ) {
            buttonContent(Modifier)
        }
    } else {
        val layoutDirection = LocalLayoutDirection.current
        val totalHorizontalButtonPadding = 16.dp

        val startPadding = totalHorizontalButtonPadding -
            ButtonDefaults.TextButtonContentPadding.calculateStartPadding(layoutDirection)
        val endPadding = totalHorizontalButtonPadding -
            ButtonDefaults.TextButtonContentPadding.calculateEndPadding(layoutDirection)

        TextButton(
            onClick = onClick,
            modifier = buttonModifier
                .heightIn(60.dp)
                .padding(start = startPadding, end = endPadding),
            // It's a rectangle so it gets clipped by the parent Surface.
            shape = RectangleShape,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        ) {
            buttonContent(Modifier.fillMaxWidth())
        }
    }
}
