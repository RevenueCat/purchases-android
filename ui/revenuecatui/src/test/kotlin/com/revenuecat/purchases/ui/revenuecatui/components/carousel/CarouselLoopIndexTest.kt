package com.revenuecat.purchases.ui.revenuecatui.components.carousel

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/** Ring index math for a looping carousel. See `CarouselComponentView`. */
internal class CarouselLoopIndexTest {

    @Test
    fun `only a multi-page looping carousel gets clones`() {
        assertThat(loopClonePad(loop = true, pageCount = 2)).isGreaterThan(0)
        assertThat(loopClonePad(loop = false, pageCount = 2)).isZero()
        // One page: nothing to loop between.
        assertThat(loopClonePad(loop = true, pageCount = 1)).isZero()
        assertThat(loopClonePad(loop = true, pageCount = 0)).isZero()
    }

    @Test
    fun `the clone-padded ring maps onto the logical pages`() {
        // Ring 0..6, real zone 2..4. Leading clones mirror the web SDK's prevPages = [last - 1,
        // last] and trailing clones its nextPages = [0, 1]. Indices below the pad must wrap rather
        // than go negative, so this fails if `%` replaces `mod`.
        assertThat((0..6).map { carouselLogicalPage(it, clonePad = 2, pageCount = 3) })
            .containsExactly(1, 2, 0, 1, 2, 0, 1)
        // Two pages is the reported repro, and the pad then exceeds the page count.
        assertThat((0..5).map { carouselLogicalPage(it, clonePad = 2, pageCount = 2) })
            .containsExactly(0, 1, 0, 1, 0, 1)
    }

    @Test
    fun `an empty carousel has no page to map to`() {
        // A page control reads the logical page before anything guards on pages being non-empty,
        // and `mod(0)` would throw.
        assertThat(carouselLogicalPage(pagerIndex = 0, clonePad = 0, pageCount = 0)).isZero()
    }

    @Test
    fun `without clones the pager index is the logical page`() {
        assertThat((0..2).map { carouselLogicalPage(it, clonePad = 0, pageCount = 3) })
            .containsExactly(0, 1, 2)
    }

    @Test
    fun `settling on a clone re-centres by one full page count`() {
        assertThat((0..6).map { carouselRecenterTarget(it, clonePad = 2, pageCount = 3) })
            .containsExactly(3, 4, null, null, null, 2, 3)
    }

    @Test
    fun `a re-centre target holds the same page as the clone it replaces`() {
        for (pagerIndex in 0..6) {
            val target = carouselRecenterTarget(pagerIndex, clonePad = 2, pageCount = 3) ?: continue
            assertThat(carouselLogicalPage(target, clonePad = 2, pageCount = 3))
                .isEqualTo(carouselLogicalPage(pagerIndex, clonePad = 2, pageCount = 3))
            assertThat(target).isBetween(2, 4)
        }
    }

    @Test
    fun `a carousel without clones never re-centres`() {
        assertThat((0..2).map { carouselRecenterTarget(it, clonePad = 0, pageCount = 3) })
            .containsOnlyNulls()
    }
}
