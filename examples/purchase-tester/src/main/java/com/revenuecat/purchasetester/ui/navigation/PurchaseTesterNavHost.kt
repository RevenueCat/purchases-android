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
import com.revenuecat.purchasetester.NavigationDestinations
import com.revenuecat.purchasetester.ui.screens.configure.ConfigureScreen
import com.revenuecat.purchasetester.ui.screens.login.LoginScreen
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
                    // Navigate to MainActivity with Logs destination (Fragment-based)
                    val intent = Intent(context, MainActivity::class.java).apply {
                        putExtra(NavigationDestinations.EXTRA_DESTINATION, NavigationDestinations.LOGS)
                    }
                    context.startActivity(intent)
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
                    // Navigate to MainActivity with Logs destination (Fragment-based)
                    val intent = Intent(context, MainActivity::class.java).apply {
                        putExtra(NavigationDestinations.EXTRA_DESTINATION, NavigationDestinations.LOGS)
                    }
                    context.startActivity(intent)
                },
                onNavigateToProxy = {
                    showProxySheet = true
                }
            )
        }

        // These routes are placeholders for future Compose screens
        // Currently handled by Fragment navigation through MainActivity
        composable(PurchaseTesterScreen.Logs.route) {
            // TODO: Replace with LogsScreen when migrated
        }

        composable(PurchaseTesterScreen.Proxy.route) {
            // TODO: Replace with ProxySettingsSheet when migrated
        }

        composable(PurchaseTesterScreen.Overview.route) {
            // TODO: Replace with OverviewScreen when migrated
        }
    }

    if (showProxySheet) {
        ProxySettingsSheet(onDismiss = { showProxySheet = false })
    }
}
