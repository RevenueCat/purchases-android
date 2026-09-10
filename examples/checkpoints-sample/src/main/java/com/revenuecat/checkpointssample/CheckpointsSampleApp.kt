package com.revenuecat.checkpointssample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.revenuecat.checkpointssample.paywall.PlayGamePaywall
import com.revenuecat.checkpointssample.paywall.SamplePaywall
import com.revenuecat.checkpointssample.paywall.SamplePaywallPresenters
import com.revenuecat.checkpointssample.ui.Screen
import com.revenuecat.checkpointssample.ui.dialogs.SetAttributeDialog
import com.revenuecat.checkpointssample.ui.screens.game.GameScreen
import com.revenuecat.checkpointssample.ui.screens.home.HomeScreen
import com.revenuecat.checkpointssample.ui.screens.onboarding.OnboardingScreen

@Composable
fun CheckpointsSampleApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    var showAttributeDialog by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            floatingActionButton = {
                SmallFloatingActionButton(onClick = { showAttributeDialog = true }) {
                    Icon(Icons.Filled.Person, contentDescription = "Set subscriber attribute")
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Onboarding.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        onFinish = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Screen.Home.route) {
                    HomeScreen(onPlay = { navController.navigate(Screen.Game.route) })
                }
                composable(Screen.Game.route) {
                    GameScreen(onExit = { navController.popBackStack() })
                }
            }
        }
        if (showAttributeDialog) SetAttributeDialog(onDismiss = { showAttributeDialog = false })
        SamplePaywallHost()
    }
}

// Renders the sample's paywalls over the whole app whenever one of its PaywallPresenters has an offering to present.
@Composable
private fun SamplePaywallHost() {
    val globalRequest by SamplePaywallPresenters.global.request.collectAsState()
    val playGameRequest by SamplePaywallPresenters.playGame.request.collectAsState()
    globalRequest?.let { SamplePaywall(request = it) }
    playGameRequest?.let { PlayGamePaywall(request = it) }
}
