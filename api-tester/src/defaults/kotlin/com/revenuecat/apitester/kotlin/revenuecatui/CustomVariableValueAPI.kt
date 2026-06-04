package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue

@Suppress("unused", "UNUSED_VARIABLE", "MagicNumber")
private class CustomVariableValueAPI {

    fun checkString() {
        val stringValue: CustomVariableValue = CustomVariableValue.String("test")
        val stringInnerValue: String = stringValue.stringValue
        val underlyingString: String = (stringValue as CustomVariableValue.String).value
    }

    fun checkNumber() {
        val fromDouble: CustomVariableValue = CustomVariableValue.Number(9.99)
        val fromInt: CustomVariableValue = CustomVariableValue.Number(42)
        val fromLong: CustomVariableValue = CustomVariableValue.Number(123L)
        val fromFloat: CustomVariableValue = CustomVariableValue.Number(3.14f)
        val underlyingDouble: Double = (fromDouble as CustomVariableValue.Number).value
        val stringRepresentation: String = fromDouble.stringValue
    }

    fun checkBoolean() {
        val boolTrue: CustomVariableValue = CustomVariableValue.Boolean(true)
        val boolFalse: CustomVariableValue = CustomVariableValue.Boolean(false)
        val underlyingBool: Boolean = (boolTrue as CustomVariableValue.Boolean).value
        val stringRepresentation: String = boolTrue.stringValue
    }

    fun checkMap() {
        val customVariables: Map<String, CustomVariableValue> = mapOf(
            "player_name" to CustomVariableValue.String("John"),
            "level" to CustomVariableValue.Number(42),
            "is_premium" to CustomVariableValue.Boolean(true),
        )

        for ((key, value) in customVariables) {
            val stringRepresentation: String = value.stringValue
        }
    }
}
