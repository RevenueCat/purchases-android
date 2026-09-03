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

// enableEdgeToEdge's default navigation bar scrims, for the API levels without contrast enforcement.
@Suppress("MagicNumber")
internal val NAVIGATION_BAR_LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)

@Suppress("MagicNumber")
internal val NAVIGATION_BAR_DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

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
    val isDarkMode = context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        @Suppress("DEPRECATION")
        statusBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        navigationBarColor = when {
            // The system's contrast enforcement scrims 3-button navigation over a transparent bar.
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> Color.TRANSPARENT
            // No contrast enforcement yet: paint enableEdgeToEdge's default scrim for the night mode.
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                if (isDarkMode) NAVIGATION_BAR_DARK_SCRIM else NAVIGATION_BAR_LIGHT_SCRIM
            // Light navigation bar icons don't exist before 26, so only the dark scrim keeps them visible.
            else -> NAVIGATION_BAR_DARK_SCRIM
        }
    }
    // The legacy dialog theme never requests light system bars, leaving light-on-light icons in light
    // mode; match enableEdgeToEdge's day/night-based appearance.
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
