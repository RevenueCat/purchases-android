package com.revenuecat.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.amazon.AmazonConfiguration
import com.revenuecat.sample.data.Constants
import com.revenuecat.sample.ui.theme.MagicWeatherComposeTheme
import java.lang.IllegalArgumentException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val builder = when (BuildConfig.STORE) {
            "amazon" -> AmazonConfiguration.Builder(this, Constants.AMAZON_API_KEY)
            "google" -> PurchasesConfiguration.Builder(this, Constants.GOOGLE_API_KEY)
            else -> throw IllegalArgumentException("Invalid store.")
        }
        Purchases.configure(
            builder
                .diagnosticsEnabled(true)
                .build(),
        )
        Purchases.logLevel = LogLevel.VERBOSE
        setContent {
            MagicWeatherComposeTheme {
                WeatherApp()
            }
        }
    }
}
