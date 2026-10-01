@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.subscriberdimensions

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.localrules.RulesDimensionValue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Date

internal class SubscriberDimensionsTest {

    private val originalLogHandler = currentLogHandler

    @Before
    fun setUp() {
        currentLogHandler = object : LogHandler {
            override fun v(tag: String, msg: String) {}
            override fun d(tag: String, msg: String) {}
            override fun i(tag: String, msg: String) {}
            override fun w(tag: String, msg: String) {}
            override fun e(tag: String, msg: String, throwable: Throwable?) {}
        }
    }

    @After
    fun tearDown() {
        currentLogHandler = originalLogHandler
    }

    @Test
    fun `parses the backend's default item`() {
        // language=json
        val item = """
            {
              "dimensions": {
                "country": "ES",
                "subscription_status": null,
                "total_renewals": 0,
                "total_spent": 12.5,
                "first_purchase_at": 1790858464004,
                "latest_auto_renew_intent": false
              },
              "as_of": 1790858464258
            }
        """

        assertThat(parse(item)).isEqualTo(
            SubscriberDimensions(
                values = mapOf(
                    "country" to RulesDimensionValue.StringValue("ES"),
                    "subscription_status" to RulesDimensionValue.NullValue,
                    "total_renewals" to RulesDimensionValue.IntValue(0),
                    "total_spent" to RulesDimensionValue.DoubleValue(12.5),
                    "first_purchase_at" to RulesDimensionValue.IntValue(1790858464004),
                    "latest_auto_renew_intent" to RulesDimensionValue.BoolValue(false),
                ),
                asOf = Date(1790858464258),
            ),
        )
    }

    @Test
    fun `an empty dimensions object is kept with its as_of`() {
        assertThat(parse("""{"dimensions":{},"as_of":1}"""))
            .isEqualTo(SubscriberDimensions(values = emptyMap(), asOf = Date(1)))
    }

    @Test
    fun `a value no rule can read is dropped`() {
        assertThat(parse("""{"dimensions":{"tags":[1,2],"country":"ES"},"as_of":1}""")?.values)
            .containsOnlyKeys("country")
    }

    @Test
    fun `an item without dimensions is unusable`() {
        assertThat(parse("""{"as_of":1}""")).isNull()
        assertThat(parse("""{"dimensions":null,"as_of":1}""")).isNull()
        assertThat(parse("""{"dimensions":"ES","as_of":1}""")).isNull()
        assertThat(parse("""{"dimensions":[],"as_of":1}""")).isNull()
    }

    @Test
    fun `an item without a timestamp is unusable`() {
        assertThat(parse("""{"dimensions":{"country":"ES"}}""")).isNull()
        assertThat(parse("""{"dimensions":{"country":"ES"},"as_of":null}""")).isNull()
        assertThat(parse("""{"dimensions":{"country":"ES"},"as_of":"yesterday"}""")).isNull()
        assertThat(parse("""{"dimensions":{"country":"ES"},"as_of":{"ms":1}}""")).isNull()
    }

    private fun parse(json: String): SubscriberDimensions? =
        SubscriberDimensions.parse(Json.parseToJsonElement(json.trimIndent()).jsonObject)
}
