package com.revenuecat.purchasetester

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.revenuecat.purchasetester.ui.navigation.PurchaseTesterApp
import com.revenuecat.purchasetester.ui.navigation.PurchaseTesterScreen
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
                    PurchaseTesterApp(
                        startDestination = PurchaseTesterScreen.Configure.route
                    )
                }
            }
        }
    }
}
