package com.revenuecat.purchases.ui.revenuecatui.snapshottests

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.Snapshot
import com.android.resources.NightMode
import com.android.resources.ScreenOrientation
import com.revenuecat.snapshots.SentrySnapshotHandler
import com.revenuecat.snapshots.SentrySnapshotMetadata
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale

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
abstract class BasePaparazziTest(private val testConfig: TestConfig) {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = testConfig.deviceConfig,
        snapshotHandler = SentrySnapshotHandler(
            metadataProvider = { snapshot -> snapshot.toSentryMetadata(testConfig) },
        ),
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

private fun Snapshot.toSentryMetadata(testConfig: TestConfig): SentrySnapshotMetadata {
    val methodName = testName.methodName.substringBefore('[')
    val variant = testName.methodName
        .substringAfter('[', missingDelimiterValue = "")
        .removeSuffix("]")
        .takeIf(String::isNotBlank)
    val snapshotName = name?.takeIf(String::isNotBlank)
    val displayName = listOfNotNull(methodName, variant, snapshotName).joinToString(" – ")
    val device = testConfig.deviceConfig
    val theme = if (device.nightMode == NightMode.NIGHT) "dark" else "light"
    val locale = device.locale.orEmpty().ifBlank { "en" }

    return SentrySnapshotMetadata(
        displayName = displayName,
        group = "revenuecatui/${testName.className}",
        tags = mapOf(
            "module" to "revenuecatui",
            "test_class" to testName.className,
            "device" to testConfig.name,
            "theme" to theme,
            "orientation" to device.orientation.name.lowercase(Locale.US),
            "locale" to locale,
        ),
        canvasTheme = if (device.nightMode == NightMode.NIGHT) {
            SentrySnapshotMetadata.CanvasTheme.DARK
        } else {
            SentrySnapshotMetadata.CanvasTheme.LIGHT
        },
        context = buildJsonObject {
            put("test_name", "${testName.packageName}.${testName.className}#$methodName")
            putJsonObject("paparazzi") {
                put("package_name", testName.packageName)
                put("class_name", testName.className)
                put("method_name", methodName)
                variant?.let { put("variant", it) }
                snapshotName?.let { put("snapshot_name", it) }
            }
            putJsonObject("device") {
                put("name", testConfig.name)
                put("screen_width", device.screenWidth)
                put("screen_height", device.screenHeight)
                put("density", device.density.toString())
                put("font_scale", device.fontScale)
                put("locale", locale)
                put("orientation", device.orientation.name.lowercase(Locale.US))
            }
        },
    )
}
