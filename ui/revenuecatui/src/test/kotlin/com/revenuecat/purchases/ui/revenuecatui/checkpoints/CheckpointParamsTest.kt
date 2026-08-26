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
        val params = CheckpointParams { customVariables { "source" to "settings" } }
        val sameFromBuilder = CheckpointParams.Builder()
            .setCustomVariables(mapOf("source" to CustomVariableValue.String("settings")))
            .build()

        assertThat(params).isEqualTo(sameFromBuilder)
        assertThat(params.hashCode()).isEqualTo(sameFromBuilder.hashCode())
        assertThat(params).isNotEqualTo(CheckpointParams { customVariables { "source" to "onboarding" } })
    }

    @Test
    fun `params default to no custom variables`() {
        assertThat(CheckpointParams {}.customVariables).isEmpty()
        assertThat(CheckpointParams.Builder().build().customVariables).isEmpty()
    }

    @Test
    fun `each infix overload maps to its custom variable variant`() {
        val params = CheckpointParams {
            customVariables {
                "string" to "value"
                "int" to 1
                "long" to 2L
                "double" to 0.5
                "float" to 0.5f
                "boolean" to true
                "value" to CustomVariableValue.String("raw")
            }
        }

        assertThat(params.customVariables).isEqualTo(
            mapOf(
                "string" to CustomVariableValue.String("value"),
                "int" to CustomVariableValue.Number(1),
                "long" to CustomVariableValue.Number(2L),
                "double" to CustomVariableValue.Number(0.5),
                "float" to CustomVariableValue.Number(0.5f),
                "boolean" to CustomVariableValue.Boolean(true),
                "value" to CustomVariableValue.String("raw"),
            ),
        )
    }

    @Test
    fun `add overloads build the same map as the infix forms`() {
        val added = CheckpointParams.CustomVariablesBuilder()
            .add("string", "value")
            .add("int", 1)
            .add("long", 2L)
            .add("double", 0.5)
            .add("float", 0.5f)
            .add("boolean", true)
            .add("value", CustomVariableValue.String("raw"))
            .build()

        val infixParams = CheckpointParams {
            customVariables {
                "string" to "value"
                "int" to 1
                "long" to 2L
                "double" to 0.5
                "float" to 0.5f
                "boolean" to true
                "value" to CustomVariableValue.String("raw")
            }
        }

        assertThat(CheckpointParams.Builder().setCustomVariables(added).build()).isEqualTo(infixParams)
    }

    @Test
    fun `a repeated key keeps the last value`() {
        val params = CheckpointParams {
            customVariables {
                "source" to "first"
                "source" to "last"
            }
        }

        assertThat(params.customVariables).isEqualTo(mapOf("source" to CustomVariableValue.String("last")))
    }

    @Test
    fun `setting custom variables again replaces the previous ones`() {
        val params = CheckpointParams {
            customVariables { "first" to "dropped" }
            customVariables { "second" to "kept" }
        }

        assertThat(params.customVariables).isEqualTo(mapOf("second" to CustomVariableValue.String("kept")))
    }

    @Test
    fun `keys that cannot be addressed as a custom variable are dropped on construction`() {
        val params = CheckpointParams.Builder()
            .setCustomVariables(
                mapOf(
                    "my_property" to CustomVariableValue.String("kept"),
                    "my.property" to CustomVariableValue.String("dropped"),
                    "2fast" to CustomVariableValue.String("dropped"),
                    "has space" to CustomVariableValue.String("dropped"),
                    "my-property" to CustomVariableValue.String("dropped"),
                    "" to CustomVariableValue.String("dropped"),
                ),
            )
            .build()

        assertThat(params.customVariables)
            .isEqualTo(mapOf("my_property" to CustomVariableValue.String("kept")))
    }
}
