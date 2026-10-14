package com.revenuecat.purchases.ui.revenuecatui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.ProvidePreviewImageLoader
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowPixelCopy
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp", sdk = [26], shadows = [ShadowPixelCopy::class])
internal class HiddenLeadingStackHeroPreviewTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `phone control preview renders a hero and purchase content`() {
        lateinit var state: PaywallState.Loaded.Components
        lateinit var view: View
        composeTestRule.setContent {
            view = LocalView.current
            state = previewHiddenLeadingStackHeroState(includeSide = false)
            CompositionLocalProvider(LocalInspectionMode provides true) {
                ProvidePreviewImageLoader(previewImageLoader(R.drawable.default_background)) {
                    LoadedPaywallComponents(state, { }, modifier = Modifier.requiredWidth(411.dp).fillMaxHeight())
                }
            }
        }
        composeTestRule.onNodeWithText("Find your next escape").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start exploring").assertIsDisplayed()
        composeTestRule.runOnIdle { assertThat(state.mainStackHasHeroImage).isTrue() }
        capturePreview(view, "phone-control.png")
    }

    @Test
    fun `responsive side preview releases and restores the phone hero when its side appears`() {
        lateinit var state: PaywallState.Loaded.Components
        lateinit var view: View
        val width = mutableStateOf(411.dp)
        composeTestRule.setContent {
            view = LocalView.current
            state = previewHiddenLeadingStackHeroState(includeSide = true)
            CompositionLocalProvider(LocalInspectionMode provides true) {
                ProvidePreviewImageLoader(previewImageLoader(R.drawable.default_background)) {
                    LoadedPaywallComponents(state, { }, modifier = Modifier.requiredWidth(width.value).fillMaxHeight())
                }
            }
        }
        composeTestRule.onNodeWithText("Find your next escape").assertIsDisplayed()
        composeTestRule.runOnIdle { assertThat(state.mainStackHasHeroImage).isTrue() }
        capturePreview(view, "phone-hidden-side.png")
        composeTestRule.runOnIdle { width.value = 1000.dp }
        composeTestRule.onNodeWithText("A little further from ordinary").assertExists()
        composeTestRule.runOnIdle { assertThat(state.mainStackHasHeroImage).isFalse() }
        composeTestRule.runOnIdle { width.value = 411.dp }
        composeTestRule.onNodeWithText("Start exploring").assertIsDisplayed()
        composeTestRule.runOnIdle { assertThat(state.mainStackHasHeroImage).isTrue() }
    }

    @Test
    @Config(qualifiers = "w1000dp-h891dp")
    fun `wide preview renders the revealed side with ordinary top safe area`() {
        lateinit var state: PaywallState.Loaded.Components
        lateinit var view: View
        composeTestRule.setContent {
            view = LocalView.current
            state = previewHiddenLeadingStackHeroState(includeSide = true)
            CompositionLocalProvider(LocalInspectionMode provides true) {
                ProvidePreviewImageLoader(previewImageLoader(R.drawable.default_background)) {
                    LoadedPaywallComponents(state, { }, modifier = Modifier.requiredWidth(1000.dp).fillMaxHeight())
                }
            }
        }
        composeTestRule.onNodeWithText("A little further from ordinary").assertIsDisplayed()
        composeTestRule.onNodeWithText("Find your next escape").assertIsDisplayed()
        composeTestRule.runOnIdle { assertThat(state.mainStackHasHeroImage).isFalse() }
        capturePreview(view, "wide-visible-side.png")
    }

    private fun capturePreview(view: View, name: String) {
        composeTestRule.waitForIdle()
        composeTestRule.runOnUiThread {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val file = File("build/hidden-hero-preview-evidence", name)
            requireNotNull(file.parentFile).mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

}
