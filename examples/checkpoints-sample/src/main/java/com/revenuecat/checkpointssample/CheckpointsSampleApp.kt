package com.revenuecat.checkpointssample

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.revenuecat.checkpointssample.ui.Screen
import com.revenuecat.checkpointssample.ui.screens.game.GameScreen
import com.revenuecat.checkpointssample.ui.screens.home.HomeScreen
import com.revenuecat.checkpointssample.ui.screens.onboarding.OnboardingScreen

@Composable
fun CheckpointsSampleApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
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
}
