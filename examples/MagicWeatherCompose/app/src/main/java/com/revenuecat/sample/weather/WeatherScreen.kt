package com.revenuecat.sample.weather

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun WeatherScreen(
    navigateToPaywallCallback: () -> Unit,
) {
    val viewModel: WeatherViewModel = viewModel()
    val uiState = viewModel.uiState.collectAsState()

    if (uiState.value.shouldNavigateToPaywall) {
        navigateToPaywallCallback()
        viewModel.resetNavigationStatus()
    }

    uiState.value.displayErrorMessage?.let { errorMessage ->
        Toast.makeText(
            LocalContext.current,
            errorMessage,
            Toast.LENGTH_SHORT,
        ).show()
        viewModel.resetErrorMessage()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = uiState.value.weatherData.weatherColor)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.weight(1f, true),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = uiState.value.weatherData.displayText,
                fontSize = 82.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 94.sp,
            )
        }
        Button(
            onClick = viewModel::changeWeather,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
        ) {
            Text(text = "Change weather")
        }
    }
}
