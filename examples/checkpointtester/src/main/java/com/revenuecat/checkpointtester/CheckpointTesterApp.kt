package com.revenuecat.checkpointtester

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.revenuecat.checkpointtester.checkpoints.DummyOfferingPaywallPresenter
import com.revenuecat.checkpointtester.ui.Screen
import com.revenuecat.checkpointtester.ui.dialogs.DummyOfferingPaywallDialog
import com.revenuecat.checkpointtester.ui.dialogs.SetAttributeDialog
import com.revenuecat.checkpointtester.ui.screens.custom.CustomCheckpointScreen
import com.revenuecat.checkpointtester.ui.screens.gate.EntitlementGateScreen
import com.revenuecat.checkpointtester.ui.screens.hardpaywall.HardPaywallScreen
import com.revenuecat.checkpointtester.ui.screens.log.ListenerLogScreen
import com.revenuecat.checkpointtester.ui.screens.onboarding.OnboardingScreen
import com.revenuecat.checkpointtester.ui.screens.softpaywall.SoftPaywallScreen
import com.revenuecat.checkpointtester.ui.screens.usecases.UseCasesScreen

private val TABS: List<Pair<Screen, ImageVector>> = listOf(
    Screen.UseCases to Icons.Filled.PlayArrow,
    Screen.ListenerLog to Icons.AutoMirrored.Filled.List,
)

private val ALL_SCREENS: List<Screen> = listOf(
    Screen.UseCases,
    Screen.ListenerLog,
    Screen.HardPaywall,
    Screen.SoftPaywall,
    Screen.Onboarding,
    Screen.EntitlementGate,
    Screen.CustomCheckpoint,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckpointTesterApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = ALL_SCREENS.firstOrNull { it.route == backStackEntry?.destination?.route }
        ?: Screen.UseCases
    val isTab = TABS.any { (screen, _) -> screen == currentScreen }
    var showAttributeDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = currentScreen.title) },
                navigationIcon = {
                    if (!isTab) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showAttributeDialog = true }) {
                        Icon(Icons.Filled.Person, contentDescription = "Set subscriber attribute")
                    }
                },
            )
        },
        bottomBar = { if (isTab) BottomBar(navController, currentScreen) },
    ) { paddingValues ->
        val contentModifier = Modifier.padding(paddingValues)
        NavHost(navController = navController, startDestination = Screen.UseCases.route) {
            composable(Screen.UseCases.route) {
                UseCasesScreen(
                    onNavigate = { navController.navigate(it.route) },
                    modifier = contentModifier,
                )
            }
            composable(Screen.ListenerLog.route) {
                ListenerLogScreen(modifier = contentModifier)
            }
            composable(Screen.HardPaywall.route) {
                HardPaywallScreen(modifier = contentModifier)
            }
            composable(Screen.SoftPaywall.route) {
                SoftPaywallScreen(modifier = contentModifier)
            }
            composable(Screen.Onboarding.route) {
                OnboardingScreen(modifier = contentModifier)
            }
            composable(Screen.EntitlementGate.route) {
                EntitlementGateScreen(modifier = contentModifier)
            }
            composable(Screen.CustomCheckpoint.route) {
                CustomCheckpointScreen(modifier = contentModifier)
            }
        }
        if (showAttributeDialog) SetAttributeDialog(onDismiss = { showAttributeDialog = false })
        DummyOfferingPaywallHost()
    }
}

// Renders the dummy custom paywall whenever the app's CheckpointOfferingPresenter has an offering to present.
@Composable
private fun DummyOfferingPaywallHost() {
    val paywallRequest by DummyOfferingPaywallPresenter.request.collectAsState()
    paywallRequest?.let { request ->
        DummyOfferingPaywallDialog(request = request)
    }
}

@Composable
private fun BottomBar(navController: NavHostController, currentScreen: Screen) {
    NavigationBar {
        TABS.forEach { (screen, icon) ->
            NavigationBarItem(
                selected = currentScreen == screen,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(icon, contentDescription = screen.title) },
                label = { Text(text = screen.title) },
            )
        }
    }
}
