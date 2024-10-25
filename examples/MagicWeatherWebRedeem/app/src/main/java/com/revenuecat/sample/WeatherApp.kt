package com.revenuecat.sample

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.revenuecat.sample.main.MainScreen
import com.revenuecat.sample.ui.theme.MagicWeatherComposeTheme
import com.revenuecat.sample.weather.NotLoggedInScreen

@Composable
fun WeatherApp(
    initialRoute: String = Screen.NotLoggedIn.route,
    navController: NavHostController = rememberNavController(),
) {
    NavGraph(navController, initialRoute)
}

@Preview
@Composable
fun WeatherAppPreview() {
    MagicWeatherComposeTheme {
        WeatherApp()
    }
}

@Composable
private fun NavGraph(
    navController: NavHostController,
    initialRoute: String = Screen.NotLoggedIn.route
) {
    NavHost(
        navController = navController,
        startDestination = initialRoute,
    ) {
        composable(route = Screen.Main.route) {
            MainScreen(
                changeScreenCallback = { screen ->
                    navController.navigate(screen.route)
                },
            )
        }
        composable(route = Screen.NotLoggedIn.route) {
            NotLoggedInScreen { }
        }
    }
}
