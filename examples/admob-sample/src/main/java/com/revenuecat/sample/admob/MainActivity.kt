package com.revenuecat.sample.admob

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.revenuecat.sample.admob.ui.HomeScreen

/**
 * Main activity for the AdMob Integration Sample app.
 *
 * This activity sets up Jetpack Compose and displays the HomeScreen
 * with all ad format examples. Each composable calls `Purchases.sharedInstance.adTracker` for load-and-track
 * to load and show ads with RevenueCat event tracking.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    HomeScreen(activity = this)
                }
            }
        }
    }
}
