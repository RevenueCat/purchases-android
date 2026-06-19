package com.revenuecat.paywallstester

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.revenuecat.paywallstester.ui.screens.AppScreen
import com.revenuecat.paywallstester.ui.screens.main.CustomerCenterScreen
import com.revenuecat.paywallstester.ui.screens.main.MainScreen
import com.revenuecat.paywallstester.ui.screens.paywall.PaywallScreen
import com.revenuecat.paywallstester.ui.screens.paywall.PaywallScreenViewModel
import com.revenuecat.paywallstester.ui.screens.paywallfooter.PaywallFooterScreen
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter

@Composable
fun PaywallTesterApp(
    navController: NavHostController = rememberNavController(),
) {
    AppNavHost(navController = navController)
}

@Suppress("LongMethod")
@Composable
private fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    var autoLoadingDone by rememberSaveable { mutableStateOf(false) }
    AutoOpenOffering(navController, onLoadingComplete = { autoLoadingDone = true })

    val currentEntry by navController.currentBackStackEntryAsState()
    val isOnMainScreen = currentEntry?.destination?.route == AppScreen.Main.route
    val showLoadingOverlay = Constants.AUTO_OPEN_OFFERING_ID.isNotEmpty() &&
        !autoLoadingDone &&
        isOnMainScreen

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController,
            startDestination = AppScreen.Main.route,
        ) {
            composable(AppScreen.Main.route) {
                MainScreen(
                    navigateToPaywallScreen = { offering ->
                        navController.navigate(AppScreen.Paywall.route.plus("/${offering?.identifier}"))
                    },
                    navigateToPaywallFooterScreen = { offering ->
                        navController.navigate(AppScreen.PaywallFooter.route.plus("/${offering?.identifier}"))
                    },
                    navigateToPaywallCondensedFooterScreen = { offering ->
                        navController.navigate(
                            AppScreen.PaywallFooter.route
                                .plus("/${offering?.identifier}")
                                .plus("?${PaywallScreenViewModel.FOOTER_CONDENSED_KEY}=true"),
                        )
                    },
                    navigateToPaywallByPlacementScreen = { placementId ->
                        navController.navigate(
                            AppScreen.PaywallByPlacement.route
                                .plus("/$placementId"),
                        )
                    },
                    navigateToCustomerCenterScreen = {
                        navController.navigate(AppScreen.CustomerCenter.route)
                    },
                )
            }
            composable(
                route = AppScreen.Paywall.route.plus("/{${PaywallScreenViewModel.OFFERING_ID_KEY}}"),
                arguments = listOf(navArgument(PaywallScreenViewModel.OFFERING_ID_KEY) { type = NavType.StringType }),
            ) {
                PaywallScreen(dismissRequest = navController::popBackStack)
            }
            composable(
                route = AppScreen.PaywallByPlacement.route.plus("/{${PaywallScreenViewModel.PLACEMENT_ID_KEY}}"),
                arguments = listOf(navArgument(PaywallScreenViewModel.PLACEMENT_ID_KEY) { type = NavType.StringType }),
            ) {
                PaywallScreen(dismissRequest = navController::popBackStack)
            }
            composable(
                route = AppScreen.PaywallFooter.route
                    .plus("/{${PaywallScreenViewModel.OFFERING_ID_KEY}}")
                    .plus(
                        "?${PaywallScreenViewModel.FOOTER_CONDENSED_KEY}" +
                            "={${PaywallScreenViewModel.FOOTER_CONDENSED_KEY}}",
                    ),
                arguments = listOf(
                    navArgument(PaywallScreenViewModel.OFFERING_ID_KEY) { type = NavType.StringType },
                    navArgument(PaywallScreenViewModel.FOOTER_CONDENSED_KEY) {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
            ) {
                PaywallFooterScreen(
                    dismissRequest = navController::popBackStack,
                )
            }
            composable(route = AppScreen.CustomerCenter.route) {
                CustomerCenterScreen(dismissRequest = navController::popBackStack)
            }
        }

        if (showLoadingOverlay) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading offering ${Constants.AUTO_OPEN_OFFERING_ID}…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

@Composable
private fun AutoOpenOffering(navController: NavHostController, onLoadingComplete: () -> Unit) {
    val currentOnLoadingComplete by rememberUpdatedState(onLoadingComplete)
    LaunchedEffect(Unit) {
        val offeringId = Constants.AUTO_OPEN_OFFERING_ID
        if (offeringId.isEmpty()) return@LaunchedEffect

        runCatching {
            Purchases.sharedInstance.awaitOfferings()
        }.onSuccess { offerings ->
            if (offerings.all.containsKey(offeringId)) {
                navController.navigate(AppScreen.Paywall.route.plus("/$offeringId"))
            } else {
                Log.w(TAG, "Configured auto-open offering '$offeringId' was not found.")
            }
        }.onFailure { error ->
            Log.w(TAG, "Could not auto-open offering '$offeringId'.", error)
        }
        currentOnLoadingComplete()
    }
}

private const val TAG = "PaywallTesterApp"
