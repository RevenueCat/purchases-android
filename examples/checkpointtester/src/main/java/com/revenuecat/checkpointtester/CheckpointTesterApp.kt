package com.revenuecat.checkpointtester

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.revenuecat.checkpointtester.ui.Screen
import com.revenuecat.checkpointtester.ui.dialogs.SetAttributeDialog
import com.revenuecat.checkpointtester.ui.screens.custom.CustomCheckpointScreen
import com.revenuecat.checkpointtester.ui.screens.gate.EntitlementGateScreen
import com.revenuecat.checkpointtester.ui.screens.hardpaywall.HardPaywallScreen
import com.revenuecat.checkpointtester.ui.screens.onboarding.OnboardingScreen
import com.revenuecat.checkpointtester.ui.screens.softpaywall.SoftPaywallScreen
import com.revenuecat.checkpointtester.ui.screens.usecases.UseCasesScreen

private val ALL_SCREENS: List<Screen> = listOf(
    Screen.UseCases,
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
    var showAttributeDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = currentScreen.title) },
                navigationIcon = {
                    if (currentScreen != Screen.UseCases) {
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
    ) { paddingValues ->
        val contentModifier = Modifier.padding(paddingValues)
        NavHost(navController = navController, startDestination = Screen.UseCases.route) {
            composable(Screen.UseCases.route) {
                UseCasesScreen(
                    onNavigate = { navController.navigate(it.route) },
                    modifier = contentModifier,
                )
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
        if (showAttributeDialog) {
            SetAttributeDialog(onDismiss = { showAttributeDialog = false })
        }
    }
}
