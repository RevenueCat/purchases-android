package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SimpleBottomSheetScaffoldTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Regression test for a bug where opening a different sheet before the previous one finished its
     * exit animation reused the previous sheet's content (identity was positional), so e.g. a video
     * from the first sheet kept playing in the second. Keying the sheet content by its identity makes
     * switching sheets dispose the previous content and compose the new one from scratch.
     */
    @Test
    fun `showing a different sheet disposes the previous sheet content`(): Unit = with(composeTestRule) {
        val sheetState = SimpleSheetState()
        val events = mutableListOf<String>()

        // A single content call site whose subtree is tracked by id. Both sheets funnel through the
        // same call site (as they do in production), so only content keying can reset their identity.
        fun trackingContent(id: String): @Composable () -> Unit = {
            DisposableEffect(Unit) {
                events += "compose:$id"
                onDispose { events += "dispose:$id" }
            }
        }

        setContent {
            SimpleBottomSheetScaffold(
                sheetState = sheetState,
                modifier = Modifier.fillMaxSize(),
                content = { Box(Modifier.fillMaxSize()) },
            )
        }

        runOnUiThread {
            sheetState.show(backgroundBlur = false, content = trackingContent("A"), contentKey = "A")
        }
        waitForIdle()

        runOnUiThread {
            sheetState.show(backgroundBlur = false, content = trackingContent("B"), contentKey = "B")
        }
        waitForIdle()

        assertThat(events).containsExactlyInAnyOrder("compose:A", "dispose:A", "compose:B")
    }
}
