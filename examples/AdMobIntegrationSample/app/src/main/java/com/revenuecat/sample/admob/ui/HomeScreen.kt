package com.revenuecat.sample.admob.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

enum class AdFormat(val title: String, val subtitle: String) {
    BANNER("Banner Ad", "Always visible, auto-loaded"),
    INTERSTITIAL("Interstitial Ad", "Full-screen ad"),
    APP_OPEN("App Open Ad", "App launch/resume ad"),
    REWARDED("Rewarded Ad", "Rewards users after viewing"),
    REWARDED_INTERSTITIAL("Rewarded Interstitial Ad", "Interstitial that rewards users"),
    NATIVE("Native Ad", "Text + images integrated into UI"),
    NATIVE_VIDEO("Native Video Ad", "Video content integrated into UI"),
    ERROR_TESTING("Error Testing", "Triggers ad load failure"),
}

internal sealed class Screen {
    data object Home : Screen()
    data class AdDetail(val adFormat: AdFormat) : Screen()
}

@Composable
fun HomeScreen(
    activity: Activity,
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    when (val screen = currentScreen) {
        is Screen.Home -> AdFormatListScreen(
            onFormatSelected = { currentScreen = Screen.AdDetail(it) },
        )
        is Screen.AdDetail -> AdFormatDetailScreen(
            format = screen.adFormat,
            activity = activity,
            onBack = { currentScreen = Screen.Home },
        )
    }
}
