package com.revenuecat.paywallstester.ui.screens.main

import android.R

sealed class Tab(val route: String, val title: String, val iconResourceId: Int) {
    object AppInfo : Tab("app-info", "App Info", R.drawable.ic_menu_call)
    object Paywalls : Tab("paywalls", "Paywalls", R.drawable.ic_dialog_map)
    object Offerings : Tab("offerings", "Offerings", R.drawable.ic_dialog_dialer)
}
