package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.customercenter.SubscriptionDetailsView
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
internal fun ManageSubscriptionsView(
    screen: CustomerCenterConfigData.Screen,
    modifier: Modifier = Modifier,
    purchaseInformation: PurchaseInformation? = null,
    onPathButtonPress: (CustomerCenterConfigData.HelpPath) -> Unit,
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
                    purchaseInformation = purchaseInformation,
                    onDetermineFlow = onPathButtonPress,
                )
            } ?: NoActiveUserManagementView(
                screen = screen,
                onDetermineFlow = onPathButtonPress,
            )
        }
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
private fun ActiveUserManagementView(
    screen: CustomerCenterConfigData.Screen,
    purchaseInformation: PurchaseInformation,
    onDetermineFlow: (CustomerCenterConfigData.HelpPath) -> Unit,
) {
    Column {
        Text(
            text = screen.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
        )

        screen.subtitle?.let { subtitle ->
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }

        LazyColumn {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp,
                    ),
                ) {
                    SubscriptionDetailsView(details = purchaseInformation)
                }
            }

            item {
                ManageSubscriptionsButtonsView(screen = screen, onDetermineFlow = onDetermineFlow)
            }
        }
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
private fun NoActiveUserManagementView(
    screen: CustomerCenterConfigData.Screen,
    onDetermineFlow: (CustomerCenterConfigData.HelpPath) -> Unit,
) {
    Column {
        CompatibilityContentUnavailableView(
            title = screen.title,
            drawableResId = R.drawable.warning,
            description = screen.subtitle,
        )

        ManageSubscriptionsButtonsView(
            screen = screen,
            onDetermineFlow = onDetermineFlow,
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
        purchaseInformation = CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing,
        onPathButtonPress = {},
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
        purchaseInformation = null,
        onPathButtonPress = {},
    )
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
private fun ManageSubscriptionsButtonsView(
    screen: CustomerCenterConfigData.Screen,
    onDetermineFlow: (CustomerCenterConfigData.HelpPath) -> Unit,
    useOutlinedButton: Boolean = false,
) {
    Column {
        screen.supportedPaths.forEach { path ->
            ManageSubscriptionButton(
                path = path,
                onDetermineFlow = onDetermineFlow,
                useOutlinedButton = useOutlinedButton,
            )
        }
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
private fun ManageSubscriptionButton(
    path: CustomerCenterConfigData.HelpPath,
    onDetermineFlow: (CustomerCenterConfigData.HelpPath) -> Unit,
    useOutlinedButton: Boolean,
) {
    val buttonModifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)

    val buttonContent: @Composable () -> Unit = {
        Text(
            text = path.title,
            color = if (useOutlinedButton) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
        )
    }

    if (useOutlinedButton) {
        OutlinedButton(
            onClick = { onDetermineFlow(path) },
            modifier = buttonModifier,
        ) {
            buttonContent()
        }
    } else {
        Button(
            onClick = { onDetermineFlow(path) },
            modifier = buttonModifier,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            buttonContent()
        }
    }
}
