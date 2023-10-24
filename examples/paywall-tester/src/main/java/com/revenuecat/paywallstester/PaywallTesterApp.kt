package com.revenuecat.paywallstester

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.revenuecat.paywallstester.ui.screens.AppScreen
import com.revenuecat.paywallstester.ui.screens.main.MainScreen
import com.revenuecat.paywallstester.ui.screens.paywall.PaywallScreen
import com.revenuecat.paywallstester.ui.screens.paywall.PaywallScreenViewModel
import com.revenuecat.paywallstester.ui.screens.paywallfooter.PaywallFooterScreen

@Composable
fun PaywallTesterApp(
    navController: NavHostController = rememberNavController(),
) {
    AppNavHost(navController = navController)
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController,
        startDestination = AppScreen.Main.route,
        modifier = modifier,
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
            )
        }
        composable(
            route = AppScreen.Paywall.route.plus("/{${PaywallScreenViewModel.OFFERING_ID_KEY}}"),
            arguments = listOf(navArgument(PaywallScreenViewModel.OFFERING_ID_KEY) { type = NavType.StringType }),
        ) {
            PaywallScreen(dismissRequest = navController::popBackStack)
        }
        composable(
            route = AppScreen.PaywallFooter.route
                .plus("/{${PaywallScreenViewModel.OFFERING_ID_KEY}}")
                .plus(
                    "?${PaywallScreenViewModel.FOOTER_CONDENSED_KEY}={${PaywallScreenViewModel.FOOTER_CONDENSED_KEY}}",
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
    }
}
