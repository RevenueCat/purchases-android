package com.revenuecat.purchases.ui.revenuecatui.snapshottests

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.NightMode
import com.android.resources.ScreenOrientation
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

data class TestConfig(
    val name: String,
    val deviceConfig: DeviceConfig,
) {
    override fun toString(): String {
        return name
    }
}

/**
 * Base class for RevenueCat Snapshot tests
 *
 * ### Automation:
 * - To run them locally you need:
 * `bundle exec fastlane verify_revenuecatui_snapshots`
 * - If your PR requires updating snapshots, you can generate them on CI:
 * `bundle exec fastlane generate_snapshots_RCUI`
 * - Once those PRs are merged in `purchases-android-snapshots`, you can update the commit:
 * `bundle exec fastlane update_snapshots_repo`
 */
@RunWith(Parameterized::class)
abstract class BasePaparazziTest(testConfig: TestConfig) {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = testConfig.deviceConfig,
    )

    companion object {
        private val landscapePixel6Device = DeviceConfig.PIXEL_6.copy(
            screenHeight = 1080,
            screenWidth = 2400,
            xdpi = 411,
            ydpi = 406,
            orientation = ScreenOrientation.LANDSCAPE
        )

        internal val testConfigs = listOf(
            TestConfig("pixel6", DeviceConfig.PIXEL_6),
            TestConfig("pixel6_landscape", landscapePixel6Device),
            TestConfig("pixel6_dark_mode", DeviceConfig.PIXEL_6.copy(nightMode = NightMode.NIGHT)),
            TestConfig("pixel6_spanish", DeviceConfig.PIXEL_6.copy(locale = "es")),
            TestConfig("nexus7", DeviceConfig.NEXUS_7),
            TestConfig("nexus10", DeviceConfig.NEXUS_10),
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            return testConfigs.map { arrayOf(it) }
        }
    }

    fun screenshotTest(content: @Composable () -> Unit) {
        paparazzi.snapshot {
            // Note that this means we will use the preview views instead of the real views.
            // This is to avoid using real images for the screenshots (which doesn't work).
            CompositionLocalProvider(LocalInspectionMode provides true) {
                content.invoke()
            }
        }
    }
}
