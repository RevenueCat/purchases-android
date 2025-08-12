package com.revenuecat.purchasetester.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.revenuecat.purchasetester.ui.screens.configure.PurchaseConfigureActions
import com.revenuecat.purchasetester.ui.screens.configure.PurchaseConfigureScreen
import com.revenuecat.purchasetester.ui.screens.configure.PurchaseConfigureViewModel
import com.revenuecat.purchasetester.ui.screens.configure.PurchaseConfigureViewModelImpl
import com.revenuecat.purchasetester.ui.utils.AppScreen

@Composable
fun PurchaseTesterApp(
    navController: NavHostController = rememberNavController(),
    viewModel: PurchaseConfigureViewModel = viewModel<PurchaseConfigureViewModelImpl>(
        factory = PurchaseConfigureViewModelImpl.Factory,
    ),
    onNavigateToLogin: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {},
    onNavigateToProxy: () -> Unit = {},
) {
    AppNavHost(
        navController = navController,
        actions = viewModel::onAction,
        onNavigateToLogin = onNavigateToLogin,
        onNavigateToLogs = onNavigateToLogs,
        onNavigateToProxy = onNavigateToProxy
    )
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    actions: (PurchaseConfigureActions) -> Unit,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {},
    onNavigateToProxy: () -> Unit = {},
) {
    NavHost(
        navController,
        startDestination = AppScreen.PurchaseConfigure.route,
        modifier = modifier,
    ) {
        composable(AppScreen.PurchaseConfigure.route) {
            PurchaseConfigureScreen(
                onNavigateToLogin = onNavigateToLogin,
                onNavigateToLogs = onNavigateToLogs,
                onNavigateToProxy = onNavigateToProxy
            )
        }

    }
}
