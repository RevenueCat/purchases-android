@file:OptIn(com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.sample.admob

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.sample.admob.manager.AdMobManager
import com.revenuecat.sample.admob.ui.HomeScreen

/**
 * Main activity for the AdMob Integration Sample app.
 *
 * This activity:
 * 1. Sets up Jetpack Compose
 * 2. Creates the AdMobManager instance
 * 3. Displays the HomeScreen with all ad examples
 * 4. Cleans up resources on destroy
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
class MainActivity : ComponentActivity() {

    private lateinit var adMobManager: AdMobManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the AdMobManager
        adMobManager = AdMobManager(this)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(
                        activity = this,
                        adMobManager = adMobManager
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up ad resources
        adMobManager.cleanup()
    }
}
