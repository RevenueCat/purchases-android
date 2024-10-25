package com.revenuecat.sample.main

import androidx.annotation.DrawableRes
import com.revenuecat.sample.R

sealed class BottomNavigationScreens(val title: String, @DrawableRes val iconResourceId: Int) {
    object Weather : BottomNavigationScreens("Weather", R.drawable.ic_weather_black_24dp)
    object User : BottomNavigationScreens("User", R.drawable.ic_user_black_24dp)
}
