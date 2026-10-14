package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
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

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
internal class HiddenLeadingStackHeroPreviewTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `phone control preview renders a hero and purchase content`() {
        lateinit var state: PaywallState.Loaded.Components
        composeTestRule.setContent {
            state = previewHiddenLeadingStackHeroState(includeSide = false)
            ProvidePreviewImageLoader(previewImageLoader(R.drawable.default_background)) {
                LoadedPaywallComponents(state, { }, modifier = Modifier.requiredSize(411.dp, 891.dp))
            }
        }
        composeTestRule.onNodeWithText("Find your next escape").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start exploring").assertIsDisplayed()
        composeTestRule.runOnIdle { assertThat(state.mainStackHasHeroImage).isTrue() }
    }

    @Test
    fun `responsive side preview releases and restores the phone hero when its side appears`() {
        lateinit var state: PaywallState.Loaded.Components
        val width = mutableStateOf(411.dp)
        composeTestRule.setContent {
            state = previewHiddenLeadingStackHeroState(includeSide = true)
            ProvidePreviewImageLoader(previewImageLoader(R.drawable.default_background)) {
                LoadedPaywallComponents(state, { }, modifier = Modifier.requiredSize(width.value, 891.dp))
            }
        }
        composeTestRule.onNodeWithText("Find your next escape").assertIsDisplayed()
        composeTestRule.runOnIdle { assertThat(state.mainStackHasHeroImage).isTrue() }
        composeTestRule.runOnIdle { width.value = 1000.dp }
        composeTestRule.onNodeWithText("A little further from ordinary").assertExists()
        composeTestRule.runOnIdle { assertThat(state.mainStackHasHeroImage).isFalse() }
        composeTestRule.runOnIdle { width.value = 411.dp }
        composeTestRule.onNodeWithText("Start exploring").assertIsDisplayed()
        composeTestRule.runOnIdle { assertThat(state.mainStackHasHeroImage).isTrue() }
    }
}
