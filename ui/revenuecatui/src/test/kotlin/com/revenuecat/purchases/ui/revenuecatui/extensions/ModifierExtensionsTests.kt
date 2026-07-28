package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModifierExtensionsTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `tracks an infinite maximum height with a non-zero minimum as unbounded`() {
        lateinit var unboundedState: MutableState<Boolean>

        composeTestRule.setContent {
            unboundedState = remember { mutableStateOf(false) }
            Layout(
                content = {
                    Box(
                        modifier = Modifier.trackMainAxisUnbounded(
                            isHorizontal = false,
                            unboundedState = unboundedState,
                            includeNonZeroMinimum = true,
                        ),
                    )
                },
            ) { measurables, _ ->
                val placeable = measurables.single().measure(
                    Constraints(
                        minHeight = 100,
                        maxHeight = Constraints.Infinity,
                    ),
                )
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }
        }

        composeTestRule.runOnIdle {
            assertThat(unboundedState.value).isTrue()
        }
    }
}
