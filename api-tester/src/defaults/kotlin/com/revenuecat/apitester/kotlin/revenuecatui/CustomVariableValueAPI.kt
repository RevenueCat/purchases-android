package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue

@Suppress("unused", "UNUSED_VARIABLE")
private class CustomVariableValueAPI {

    fun check() {
        // String type is public
        val stringValue: CustomVariableValue = CustomVariableValue.String("test")
        val stringInnerValue: String = stringValue.stringValue

        // Access the underlying value
        val underlyingString: String = (stringValue as CustomVariableValue.String).value
    }

    fun checkMap() {
        // Typical usage with map
        val customVariables: Map<String, CustomVariableValue> = mapOf(
            "player_name" to CustomVariableValue.String("John"),
            "app_name" to CustomVariableValue.String("My App"),
        )

        // Access values
        for ((key, value) in customVariables) {
            val stringRepresentation: String = value.stringValue
        }
    }
}
