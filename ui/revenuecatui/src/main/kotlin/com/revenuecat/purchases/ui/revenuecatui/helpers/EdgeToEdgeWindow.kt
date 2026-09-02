package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat

/**
 * The theme a window has to be created with for [applyEdgeToEdge] to have anything to work with: it is not a
 * floating theme, so installing the decor grants the window the same layout flags and `fitInsetsTypes = 0` an
 * activity window gets, which is what makes the window frame itself cover the system bar regions.
 */
internal const val EDGE_TO_EDGE_WINDOW_THEME: Int = android.R.style.Theme_Translucent_NoTitleBar

/**
 * Hand-rolls what `enableEdgeToEdge` does for activity windows, for windows it cannot be called on (a
 * dialog's, for instance). Expects a window created with [EDGE_TO_EDGE_WINDOW_THEME].
 */
internal fun Window.applyEdgeToEdge() {
    // Do not remove this seemingly unused read: it forces decor installation, without which the framework's
    // generateLayout() would run later and overwrite the attributes set below.
    val decor = decorView
    // Hardware acceleration is not inherited from the host: under a software-rendered host (e.g. Unity)
    // Compose's hardware bitmaps would crash without it.
    addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        attributes = attributes.apply {
            layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }
    // <30: extends the frame behind the bars via the legacy systemUiVisibility flags; 30-34: stops the
    // decor from padding the content; 35+: no-op (enforced).
    WindowCompat.setDecorFitsSystemWindows(this, false)
    // Legacy framework themes don't set windowDrawsSystemBarBackgrounds, without which the decor paints
    // the bar regions black instead of transparent - including on 35+, where the bar color itself is
    // already enforced transparent.
    addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        @Suppress("DEPRECATION")
        statusBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        navigationBarColor = Color.TRANSPARENT
    }
    // The legacy dialog theme never requests light system bars, leaving light-on-light icons in light
    // mode; match enableEdgeToEdge's day/night-based appearance.
    val isDarkMode = context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    WindowCompat.getInsetsController(this, decor).apply {
        isAppearanceLightStatusBars = !isDarkMode
        isAppearanceLightNavigationBars = !isDarkMode
    }
    setSoftInputMode(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        },
    )
}
