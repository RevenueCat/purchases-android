package com.revenuecat.sample.weather

import androidx.lifecycle.ViewModel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.sample.data.SampleWeatherData
import com.revenuecat.sample.utils.hasActiveEntitlements
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


class WeatherViewModel : ViewModel() {

    private val _uiState: MutableStateFlow<WeatherState> = MutableStateFlow(
        WeatherState(
            SampleWeatherData.testCold,
        ),
    )
    val uiState: StateFlow<WeatherState> = _uiState.asStateFlow()

    fun changeWeather() {
        Purchases.sharedInstance.getCustomerInfoWith(
            onError = {
                _uiState.update { currentState ->
                    currentState.copy(
                        displayErrorMessage = it.message,
                    )
                }
            },
            onSuccess = {
                if (it.entitlements.hasActiveEntitlements()) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            weatherData = SampleWeatherData.generateSampleData(
                                SampleWeatherData.Environment.EARTH,
                                null,
                            ),
                        )
                    }
                } else {
                    _uiState.update { currentState ->
                        currentState.copy(
                            shouldNavigateToPaywall = true,
                        )
                    }
                }
            },
        )
    }

    fun resetNavigationStatus() {
        _uiState.update { currentState ->
            currentState.copy(
                shouldNavigateToPaywall = false,
            )
        }
    }

    fun resetErrorMessage() {
        _uiState.update { currentState ->
            currentState.copy(
                displayErrorMessage = null,
            )
        }
    }

}
