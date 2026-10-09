package com.revenuecat.paywallstester.ui.screens.main

import com.revenuecat.paywallstester.R
import android.R as AndroidR

sealed class Tab(val route: String, val title: String, val iconResourceId: Int) {
    object AppInfo : Tab("app-info", "App Info", AndroidR.drawable.ic_menu_call)
    object Paywalls : Tab("paywalls", "Paywalls", AndroidR.drawable.ic_dialog_map)
    object Offerings : Tab("offerings", "Flows", AndroidR.drawable.ic_dialog_dialer)
    object Checkpoints : Tab("checkpoints", "Checkpoints", R.drawable.ic_sports_score)
    object Locale : Tab("locale", "Locale", AndroidR.drawable.ic_menu_edit)
}
