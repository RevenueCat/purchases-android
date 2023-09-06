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
            MainScreen(navigateToPaywallScreen = { offering ->
                navController.navigate(AppScreen.Paywall.route.plus("/${offering?.identifier}"))
            })
        }
        composable(
            route = AppScreen.Paywall.route.plus("/{offering_id}"),
            arguments = listOf(navArgument("offering_id") { type = NavType.StringType }),
        ) {
            val offeringId = it.arguments?.getString("offering_id")
            PaywallScreen(offeringId)
        }
    }
}
