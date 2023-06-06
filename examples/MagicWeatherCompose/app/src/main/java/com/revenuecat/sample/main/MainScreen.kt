package com.revenuecat.sample.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
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
import com.revenuecat.sample.Screen
import com.revenuecat.sample.user.UserScreen
import com.revenuecat.sample.weather.WeatherScreen

@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController(),
    changeScreenCallback: (Screen) -> Unit,
) {
    Scaffold(
        bottomBar = { BottomBarNavigation(navController) },
    ) {
        MainNavHost(navController, changeScreenCallback, Modifier.padding(it))
    }
}

@Preview
@Composable
fun MainScreenPreview() {
    MainScreen {}
}

private val bottomNavigationItems = listOf(
    BottomNavigationScreens.Weather,
    BottomNavigationScreens.User,
)

@Composable
private fun MainNavHost(
    navController: NavHostController,
    changeScreenCallback: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController,
        startDestination = BottomNavigationScreens.Weather.title,
        modifier = modifier,
    ) {
        composable(BottomNavigationScreens.Weather.title) {
            WeatherScreen(navigateToPaywallCallback = { changeScreenCallback(Screen.Paywall) })
        }
        composable(BottomNavigationScreens.User.title) {
            UserScreen()
        }
    }
}

@Composable
private fun BottomBarNavigation(
    navController: NavHostController,
) {
    BottomNavigation {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        bottomNavigationItems.forEach { screen ->
            BottomNavigationItem(
                icon = {
                    Icon(painterResource(id = screen.iconResourceId), contentDescription = null)
                },
                label = { Text(screen.title) },
                selected = currentRoute == screen.title,
                onClick = {
                    if (currentRoute != screen.title) {
                        navController.navigate(screen.title)
                    }
                },
            )
        }
    }
}
