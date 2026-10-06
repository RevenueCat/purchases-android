package com.revenuecat.purchases.ui.revenuecatui.workflow

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.VideoComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FitMode
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.ThemeVideoUrls
import com.revenuecat.purchases.paywalls.components.properties.VideoUrls
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

/**
 * The stand-in replaces the real page in place, so a block that reserves no height moves everything
 * below it. Asserting the size a component stores is not enough: `Modifier.size` maps a Fit axis to
 * `wrapContent` and ignores its default, so a stored default measures at zero all the same.
 */
@OptIn(InternalRevenueCatAPI::class)
@RunWith(AndroidJUnit4::class)
internal class WorkflowSkeletonMeasureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun measuredHeightOfEmptyBlock(height: SizeConstraint): Float {
        // The placeholder shimmer is an infiniteRepeatable, so a shared clock never reports idle.
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .size(Size(width = SizeConstraint.Fill(), height = height))
                        .testTag("block"),
                )
            }
        }
        val bounds = composeTestRule.onNodeWithTag("block").getUnclippedBoundsInRoot()
        return (bounds.bottom - bounds.top).value
    }

    @Test
    fun `a fit height on an empty block measures at zero`() {
        assertThat(measuredHeightOfEmptyBlock(SizeConstraint.Fit(default = 720u))).isZero()
    }

    @Test
    fun `the height the transform gives a video stand-in reserves real space`() {
        val video = VideoComponent(
            source = ThemeVideoUrls(
                light = VideoUrls(width = 1280u, height = 720u, url = URL("https://example.com/v.mp4")),
                dark = null,
            ),
            fallbackSource = null,
            visible = null,
            showControls = false,
            autoplay = true,
            loop = true,
            muteAudio = true,
            size = Size(width = SizeConstraint.Fill(), height = SizeConstraint.Fit()),
            fitMode = FitMode.FIT,
            maskShape = null,
            colorOverlay = null,
            padding = null,
            margin = null,
            border = null,
            shadow = null,
            overrides = null,
        )
        val data = PaywallComponentsData(
            templateName = "template",
            assetBaseURL = URL("https://example.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = listOf(video)),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(0xFFFFFFFF.toInt()))),
                ),
            ),
            componentsLocalizations = mapOf(LocaleId("en_US") to emptyMap()),
        )

        val standIn = WorkflowSkeleton.transform(data)
            .componentsConfig.base.stack.components.first() as StackComponent

        // Clamped by the window, so assert it reserves space rather than a specific number. The Fit
        // case above measures at zero, which is what this has to avoid.
        assertThat(measuredHeightOfEmptyBlock(standIn.size.height)).isGreaterThan(0f)
    }
}
