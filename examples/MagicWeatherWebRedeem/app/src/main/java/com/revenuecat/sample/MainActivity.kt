package com.revenuecat.sample

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.interfaces.RedeemWebResultListener
import com.revenuecat.sample.ui.theme.MagicWeatherComposeTheme
import java.util.logging.Logger

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
class MainActivity : ComponentActivity() {


    internal var rcDeepLink: Purchases.DeepLink? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MagicWeatherComposeTheme {
                WeatherApp(Screen.NotLoggedIn.route)
            }
        }
        this.handleDeepLink(intent);
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        this.handleDeepLink(intent)
    }

    fun handleDeepLink(intent: Intent?) {
        rcDeepLink = Purchases.parseIntent(intent)
        val deepLink = this.rcDeepLink ?: return

        if (deepLink is Purchases.DeepLink.WebRedemptionLink) {
            Purchases.sharedInstance.redeemWebPurchase(deepLink) { result ->
                when (result) {
                    is RedeemWebResultListener.Result.Success -> {
                        setContent {
                            MagicWeatherComposeTheme {
                                WeatherApp(Screen.Main.route)
                            }
                        }
                    }
                    is RedeemWebResultListener.Result.Error -> {
                        setContent {
                            MagicWeatherComposeTheme {
                                WeatherApp(Screen.Main.route)
                            }
                        }
                    }
                }
            }
        }
    }
}
