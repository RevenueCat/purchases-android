package com.revenuecat.sample.weather

import com.revenuecat.sample.data.SampleWeatherData

data class WeatherState(
    val weatherData: SampleWeatherData,
    val displayErrorMessage: String? = null,
    val shouldDisplayDebugView: Boolean = true,
    val shouldNavigateToPaywall: Boolean = false,
)
