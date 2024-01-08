package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntSizeExtensionsTest {
    @Test
    fun `aspectRatio with square screen`() {
        assertThat(IntSize(100, 100).aspectRatio).isEqualTo(1.0f)
    }

    @Test
    fun `aspectRatio with portrait screen`() {
        assertThat(IntSize(100, 300).aspectRatio)
            .isCloseTo(0.333f, Offset.offset(0.001f))
    }

    @Test
    fun `aspectRatio with landscape screen`() {
        assertThat(IntSize(300, 100).aspectRatio).isEqualTo(3.0f)
    }

    @Test
    fun `isLandscape with square screen`() {
        assertThat(IntSize(100, 100).isLandscape).isFalse()
    }

    @Test
    fun `isLandscape with portrait screen`() {
        assertThat(IntSize(100, 300).isLandscape).isFalse()
    }

    @Test
    fun `isLandscape with landscape screen`() {
        assertThat(IntSize(300, 100).isLandscape).isTrue()
    }
}