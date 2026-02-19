@file:Suppress("TooManyFunctions")
@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterListener
import com.revenuecat.purchases.customercenter.events.PromoOfferRejectionSource
import com.revenuecat.purchases.ui.revenuecatui.composables.ErrorDialog
import com.revenuecat.purchases.ui.revenuecatui.customercenter.actions.CustomerCenterAction
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.getColorForTheme
import com.revenuecat.purchases.ui.revenuecatui.customercenter.dialogs.RestorePurchasesDialog
import com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation.CustomerCenterAnimations
import com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation.CustomerCenterDestination
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModel
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModelFactory
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.customercenter.views.CreateSupportTicketView
import com.revenuecat.purchases.ui.revenuecatui.customercenter.views.CustomerCenterErrorView
import com.revenuecat.purchases.ui.revenuecatui.customercenter.views.CustomerCenterLoadingView
import com.revenuecat.purchases.ui.revenuecatui.customercenter.views.FeedbackSurveyView
import com.revenuecat.purchases.ui.revenuecatui.customercenter.views.NoActiveUserManagementView
import com.revenuecat.purchases.ui.revenuecatui.customercenter.views.PromotionalOfferScreen
import com.revenuecat.purchases.ui.revenuecatui.customercenter.views.RelevantPurchasesListView
import com.revenuecat.purchases.ui.revenuecatui.customercenter.views.SelectedPurchaseDetailView
import com.revenuecat.purchases.ui.revenuecatui.customercenter.views.VirtualCurrencyBalancesScreen
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesImpl
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.getActivity
import com.revenuecat.purchases.ui.revenuecatui.icons.ArrowBack
import com.revenuecat.purchases.ui.revenuecatui.icons.Close
import kotlinx.coroutines.launch

