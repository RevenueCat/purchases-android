package com.revenuecat.sample.vanilla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.revenuecat.sample.vanilla.ui.HomeScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MainApplication

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    when (val state = app.levelPlayInitializationState.value) {
                        LevelPlayInitializationState.Initialized -> HomeScreen(activity = this)
                        LevelPlayInitializationState.Initializing -> InitializationStatus(
                            message = "Initializing LevelPlay…",
                        )
                        is LevelPlayInitializationState.Failed -> InitializationStatus(
                            message = "LevelPlay initialization failed:\n${state.message}",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InitializationStatus(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message)
    }
}
