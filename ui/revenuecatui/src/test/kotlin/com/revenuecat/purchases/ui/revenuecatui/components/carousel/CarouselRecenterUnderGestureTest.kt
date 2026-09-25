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

@Config(sdk = [26])
@RunWith(AndroidJUnit4::class)
class CarouselRecenterUnderGestureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `a recentre started while a gesture holds the scroll mutex still lands`() {
        val pageCount = 2
        // Any non-zero pad exercises this; where it comes from is CarouselLoopIndexTest's business.
        val clonePad = 2
        val ringCount = pageCount + 2 * clonePad
        val trailingClone = ringCount - 1

        var holdMutexWithGesture by mutableStateOf(false)
        var recenterEnabled by mutableStateOf(false)
        lateinit var pagerState: PagerState

        composeTestRule.setContent {
            pagerState = rememberPagerState(initialPage = trailingClone) { ringCount }

            if (holdMutexWithGesture) {
                LaunchedEffect(pagerState) {
                    pagerState.scroll(MutatePriority.UserInput) { awaitCancellation() }
                }
            }
            if (recenterEnabled) {
                RecenterLoopClones(pagerState = pagerState, clonePad = clonePad, pageCount = pageCount)
            }

            Box(Modifier.fillMaxSize()) {
                HorizontalPager(state = pagerState) { }
            }
        }
        composeTestRule.waitForIdle()

        holdMutexWithGesture = true
        composeTestRule.waitForIdle()
        assertThat(pagerState.isScrollInProgress)
            .describedAs("the gesture must hold the scroll mutex before the re-centre is attempted")
            .isTrue()

        recenterEnabled = true
        composeTestRule.waitForIdle()

        assertThat(pagerState.currentPage)
            .describedAs("requestScrollToPage bypasses the scroll mutex, so it lands even mid-gesture")
            .isEqualTo(carouselRecenterTarget(trailingClone, clonePad, pageCount))
        assertThat(pagerState.isScrollInProgress)
            .describedAs("the re-centre must not have cancelled the gesture's hold on the mutex")
            .isTrue()
    }
}
