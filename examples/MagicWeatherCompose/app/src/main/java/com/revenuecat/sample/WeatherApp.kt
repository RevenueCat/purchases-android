package com.revenuecat.sample

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.revenuecat.sample.main.MainScreen
import com.revenuecat.sample.paywall.PaywallScreen
import com.revenuecat.sample.ui.theme.MagicWeatherComposeTheme

@Composable
fun WeatherApp(
    navController: NavHostController = rememberNavController(),
) {
    NavGraph(navController)
}

@Preview
@Composable
fun WeatherAppPreview() {
    MagicWeatherComposeTheme {
        WeatherApp()
    }
}

@Composable
private fun NavGraph(navController: NavHostController) {
    navController.navigatorProvider.navigators
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route,
    ) {
        composable(route = Screen.Main.route) {
            MainScreen(
                changeScreenCallback = { screen ->
                    navController.navigate(screen.route)
                },
            )
        }

        composable(route = Screen.Paywall.route) {
            PaywallScreen()
        }
    }
}
