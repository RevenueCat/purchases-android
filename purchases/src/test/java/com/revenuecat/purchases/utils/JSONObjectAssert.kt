package com.revenuecat.purchases.utils

import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions
import org.json.JSONObject

class JSONObjectAssert(actual: JSONObject) :
    AbstractAssert<JSONObjectAssert, JSONObject>(actual, JSONObjectAssert::class.java) {

    companion object {
        fun assertThat(actual: JSONObject): JSONObjectAssert {
            return JSONObjectAssert(actual)
        }
    }

    fun isEqualToMap(expected: Map<String, String>): JSONObjectAssert {
        expected.forEach { (key, value) ->
            Assertions.assertThat(actual[key]).isEqualTo(value)
        }
        return this
    }
}
