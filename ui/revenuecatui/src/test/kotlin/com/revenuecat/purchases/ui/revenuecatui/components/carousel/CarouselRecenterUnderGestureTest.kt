package com.revenuecat.purchases.ui.revenuecatui.components.carousel

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.awaitCancellation
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Regression test for the Bugbot finding on purchases-android#3911: a re-centre attempted while
 * a gesture holds the pager's scroll mutex at `UserInput` priority must still land.
 */
@Config(sdk = [26])
@RunWith(AndroidJUnit4::class)
class CarouselRecenterUnderGestureTest {

    private companion object {
        const val CLONE_PAD = 2
        const val PAGE_COUNT = 2
        const val RING_COUNT = PAGE_COUNT + 2 * CLONE_PAD
        // Ring 0..5, real zone 2..3. Index 5 is the trailing clone of logical page 0.
        const val TRAILING_CLONE = RING_COUNT - 1
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `a recentre started while a gesture holds the scroll mutex still lands`() {
        var holdMutexWithGesture by mutableStateOf(false)
        var recenterEnabled by mutableStateOf(false)
        lateinit var pagerState: PagerState

        composeTestRule.setContent {
            pagerState = rememberPagerState(initialPage = TRAILING_CLONE) { RING_COUNT }

            if (holdMutexWithGesture) {
                LaunchedEffect(pagerState) {
                    // Mirrors a drag: same priority, holds the mutex until cancelled.
                    pagerState.scroll(MutatePriority.UserInput) { awaitCancellation() }
                }
            }
            if (recenterEnabled) {
                RecenterLoopClones(pagerState = pagerState, clonePad = CLONE_PAD, pageCount = PAGE_COUNT)
            }

            Box(Modifier.fillMaxSize()) {
                HorizontalPager(state = pagerState) { }
            }
        }
        composeTestRule.waitForIdle()
        assertThat(pagerState.currentPage).isEqualTo(TRAILING_CLONE)

        holdMutexWithGesture = true
        composeTestRule.waitForIdle()
        assertThat(pagerState.isScrollInProgress)
            .describedAs("the gesture must hold the scroll mutex before the re-centre is attempted")
            .isTrue()

        recenterEnabled = true
        composeTestRule.waitForIdle()

        assertThat(pagerState.currentPage)
            .describedAs("requestScrollToPage bypasses the scroll mutex, so it lands even mid-gesture")
            .isEqualTo(carouselRecenterTarget(TRAILING_CLONE, CLONE_PAD, PAGE_COUNT))
        assertThat(pagerState.isScrollInProgress)
            .describedAs("the re-centre must not have cancelled the gesture's hold on the mutex")
            .isTrue()
    }
}
