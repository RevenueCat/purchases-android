package com.revenuecat.purchases.ui.revenuecatui.components

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.Test

internal class WorkflowHeaderAlphaTest {

    @Test
    fun `entering fades from 0 to 1 with progress`() {
        assertThat(headerAlpha(WorkflowHeaderTransitionRole.ENTERING, 0f)).isEqualTo(0f)
        assertThat(headerAlpha(WorkflowHeaderTransitionRole.ENTERING, 0.5f)).isEqualTo(0.5f, within(0.0001f))
        assertThat(headerAlpha(WorkflowHeaderTransitionRole.ENTERING, 1f)).isEqualTo(1f)
    }

    @Test
    fun `leaving fades from 1 to 0 with progress`() {
        assertThat(headerAlpha(WorkflowHeaderTransitionRole.LEAVING, 0f)).isEqualTo(1f)
        assertThat(headerAlpha(WorkflowHeaderTransitionRole.LEAVING, 0.5f)).isEqualTo(0.5f, within(0.0001f))
        assertThat(headerAlpha(WorkflowHeaderTransitionRole.LEAVING, 1f)).isEqualTo(0f)
    }

    @Test
    fun `stable is always fully opaque`() {
        assertThat(headerAlpha(WorkflowHeaderTransitionRole.STABLE, 0f)).isEqualTo(1f)
        assertThat(headerAlpha(WorkflowHeaderTransitionRole.STABLE, 0.5f)).isEqualTo(1f)
        assertThat(headerAlpha(WorkflowHeaderTransitionRole.STABLE, 1f)).isEqualTo(1f)
    }
}
