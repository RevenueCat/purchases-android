package com.revenuecat.sample.ui.weather

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.revenuecat.sample.data.SampleWeatherData

class WeatherViewModel : ViewModel() {
    companion object {
        val shared = WeatherViewModel()
    }

    /*
    The current weather data displayed in the Weather tab
     */
    val currentData: MutableLiveData<SampleWeatherData> by lazy {
        MutableLiveData<SampleWeatherData>().apply {
            value = SampleWeatherData.testCold
        }
    }
}
