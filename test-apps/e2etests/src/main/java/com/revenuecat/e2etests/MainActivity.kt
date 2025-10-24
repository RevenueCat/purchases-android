package com.revenuecat.e2etests

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.revenuecat.e2etests.main.MainPage
import com.revenuecat.e2etests.ui.theme.PurchasesandroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PurchasesandroidTheme {
                MainPage()
            }
        }
    }
}
