package com.revenuecat.purchasetester.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.revenuecat.purchases.WebPurchaseRedemption
import com.revenuecat.purchasetester.ui.screens.ConfigureScreen
import com.revenuecat.purchasetester.ui.screens.LoginScreen
import com.revenuecat.purchasetester.ui.screens.LogsScreen
import com.revenuecat.purchasetester.ui.screens.OfferingScreen
import com.revenuecat.purchasetester.ui.screens.OverviewScreen
import com.revenuecat.purchasetester.ui.screens.ProxySettingsScreen

@Composable
fun PurchaseTesterApp(
    webPurchaseRedemption: WebPurchaseRedemption?,
    onWebPurchaseRedemptionConsume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        NavHost(
            navController = navController,
            startDestination = "configure",
        ) {
            composable(route = "configure") {
                ConfigureScreen(
                    onNavigateToLogin = { navController.navigate("login") },
                    onNavigateToLogs = { navController.navigate("logs") },
                    onNavigateToProxy = { navController.navigate("proxy") },
                )
            }

            composable(route = "login") {
                LoginScreen(
                    onNavigateToOverview = { navController.navigate("overview") },
                    onNavigateToConfigure = { navController.navigate("configure") },
                    onNavigateToLogs = { navController.navigate("logs") },
                    onNavigateToProxy = { navController.navigate("proxy") },
                )
            }

            composable(route = "overview") {
                OverviewScreen(
                    webPurchaseRedemption = webPurchaseRedemption,
                    onWebPurchaseRedemptionConsume = onWebPurchaseRedemptionConsume,
                    onNavigateToLogin = { navController.navigate("login") },
                    onNavigateToOffering = { offeringId ->
                        navController.navigate("offering/$offeringId")
                    },
                    onNavigateToLogs = { navController.navigate("logs") },
                    onNavigateToProxy = { navController.navigate("proxy") },
                )
            }

            composable(route = "offering/{offeringId}") { backStackEntry ->
                val offeringId = backStackEntry.arguments?.getString("offeringId") ?: ""
                OfferingScreen(
                    offeringId = offeringId,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(route = "logs") {
                LogsScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(route = "proxy") {
                ProxySettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}
