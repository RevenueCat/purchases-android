package com.revenuecat.purchases.ui.revenuecatui.layoutvalidation

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ui.revenuecatui.BuildConfig
import com.revenuecat.purchases.ui.revenuecatui.InternalPaywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockViewModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.util.Locale

/**
 * Renders each registered [PaywallLayoutValidationFixture] inside a viewport-controlled Compose
 * test, exports the merged + flattened semantics tree, and writes both raw and normalized JSON
 * files to `build/layout-validation/`.
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
 * Outputs (one pair per fixture):
 * ```
 * ui/revenuecatui/build/layout-validation/<offeringId>-paywall-semantics.json
 * ui/revenuecatui/build/layout-validation/<offeringId>-paywall-semantics-normalized.json
 * ```
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w${VIEWPORT_WIDTH_DP}dp-h${VIEWPORT_HEIGHT_DP}dp")
internal class SemanticsLayoutExporterTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `monika-web paywall exports deterministic semantics JSON`() {
        exportPaywall(MonikaWebTestData)
    }

    /**
     * Renders the [fixture]'s paywall, fetches the unmerged semantics tree, exports both the
     * raw and normalized JSON, and asserts that the output files exist with the expected shape.
     */
    private fun exportPaywall(fixture: PaywallLayoutValidationFixture) {
        val metadata = SemanticsLayoutExporter.metadata(
            viewportWidth = VIEWPORT_WIDTH_DP,
            viewportHeight = VIEWPORT_HEIGHT_DP,
            // Frames are emitted in dp (logical pixels), matching iOS points. Use scale = 1.
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
        requireNotNull(rawFile.parentFile).mkdirs()
        rawFile.writeText(rawJson)
        normalizedFile.writeText(normalizedJson)

        assertThat(rawJson).contains("\"offeringId\": \"${fixture.offeringId}\"")
        assertThat(normalizedJson).contains("\"components\"")
        assertThat(rawFile).exists()
        assertThat(normalizedFile).exists()
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
