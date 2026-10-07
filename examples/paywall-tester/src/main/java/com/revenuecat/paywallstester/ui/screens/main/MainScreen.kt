package com.revenuecat.paywallstester.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.revenuecat.paywallstester.ui.screens.checkpoints.AppErrorDialog
import com.revenuecat.paywallstester.ui.screens.checkpoints.AppPaywall
import com.revenuecat.paywallstester.ui.screens.checkpoints.CheckpointsScreen
import com.revenuecat.paywallstester.ui.screens.checkpoints.CheckpointsViewModel
import com.revenuecat.paywallstester.ui.screens.checkpoints.CheckpointsViewModelImpl
import com.revenuecat.paywallstester.ui.screens.main.appinfo.AppInfoScreen
import com.revenuecat.paywallstester.ui.screens.main.locale.LocaleScreen
import com.revenuecat.paywallstester.ui.screens.main.offerings.OfferingsScreen
import com.revenuecat.paywallstester.ui.screens.main.paywalls.PaywallsScreen
import com.revenuecat.purchases.Offering

@SuppressWarnings("LongParameterList", "ViewModelForwarding")
@Composable
fun MainScreen(
    navigateToPaywallScreen: (Offering?) -> Unit,
    navigateToPaywallFooterScreen: (Offering?) -> Unit,
    navigateToPaywallCondensedFooterScreen: (Offering?) -> Unit,
    navigateToPaywallByPlacementScreen: (String) -> Unit,
    navigateToCustomerCenterScreen: () -> Unit,
    navController: NavHostController = rememberNavController(),
    checkpointsViewModel: CheckpointsViewModel = viewModel<CheckpointsViewModelImpl>(
        factory = CheckpointsViewModelImpl.Factory,
    ),
) {
    val paywallRequest by checkpointsViewModel.paywallRequest.collectAsStateWithLifecycle()
    val errorRequest by checkpointsViewModel.errorRequest.collectAsStateWithLifecycle()

    Box {
        Scaffold(
            bottomBar = { BottomBarNavigation(navController) },
        ) { paddingValues ->
            MainNavHost(
                navController = navController,
                navigateToPaywallScreen = navigateToPaywallScreen,
                navigateToPaywallFooterScreen = navigateToPaywallFooterScreen,
                navigateToPaywallCondensedFooterScreen = navigateToPaywallCondensedFooterScreen,
                navigateToPaywallByPlacementScreen = navigateToPaywallByPlacementScreen,
                navigateToCustomerCenterScreen = navigateToCustomerCenterScreen,
                checkpointsViewModel = checkpointsViewModel,
                modifier = Modifier.padding(paddingValues)
                    .consumeWindowInsets(paddingValues),
            )
        }
        paywallRequest?.let { AppPaywall(request = it) }
        errorRequest?.let { AppErrorDialog(request = it) }
    }
}

@Preview
@Composable
fun MainScreenPreview() {
    MainScreen(
        navigateToPaywallScreen = {},
        navigateToPaywallFooterScreen = {},
        navigateToPaywallCondensedFooterScreen = {},
        navigateToPaywallByPlacementScreen = {},
        navigateToCustomerCenterScreen = {},
    )
}

private val bottomNavigationItems = listOf(
    Tab.AppInfo,
    Tab.Paywalls,
    Tab.Offerings,
    Tab.Checkpoints,
    Tab.Locale,
)

@Suppress("LongParameterList", "ViewModelForwarding")
@Composable
private fun MainNavHost(
    navController: NavHostController,
    navigateToPaywallScreen: (Offering?) -> Unit,
    navigateToPaywallFooterScreen: (Offering?) -> Unit,
    navigateToPaywallCondensedFooterScreen: (Offering?) -> Unit,
    navigateToPaywallByPlacementScreen: (String) -> Unit,
    navigateToCustomerCenterScreen: () -> Unit,
    checkpointsViewModel: CheckpointsViewModel,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController,
        startDestination = Tab.Paywalls.route,
        modifier = modifier,
    ) {
        composable(Tab.AppInfo.route) {
            AppInfoScreen(
                tappedOnCustomerCenter = navigateToCustomerCenterScreen,
            )
        }
        composable(Tab.Paywalls.route) {
            PaywallsScreen()
        }
        composable(Tab.Offerings.route) {
            OfferingsScreen(
                tappedOnOffering = { offering -> navigateToPaywallScreen(offering) },
                tappedOnOfferingFooter = { offering -> navigateToPaywallFooterScreen(offering) },
                tappedOnOfferingCondensedFooter = { offering -> navigateToPaywallCondensedFooterScreen(offering) },
                tappedOnOfferingByPlacement = { placementId -> navigateToPaywallByPlacementScreen(placementId) },
            )
        }
        composable(Tab.Checkpoints.route) {
            CheckpointsScreen(viewModel = checkpointsViewModel)
        }
        composable(Tab.Locale.route) {
            LocaleScreen()
        }
    }
}

@Composable
private fun BottomBarNavigation(
    navController: NavHostController,
) {
    NavigationBar {
        val currentRoute = currentRoute(navController)
        bottomNavigationItems.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painterResource(id = screen.iconResourceId),
                        contentDescription = null,
                    )
                },
                label = {
                    Text(
                        screen.title,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        softWrap = false,
                    )
                },
                selected = currentRoute == screen.route,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route)
                    }
                },
            )
        }
    }
}

@Composable
fun currentRoute(navController: NavHostController): String {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route ?: ""
}
