package com.revenuecat.purchases.ui.revenuecatui.components.webview

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class DragGestureArbitrationTest {

    private val slop = 8

    @Suppress("LongParameterList")
    private fun shouldOwn(
        dx: Float,
        dy: Float,
        webContentWantsGesture: Boolean? = null,
        canScrollUp: Boolean = false,
        canScrollDown: Boolean = false,
        canScrollLeft: Boolean = false,
        canScrollRight: Boolean = false,
    ) = shouldWebViewOwnGesture(
        totalDx = dx,
        totalDy = dy,
        touchSlop = slop,
        webContentWantsGesture = webContentWantsGesture,
        // direction > 0 = towards the end (right/bottom), < 0 = towards the start (left/top)
        canScrollHorizontally = { direction -> if (direction > 0) canScrollRight else canScrollLeft },
        canScrollVertically = { direction -> if (direction > 0) canScrollDown else canScrollUp },
    )

    @Test
    fun `content verdict to own claims immediately, even within touch slop`() {
        assertThat(shouldOwn(dx = 0f, dy = 1f, webContentWantsGesture = true)).isTrue()
    }

    @Test
    fun `content that wants the gesture owns it regardless of native scrollability`() {
        assertThat(shouldOwn(dx = 0f, dy = -50f, webContentWantsGesture = true, canScrollDown = false)).isTrue()
    }

    @Test
    fun `a release verdict still yields to native root scroll when the page can scroll`() {
        assertThat(shouldOwn(dx = 0f, dy = -50f, webContentWantsGesture = false, canScrollDown = true)).isTrue()
    }

    @Test
    fun `a release verdict with nothing to scroll hands off to the paywall`() {
        assertThat(shouldOwn(dx = 0f, dy = -50f, webContentWantsGesture = false, canScrollDown = false)).isFalse()
    }

    @Test
    fun `no verdict yet - native root scroll owns while it can scroll the dragged direction`() {
        assertThat(shouldOwn(dx = 0f, dy = -50f, webContentWantsGesture = null, canScrollDown = true)).isTrue()
    }

    @Test
    fun `no verdict yet - hands off to the paywall when the root cannot scroll`() {
        assertThat(shouldOwn(dx = 0f, dy = -50f, webContentWantsGesture = null, canScrollDown = false)).isFalse()
    }

    @Test
    fun `movement within touch slop does not claim without an own verdict`() {
        assertThat(shouldOwn(dx = 7f, dy = -7f, canScrollUp = true)).isFalse()
    }

    @Test
    fun `dragging up at the bottom edge hands off to the paywall`() {
        assertThat(shouldOwn(dx = 0f, dy = -50f, canScrollDown = false)).isFalse()
    }

    @Test
    fun `dragging down while the root can scroll up is owned by the web view`() {
        assertThat(shouldOwn(dx = 0f, dy = 50f, canScrollUp = true)).isTrue()
    }

    @Test
    fun `horizontal-dominant drag uses horizontal scrollability`() {
        assertThat(shouldOwn(dx = -50f, dy = 10f, canScrollRight = true)).isTrue()
        assertThat(shouldOwn(dx = -50f, dy = 10f, canScrollRight = false)).isFalse()
    }

    @Test
    fun `vertical-dominant diagonal drag uses vertical scrollability`() {
        assertThat(shouldOwn(dx = 30f, dy = -50f, canScrollDown = true, canScrollLeft = true)).isTrue()
        assertThat(shouldOwn(dx = 30f, dy = -50f, canScrollDown = false, canScrollLeft = true)).isFalse()
    }
}
