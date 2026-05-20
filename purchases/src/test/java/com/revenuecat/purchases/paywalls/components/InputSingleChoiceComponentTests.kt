package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.JsonTools
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
internal class InputSingleChoiceComponentTests {

    @RunWith(Parameterized::class)
    class DeserializeInputSingleChoiceComponentTests(
        @Suppress("UNUSED_PARAMETER") name: String,
        private val args: Args,
    ) {

        class Args(
            @Language("json")
            val json: String,
            val expected: PaywallComponent,
        )

        companion object {

            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "input_single_choice minimal",
                    Args(
                        json = """
                        {
                          "type": "input_single_choice",
                          "field_id": "plan_type",
                          "stack": {
                            "type": "stack",
                            "components": []
                          }
                        }
                        """.trimIndent(),
                        expected = InputSingleChoiceComponent(
                            fieldId = "plan_type",
                            required = false,
                            stack = StackComponent(components = emptyList()),
                        ),
                    ),
                ),
                arrayOf(
                    "input_single_choice with required=true",
                    Args(
                        json = """
                        {
                          "type": "input_single_choice",
                          "field_id": "survey_q1",
                          "required": true,
                          "stack": {
                            "type": "stack",
                            "components": []
                          }
                        }
                        """.trimIndent(),
                        expected = InputSingleChoiceComponent(
                            fieldId = "survey_q1",
                            required = true,
                            stack = StackComponent(components = emptyList()),
                        ),
                    ),
                ),
            )
        }

        @Test
        fun `Should properly deserialize as PaywallComponent`() {
            val actual = JsonTools.json.decodeFromString<PaywallComponent>(args.json)
            assert(actual == args.expected)
        }
    }

    @RunWith(Parameterized::class)
    class DeserializeInputOptionComponentTests(
        @Suppress("UNUSED_PARAMETER") name: String,
        private val args: Args,
    ) {

        class Args(
            @Language("json")
            val json: String,
            val expected: PaywallComponent,
        )

        companion object {

            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "input_option minimal",
                    Args(
                        json = """
                        {
                          "type": "input_option",
                          "option_id": "monthly",
                          "option_value": "Monthly",
                          "stack": {
                            "type": "stack",
                            "components": []
                          }
                        }
                        """.trimIndent(),
                        expected = InputOptionComponent(
                            optionId = "monthly",
                            optionValue = "Monthly",
                            stack = StackComponent(components = emptyList()),
                        ),
                    ),
                ),
                arrayOf(
                    "input_option with triggers",
                    Args(
                        json = """
                        {
                          "type": "input_option",
                          "option_id": "annual",
                          "option_value": "Annual",
                          "triggers": {
                            "on_press": "step_2"
                          },
                          "stack": {
                            "type": "stack",
                            "components": []
                          }
                        }
                        """.trimIndent(),
                        expected = InputOptionComponent(
                            optionId = "annual",
                            optionValue = "Annual",
                            stack = StackComponent(components = emptyList()),
                            triggers = mapOf("on_press" to "step_2"),
                        ),
                    ),
                ),
            )
        }

        @Test
        fun `Should properly deserialize as PaywallComponent`() {
            val actual = JsonTools.json.decodeFromString<PaywallComponent>(args.json)
            assert(actual == args.expected)
        }
    }

    @RunWith(Parameterized::class)
    class DeserializePartialInputSingleChoiceComponentTests(
        @Suppress("UNUSED_PARAMETER") name: String,
        private val args: Args,
    ) {

        class Args(
            @Language("json")
            val json: String,
            val expected: PartialInputSingleChoiceComponent,
        )

        companion object {

            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "partial input_single_choice empty",
                    Args(
                        json = "{}",
                        expected = PartialInputSingleChoiceComponent(),
                    ),
                ),
                arrayOf(
                    "partial input_single_choice with fieldId",
                    Args(
                        json = """{"field_id": "plan_type", "required": true}""",
                        expected = PartialInputSingleChoiceComponent(
                            fieldId = "plan_type",
                            required = true,
                        ),
                    ),
                ),
            )
        }

        @Test
        fun `Should properly deserialize as PartialInputSingleChoiceComponent`() {
            val actual = JsonTools.json.decodeFromString<PartialInputSingleChoiceComponent>(args.json)
            assert(actual == args.expected)
        }
    }

    @RunWith(Parameterized::class)
    class DeserializePartialInputOptionComponentTests(
        @Suppress("UNUSED_PARAMETER") name: String,
        private val args: Args,
    ) {

        class Args(
            @Language("json")
            val json: String,
            val expected: PartialInputOptionComponent,
        )

        companion object {

            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun parameters(): Collection<*> = listOf(
                arrayOf(
                    "partial input_option empty",
                    Args(
                        json = "{}",
                        expected = PartialInputOptionComponent(),
                    ),
                ),
                arrayOf(
                    "partial input_option with all fields",
                    Args(
                        json = """
                        {
                          "option_id": "monthly",
                          "option_value": "Monthly",
                          "triggers": {"on_press": "step_2"}
                        }
                        """.trimIndent(),
                        expected = PartialInputOptionComponent(
                            optionId = "monthly",
                            optionValue = "Monthly",
                            triggers = mapOf("on_press" to "step_2"),
                        ),
                    ),
                ),
            )
        }

        @Test
        fun `Should properly deserialize as PartialInputOptionComponent`() {
            val actual = JsonTools.json.decodeFromString<PartialInputOptionComponent>(args.json)
            assert(actual == args.expected)
        }
    }
}
