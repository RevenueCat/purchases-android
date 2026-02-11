package com.revenuecat.purchases.utils

import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions
import org.json.JSONObject

public class JSONObjectAssert(actual: JSONObject) :
    AbstractAssert<JSONObjectAssert, JSONObject>(actual, JSONObjectAssert::class.java) {

    public companion object {
        public fun assertThat(actual: JSONObject): JSONObjectAssert {
            return JSONObjectAssert(actual)
        }
    }

    public fun isEqualToMap(expected: Map<String, String>): JSONObjectAssert {
        expected.forEach { (key, value) ->
            Assertions.assertThat(actual[key]).isEqualTo(value)
        }
        return this
    }
}
