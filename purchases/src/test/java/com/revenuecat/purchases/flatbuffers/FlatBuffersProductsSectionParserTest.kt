package com.revenuecat.purchases.flatbuffers

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.flatbuffers.generated.ProductType
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlatBuffersProductsSectionParserTest {

    private val fetchedAtMs = 1_717_000_000_000L

    private val fixtureProducts = listOf(
        FixtureProduct(
            id = "rc_monthly",
            title = "Monthly",
            priceMicros = 9_990_000L,
            currencyCode = "USD",
            type = ProductType.SUBSCRIPTION,
        ),
        FixtureProduct(
            id = "rc_coins_100",
            title = null,
            priceMicros = 1_990_000L,
            currencyCode = "EUR",
            type = ProductType.CONSUMABLE,
        ),
    )

    private fun bodyWithSection(base64: String): JSONObject =
        JSONObject().apply { put("products_section_fb", base64) }

    @Test
    fun `parses a base64 FlatBuffers section embedded in a JSON body`() {
        val body = bodyWithSection(encodeProductsSectionBase64(fixtureProducts, fetchedAtMs))

        val result = FlatBuffersProductsSectionParser.parse(body)

        assertThat(result).isNotNull
        assertThat(result!!.fetchedAtMs).isEqualTo(fetchedAtMs)
        assertThat(result.products).hasSize(2)

        val monthly = result.products[0]
        assertThat(monthly.id).isEqualTo("rc_monthly")
        assertThat(monthly.title).isEqualTo("Monthly")
        assertThat(monthly.priceMicros).isEqualTo(9_990_000L)
        assertThat(monthly.currencyCode).isEqualTo("USD")
        assertThat(monthly.type).isEqualTo(ProductTypeData.SUBSCRIPTION)

        val coins = result.products[1]
        assertThat(coins.id).isEqualTo("rc_coins_100")
        assertThat(coins.title).isNull()
        assertThat(coins.currencyCode).isEqualTo("EUR")
        assertThat(coins.type).isEqualTo(ProductTypeData.CONSUMABLE)
    }

    @Test
    fun `returns null when the section field is absent`() {
        val result = FlatBuffersProductsSectionParser.parse(JSONObject().apply { put("other", 1) })

        assertThat(result).isNull()
    }

    @Test
    fun `returns null without throwing when the section is malformed`() {
        val result = FlatBuffersProductsSectionParser.parse(bodyWithSection("not-valid-base64-flatbuffer!!"))

        assertThat(result).isNull()
    }
}
