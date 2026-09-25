package com.revenuecat.purchases.ui.revenuecatui.components.tabs

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.ui.revenuecatui.components.previewEmptyState
import com.revenuecat.purchases.ui.revenuecatui.components.previewStackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabControlButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabControlToggleComponentStyle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class TabControlHapticFeedbackTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val noHaptic = -1

    @Test
    fun `Tab button performs a segment tick when selecting another tab`() {
        val view = setTabButtons(initialTabIndex = 0)

        composeTestRule.onNodeWithTag("t1").performClick()

        assertThat(view.lastHaptic()).isEqualTo(HapticFeedbackConstants.SEGMENT_TICK)
    }

    @Test
    fun `Tab button performs no haptic when tapping the selected tab`() {
        val view = setTabButtons(initialTabIndex = 0)

        composeTestRule.onNodeWithTag("t0").performClick()

        assertThat(view.lastHaptic()).isEqualTo(noHaptic)
    }

    @Test
    fun `Tab button performs no haptic when tapping the selected tab without ordered tab ids`() {
        val view = setTabButtons(initialTabIndex = 0, tabIdsOrdered = emptyList())

        composeTestRule.onNodeWithTag("t0").performClick()

        assertThat(view.lastHaptic()).isEqualTo(noHaptic)
    }

    @Test
    fun `Tab button performs a segment tick when selecting another tab without ordered tab ids`() {
        val view = setTabButtons(initialTabIndex = 0, tabIdsOrdered = emptyList())

        composeTestRule.onNodeWithTag("t1").performClick()

        assertThat(view.lastHaptic()).isEqualTo(HapticFeedbackConstants.SEGMENT_TICK)
    }

    @Test
    fun `Tab button performs no haptic when disabled`() {
        val view = setTabButtons(initialTabIndex = 0, hapticFeedbackEnabled = false)

        composeTestRule.onNodeWithTag("t1").performClick()

        assertThat(view.lastHaptic()).isEqualTo(noHaptic)
    }

    @Config(sdk = [33])
    @Test
    fun `Tab button performs no haptic below API 34`() {
        val view = setTabButtons(initialTabIndex = 0)

        composeTestRule.onNodeWithTag("t1").performClick()

        assertThat(view.lastHaptic()).isEqualTo(noHaptic)
    }

    @Test
    fun `Toggle performs toggle on when switching on`() {
        val view = setToggle(initialTabIndex = 0)

        composeTestRule.onNode(isToggleable()).performClick()

        assertThat(view.lastHaptic()).isEqualTo(HapticFeedbackConstants.TOGGLE_ON)
    }

    @Test
    fun `Toggle performs toggle off when switching off`() {
        val view = setToggle(initialTabIndex = 1)

        composeTestRule.onNode(isToggleable()).performClick()

        assertThat(view.lastHaptic()).isEqualTo(HapticFeedbackConstants.TOGGLE_OFF)
    }

    @Test
    fun `Toggle performs no haptic when disabled`() {
        val view = setToggle(initialTabIndex = 0, hapticFeedbackEnabled = false)

        composeTestRule.onNode(isToggleable()).performClick()

        assertThat(view.lastHaptic()).isEqualTo(noHaptic)
    }

    @Config(sdk = [33])
    @Test
    fun `Toggle performs no haptic below API 34`() {
        val view = setToggle(initialTabIndex = 0)

        composeTestRule.onNode(isToggleable()).performClick()

        assertThat(view.lastHaptic()).isEqualTo(noHaptic)
    }

    private fun setTabButtons(
        initialTabIndex: Int,
        tabIdsOrdered: List<String> = listOf("t0", "t1"),
        hapticFeedbackEnabled: Boolean = true,
    ): View = composeTestRule.setContentCapturingView {
        val state = previewEmptyState(initialSelectedTabIndex = initialTabIndex)
        Column {
            listOf("t0", "t1").forEachIndexed { index, tabId ->
                TabControlButtonView(
                    style = TabControlButtonComponentStyle(
                        tabIndex = index,
                        tabId = tabId,
                        stack = previewStackComponentStyle(
                            children = emptyList(),
                            size = Size(width = Fixed(100u), height = Fixed(100u)),
                        ),
                        tabIdsOrdered = tabIdsOrdered,
                        hapticFeedbackEnabled = hapticFeedbackEnabled,
                    ),
                    state = state,
                    modifier = Modifier.testTag(tabId),
                )
            }
        }
    }

    private fun setToggle(
        initialTabIndex: Int,
        hapticFeedbackEnabled: Boolean = true,
    ): View = composeTestRule.setContentCapturingView {
        val color = ColorStyles(light = ColorStyle.Solid(Color.Red))
        TabControlToggleView(
            style = TabControlToggleComponentStyle(
                thumbColorOn = color,
                thumbColorOff = color,
                trackColorOn = color,
                trackColorOff = color,
                hapticFeedbackEnabled = hapticFeedbackEnabled,
            ),
            state = previewEmptyState(initialSelectedTabIndex = initialTabIndex),
        )
    }

    private fun ComposeContentTestRule.setContentCapturingView(
        content: @Composable () -> Unit,
    ): View {
        lateinit var view: View
        setContent {
            view = LocalView.current
            content()
        }
        waitForIdle()
        return view
    }

    private fun View.lastHaptic(): Int = shadowOf(this).lastHapticFeedbackPerformed()
}
