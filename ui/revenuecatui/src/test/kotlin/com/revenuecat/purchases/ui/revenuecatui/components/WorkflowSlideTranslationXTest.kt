package com.revenuecat.purchases.ui.revenuecatui.components

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class WorkflowSlideTranslationXTest {

    private val to = "to"
    private val from = "from"

    @Test
    fun `both surfaces are snapped to whole pixels`() {
        // progress = 0.3335 → (1 - p) * width = 0.6665 * 1001 = 667.1665, rounds to 667.
        // Outgoing is derived from incoming so they share a byte-identical edge.
        val width = 1001f
        val directionFactor = 1f
        val progress = 0.3335f
        val toTx = workflowSlideTranslationX(to, to, from, progress, width, directionFactor)
        val fromTx = workflowSlideTranslationX(from, to, from, progress, width, directionFactor)
        assertThat(toTx).isEqualTo(667f)
        assertThat(fromTx).isEqualTo(667f - 1001f)
        assertThat(toTx % 1f).isEqualTo(0f)
        assertThat(fromTx % 1f).isEqualTo(0f)
    }
}
