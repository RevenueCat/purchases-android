package com.revenuecat.purchasetester

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.revenuecat.purchasetester.ui.screens.configure.ConfigureScreen
import com.revenuecat.purchasetester.ui.theme.PurchaseTesterTheme

class ConfigureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PurchaseTesterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ConfigureScreen(
                        onNavigateToLogin = {
                            navigateToMainActivity(NavigationDestinations.LOGIN)
                        },
                        onNavigateToLogs = {
                            navigateToMainActivity(NavigationDestinations.LOGS)
                        },
                        onNavigateToProxy = {
                            navigateToMainActivity(NavigationDestinations.PROXY)
                        }
                    )
                }
            }
        }
    }

    private fun navigateToMainActivity(destination: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(NavigationDestinations.EXTRA_DESTINATION, destination)
        }
        startActivity(intent)
    }
}
