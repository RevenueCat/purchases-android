package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.down
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModalSheetPresentationTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val contentText = "Sheet content"
    private val scrollableTag = "scrollable"

    @Test
    fun `the sheet slides in and settles below its top margin`(): Unit = with(composeTestRule) {
        val state = ModalSheetState()
        setSheet(state)

        mainClock.advanceTimeBy(ANIMATION_SLACK_MILLIS)
        waitForIdle()

        onNodeWithText(contentText).assertIsDisplayed()
        assertThat(state.hiddenFraction).isEqualTo(0f)
        val contentTop = onNodeWithText(contentText).getUnclippedBoundsInRoot().top
        assertThat(contentTop.value).isGreaterThanOrEqualTo(16.dp.value)
    }

    @Test
    fun `tapping the scrim requests a dismissal`(): Unit = with(composeTestRule) {
        val state = ModalSheetState()
        var dismissRequests = 0
        setSheet(state, onDismissRequest = { dismissRequests++ })
        mainClock.advanceTimeBy(ANIMATION_SLACK_MILLIS)
        waitForIdle()

        // The sheet covers the scrim except for the top margin, so the tap has to land there.
        onNodeWithTag(MODAL_SHEET_SCRIM_TEST_TAG).performTouchInput { click(Offset(centerX, 1f)) }

        assertThat(dismissRequests).isEqualTo(1)
    }

    @Test
    fun `tapping the sheet does not request a dismissal`(): Unit = with(composeTestRule) {
        val state = ModalSheetState()
        var dismissRequests = 0
        setSheet(state, onDismissRequest = { dismissRequests++ })
        mainClock.advanceTimeBy(ANIMATION_SLACK_MILLIS)
        waitForIdle()

        onNodeWithText(contentText).performClick()

        assertThat(dismissRequests).isEqualTo(0)
    }

    @Test
    fun `swiping the sheet down requests a dismissal once it is off screen`(): Unit = with(composeTestRule) {
        val state = ModalSheetState()
        var dismissRequests = 0
        setSheet(state, onDismissRequest = { dismissRequests++ })
        mainClock.advanceTimeBy(ANIMATION_SLACK_MILLIS)
        waitForIdle()

        onNodeWithText(contentText).performTouchInput { swipeDown() }
        mainClock.advanceTimeBy(ANIMATION_SLACK_MILLIS)
        waitForIdle()

        assertThat(dismissRequests).isEqualTo(1)
        assertThat(state.hiddenFraction).isEqualTo(1f)
    }

    @Test
    fun `swiping scrollable content at its top down requests a dismissal once`(): Unit = with(composeTestRule) {
        val state = ModalSheetState()
        var dismissRequests = 0
        setSheet(state, onDismissRequest = { dismissRequests++ }) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag(scrollableTag)) {
                Text(contentText)
                Box(Modifier.height(4000.dp))
            }
        }
        mainClock.advanceTimeBy(ANIMATION_SLACK_MILLIS)
        waitForIdle()

        onNodeWithTag(scrollableTag).performTouchInput { swipeDown() }
        mainClock.advanceTimeBy(ANIMATION_SLACK_MILLIS)
        waitForIdle()

        assertThat(dismissRequests).isEqualTo(1)
        assertThat(state.hiddenFraction).isEqualTo(1f)
    }

    @Test
    fun `hiding runs its callback only once the sheet is off screen`(): Unit = with(composeTestRule) {
        val state = ModalSheetState()
        setSheet(state)
        mainClock.advanceTimeBy(ANIMATION_SLACK_MILLIS)
        waitForIdle()
        var hidden = false

        runOnUiThread { state.hide { hidden = true } }
        // The state change reaches the recomposer through the main looper, before any frame is advanced.
        waitForIdle()
        mainClock.advanceTimeBy(ANIMATION_SLACK_MILLIS / 4)
        assertThat(hidden).isFalse
        assertThat(state.hiddenFraction).isLessThan(1f)

        mainClock.advanceTimeBy(ANIMATION_SLACK_MILLIS)
        waitForIdle()
        assertThat(hidden).isTrue
        assertThat(state.hiddenFraction).isEqualTo(1f)
    }

    @Test
    fun `releasing the sheet during a programmatic hide does not bring it back`(): Unit = with(composeTestRule) {
        val state = ModalSheetState()
        var dismissRequests = 0
        setSheet(state, onDismissRequest = { dismissRequests++ })
        mainClock.advanceTimeBy(ANIMATION_SLACK_MILLIS)
        waitForIdle()
        var hidden = false
        onNodeWithText(contentText).performTouchInput {
            down(center)
            moveBy(Offset(0f, 100f))
        }
        assertThat(state.hiddenFraction).isGreaterThan(0f)

        runOnUiThread { state.hide { hidden = true } }
        waitForIdle()
        mainClock.advanceTimeBy(ANIMATION_SLACK_MILLIS / 4)
        onNodeWithText(contentText).performTouchInput { up() }
        mainClock.advanceTimeBy(ANIMATION_SLACK_MILLIS)
        waitForIdle()

        assertThat(hidden).isTrue
        assertThat(state.hiddenFraction).isEqualTo(1f)
        assertThat(dismissRequests).isEqualTo(0)
    }

    @Test
    fun `hiding a sheet that is not composed runs its callback right away`() {
        val state = ModalSheetState()
        var hidden = false

        state.hide { hidden = true }

        assertThat(hidden).isTrue
    }

    private fun setSheet(
        state: ModalSheetState,
        onDismissRequest: () -> Unit = {},
        content: @Composable () -> Unit = { Box(Modifier.fillMaxSize()) { Text(contentText) } },
    ) = with(composeTestRule) {
        mainClock.autoAdvance = false
        setContent {
            ModalSheetPresentation(state = state, onDismissRequest = onDismissRequest, content = content)
        }
        onRoot().assertExists()
    }

    private companion object {
        // Longer than the sheet's slide, so a full advance always lands past it.
        const val ANIMATION_SLACK_MILLIS = 400L
    }
}
