@file:Suppress("TooManyFunctions")
@file:JvmSynthetic
@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.revenuecat.purchases.ui.revenuecatui.composables.ErrorDialog
import com.revenuecat.purchases.ui.revenuecatui.customercenter.actions.CustomerCenterAction
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.getColorForTheme
import com.revenuecat.purchases.ui.revenuecatui.customercenter.dialogs.RestorePurchasesDialog
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModel
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModelFactory
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.customercenter.views.FeedbackSurveyView
import com.revenuecat.purchases.ui.revenuecatui.customercenter.views.ManageSubscriptionsView
import com.revenuecat.purchases.ui.revenuecatui.customercenter.views.PromotionalOfferView
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesImpl
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import com.revenuecat.purchases.ui.revenuecatui.helpers.getActivity
import kotlinx.coroutines.launch

@JvmSynthetic
@Composable
internal fun InternalCustomerCenter(
    modifier: Modifier = Modifier,
    viewModel: CustomerCenterViewModel = getCustomerCenterViewModel(),
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    if (state is CustomerCenterState.NotLoaded) {
        coroutineScope.launch {
            viewModel.loadCustomerCenter()
        }
    }

    BackHandler {
        val buttonType = state.navigationButtonType
        viewModel.onNavigationButtonPressed()
        if (buttonType == CustomerCenterState.NavigationButtonType.CLOSE) {
            onDismiss()
        }
    }

    viewModel.actionError.value?.let {
        ErrorDialog(
            dismissRequest = viewModel::clearActionError,
            error = it.message,
        )
    }

    InternalCustomerCenter(
        state,
        modifier,
        onAction = { action ->
            when (action) {
                is CustomerCenterAction.PathButtonPressed -> {
                    coroutineScope.launch {
                        viewModel.pathButtonPressed(context, action.path, action.product)
                    }
                }

                is CustomerCenterAction.PerformRestore -> {
                    coroutineScope.launch {
                        viewModel.restorePurchases()
                    }
                }

                is CustomerCenterAction.DismissRestoreDialog -> viewModel.dismissRestoreDialog()
                is CustomerCenterAction.ContactSupport -> viewModel.contactSupport(context, action.email)
                is CustomerCenterAction.OpenURL -> viewModel.openURL(context, action.url)
                is CustomerCenterAction.NavigationButtonPressed -> {
                    val buttonType = state.navigationButtonType
                    viewModel.onNavigationButtonPressed()
                    if (buttonType == CustomerCenterState.NavigationButtonType.CLOSE) {
                        onDismiss()
                    }
                }

                is CustomerCenterAction.DismissPromotionalOffer ->
                    viewModel.dismissPromotionalOffer(action.originalPath, context)
                is CustomerCenterAction.PurchasePromotionalOffer -> {
                    val activity = context.getActivity()
                    coroutineScope.launch {
                        viewModel.onAcceptedPromotionalOffer(action.subscriptionOption, activity)
                    }
                }
            }
        },
    )
}

@Composable
private fun InternalCustomerCenter(
    state: CustomerCenterState,
    modifier: Modifier = Modifier,
    onAction: (CustomerCenterAction) -> Unit,
) {
    val title = getTitleForState(state)

    val colorScheme = if (state is CustomerCenterState.Success) {
        val isDark = isSystemInDarkTheme()
        val appearance: CustomerCenterConfigData.Appearance = state.customerCenterConfigData.appearance
        val accentColor = appearance.getColorForTheme(isDark) { it.accentColor }

        // Only change background when presenting a promotional offer
        val backgroundColor = if (state.promotionalOfferData != null) {
            appearance.getColorForTheme(isDark) { it.backgroundColor }
        } else {
            null
        }

        MaterialTheme.colorScheme.copy(
            primary = accentColor ?: MaterialTheme.colorScheme.primary,
            background = backgroundColor ?: MaterialTheme.colorScheme.background,
        )
    } else {
        MaterialTheme.colorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
    ) {
        CustomerCenterScaffold(
            modifier = modifier
                .background(MaterialTheme.colorScheme.background),
            title = title,
            onAction = onAction,
            navigationButtonType =
            if (state is CustomerCenterState.Success) {
                state.navigationButtonType
            } else {
                CustomerCenterState.NavigationButtonType.CLOSE
            },
        ) {
            when (state) {
                is CustomerCenterState.NotLoaded -> {}
                is CustomerCenterState.Loading -> CustomerCenterLoading()
                is CustomerCenterState.Error -> CustomerCenterError(state)
                is CustomerCenterState.Success -> CustomerCenterLoaded(
                    state,
                    onAction,
                )
            }
        }
    }
}

