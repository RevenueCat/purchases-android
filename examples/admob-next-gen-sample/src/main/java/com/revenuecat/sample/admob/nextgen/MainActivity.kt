package com.revenuecat.sample.admob.nextgen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.revenuecat.sample.admob.nextgen.ui.AdMobNextGenSample

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var screen by remember { mutableStateOf<SampleScreen?>(null) }
            BackHandler(enabled = screen != null) { screen = null }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AdMobNextGenSample(
                        activity = this,
                        application = application as MainApplication,
                        selectedScreen = screen,
                        onSelectScreen = { screen = it },
                        onBack = { screen = null },
                    )
                }
            }
        }
    }
}

enum class SampleScreen(val title: String, val description: String) {
    BANNER("Banner", "Direct loading, preloading, registration, refresh callbacks"),
    INTERSTITIAL("Interstitial", "Suspending load, preload buffer, show-time placement"),
    APP_OPEN("App open", "Direct and preloaded full-screen app-open ads"),
    REWARDED("Rewarded", "Standard rewards and RevenueCat reward verification"),
    REWARDED_INTERSTITIAL("Rewarded interstitial", "Rewarded interstitial with verification"),
    NATIVE("Native", "Single, batch Flow, and preloaded native ads"),
    DIAGNOSTICS("Diagnostics", "Intentional direct and preload failures"),
}
