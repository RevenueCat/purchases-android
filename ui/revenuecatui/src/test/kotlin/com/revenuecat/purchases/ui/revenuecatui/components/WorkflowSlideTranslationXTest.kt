package com.revenuecat.purchases.ui.revenuecatui.components

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class WorkflowSlideTranslationXTest {

    private val to = "to"
    private val from = "from"

    @Test
    fun `incoming and outgoing surfaces stay exactly adjacent so there is no sub-pixel gap`() {
        val width = 1001f
        val directionFactor = 1f
        listOf(0f, 0.2f, 0.333f, 0.5f, 0.6665f, 0.9f, 1f).forEach { progress ->
            val toTx = workflowSlideTranslationX(to, to, from, progress, width, directionFactor)
            val fromTx = workflowSlideTranslationX(from, to, from, progress, width, directionFactor)
            // Outgoing sits exactly one width behind incoming in the slide direction: edges coincide.
            assertThat(toTx - fromTx).isEqualTo(directionFactor * width)
        }
    }

    @Test
    fun `both surfaces are snapped to whole pixels`() {
        val width = 1001f
        val directionFactor = 1f
        val progress = 0.3335f // (1 - p) * width = 0.6665 * 1001 = 667.1665 -> rounds to 667
        val toTx = workflowSlideTranslationX(to, to, from, progress, width, directionFactor)
        val fromTx = workflowSlideTranslationX(from, to, from, progress, width, directionFactor)
        assertThat(toTx).isEqualTo(667f)
        assertThat(fromTx).isEqualTo(667f - 1001f)
        assertThat(toTx % 1f).isEqualTo(0f)
        assertThat(fromTx % 1f).isEqualTo(0f)
    }

    @Test
    fun `at progress 0 incoming is offscreen by one width and outgoing is centered`() {
        val width = 1080f
        val directionFactor = 1f
        assertThat(workflowSlideTranslationX(to, to, from, 0f, width, directionFactor)).isEqualTo(1080f)
        assertThat(workflowSlideTranslationX(from, to, from, 0f, width, directionFactor)).isEqualTo(0f)
    }

    @Test
    fun `at progress 1 incoming is centered and outgoing is offscreen`() {
        val width = 1080f
        val directionFactor = 1f
        assertThat(workflowSlideTranslationX(to, to, from, 1f, width, directionFactor)).isEqualTo(0f)
        assertThat(workflowSlideTranslationX(from, to, from, 1f, width, directionFactor)).isEqualTo(-1080f)
    }

    @Test
    fun `backward direction keeps surfaces adjacent`() {
        val width = 1080f
        val directionFactor = -1f
        val progress = 0.4f
        val toTx = workflowSlideTranslationX(to, to, from, progress, width, directionFactor)
        val fromTx = workflowSlideTranslationX(from, to, from, progress, width, directionFactor)
        assertThat(toTx - fromTx).isEqualTo(directionFactor * width)
    }

    @Test
    fun `parked step has no offset`() {
        assertThat(workflowSlideTranslationX("other", to, from, 0.5f, 1080f, 1f)).isEqualTo(0f)
    }
}
