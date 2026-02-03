package com.revenuecat.purchases.ui.revenuecatui.components.video

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class IsVisibleInWindowTests(
    private val testCase: TestCase,
) {
    data class TestCase(
        val description: String,
        val componentX: Float,
        val componentY: Float,
        val componentWidth: Int,
        val componentHeight: Int,
        val windowWidth: Int,
        val windowHeight: Int,
        val expected: Boolean,
    )

    companion object {
        private const val WINDOW_WIDTH = 1080
        private const val WINDOW_HEIGHT = 1920

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<TestCase> = listOf(
            TestCase(
                description = "Fully visible component in center",
                componentX = 100f,
                componentY = 100f,
                componentWidth = 200,
                componentHeight = 200,
                windowWidth = WINDOW_WIDTH,
                windowHeight = WINDOW_HEIGHT,
                expected = true,
            ),
            TestCase(
                description = "Component at origin",
                componentX = 0f,
                componentY = 0f,
                componentWidth = 100,
                componentHeight = 100,
                windowWidth = WINDOW_WIDTH,
                windowHeight = WINDOW_HEIGHT,
                expected = true,
            ),
            TestCase(
                description = "Component partially visible on left edge",
                componentX = -50f,
                componentY = 100f,
                componentWidth = 100,
                componentHeight = 100,
                windowWidth = WINDOW_WIDTH,
                windowHeight = WINDOW_HEIGHT,
                expected = true,
            ),
            TestCase(
                description = "Component partially visible on right edge",
                componentX = (WINDOW_WIDTH - 50).toFloat(),
                componentY = 100f,
                componentWidth = 100,
                componentHeight = 100,
                windowWidth = WINDOW_WIDTH,
                windowHeight = WINDOW_HEIGHT,
                expected = true,
            ),
            TestCase(
                description = "Component partially visible on top edge",
                componentX = 100f,
                componentY = -50f,
                componentWidth = 100,
                componentHeight = 100,
                windowWidth = WINDOW_WIDTH,
                windowHeight = WINDOW_HEIGHT,
                expected = true,
            ),
            TestCase(
                description = "Component partially visible on bottom edge",
                componentX = 100f,
                componentY = (WINDOW_HEIGHT - 50).toFloat(),
                componentWidth = 100,
                componentHeight = 100,
                windowWidth = WINDOW_WIDTH,
                windowHeight = WINDOW_HEIGHT,
                expected = true,
            ),
            TestCase(
                description = "Component completely off-screen to the left",
                componentX = -200f,
                componentY = 100f,
                componentWidth = 100,
                componentHeight = 100,
                windowWidth = WINDOW_WIDTH,
                windowHeight = WINDOW_HEIGHT,
                expected = false,
            ),
            TestCase(
                description = "Component completely off-screen to the right",
                componentX = (WINDOW_WIDTH + 100).toFloat(),
                componentY = 100f,
                componentWidth = 100,
                componentHeight = 100,
                windowWidth = WINDOW_WIDTH,
                windowHeight = WINDOW_HEIGHT,
                expected = false,
            ),
            TestCase(
                description = "Component completely off-screen above",
                componentX = 100f,
                componentY = -200f,
                componentWidth = 100,
                componentHeight = 100,
                windowWidth = WINDOW_WIDTH,
                windowHeight = WINDOW_HEIGHT,
                expected = false,
            ),
            TestCase(
                description = "Component completely off-screen below",
                componentX = 100f,
                componentY = (WINDOW_HEIGHT + 100).toFloat(),
                componentWidth = 100,
                componentHeight = 100,
                windowWidth = WINDOW_WIDTH,
                windowHeight = WINDOW_HEIGHT,
                expected = false,
            ),
            TestCase(
                description = "Component just touching left edge (1px visible)",
                componentX = -99f,
                componentY = 100f,
                componentWidth = 100,
                componentHeight = 100,
                windowWidth = WINDOW_WIDTH,
                windowHeight = WINDOW_HEIGHT,
                expected = true,
            ),
            TestCase(
                description = "Component just off left edge (0px visible)",
                componentX = -100f,
                componentY = 100f,
                componentWidth = 100,
                componentHeight = 100,
                windowWidth = WINDOW_WIDTH,
                windowHeight = WINDOW_HEIGHT,
                expected = false,
            ),
            TestCase(
                description = "Component just touching right edge (1px visible)",
                componentX = (WINDOW_WIDTH - 1).toFloat(),
                componentY = 100f,
                componentWidth = 100,
                componentHeight = 100,
                windowWidth = WINDOW_WIDTH,
                windowHeight = WINDOW_HEIGHT,
                expected = true,
            ),
            TestCase(
                description = "Component just off right edge (0px visible)",
                componentX = WINDOW_WIDTH.toFloat(),
                componentY = 100f,
                componentWidth = 100,
                componentHeight = 100,
                windowWidth = WINDOW_WIDTH,
                windowHeight = WINDOW_HEIGHT,
                expected = false,
            ),
            TestCase(
                description = "Large component covering entire window",
                componentX = -100f,
                componentY = -100f,
                componentWidth = WINDOW_WIDTH + 200,
                componentHeight = WINDOW_HEIGHT + 200,
                windowWidth = WINDOW_WIDTH,
                windowHeight = WINDOW_HEIGHT,
                expected = true,
            ),
        )
    }

    @Test
    fun `isVisibleInWindow returns correct result`() {
        val result = isVisibleInWindow(
            componentX = testCase.componentX,
            componentY = testCase.componentY,
            componentWidth = testCase.componentWidth,
            componentHeight = testCase.componentHeight,
            windowWidth = testCase.windowWidth,
            windowHeight = testCase.windowHeight,
        )
        assertThat(result)
            .describedAs(testCase.description)
            .isEqualTo(testCase.expected)
    }
}

