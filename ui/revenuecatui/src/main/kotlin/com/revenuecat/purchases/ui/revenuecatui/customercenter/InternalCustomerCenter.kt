@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModel
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModelFactory
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.customercenter.views.ManageSubscriptionsView
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesImpl
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import kotlinx.coroutines.launch

@Composable
internal fun InternalCustomerCenter(
    modifier: Modifier = Modifier,
    viewModel: CustomerCenterViewModel = getCustomerCenterViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    InternalCustomerCenter(
        state,
        modifier,
        onDetermineFlow = { path ->
            coroutineScope.launch {
                viewModel.determineFlow(path)
            }
        },
        onPerformRestore = {
            coroutineScope.launch {
                viewModel.restorePurchases()
            }
        },
        onDismissRestoreDialog = {
            viewModel.dismissRestoreDialog()
        },
        onContactSupport = { email ->
            viewModel.contactSupport(context, email)
        },
    )
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
private fun InternalCustomerCenter(
    state: CustomerCenterState,
    modifier: Modifier = Modifier,
    onDetermineFlow: (CustomerCenterConfigData.HelpPath) -> Unit,
    onPerformRestore: () -> Unit,
    onDismissRestoreDialog: () -> Unit,
    onContactSupport: (String) -> Unit,
) {
    CustomerCenterScaffold(modifier) {
        when (state) {
            is CustomerCenterState.Loading -> CustomerCenterLoading()
            is CustomerCenterState.Error -> CustomerCenterError(state)
            is CustomerCenterState.Success -> CustomerCenterLoaded(
                state,
                onDetermineFlow,
                onPerformRestore,
                onDismissRestoreDialog,
                onContactSupport,
            )
        }
    }
}

@Composable
private fun CustomerCenterScaffold(
    modifier: Modifier = Modifier,
    mainContent: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        mainContent()
    }
}

@Composable
private fun CustomerCenterLoading() {
    // CustomerCenter WIP: Add proper loading UI
    Text("Loading...")
}

