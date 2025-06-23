@file:Suppress("TooManyFunctions")
@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterListener
import com.revenuecat.purchases.ui.revenuecatui.composables.ErrorDialog
import com.revenuecat.purchases.ui.revenuecatui.customercenter.actions.CustomerCenterAction
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.getColorForTheme
import com.revenuecat.purchases.ui.revenuecatui.customercenter.dialogs.RestorePurchasesDialog
import com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation.CustomerCenterDestination
import com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation.CustomerCenterNavigationViewModel
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModel
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModelFactory
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.customercenter.views.FeedbackSurveyView
import com.revenuecat.purchases.ui.revenuecatui.customercenter.views.ManageSubscriptionsView
import com.revenuecat.purchases.ui.revenuecatui.customercenter.views.PromotionalOfferScreen
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesImpl
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import com.revenuecat.purchases.ui.revenuecatui.helpers.getActivity
import kotlinx.coroutines.launch

private sealed class NavigationAction {
    data class NavigateTo(val destination: CustomerCenterDestination) : NavigationAction()
    data class ReplaceCurrentDestination(val destination: CustomerCenterDestination) : NavigationAction()
    object NavigateBack : NavigationAction()
    object NavigateToMain : NavigationAction()
}

@Suppress("LongMethod")
@JvmSynthetic
@Composable
internal fun InternalCustomerCenter(
    modifier: Modifier = Modifier,
    listener: CustomerCenterListener? = null,
    viewModel: CustomerCenterViewModel = getCustomerCenterViewModel(
        isDarkMode = isSystemInDarkTheme(),
        listener = listener,
    ),
    onDismiss: () -> Unit,
) {
    viewModel.refreshStateIfColorsChanged(MaterialTheme.colorScheme, isSystemInDarkTheme())

    val state by viewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    if (state is CustomerCenterState.NotLoaded) {
        coroutineScope.launch {
            viewModel.loadCustomerCenter()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.trackImpressionIfNeeded()
    }

    BackHandler {
        viewModel.onNavigationButtonPressed(context, onDismiss)
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
                    viewModel.pathButtonPressed(context, action.path, action.purchaseInformation)
                }

                is CustomerCenterAction.PerformRestore -> {
                    coroutineScope.launch {
                        viewModel.restorePurchases()
                    }
                }

                is CustomerCenterAction.DismissRestoreDialog ->
                    coroutineScope.launch {
                        viewModel.dismissRestoreDialog()
                    }

                is CustomerCenterAction.ContactSupport -> viewModel.contactSupport(context, action.email)
                is CustomerCenterAction.OpenURL -> viewModel.openURL(context, action.url)
                is CustomerCenterAction.NavigationButtonPressed -> {
                    viewModel.onNavigationButtonPressed(context, onDismiss)
                }

                is CustomerCenterAction.DismissPromotionalOffer ->
                    viewModel.dismissPromotionalOffer(context, action.originalPath)

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

    // Create navigation ViewModel at the top level
    val navigationViewModel = if (state is CustomerCenterState.Success) {
        viewModel<CustomerCenterNavigationViewModel>()
    } else {
        null
    }

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
                is CustomerCenterState.Success -> {
                    navigationViewModel?.let { navViewModel ->
                        CustomerCenterLoaded(
                            state = state,
                            onAction = onAction,
                            navigationViewModel = navViewModel,
                        )
                    }
                }
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
    Text("Loading...")
}

@Composable
private fun CustomerCenterError(state: CustomerCenterState.Error) {
    Text("Error: ${state.error}")
}

@Composable
private fun CustomerCenterLoaded(
    state: CustomerCenterState.Success,
    onAction: (CustomerCenterAction) -> Unit,
    navigationViewModel: CustomerCenterNavigationViewModel,
) {
    val navigationState by navigationViewModel.navigationState.collectAsState()

    LaunchedEffect(state.feedbackSurveyData, state.promotionalOfferData, state.restorePurchasesState) {
        updateNavigationFromState(
            state = state,
            currentDestination = navigationState.currentDestination,
            onNavigationAction = { action ->
                when (action) {
                    is NavigationAction.NavigateTo -> navigationViewModel.navigateTo(action.destination)
                    is NavigationAction.NavigateBack -> navigationViewModel.navigateBack()
                    is NavigationAction.NavigateToMain -> navigationViewModel.navigateToMain()
                    is NavigationAction.ReplaceCurrentDestination -> navigationViewModel.replaceCurrentDestination(
                        action.destination,
                    )
                }
            },
        )
    }

    CustomerCenterNavigationHost(
        currentDestination = navigationState.currentDestination,
        customerCenterState = state,
        onAction = onAction,
        navigationViewModel = navigationViewModel,
    )
}

private fun getTitleForState(state: CustomerCenterState): String? {
    return when (state) {
        is CustomerCenterState.Success -> {
            state.title
        }

        else -> null
    }
}

private fun getAnimationForTransition(
    navigationViewModel: CustomerCenterNavigationViewModel,
) = if (navigationViewModel.isLastActionBackward()) {
    slideInHorizontally(initialOffsetX = { -it }) togetherWith
        slideOutHorizontally(targetOffsetX = { it })
} else {
    slideInHorizontally(initialOffsetX = { it }) togetherWith
        slideOutHorizontally(targetOffsetX = { -it })
}

@Composable
private fun CustomerCenterNavigationHost(
    currentDestination: CustomerCenterDestination,
    customerCenterState: CustomerCenterState.Success,
    onAction: (CustomerCenterAction) -> Unit,
    navigationViewModel: CustomerCenterNavigationViewModel,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                getAnimationForTransition(navigationViewModel)
            },
            label = "CustomerCenterNavigation",
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
        ) { destination ->
            when (destination) {
                is CustomerCenterDestination.Main -> {
                    MainScreenContent(
                        state = customerCenterState,
                        onAction = onAction,
                    )
                }

                is CustomerCenterDestination.FeedbackSurvey -> {
                    FeedbackSurveyView(destination.data)
                }

                is CustomerCenterDestination.PromotionalOffer -> {
                    PromotionalOfferScreen(
                        promotionalOfferData = destination.data,
                        appearance = customerCenterState.customerCenterConfigData.appearance,
                        localization = customerCenterState.customerCenterConfigData.localization,
                        onAccept = { subscriptionOption ->
                            onAction(CustomerCenterAction.PurchasePromotionalOffer(subscriptionOption))
                        },
                        onDismiss = {
                            onAction(CustomerCenterAction.DismissPromotionalOffer(destination.data.originalPath))
                        },
                    )
                }
            }
        }

        // Show RestorePurchases dialog as overlay
        if (customerCenterState.restorePurchasesState != null) {
            RestorePurchasesDialog(
                state = customerCenterState.restorePurchasesState,
                localization = customerCenterState.customerCenterConfigData.localization,
                onDismiss = { onAction(CustomerCenterAction.DismissRestoreDialog) },
                onRestore = { onAction(CustomerCenterAction.PerformRestore) },
                onContactSupport = customerCenterState.customerCenterConfigData.support.email?.let { email ->
                    {
                        onAction(CustomerCenterAction.ContactSupport(email))
                    }
                },
            )
        }
    }
}

