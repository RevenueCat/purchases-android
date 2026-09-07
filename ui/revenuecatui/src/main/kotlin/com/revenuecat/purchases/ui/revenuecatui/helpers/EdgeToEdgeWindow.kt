package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.os.Build
import android.view.Window
import android.view.WindowManager

/**
 * The theme a window has to be created with for [applyEdgeToEdge] to have anything to work with: it is not a
 * floating theme, so installing the decor grants the window the same layout flags and `fitInsetsTypes = 0` an
 * activity window gets, which is what makes the window frame itself cover the system bar regions.
 */
internal const val EDGE_TO_EDGE_WINDOW_THEME: Int = android.R.style.Theme_Translucent_NoTitleBar

/**
 * Makes a window created with [EDGE_TO_EDGE_WINDOW_THEME] edge to edge: [enableEdgeToEdge] with its default
 * styles, plus the window properties `enableEdgeToEdge` takes for granted on an activity window but a
 * dialog's window doesn't get.
 */
internal fun Window.applyEdgeToEdge() {
    // Must run before anything else touches the window: its decorView read forces decor installation,
    // without which the framework's generateLayout() would run later and overwrite the properties set here.
    enableEdgeToEdge()
    // Hardware acceleration is not inherited from the host: under a software-rendered host (e.g. Unity)
    // Compose's hardware bitmaps would crash without it.
    addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
    // Legacy framework themes don't set windowDrawsSystemBarBackgrounds, without which the decor paints
    // the bar regions black instead of transparent - including on 35+, where the bar color itself is
    // already enforced transparent.
    addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    setSoftInputMode(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        },
    )
}
