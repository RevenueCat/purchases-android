/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Ported from androidx.activity:activity:1.9.3 (androidx/activity/EdgeToEdge.kt), modified to apply to a
// Window instead of a ComponentActivity so it also works for dialog windows, with the implementations below
// this module's minSdk removed. Kept otherwise as close to the original as possible.

package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.app.UiModeManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

// The light scrim color used in the platform API 29+
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/com/android/internal/policy/DecorView.java;drc=6ef0f022c333385dba2c294e35b8de544455bf19;l=142
@Suppress("MagicNumber")
internal val DefaultLightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)

// The dark scrim color used in the platform.
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/res/res/color/system_bar_background_semi_transparent.xml
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/res/remote_color_resources_res/values/colors.xml;l=67
@Suppress("MagicNumber")
internal val DefaultDarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

/**
 * Enables the edge-to-edge display for this [Window].
 *
 * The default style configures the system bars with a transparent background when contrast can be
 * enforced by the system (API 29 or above). On older platforms (which only have 3-button/2-button
 * navigation modes), an equivalent scrim is applied to ensure contrast with the system bars.
 *
 * See [SystemBarStyle] for more customization options.
 *
 * @param statusBarStyle The [SystemBarStyle] for the status bar.
 * @param navigationBarStyle The [SystemBarStyle] for the navigation bar.
 */
internal fun Window.enableEdgeToEdge(
    statusBarStyle: SystemBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
    navigationBarStyle: SystemBarStyle = SystemBarStyle.auto(DefaultLightScrim, DefaultDarkScrim),
) {
    val view = decorView
    val statusBarIsDark = statusBarStyle.detectDarkMode(view.resources)
    val navigationBarIsDark = navigationBarStyle.detectDarkMode(view.resources)
    val impl = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        EdgeToEdgeApi30()
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        EdgeToEdgeApi29()
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        EdgeToEdgeApi28()
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        EdgeToEdgeApi26()
    } else {
        EdgeToEdgeApi23()
    }
    impl.setUp(
        statusBarStyle,
        navigationBarStyle,
        this,
        view,
        statusBarIsDark,
        navigationBarIsDark,
    )
    impl.adjustLayoutInDisplayCutoutMode(this)
}

/**
 * The style for the status bar or the navigation bar used in [enableEdgeToEdge].
 */
