package com.revenuecat.samplecat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.samplecat.ui.navigation.SampleCatNavHost
import com.revenuecat.samplecat.ui.theme.SampleCatTheme
import com.revenuecat.samplecat.viewmodel.UserViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SampleCatTheme {
                val userViewModel: UserViewModel = viewModel()

                Surface(modifier = Modifier.fillMaxSize()) {
                    SampleCatNavHost(userViewModel = userViewModel)
                }
            }
        }
    }
}
