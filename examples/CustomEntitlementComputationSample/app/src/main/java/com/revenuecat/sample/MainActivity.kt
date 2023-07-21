package com.revenuecat.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.revenuecat.sample.ui.theme.CustomEntitlementComputationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CustomEntitlementComputationTheme {
                CustomEntitlementComputationApp()
            }
        }
    }
}
