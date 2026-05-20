package com.revenuecat.purchases.ui.revenuecatui.data

import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InputSingleChoiceStateTests {

    @Test
    fun `selectedOptionIdsByFieldId is empty on initial state`() {
        val state = FakePaywallState()
        assertThat(state.selectedOptionIdsByFieldId).isEmpty()
    }

    @Test
    fun `update sets the selected option for a field`() {
        val state = FakePaywallState()
        state.update(fieldId = "plan_type", selectedOptionId = "monthly")
        assertThat(state.selectedOptionIdsByFieldId["plan_type"]).isEqualTo("monthly")
    }

    @Test
    fun `update replaces the selected option for the same field`() {
        val state = FakePaywallState()
        state.update(fieldId = "plan_type", selectedOptionId = "monthly")
        state.update(fieldId = "plan_type", selectedOptionId = "annual")
        assertThat(state.selectedOptionIdsByFieldId["plan_type"]).isEqualTo("annual")
    }

    @Test
    fun `update with null clears the selected option for a field`() {
        val state = FakePaywallState()
        state.update(fieldId = "plan_type", selectedOptionId = "monthly")
        state.update(fieldId = "plan_type", selectedOptionId = null)
        assertThat(state.selectedOptionIdsByFieldId["plan_type"]).isNull()
    }

    @Test
    fun `updating one field does not affect another field`() {
        val state = FakePaywallState()
        state.update(fieldId = "plan_type", selectedOptionId = "monthly")
        state.update(fieldId = "survey_q1", selectedOptionId = "yes")
        assertThat(state.selectedOptionIdsByFieldId["plan_type"]).isEqualTo("monthly")
        assertThat(state.selectedOptionIdsByFieldId["survey_q1"]).isEqualTo("yes")
    }

    @Test
    fun `clearing one field does not affect another field`() {
        val state = FakePaywallState()
        state.update(fieldId = "plan_type", selectedOptionId = "monthly")
        state.update(fieldId = "survey_q1", selectedOptionId = "yes")
        state.update(fieldId = "plan_type", selectedOptionId = null)
        assertThat(state.selectedOptionIdsByFieldId["plan_type"]).isNull()
        assertThat(state.selectedOptionIdsByFieldId["survey_q1"]).isEqualTo("yes")
    }
}