@Composable
private fun MainScreenContent(
    state: CustomerCenterState.Success,
    onAction: (CustomerCenterAction) -> Unit,
) {
    val configuration = state.customerCenterConfigData

    if (state.purchaseInformation != null) {
        configuration.getManagementScreen()?.let { managementScreen ->
            ManageSubscriptionsView(
                screenTitle = managementScreen.title,
                screenSubtitle = managementScreen.subtitle,
                screenType = managementScreen.type,
                supportedPaths = state.supportedPathsForManagementScreen ?: emptyList(),
                contactEmail = configuration.support.email,
                localization = configuration.localization,
                purchaseInformation = state.purchaseInformation,
                onAction = onAction,
            )
        } ?: run {
            // Handle missing management screen
            // WrongPlatformView
        }
    } else {
        configuration.getNoActiveScreen()?.let { noActiveScreen ->
            ManageSubscriptionsView(
                screenTitle = noActiveScreen.title,
                screenSubtitle = noActiveScreen.subtitle,
                screenType = noActiveScreen.type,
                supportedPaths = noActiveScreen.paths,
                contactEmail = configuration.support.email,
                localization = configuration.localization,
                onAction = onAction,
            )
        } ?: run {
            // Fallback with a restore button
            // NoSubscriptionsView(configuration = configuration)
        }
    }
}

private fun updateNavigationFromState(
    state: CustomerCenterState.Success,
    currentDestination: CustomerCenterDestination,
    onNavigationAction: (NavigationAction) -> Unit,
) {
    when {
        state.feedbackSurveyData != null -> {
            if (currentDestination !is CustomerCenterDestination.FeedbackSurvey) {
                onNavigationAction(
                    NavigationAction.NavigateTo(
                        CustomerCenterDestination.FeedbackSurvey(
                            data = state.feedbackSurveyData,
                            title = state.title ?: "",
                        ),
                    ),
                )
            }
        }

        state.promotionalOfferData != null -> {
            if (currentDestination !is CustomerCenterDestination.PromotionalOffer) {
                val promotionalOfferDestination = CustomerCenterDestination.PromotionalOffer(
                    data = state.promotionalOfferData,
                )

                // If coming from FeedbackSurvey, replace it to prevent going back to completed survey
                if (currentDestination is CustomerCenterDestination.FeedbackSurvey) {
                    onNavigationAction(NavigationAction.ReplaceCurrentDestination(promotionalOfferDestination))
                } else {
                    onNavigationAction(NavigationAction.NavigateTo(promotionalOfferDestination))
                }
            }
        }

        // Don't handle restorePurchasesState in navigation - it's now an overlay

        else -> {
            if (currentDestination !is CustomerCenterDestination.Main) {
                onNavigationAction(NavigationAction.NavigateToMain)
            }
        }
    }
}

@Composable
private fun getCustomerCenterViewModel(
    isDarkMode: Boolean,
    listener: CustomerCenterListener? = null,
    purchases: PurchasesType = PurchasesImpl(),
    viewModel: CustomerCenterViewModel = viewModel<CustomerCenterViewModelImpl>(
        factory = CustomerCenterViewModelFactory(
            purchases,
            MaterialTheme.colorScheme,
            isDarkMode = isDarkMode,
            listener = listener,
        ),
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
        CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE to CustomerCenterConfigData.Screen(
            type = CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE,
            title = "No subscriptions found",
            subtitle = "We can try checking your account for any previous purchases",
            paths = listOf(
                CustomerCenterConfigData.HelpPath(
                    id = "9q9719171o",
                    title = "Check for previous purchases",
                    type = CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE,
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
internal fun CustomerCenterNoActiveScreenPreview() {
    InternalCustomerCenter(
        state = CustomerCenterState.Success(
            customerCenterConfigData = previewConfigData,
            purchaseInformation = null,
            supportedPathsForManagementScreen = listOf(),
        ),
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        onAction = {},
    )
}

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
            supportedPathsForManagementScreen = previewConfigData.getManagementScreen()?.paths,
        ),
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        onAction = {},
    )
}
