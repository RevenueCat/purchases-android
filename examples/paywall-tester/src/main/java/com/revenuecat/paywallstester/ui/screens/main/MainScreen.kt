package com.revenuecat.paywallstester.ui.screens.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.revenuecat.paywallstester.ui.screens.appinfo.AppInfoScreen
import com.revenuecat.paywallstester.ui.screens.offerings.OfferingsScreen
import com.revenuecat.paywallstester.ui.screens.paywalls.PaywallsScreen

@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController(),
) {
    Scaffold(
        topBar = {
            val currentRoute = currentRoute(navController)
            TopAppBar(
                title = {
                    Text(
                        text = currentRoute,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Black,
                    )
                },
                backgroundColor = Color.Transparent,
                elevation = 0.dp,
            )
        },
        bottomBar = { BottomBarNavigation(navController) },
    ) {
        MainNavHost(navController, Modifier.padding(it))
    }
}

@Preview
@Composable
fun MainScreenPreview() {
    MainScreen()
}

private val bottomNavigationItems = listOf(
    Tab.AppInfo,
    Tab.Paywalls,
    Tab.Offerings,
)

@Composable
private fun MainNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController,
        startDestination = Tab.AppInfo.route,
        modifier = modifier,
    ) {
        composable(Tab.AppInfo.route) {
            AppInfoScreen()
        }
        composable(Tab.Paywalls.route) {
            PaywallsScreen()
        }
        composable(Tab.Offerings.route) {
            OfferingsScreen()
        }
    }
}

@Composable
private fun BottomBarNavigation(
    navController: NavHostController,
) {
    BottomNavigation(
        backgroundColor = Color.White,
        contentColor = Color.Black,
    ) {
        val currentRoute = currentRoute(navController)
        bottomNavigationItems.forEach { screen ->
            BottomNavigationItem(
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