internal class SystemBarStyle private constructor(
    private val lightScrim: Int,
    internal val darkScrim: Int,
    internal val nightMode: Int,
    internal val detectDarkMode: (Resources) -> Boolean,
) {

    companion object {

        /**
         * Creates a new instance of [SystemBarStyle]. This style detects the dark mode
         * automatically and applies the recommended style for each of the status bar and the
         * navigation bar. If this style doesn't work for your app, consider using either [dark] or
         * [light].
         * - On API level 29 and above, both the status bar and the navigation bar will be
         *   transparent. However, the navigation bar with 3 or 2 buttons will have a translucent
         *   scrim. This scrim color is provided by the platform and *cannot be customized*.
         * - On API level 28 and below, the status bar will be transparent, and the navigation bar
         *   will have one of the specified scrim colors depending on the dark mode.
         * @param lightScrim The scrim color to be used for the background when the app is in light
         * mode. Note that this is used only on API level 28 and below.
         * @param darkScrim The scrim color to be used for the background when the app is in dark
         * mode. This is also used on devices where the system icon color is always light. Note that
         * this is used only on API level 28 and below.
         * @param detectDarkMode Optional. Detects whether UI currently uses dark mode or not. The
         * default implementation can detect any of the standard dark mode features from the
         * platform, appcompat, and Jetpack Compose.
         */
        fun auto(
            @ColorInt lightScrim: Int,
            @ColorInt darkScrim: Int,
            detectDarkMode: (Resources) -> Boolean = { resources ->
                (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
            },
        ): SystemBarStyle {
            return SystemBarStyle(
                lightScrim = lightScrim,
                darkScrim = darkScrim,
                nightMode = UiModeManager.MODE_NIGHT_AUTO,
                detectDarkMode = detectDarkMode,
            )
        }

        /**
         * Creates a new instance of [SystemBarStyle]. This style consistently applies the specified
         * scrim color regardless of the system navigation mode.
         *
         * @param scrim The scrim color to be used for the background. It is expected to be dark
         * for the contrast against the light system icons.
         */
        fun dark(@ColorInt scrim: Int): SystemBarStyle {
            return SystemBarStyle(
                lightScrim = scrim,
                darkScrim = scrim,
                nightMode = UiModeManager.MODE_NIGHT_YES,
                detectDarkMode = { _ -> true },
            )
        }

        /**
         * Creates a new instance of [SystemBarStyle]. This style consistently applies the specified
         * scrim color regardless of the system navigation mode.
         *
         * @param scrim The scrim color to be used for the background. It is expected to be light
         * for the contrast against the dark system icons.
         * @param darkScrim The scrim color to be used for the background on devices where the
         * system icon color is always light. It is expected to be dark.
         */
        fun light(@ColorInt scrim: Int, @ColorInt darkScrim: Int): SystemBarStyle {
            return SystemBarStyle(
                lightScrim = scrim,
                darkScrim = darkScrim,
                nightMode = UiModeManager.MODE_NIGHT_NO,
                detectDarkMode = { _ -> false },
            )
        }
    }

    internal fun getScrim(isDark: Boolean) = if (isDark) darkScrim else lightScrim

    internal fun getScrimWithEnforcedContrast(isDark: Boolean): Int {
        return when {
            nightMode == UiModeManager.MODE_NIGHT_AUTO -> Color.TRANSPARENT
            isDark -> darkScrim
            else -> lightScrim
        }
    }
}

private interface EdgeToEdgeImpl {

    @Suppress("LongParameterList")
    fun setUp(
        statusBarStyle: SystemBarStyle,
        navigationBarStyle: SystemBarStyle,
        window: Window,
        view: View,
        statusBarIsDark: Boolean,
        navigationBarIsDark: Boolean,
    )

    fun adjustLayoutInDisplayCutoutMode(window: Window)
}

// Upstream's base class for this level is EdgeToEdgeApi23; the module's minSdk makes it the base impl here.
private open class EdgeToEdgeApi23 : EdgeToEdgeImpl {

    @Suppress("DEPRECATION")
    override fun setUp(
        statusBarStyle: SystemBarStyle,
        navigationBarStyle: SystemBarStyle,
        window: Window,
        view: View,
        statusBarIsDark: Boolean,
        navigationBarIsDark: Boolean,
    ) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = statusBarStyle.getScrim(statusBarIsDark)
        window.navigationBarColor = navigationBarStyle.darkScrim
        WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !statusBarIsDark
    }

    override fun adjustLayoutInDisplayCutoutMode(window: Window) {
        // No display cutout before SDK 28.
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private open class EdgeToEdgeApi26 : EdgeToEdgeApi23() {

    @Suppress("DEPRECATION")
    override fun setUp(
        statusBarStyle: SystemBarStyle,
        navigationBarStyle: SystemBarStyle,
        window: Window,
        view: View,
        statusBarIsDark: Boolean,
        navigationBarIsDark: Boolean,
    ) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = statusBarStyle.getScrim(statusBarIsDark)
        window.navigationBarColor = navigationBarStyle.getScrim(navigationBarIsDark)
        WindowInsetsControllerCompat(window, view).run {
            isAppearanceLightStatusBars = !statusBarIsDark
            isAppearanceLightNavigationBars = !navigationBarIsDark
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
private open class EdgeToEdgeApi28 : EdgeToEdgeApi26() {

    override fun adjustLayoutInDisplayCutoutMode(window: Window) {
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private open class EdgeToEdgeApi29 : EdgeToEdgeApi28() {

    @Suppress("DEPRECATION")
    override fun setUp(
        statusBarStyle: SystemBarStyle,
        navigationBarStyle: SystemBarStyle,
        window: Window,
        view: View,
        statusBarIsDark: Boolean,
        navigationBarIsDark: Boolean,
    ) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = statusBarStyle.getScrimWithEnforcedContrast(statusBarIsDark)
        window.navigationBarColor =
            navigationBarStyle.getScrimWithEnforcedContrast(navigationBarIsDark)
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced =
            navigationBarStyle.nightMode == UiModeManager.MODE_NIGHT_AUTO
        WindowInsetsControllerCompat(window, view).run {
            isAppearanceLightStatusBars = !statusBarIsDark
            isAppearanceLightNavigationBars = !navigationBarIsDark
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
private class EdgeToEdgeApi30 : EdgeToEdgeApi29() {

    override fun adjustLayoutInDisplayCutoutMode(window: Window) {
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    }
}
