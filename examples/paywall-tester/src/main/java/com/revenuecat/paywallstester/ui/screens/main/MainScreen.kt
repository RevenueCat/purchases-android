package com.revenuecat.paywallstester.ui.screens.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.revenuecat.paywallstester.ui.screens.main.appinfo.AppInfoScreen
import com.revenuecat.paywallstester.ui.screens.main.offerings.OfferingsScreen
import com.revenuecat.paywallstester.ui.screens.main.paywalls.PaywallsScreen
import com.revenuecat.purchases.Offering

@Composable
fun MainScreen(
    navigateToPaywallScreen: (Offering?) -> Unit,
    navigateToPaywallFooterScreen: (Offering?) -> Unit,
    navigateToPaywallCondensedFooterScreen: (Offering?) -> Unit,
    navigateToPaywallByPlacementScreen: (String) -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    Scaffold(
        bottomBar = { BottomBarNavigation(navController) },
    ) { paddingValues ->
        MainNavHost(
            navController = navController,
            navigateToPaywallScreen = navigateToPaywallScreen,
            navigateToPaywallFooterScreen = navigateToPaywallFooterScreen,
            navigateToPaywallCondensedFooterScreen = navigateToPaywallCondensedFooterScreen,
            navigateToPaywallByPlacementScreen = navigateToPaywallByPlacementScreen,
            contentPadding = paddingValues,
        )
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
    )
}

private val bottomNavigationItems = listOf(
    Tab.AppInfo,
    Tab.Paywalls,
    Tab.Offerings,
)

@Suppress("LongParameterList")
@Composable
private fun MainNavHost(
    navController: NavHostController,
    navigateToPaywallScreen: (Offering?) -> Unit,
    navigateToPaywallFooterScreen: (Offering?) -> Unit,
    navigateToPaywallCondensedFooterScreen: (Offering?) -> Unit,
    navigateToPaywallByPlacementScreen: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    NavHost(
        navController,
        startDestination = Tab.Paywalls.route,
    ) {
        composable(Tab.AppInfo.route) {
            AppInfoScreen(
                modifier = Modifier
                    .padding(contentPadding)
                    .consumeWindowInsets(contentPadding),
            )
        }
        composable(Tab.Paywalls.route) {
            PaywallsScreen(
                modifier = Modifier
                    .padding(contentPadding)
                    .consumeWindowInsets(contentPadding),
            )
        }
        composable(Tab.Offerings.route) {
            OfferingsScreen(
                tappedOnOffering = { offering -> navigateToPaywallScreen(offering) },
                tappedOnOfferingFooter = { offering -> navigateToPaywallFooterScreen(offering) },
                tappedOnOfferingCondensedFooter = { offering -> navigateToPaywallCondensedFooterScreen(offering) },
                tappedOnOfferingByPlacement = { placementId -> navigateToPaywallByPlacementScreen(placementId) },
                modifier = Modifier
                    .padding(contentPadding)
                    .consumeWindowInsets(contentPadding),
            )
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
