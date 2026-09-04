package com.revenuecat.checkpointssample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.revenuecat.checkpointssample.ui.theme.PurchasesandroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PurchasesandroidTheme {
                CheckpointsSampleApp()
            }
        }
    }
}
