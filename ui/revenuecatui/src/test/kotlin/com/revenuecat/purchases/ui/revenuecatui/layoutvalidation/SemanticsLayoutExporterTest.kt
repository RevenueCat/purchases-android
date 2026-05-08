package com.revenuecat.purchases.ui.revenuecatui.layoutvalidation

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ui.revenuecatui.BuildConfig
import com.revenuecat.purchases.ui.revenuecatui.InternalPaywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockViewModel
import com.revenuecat.purchases.ui.revenuecatui.helpers.captureToImageCompat
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowPixelCopy
import java.io.File
import java.util.Locale

/**
 * Renders each registered [PaywallLayoutValidationFixture] inside a viewport-controlled Compose
 * test, exports the merged + flattened semantics tree, and writes JSON files and PNG snapshots
 * to `build/layout-validation/`.
 *
 * To add a new paywall to the suite:
 * 1. Create a `<Name>TestData` object that implements [PaywallLayoutValidationFixture] and
 *    points at its `*-components.json` / `*-localizations.json` fixtures.
 * 2. Add a one-line `@Test` method below that delegates to [exportPaywall].
 *
 * Run with:
 * ```
 * ./gradlew :ui:revenuecatui:testDefaultsBc8DebugUnitTest \
 *   --tests "com.revenuecat.purchases.ui.revenuecatui.layoutvalidation.SemanticsLayoutExporterTest"
 * ```
 *
 * Outputs (one set per fixture):
 * ```
 * ui/revenuecatui/build/layout-validation/<offeringId>-paywall-semantics.json
 * ui/revenuecatui/build/layout-validation/<offeringId>-paywall-semantics-normalized.json
 * ui/revenuecatui/build/layout-validation/<offeringId>-paywall-snapshot.png
 * ```
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w${VIEWPORT_WIDTH_DP}dp-h${VIEWPORT_HEIGHT_DP}dp", shadows = [ShadowPixelCopy::class])
@RunWith(AndroidJUnit4::class)
internal class SemanticsLayoutExporterTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `monika-web paywall exports deterministic semantics JSON`() {
        exportPaywall(MonikaWebTestData)
    }

    /**
     * Renders the [fixture]'s paywall, fetches the unmerged semantics tree, exports the raw and
     * normalized JSONs, captures a PNG snapshot, and asserts that all output files exist with
     * the expected shape.
     */
    private fun exportPaywall(fixture: PaywallLayoutValidationFixture) {
        val metadata = SemanticsLayoutExporter.metadata(
            viewportWidth = VIEWPORT_WIDTH_DP,
            viewportHeight = VIEWPORT_HEIGHT_DP,
            // Frames are emitted in dp (logical pixels). Use scale = 1.
            scale = 1.0,
            offeringId = fixture.offeringId,
            timestamp = EXPORT_TIMESTAMP,
            locale = EXPORT_LOCALE,
        )

        composeTestRule.setContent {
            MaterialTheme {
                InternalPaywall(
                    options = PaywallOptions.Builder(dismissRequest = {}).build(),
                    viewModel = MockViewModel(offering = fixture.offering),
                )
            }
        }
        composeTestRule.waitForIdle()

        val root = composeTestRule.onRoot(useUnmergedTree = true).fetchSemanticsNode()
        val layoutTree = SemanticsLayoutExporter.export(root, metadata)
        val normalizedLayoutTree = SemanticsLayoutExporter.flattenAndNormalize(
            export = layoutTree,
            componentNames = fixture.componentNames,
            paywallSyntheticIds = fixture.paywallSyntheticIds,
        )

        val rawJson = SemanticsLayoutExporter.encodeToJson(layoutTree)
        val normalizedJson = SemanticsLayoutExporter.encodeToJson(normalizedLayoutTree)

        val rawFile = outputFile("${fixture.offeringId}-paywall-semantics.json")
        val normalizedFile = outputFile("${fixture.offeringId}-paywall-semantics-normalized.json")
        val snapshotFile = outputFile("${fixture.offeringId}-paywall-snapshot.png")

        requireNotNull(rawFile.parentFile).mkdirs()
        rawFile.writeText(rawJson)
        normalizedFile.writeText(normalizedJson)

        // Capture PNG snapshot via the project's Robolectric-friendly captureToImageCompat()
        // helper. The stock captureToImage() path forces a redraw that hangs under Robolectric
        // (https://github.com/robolectric/robolectric/issues/8071) and ends in
        // ComposeTimeoutException; captureToImageCompat() skips the forced redraw. We still
        // pause the main clock to freeze any in-flight animations, and swallow any Throwable so
        // a missing PNG doesn't fail the JSON export.
        @Suppress("TooGenericExceptionCaught")
        try {
            composeTestRule.mainClock.autoAdvance = false
            val imageBitmap = composeTestRule.onRoot().captureToImageCompat()
            val androidBitmap = imageBitmap.asAndroidBitmap()
            snapshotFile.outputStream().use { fos ->
                androidBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
        } catch (t: Throwable) {
            System.err.println("WARNING: Failed to capture PNG snapshot: ${t.javaClass.simpleName}: ${t.message}")
        } finally {
            composeTestRule.mainClock.autoAdvance = true
        }

        assertThat(rawJson).contains("\"offeringId\": \"${fixture.offeringId}\"")
        assertThat(normalizedJson).contains("\"components\"")
        assertThat(rawFile).exists()
        assertThat(normalizedFile).exists()
        if (snapshotFile.exists()) {
            assertThat(snapshotFile.length()).isGreaterThan(0L)
        }
    }

    private fun outputFile(name: String): File =
        File(BuildConfig.PROJECT_DIR, "build/layout-validation/$name")
}

// Top-level constants so they can be referenced from the @Config annotation.
private const val VIEWPORT_WIDTH_DP = 402
private const val VIEWPORT_HEIGHT_DP = 874

// Deterministic timestamp written into the metadata so the exported JSON is byte-stable across
// test runs. Update when intentionally bumping the export format.
private const val EXPORT_TIMESTAMP = "2026-04-29T13:59:58Z"

// Locale used to build the exporter metadata. Localized strings come from the fixture's
// UiConfig, not from this value.
private val EXPORT_LOCALE: Locale = Locale.US