@Composable
private fun CustomerCenterScaffold(
    onAction: (CustomerCenterAction) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    navigationButtonType: CustomerCenterState.NavigationButtonType = CustomerCenterState.NavigationButtonType.CLOSE,
    mainContent: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            IconButton(onClick = {
                onAction(CustomerCenterAction.NavigationButtonPressed)
            }) {
                Icon(
                    imageVector = when (navigationButtonType) {
                        CustomerCenterState.NavigationButtonType.BACK -> Icons.AutoMirrored.Filled.ArrowBack
                        CustomerCenterState.NavigationButtonType.CLOSE -> Icons.Default.Close
                    },
                    contentDescription = null,
                )
            }
            title?.let {
                Text(
                    text = title,
                    modifier = Modifier.padding(start = 4.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
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

@Composable
private fun CustomerCenterLoaded(
    state: CustomerCenterState.Success,
    onAction: (CustomerCenterAction) -> Unit,
) {
    if (state.feedbackSurveyData != null) {
        FeedbackSurveyView(state.feedbackSurveyData)
    } else if (state.promotionalOfferData != null) {
        val promotionalOfferData = state.promotionalOfferData
        PromotionalOfferView(
            promotionalOfferData = promotionalOfferData,
            appearance = state.customerCenterConfigData.appearance,
            onAccept = { subscriptionOption ->
                onAction(CustomerCenterAction.PurchasePromotionalOffer(subscriptionOption))
            },
            onDismiss = {
                onAction(CustomerCenterAction.DismissPromotionalOffer(promotionalOfferData.originalPath))
            },
        )
    } else if (state.showRestoreDialog) {
        RestorePurchasesDialog(
            state = state.restorePurchasesState,
            onDismiss = { onAction(CustomerCenterAction.DismissRestoreDialog) },
            onRestore = { onAction(CustomerCenterAction.PerformRestore) },
            onContactSupport = {
                state.customerCenterConfigData.support.email?.let { email ->
                    onAction(CustomerCenterAction.ContactSupport(email))
                }
            },
        )
    } else {
        val configuration = state.customerCenterConfigData
        MainScreen(state, configuration, onAction)
    }
}

@Composable
private fun MainScreen(
    state: CustomerCenterState.Success,
    configuration: CustomerCenterConfigData,
    onAction: (CustomerCenterAction) -> Unit,
) {
    if (state.purchaseInformation != null) {
        configuration.getManagementScreen()?.let { managementScreen ->
            ManageSubscriptionsView(
                screen = managementScreen,
                localization = configuration.localization,
                purchaseInformation = state.purchaseInformation,
                support = configuration.support,
                onAction = onAction,
            )
        } ?: run {
            // Handle missing management screen
            // WrongPlatformView
        }
    } else {
        configuration.getNoActiveScreen()?.let { noActiveScreen ->
            ManageSubscriptionsView(
                screen = noActiveScreen,
                localization = configuration.localization,
                support = configuration.support,
                onAction = onAction,
            )
        } ?: run {
            // Fallback with a restore button
            // NoSubscriptionsView(configuration = configuration)
        }
    }
}

private fun getTitleForState(state: CustomerCenterState): String? {
    return when (state) {
        is CustomerCenterState.Success -> {
            state.title
        }

        else -> null
    }
}

@Composable
private fun getCustomerCenterViewModel(
    purchases: PurchasesType = PurchasesImpl(),
    viewModel: CustomerCenterViewModel = viewModel<CustomerCenterViewModelImpl>(
        factory = CustomerCenterViewModelFactory(purchases),
    ),
): CustomerCenterViewModel {
    return viewModel
}

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
                    openMethod = null,
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

@Preview
@Composable
internal fun CustomerCenterLoadingPreview() {
    InternalCustomerCenter(
        state = CustomerCenterState.Loading,
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        onAction = {},
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
        onAction = {},
    )
}

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
        onAction = {},
    )
}
