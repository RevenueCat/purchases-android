package com.revenuecat.checkpointtester

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.revenuecat.checkpointtester.ui.theme.CheckpointTesterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CheckpointTesterTheme {
                CheckpointTesterApp()
            }
        }
    }
}
