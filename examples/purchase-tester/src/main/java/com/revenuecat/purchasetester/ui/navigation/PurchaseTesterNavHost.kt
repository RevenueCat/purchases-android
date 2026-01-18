package com.revenuecat.purchasetester.ui.navigation

import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.revenuecat.purchasetester.MainActivity
import com.revenuecat.purchasetester.ui.screens.configure.ConfigureScreen
import com.revenuecat.purchasetester.ui.screens.login.LoginScreen
import com.revenuecat.purchasetester.ui.screens.logs.LogsScreen
import com.revenuecat.purchasetester.ui.screens.proxysettings.ProxySettingsSheet

@Composable
fun PurchaseTesterApp(
    navController: NavHostController = rememberNavController(),
    startDestination: String = PurchaseTesterScreen.Configure.route,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var showProxySheet by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(PurchaseTesterScreen.Configure.route) {
            ConfigureScreen(
                onNavigateToLogin = {
                    navController.navigate(PurchaseTesterScreen.Login.route) {
                        popUpTo(PurchaseTesterScreen.Configure.route) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToLogs = {
                    navController.navigate(PurchaseTesterScreen.Logs.route)
                },
                onNavigateToProxy = {
                    showProxySheet = true
                }
            )
        }

        composable(PurchaseTesterScreen.Login.route) {
            LoginScreen(
                onNavigateToOverview = {
                    // Navigate to MainActivity (Fragment-based Overview)
                    // Finish ConfigureActivity so back button doesn't return to it
                    val intent = Intent(context, MainActivity::class.java)
                    context.startActivity(intent)
                    activity?.finish()
                },
                onNavigateToConfigure = {
                    navController.navigate(PurchaseTesterScreen.Configure.route) {
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToLogs = {
                    navController.navigate(PurchaseTesterScreen.Logs.route)
                },
                onNavigateToProxy = {
                    showProxySheet = true
                }
            )
        }

        composable(PurchaseTesterScreen.Logs.route) {
            LogsScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(PurchaseTesterScreen.Overview.route) {
            // TODO: Replace with OverviewScreen when migrated
        }
    }

    if (showProxySheet) {
        ProxySettingsSheet(onDismiss = { showProxySheet = false })
    }
}
