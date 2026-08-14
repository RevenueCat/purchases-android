package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.localrules.RulesDimensionValue
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CheckpointParamsTest {

    @Test
    fun `each custom variable maps to its rule dimension`() {
        assertThat(CustomVariableValue.String("settings").asRulesDimensionValue)
            .isEqualTo(RulesDimensionValue.StringValue("settings"))
        assertThat(CustomVariableValue.Boolean(true).asRulesDimensionValue)
            .isEqualTo(RulesDimensionValue.BoolValue(true))
        assertThat(CustomVariableValue.Number(0.5).asRulesDimensionValue)
            .isEqualTo(RulesDimensionValue.DoubleValue(0.5))
    }

    @Test
    fun `whole numbers stay doubles`() {
        // JSON Logic has a single number type, and the engine compares 42.0 and 42 as equal.
        assertThat(CustomVariableValue.Number(42).asRulesDimensionValue)
            .isEqualTo(RulesDimensionValue.DoubleValue(42.0))
        assertThat(CustomVariableValue.Number(42L).asRulesDimensionValue)
            .isEqualTo(RulesDimensionValue.DoubleValue(42.0))
    }

    @Test
    fun `params are equal when their custom variables are`() {
        val params = CheckpointParams("source" to CustomVariableValue.String("settings"))

        assertThat(params).isEqualTo(CheckpointParams(mapOf("source" to CustomVariableValue.String("settings"))))
        assertThat(params.hashCode())
            .isEqualTo(CheckpointParams(mapOf("source" to CustomVariableValue.String("settings"))).hashCode())
        assertThat(params).isNotEqualTo(CheckpointParams("source" to CustomVariableValue.String("onboarding")))
    }

    @Test
    fun `params default to no custom variables`() {
        assertThat(CheckpointParams().customVariables).isEmpty()
    }

    @Test
    fun `keys that cannot be addressed as a custom variable are dropped on construction`() {
        val params = CheckpointParams(
            mapOf(
                "my_property" to CustomVariableValue.String("kept"),
                "my.property" to CustomVariableValue.String("dropped"),
                "2fast" to CustomVariableValue.String("dropped"),
                "has space" to CustomVariableValue.String("dropped"),
                "my-property" to CustomVariableValue.String("dropped"),
                "" to CustomVariableValue.String("dropped"),
            ),
        )

        assertThat(params.customVariables)
            .isEqualTo(mapOf("my_property" to CustomVariableValue.String("kept")))
    }
}
