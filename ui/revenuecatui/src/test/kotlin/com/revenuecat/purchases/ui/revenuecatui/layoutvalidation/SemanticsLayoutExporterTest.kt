package com.revenuecat.purchases.ui.revenuecatui.layoutvalidation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.BuildConfig
import com.revenuecat.purchases.ui.revenuecatui.InternalPaywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.TestTag
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

@RunWith(AndroidJUnit4::class)
internal class SemanticsLayoutExporterTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `exports real paywall semantics tree to deterministic JSON`() {
        val config = HostLayoutValidationConfig(
            widthPx = 320,
            heightPx = 414,
            densityValue = 2f,
            fontScale = 1f,
        )
        val metadata = SemanticsLayoutExporter.metadata(
            viewportWidth = config.widthPx,
            viewportHeight = config.heightPx,
            scale = config.densityValue.toDouble(),
            offeringId = OFFERING_ID,
            timestamp = "2026-04-29T13:59:58Z",
            locale = Locale.US,
        )

        // Run with:
        // ./gradlew :ui:revenuecatui:testDefaultsBc8DebugUnitTest \
        //   --tests "com.revenuecat.purchases.ui.revenuecatui.layoutvalidation.SemanticsLayoutExporterTest"
        //
        // Output:
        // ui/revenuecatui/build/layout-validation/Template2-paywall-semantics.json
        composeTestRule.setContent {
            FixedHostConfiguration(config = config) {
                InternalPaywall(
                    options = PaywallOptions.Builder(dismissRequest = {}).build(),
                    viewModel = MockViewModel(offering = offeringById(OFFERING_ID)),
                )
            }
        }

        val root = composeTestRule.onRoot(useUnmergedTree = true).fetchSemanticsNode()
        val layoutTree = SemanticsLayoutExporter.export(root, metadata)
        val json = SemanticsLayoutExporter.encodeToJson(layoutTree)
        val outputFile = File(
            BuildConfig.PROJECT_DIR,
            "build/layout-validation/$OFFERING_ID-paywall-semantics.json",
        )

        requireNotNull(outputFile.parentFile).mkdirs()
        outputFile.writeText(json)

        assertThat(json).contains("\"offeringId\": \"$OFFERING_ID\"")
        assertThat(json).contains(TestTag.PURCHASE_BUTTON_TAG)
        assertThat(outputFile).exists()
    }

    @Composable
    private fun FixedHostConfiguration(
        config: HostLayoutValidationConfig,
        content: @Composable () -> Unit,
    ) {
        CompositionLocalProvider(LocalDensity provides config.density) {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .width(config.widthDp)
                        .height(config.heightDp),
                    contentAlignment = Alignment.TopStart,
                ) {
                    content()
                }
            }
        }
    }

    private data class HostLayoutValidationConfig(
        val widthPx: Int,
        val heightPx: Int,
        val densityValue: Float,
        val fontScale: Float,
    ) {
        val density: Density = Density(density = densityValue, fontScale = fontScale)
        val widthDp: Dp = (widthPx / densityValue).dp
        val heightDp: Dp = (heightPx / densityValue).dp
    }

    private fun offeringById(offeringId: String): Offering {
        return listOf(
            TestData.template1Offering,
            TestData.template2Offering,
            TestData.template3Offering,
            TestData.template4Offering,
            TestData.template5Offering,
            TestData.template7Offering,
            TestData.template7CustomPackageOffering,
        ).first { it.identifier == offeringId }
    }

    private companion object {
        const val OFFERING_ID = "Template2"
    }
}
