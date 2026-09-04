package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

// robolectric.properties pins the default sdk at 34 (Paparazzi shares this test JVM), so tests without an
// explicit @Config run there.
@RunWith(AndroidJUnit4::class)
class EdgeToEdgeWindowTest {

    private lateinit var activity: Activity

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    }

    @Test
    fun `the window frame covers the system bar regions`() {
        val attributes = edgeToEdgeWindow().attributes

        assertThat(attributes.fitInsetsTypes).isEqualTo(0)
        assertThat(attributes.fitInsetsSides).isEqualTo(0)
        assertThat(attributes.flags and WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN).isNotEqualTo(0)
        @Suppress("DEPRECATION")
        assertThat(attributes.flags and WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR).isNotEqualTo(0)
        assertThat(attributes.flags and WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            .isNotEqualTo(0)
    }

    // Not inherited from the host window: under a software-rendered host Compose's hardware bitmaps crash.
    @Test
    fun `the window is hardware accelerated`() {
        val attributes = edgeToEdgeWindow().attributes

        assertThat(attributes.flags and WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED).isNotEqualTo(0)
    }

    // layoutInDisplayCutoutMode does not exist in the api 26 framework jar, so this only smoke-tests the
    // properties that do. Contrast enforcement doesn't exist before api 29, so the navigation bar gets
    // enableEdgeToEdge's default scrim instead of being transparent.
    @Test
    @Config(sdk = [26])
    @Suppress("DEPRECATION")
    fun `applying edge to edge works before api 28`() {
        val window = edgeToEdgeWindow()

        assertThat(window.attributes.flags and WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
            .isNotEqualTo(0)
        assertThat(window.statusBarColor).isEqualTo(Color.TRANSPARENT)
        assertThat(window.navigationBarColor).isEqualTo(DefaultLightScrim)
    }

    @Test
    @Config(sdk = [28], qualifiers = "night")
    @Suppress("DEPRECATION")
    fun `the navigation bar gets the dark scrim in dark mode between api 26 and 28`() {
        assertThat(edgeToEdgeWindow().navigationBarColor).isEqualTo(DefaultDarkScrim)
    }

    // Navigation bar icons are always light before api 26 (dark icons arrived with the light-navigation-bar
    // appearance in 26), so the scrim is the dark one even in light mode.
    @Test
    @Config(sdk = [24])
    @Suppress("DEPRECATION")
    fun `the navigation bar always gets the dark scrim before api 26`() {
        val window = edgeToEdgeWindow()

        assertThat(window.statusBarColor).isEqualTo(Color.TRANSPARENT)
        assertThat(window.navigationBarColor).isEqualTo(DefaultDarkScrim)
    }

    @Test
    @Config(sdk = [28])
    fun `the display cutout mode is short edges between api 28 and 29`() {
        val attributes = edgeToEdgeWindow().attributes

        assertThat(attributes.layoutInDisplayCutoutMode)
            .isEqualTo(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES)
    }

    @Test
    @Config(sdk = [30])
    fun `the display cutout mode is always from api 30`() {
        val attributes = edgeToEdgeWindow().attributes

        assertThat(attributes.layoutInDisplayCutoutMode)
            .isEqualTo(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS)
    }

    @Test
    @Config(sdk = [29])
    @Suppress("DEPRECATION")
    fun `the decor extends behind the bars through systemUiVisibility before api 30`() {
        val systemUiVisibility = edgeToEdgeWindow().decorView.systemUiVisibility

        assertThat(systemUiVisibility and View.SYSTEM_UI_FLAG_LAYOUT_STABLE).isNotEqualTo(0)
        assertThat(systemUiVisibility and View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN).isNotEqualTo(0)
        assertThat(systemUiVisibility and View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION).isNotEqualTo(0)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `the system bars are transparent between api 29 and 34`() {
        val window = edgeToEdgeWindow()

        assertThat(window.statusBarColor).isEqualTo(Color.TRANSPARENT)
        assertThat(window.navigationBarColor).isEqualTo(Color.TRANSPARENT)
    }

    // The auto style keeps navigation bar contrast enforcement on (the system scrims 3-button navigation so
    // the buttons stay readable over arbitrary paywall content) and turns the status bar counterpart off.
    @Test
    @Config(sdk = [29])
    @Suppress("DEPRECATION")
    fun `contrast enforcement follows the enableEdgeToEdge auto style`() {
        val window = edgeToEdgeWindow()

        assertThat(window.isNavigationBarContrastEnforced).isTrue
        assertThat(window.isStatusBarContrastEnforced).isFalse
    }

    @Test
    @Config(sdk = [30])
    fun `the soft keyboard does not resize the window from api 30`() {
        val attributes = edgeToEdgeWindow().attributes

        assertThat(attributes.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST)
            .isEqualTo(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
    }

    @Test
    @Config(sdk = [29])
    @Suppress("DEPRECATION")
    fun `the soft keyboard resizes the window before api 30`() {
        val attributes = edgeToEdgeWindow().attributes

        assertThat(attributes.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST)
            .isEqualTo(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun edgeToEdgeWindow(): Window =
        Dialog(activity, EDGE_TO_EDGE_WINDOW_THEME).window!!.apply { applyEdgeToEdge() }
}
