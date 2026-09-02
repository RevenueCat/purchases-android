package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(sdk = [26])
@RunWith(AndroidJUnit4::class)
class SizeModifierTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `fit respects minimum width and height`() {
        val measured = measure(
            size = Size(
                width = Fit(min = 20u),
                height = Fit(min = 30u),
            ),
            contentWidth = 10,
            contentHeight = 10,
        )

        assertThat(measured).isEqualTo(IntSize(width = 20, height = 30))
    }

    @Test
    fun `fit respects maximum width and height`() {
        val measured = measure(
            size = Size(
                width = Fit(max = 20u),
                height = Fit(max = 30u),
            ),
            contentWidth = 80,
            contentHeight = 90,
        )

        assertThat(measured).isEqualTo(IntSize(width = 20, height = 30))
    }

    @Test
    fun `fill respects maximum width`() {
        val measured = measure(
            size = Size(
                width = Fill(max = 20u),
                height = Fixed(10u),
            ),
        )

        assertThat(measured.width).isEqualTo(20)
    }

    @Test
    fun `fill minimum can exceed parent width`() {
        val measured = measure(
            size = Size(
                width = Fill(min = 120u),
                height = Fixed(10u),
            ),
        )

        assertThat(measured.width).isEqualTo(120)
    }

    @Test
    fun `minimum takes precedence over maximum`() {
        val measured = measure(
            size = Size(
                width = Fill(min = 40u, max = 20u),
                height = Fixed(10u),
            ),
        )

        assertThat(measured.width).isEqualTo(40)
    }

    private fun measure(
        size: Size,
        contentWidth: Int = 10,
        contentHeight: Int = 10,
    ): IntSize {
        composeTestRule.setContent {
            Box(Modifier.requiredSize(100.dp)) {
                Box(
                    modifier = Modifier
                        .size(size)
                        .testTag(SUBJECT_TAG)
                        .width(contentWidth.dp)
                        .height(contentHeight.dp),
                )
            }
        }

        composeTestRule.waitForIdle()
        return composeTestRule.onNodeWithTag(SUBJECT_TAG).fetchSemanticsNode().size
    }

    private companion object {
        const val SUBJECT_TAG = "subject"
    }
}