@Suppress("LongMethod", "CyclomaticComplexMethod")
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
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(colorScheme, isDark) {
        viewModel.refreshColors(colorScheme, isDark)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(state !is CustomerCenterState.Success) {
        if (state is CustomerCenterState.NotLoaded) {
            viewModel.loadCustomerCenter()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.trackImpressionIfNeeded()
    }

    // Refresh Customer Center data when activity resumes after being backgrounded.
    // This matches iOS behavior where we refresh when the manage subscriptions sheet is dismissed.
    // When the user opens the manage subscriptions screen (Google Play Store), the activity stops.
    // When they return, the activity starts again, and we refresh to show updated subscription status.
    // Using ON_STOP/ON_START with isChangingConfigurations check to properly handle configuration changes
    // (e.g., rotation) without triggering false refreshes.
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context.getActivity()
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    viewModel.onActivityStopped(activity?.isChangingConfigurations == true)
                }
                Lifecycle.Event.ON_START -> {
                    viewModel.onActivityStarted()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
                    viewModel.dismissPromotionalOffer(
                        context,
                        action.originalPath,
                        source = PromoOfferRejectionSource.CANCEL,
                    )

                is CustomerCenterAction.PurchasePromotionalOffer -> {
                    val activity = context.getActivity()
                    coroutineScope.launch {
                        viewModel.onAcceptedPromotionalOffer(action.subscriptionOption, activity)
                    }
                }
                is CustomerCenterAction.CustomActionSelected -> {
                    viewModel.onCustomActionSelected(action.customActionData)
                }
                is CustomerCenterAction.SelectPurchase -> viewModel.selectPurchase(action.purchase)
                is CustomerCenterAction.ShowPaywall -> viewModel.showPaywall(context)
                is CustomerCenterAction.ShowVirtualCurrencyBalances -> viewModel.showVirtualCurrencyBalances()
                is CustomerCenterAction.ShowSupportTicketCreation -> viewModel.showCreateSupportTicket()
                is CustomerCenterAction.DismissSupportTicketSuccessSnackbar -> {
                    viewModel.dismissSupportTicketSuccessSnackbar()
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
    val colorScheme = createColorScheme(state)
    val (title, navigationButtonType, shouldUseLargeTopBar) = createScaffoldState(state)

    MaterialTheme(
        colorScheme = colorScheme,
    ) {
        CustomerCenterScaffold(
            modifier = modifier
                .background(MaterialTheme.colorScheme.background),
            scaffoldConfig = CustomerCenterScaffoldConfig(
                title = title,
                shouldUseLargeTopBar = shouldUseLargeTopBar,
                navigationButtonType = navigationButtonType,
            ),
            onAction = onAction,
        ) {
            when (state) {
                is CustomerCenterState.NotLoaded -> {
                }

                is CustomerCenterState.Loading -> {
                    CustomerCenterLoadingView()
                }

                is CustomerCenterState.Error -> {
                    CustomerCenterErrorView(state)
                }

                is CustomerCenterState.Success -> {
                    CustomerCenterLoaded(
                        state = state,
                        onAction = onAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun createColorScheme(state: CustomerCenterState): ColorScheme {
    val isDark = isSystemInDarkTheme()
    val baseColorScheme = MaterialTheme.colorScheme

    return remember(state, isDark, baseColorScheme) {
        if (state is CustomerCenterState.Success) {
            val appearance: CustomerCenterConfigData.Appearance = state.customerCenterConfigData.appearance
            val accentColor = appearance.getColorForTheme(isDark) { it.accentColor }

            // Only change background when presenting a promotional offer
            val backgroundColor = if (state.currentDestination is CustomerCenterDestination.PromotionalOffer) {
                appearance.getColorForTheme(isDark) { it.backgroundColor }
            } else {
                null
            }

            baseColorScheme.copy(
                primary = accentColor ?: baseColorScheme.primary,
                background = backgroundColor ?: baseColorScheme.background,
            )
        } else {
            baseColorScheme
        }
    }
}

private data class ScaffoldConfigData(
    val title: String?,
    val navigationButtonType: CustomerCenterState.NavigationButtonType,
    val shouldUseLargeTopBar: Boolean,
)

@Composable
private fun createScaffoldState(state: CustomerCenterState): ScaffoldConfigData {
    return remember(state) {
        if (state is CustomerCenterState.Success) {
            val title = state.navigationState.currentDestination.title
            val navigationButtonType = state.navigationButtonType
            val shouldUseLargeTopBar = state.currentDestination is CustomerCenterDestination.Main &&
                title != null
            ScaffoldConfigData(title, navigationButtonType, shouldUseLargeTopBar)
        } else {
            ScaffoldConfigData(
                title = null,
                navigationButtonType = CustomerCenterState.NavigationButtonType.CLOSE,
                shouldUseLargeTopBar = false,
            )
        }
    }
}

@Immutable
private data class CustomerCenterScaffoldConfig(
    val title: String?,
    val shouldUseLargeTopBar: Boolean,
    val navigationButtonType: CustomerCenterState.NavigationButtonType,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerCenterScaffold(
    onAction: (CustomerCenterAction) -> Unit,
    scaffoldConfig: CustomerCenterScaffoldConfig,
    modifier: Modifier = Modifier,
    mainContent: @Composable () -> Unit,
) {
    val scrollBehavior = if (scaffoldConfig.title != null && scaffoldConfig.shouldUseLargeTopBar) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    } else {
        null
    }

    Scaffold(
        modifier = modifier.applyIfNotNull(scrollBehavior) { nestedScroll(it.nestedScrollConnection) },
        topBar = {
            CustomerCenterTopBar(
                scaffoldConfig = scaffoldConfig,
                scrollBehavior = scrollBehavior,
                onAction = onAction,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            mainContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerCenterTopBar(
    scaffoldConfig: CustomerCenterScaffoldConfig,
    scrollBehavior: TopAppBarScrollBehavior?,
    onAction: (CustomerCenterAction) -> Unit,
) {
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.background,
        scrolledContainerColor = MaterialTheme.colorScheme.background,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
    )
    if (scaffoldConfig.shouldUseLargeTopBar) {
        LargeTopAppBar(
            title = {
                scaffoldConfig.title?.let { Text(text = it) }
            },
            navigationIcon = {
                CustomerCenterNavigationIcon(
                    navigationButtonType = scaffoldConfig.navigationButtonType,
                    onAction = onAction,
                )
            },
            colors = colors,
            scrollBehavior = scrollBehavior,
        )
    } else {
        TopAppBar(
            title = {
                scaffoldConfig.title?.let { Text(text = it) }
            },
            navigationIcon = {
                CustomerCenterNavigationIcon(
                    navigationButtonType = scaffoldConfig.navigationButtonType,
                    onAction = onAction,
                )
            },
            colors = colors,
        )
    }
}

@Composable
private fun CustomerCenterNavigationIcon(
    navigationButtonType: CustomerCenterState.NavigationButtonType,
    onAction: (CustomerCenterAction) -> Unit,
) {
    IconButton(onClick = {
        onAction(CustomerCenterAction.NavigationButtonPressed)
    }) {
        Icon(
            imageVector = when (navigationButtonType) {
                CustomerCenterState.NavigationButtonType.BACK -> ArrowBack
                CustomerCenterState.NavigationButtonType.CLOSE -> Close
            },
            contentDescription = null,
        )
    }
}

@Composable
private fun CustomerCenterLoaded(
    state: CustomerCenterState.Success,
    onAction: (CustomerCenterAction) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val latestOnAction by rememberUpdatedState(newValue = onAction)
    val latestMessage by rememberUpdatedState(
        newValue = state.customerCenterConfigData.localization.commonLocalizedString(
            CustomerCenterConfigData.Localization.CommonLocalizedString.SENT,
        ),
    )

    LaunchedEffect(state.showSupportTicketSuccessSnackbar) {
        if (state.showSupportTicketSuccessSnackbar) {
            snackbarHostState.showSnackbar(latestMessage)
            latestOnAction(CustomerCenterAction.DismissSupportTicketSuccessSnackbar)
        }
    }

    // Animate opacity when refreshing (similar to iOS)
    val contentAlpha by animateFloatAsState(
        targetValue = if (state.isRefreshing) 0.5f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "refreshAlpha",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = contentAlpha },
        ) {
            CustomerCenterNavHost(
                currentDestination = state.currentDestination,
                customerCenterState = state,
                onAction = onAction,
            )
        }

        // Show loading indicator when refreshing (similar to iOS)
        if (state.isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun CustomerCenterNavHost(
    currentDestination: CustomerCenterDestination,
    customerCenterState: CustomerCenterState.Success,
    onAction: (CustomerCenterAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = currentDestination,
        transitionSpec = {
            CustomerCenterAnimations.getTransitionForNavigation(
                from = initialState,
                to = targetState,
                navigationState = customerCenterState.navigationState,
            )
        },
        label = "CustomerCenterScreens",
        modifier = modifier,
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

            is CustomerCenterDestination.SelectedPurchaseDetail -> {
                SelectedPurchaseDetailView(
                    contactEmail = customerCenterState.customerCenterConfigData.support.email,
                    localization = customerCenterState.customerCenterConfigData.localization,
                    purchaseInformation = destination.purchaseInformation,
                    supportedPaths = customerCenterState.detailScreenPaths,
                    onAction = onAction,
                )
            }

            is CustomerCenterDestination.VirtualCurrencyBalances -> {
                VirtualCurrencyBalancesScreen(
                    appearance = customerCenterState.customerCenterConfigData.appearance,
                    localization = customerCenterState.customerCenterConfigData.localization,
                )
            }

            is CustomerCenterDestination.CreateSupportTicket -> {
                CreateSupportTicketView(
                    data = destination.data,
                    localization = customerCenterState.customerCenterConfigData.localization,
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

@Composable
private fun MainScreenContent(
    state: CustomerCenterState.Success,
    onAction: (CustomerCenterAction) -> Unit,
) {
    val configuration = state.customerCenterConfigData
    if (state.purchases.isNotEmpty()) {
        configuration.getManagementScreen()?.let { managementScreen ->
            RelevantPurchasesListView(
                supportedPaths = state.mainScreenPaths,
                contactEmail = configuration.support.email,
                virtualCurrencies = state.virtualCurrencies,
                appearance = configuration.appearance,
                localization = configuration.localization,
                supportTickets = configuration.support.supportTickets,
                onPurchaseSelect = { purchase ->
                    // Only allow selection if there are multiple purchases
                    if (state.purchases.size > 1) {
                        onAction(CustomerCenterAction.SelectPurchase(purchase))
                    }
                },
                onAction = onAction,
                purchases = state.purchases,
            )
        } ?: run {
            // Handle missing management screen
            // WrongPlatformView
        }
    } else {
        configuration.getNoActiveScreen()?.let { noActiveScreen ->
            NoActiveUserManagementView(
                screen = noActiveScreen,
                contactEmail = configuration.support.email,
                appearance = configuration.appearance,
                localization = configuration.localization,
                supportTickets = configuration.support.supportTickets,
                offering = state.noActiveScreenOffering,
                virtualCurrencies = state.virtualCurrencies,
                onAction = onAction,
            )
        } ?: run {
            // Fallback with a restore button
            // NoSubscriptionsView(configuration = configuration)
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
    support = CustomerCenterConfigData.Support(
        email = "test@revenuecat.com",
        supportTickets = CustomerCenterConfigData.Support.SupportTickets(),
    ),
)

@Preview
@Composable
internal fun CustomerCenterNoActiveScreenPreview() {
    InternalCustomerCenter(
        state = CustomerCenterState.Success(
            customerCenterConfigData = previewConfigData,
            purchases = emptyList(),
            mainScreenPaths = listOf(),
            detailScreenPaths = listOf(),
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
            purchases = listOf(CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing),
            mainScreenPaths = previewConfigData.getManagementScreen()?.paths ?: emptyList(),
            detailScreenPaths = previewConfigData.getManagementScreen()?.paths?.filter {
                it.type == CustomerCenterConfigData.HelpPath.PathType.CANCEL
            } ?: emptyList(),
        ),
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        onAction = {},
    )
}

@Preview
@Composable
internal fun CustomerCenterMultiplePurchasesPreview() {
    InternalCustomerCenter(
        state = CustomerCenterState.Success(
            customerCenterConfigData = previewConfigData,
            purchases = listOf(
                CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing,
                CustomerCenterConfigTestData.purchaseInformationYearlyExpiring,
            ),
            mainScreenPaths = previewConfigData.getManagementScreen()?.paths?.filter {
                it.type == CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE ||
                    it.type == CustomerCenterConfigData.HelpPath.PathType.CUSTOM_URL
            } ?: emptyList(),
            detailScreenPaths = previewConfigData.getManagementScreen()?.paths?.filter {
                it.type == CustomerCenterConfigData.HelpPath.PathType.CANCEL
            } ?: emptyList(),
        ),
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        onAction = {},
    )
}
