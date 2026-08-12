package com.revenuecat.purchases.ui.revenuecatui.components.carousel

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/** Ring index math for a looping carousel. See `CarouselComponentView`. */
internal class CarouselLoopIndexTest {

    @Test
    fun `only a multi-page looping carousel gets clones`() {
        assertThat(clonePad(loop = true, pageCount = 2)).isGreaterThan(0)
        assertThat(clonePad(loop = false, pageCount = 2)).isZero()
        // One page: nothing to loop between.
        assertThat(clonePad(loop = true, pageCount = 1)).isZero()
        assertThat(clonePad(loop = true, pageCount = 0)).isZero()
    }

    @Test
    fun `an ordinary peek needs the minimum pad`() {
        // The reported repro: one neighbour visible per side.
        assertThat(clonePad(pagePeek = 28.dp, viewportWidth = 320.dp)).isEqualTo(2)
        assertThat(clonePad(pagePeek = 0.dp, viewportWidth = 320.dp)).isEqualTo(2)
        // The boundary. Peek, page and pitch are all a third of the viewport, so exactly one
        // neighbour fits per side: a 3-across carousel.
        assertThat(clonePad(pagePeek = 100.dp, viewportWidth = 300.dp)).isEqualTo(2)
    }

    @Test
    fun `a peek wider than a page needs a clone for every page it exposes`() {
        // Past the boundary the pager would run out of ring and render blank in the peek.
        assertThat(clonePad(pagePeek = 110.dp, viewportWidth = 300.dp)).isEqualTo(3)
        // 70dp pages behind a 165dp peek: three neighbours a side, all of them on screen anyway.
        assertThat(clonePad(pagePeek = 165.dp, viewportWidth = 400.dp)).isEqualTo(4)
    }

    @Test
    fun `page spacing counts towards the peek and the pitch`() {
        assertThat(clonePad(pagePeek = 90.dp, pageSpacing = 20.dp, viewportWidth = 300.dp)).isEqualTo(3)
    }

    @Test
    fun `the pad is capped, because every ring index stays composed`() {
        // A 2dp pitch would otherwise ask for 101 clones a side, so 200+ live pages.
        assertThat(clonePad(pagePeek = 199.dp, viewportWidth = 400.dp)).isEqualTo(8)
    }

    @Test
    fun `a peek leaving no room for a page falls back to the minimum`() {
        // Nothing renders sensibly at this point; the guard is here because the wire allows it.
        assertThat(clonePad(pagePeek = 150.dp, viewportWidth = 300.dp)).isEqualTo(2)
        assertThat(clonePad(viewportWidth = Dp.Infinity)).isEqualTo(2)
    }

    private fun clonePad(
        loop: Boolean = true,
        pageCount: Int = 3,
        viewportWidth: Dp = 320.dp,
        pagePeek: Dp = 28.dp,
        pageSpacing: Dp = 0.dp,
    ) = loopClonePad(
        loop = loop,
        pageCount = pageCount,
        viewportWidth = viewportWidth,
        contentPadding = pagePeek + pageSpacing,
        pageSpacing = pageSpacing,
    )

    @Test
    fun `the ring maps onto the logical pages`() {
        // Content is periodic across the whole ring, which is what makes every clone mirror the
        // page one page count away from it.
        assertThat((0..6).map { carouselLogicalPage(it, pageCount = 3) })
            .containsExactly(0, 1, 2, 0, 1, 2, 0)
        // Two pages is the reported repro, where the pad exceeds the page count.
        assertThat((0..5).map { carouselLogicalPage(it, pageCount = 2) })
            .containsExactly(0, 1, 0, 1, 0, 1)
    }

    @Test
    fun `an empty carousel has no page to map to`() {
        // A page control reads this before anything guards on `pages`, and `mod(0)` would throw.
        assertThat(carouselLogicalPage(pagerIndex = 0, pageCount = 0)).isZero()
        assertThat(carouselRealZoneIndex(pageIndex = 0, clonePad = 2, pageCount = 0)).isZero()
    }

    @Test
    fun `the real zone index holds the page it was asked for, whatever the pad`() {
        // The pad tracks the viewport, so it changes on a resize or a partial that moves page_peek.
        // A page must not move between indices when it does, or every page would be rebuilt.
        for (clonePad in 0..4) {
            for (pageIndex in 0..2) {
                val index = carouselRealZoneIndex(pageIndex, clonePad, pageCount = 3)
                assertThat(carouselLogicalPage(index, pageCount = 3)).isEqualTo(pageIndex)
                assertThat(index).isBetween(clonePad, clonePad + 2)
            }
        }
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
            assertThat(carouselLogicalPage(target, pageCount = 3))
                .isEqualTo(carouselLogicalPage(pagerIndex, pageCount = 3))
            assertThat(target).isBetween(2, 4)
        }
    }

    @Test
    fun `a re-centre lands in the real zone in one hop, even with more clones than pages`() {
        // A wide peek gives more clones per side than there are pages, so stepping by a single
        // page count is not enough to clear the clone zone.
        assertThat((0..7).map { carouselRecenterTarget(it, clonePad = 3, pageCount = 2) })
            .containsExactly(4, 3, 4, null, null, 3, 4, 3)
    }

    @Test
    fun `a carousel without clones never re-centres`() {
        assertThat((0..2).map { carouselRecenterTarget(it, clonePad = 0, pageCount = 3) })
            .containsOnlyNulls()
    }
}