@Composable
private fun CustomerCenterError(state: CustomerCenterState.Error) {
    // CustomerCenter WIP: Add proper error UI
    Text("Error: ${state.error}")
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
private fun CustomerCenterLoaded(
    state: CustomerCenterState.Success,
    onDetermineFlow: (CustomerCenterConfigData.HelpPath) -> Unit,
    onPerformRestore: () -> Unit,
    onDismissRestoreDialog: () -> Unit,
    onContactSupport: (String) -> Unit,
) {
    if (state.showRestoreDialog) {
        RestorePurchasesDialog(
            state = state.restorePurchasesState,
            onDismiss = onDismissRestoreDialog,
            onRestore = onPerformRestore,
            onContactSupport = {
                state.customerCenterConfigData.support.email?.let { email ->
                    onContactSupport(email)
                }
            },
        )
    }

    val configuration = state.customerCenterConfigData
    if (state.purchaseInformation != null) {
        configuration.getManagementScreen()?.let { managementScreen ->
            ManageSubscriptionsView(
                screen = managementScreen,
                purchaseInformation = state.purchaseInformation,
                onDetermineFlow = onDetermineFlow,
            )
        } ?: run {
            // Handle missing management screen
            // WrongPlatformView
        }
    } else {
        configuration.getNoActiveScreen()?.let { noActiveScreen ->
            ManageSubscriptionsView(
                screen = noActiveScreen,
                onDetermineFlow = onDetermineFlow,
            )
        } ?: run {
            // Fallback with a restore button
            // NoSubscriptionsView(configuration = configuration)
        }
    }
}

@Composable
private fun RestorePurchasesDialog(
    state: RestorePurchasesState,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onContactSupport: () -> Unit,
) {
    var isRestoring by remember { mutableStateOf(false) }

    when (state) {
        RestorePurchasesState.INITIAL -> {
            if (isRestoring) {
                AlertDialog(
                    onDismissRequest = { /* Prevent dismiss while restoring */ },
                    title = {
                        Text(
                            text = "Restoring Purchases...",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    },
                    confirmButton = { },
                )
            } else {
                AlertDialog(
                    onDismissRequest = {
                        isRestoring = false
                        onDismiss()
                    },
                    title = {
                        Text(
                            text = "Restore Purchases",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    },
                    text = {
                        Text(
                            text = "Going to check for previous purchases",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                isRestoring = true
                                onRestore()
                            },
                        ) {
                            Text("Check Past Purchases")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                isRestoring = false
                                onDismiss()
                            },
                        ) {
                            Text("Cancel")
                        }
                    },
                )
            }
        }
        RestorePurchasesState.PURCHASES_RECOVERED -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Text(
                        text = "Purchases Recovered",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                text = {
                    Text(
                        text = "Your purchases have been restored successfully",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                confirmButton = {
                    Button(onClick = onDismiss) {
                        Text("Dismiss")
                    }
                },
            )
        }
        RestorePurchasesState.PURCHASES_NOT_FOUND -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Text(
                        text = "No Purchases Found",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                text = {
                    Text(
                        text = "No previous purchases were found",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                confirmButton = {
                    Button(onClick = onContactSupport) {
                        Text("Contact Support")
                    }
                },
                dismissButton = {
                    Button(onClick = onDismiss) {
                        Text("Dismiss")
                    }
                },
            )
        }
    }
}

internal enum class RestorePurchasesState {
    INITIAL,
    PURCHASES_RECOVERED,
    PURCHASES_NOT_FOUND,
}

@Composable
internal fun getCustomerCenterViewModel(
    purchases: PurchasesType = PurchasesImpl(),
    viewModel: CustomerCenterViewModel = viewModel<CustomerCenterViewModelImpl>(
        factory = CustomerCenterViewModelFactory(purchases),
    ),
): CustomerCenterViewModel {
    return viewModel
}

@Preview
@Composable
internal fun CustomerCenterLoadingPreview() {
    InternalCustomerCenter(
        state = CustomerCenterState.Loading,
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        onDetermineFlow = {},
        onPerformRestore = {},
        onDismissRestoreDialog = {},
        onContactSupport = {},
    )
}

@Preview
@Composable
internal fun CustomerCenterErrorPreview() {
    InternalCustomerCenter(
        state = CustomerCenterState.Error(PurchasesError(PurchasesErrorCode.UnknownBackendError)),
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        onDetermineFlow = {},
        onPerformRestore = {},
        onDismissRestoreDialog = {},
        onContactSupport = {},
    )
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Preview
@Composable
internal fun CustomerCenterLoadedPreview() {
    InternalCustomerCenter(
        state = CustomerCenterState.Success(
            customerCenterConfigData = previewConfigData,
            purchaseInformation = CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing,
        ),
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        onDetermineFlow = {},
        onPerformRestore = {},
        onDismissRestoreDialog = {},
        onContactSupport = {},
    )
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
private val previewConfigData = CustomerCenterConfigData(
    screens = mapOf(
        CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT to CustomerCenterConfigData.Screen(
            type = CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT,
            title = "Manage Subscription",
            subtitle = "Manage subscription subtitle",
            paths = listOf(
                CustomerCenterConfigData.HelpPath(
                    id = "path-id-1",
                    title = "Subscription",
                    type = CustomerCenterConfigData.HelpPath.PathType.CANCEL,
                    promotionalOffer = null,
                    feedbackSurvey = null,
                ),
            ),
        ),
    ),
    appearance = CustomerCenterConfigData.Appearance(),
    localization = CustomerCenterConfigData.Localization(
        locale = "en_US",
        localizedStrings = mapOf(
            "cancel" to "Cancel",
            "subscription" to "Subscription",
        ),
    ),
    support = CustomerCenterConfigData.Support(email = "test@revenuecat.com"),
)
