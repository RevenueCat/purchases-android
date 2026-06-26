package com.revenuecat.purchases.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
@OptIn(InternalRevenueCatAPI::class)
class FixtureOfferingsFactoryTest {

    private fun offeringsJson(vararg packages: Pair<String, String>): JSONObject = JSONObject(
        """
        {
          "current_offering_id": "default",
          "offerings": [
            {
              "identifier": "default",
              "description": "Default offering",
              "packages": [
                ${packages.joinToString(",") { (packageId, productId) ->
            """{"identifier": "$packageId", "platform_product_identifier": "$productId"}"""
        }}
              ]
            }
          ]
        }
        """.trimIndent(),
    )

    @Test
    fun `fabricates default test products for known package types`() {
        val offerings = FixtureOfferingsFactory.createOfferings(
            offeringsJson("\$rc_annual" to "my_annual_product"),
        )

        val annualPackage = offerings.current!!.availablePackages.single()
        assertThat(annualPackage.product.price.formatted).isEqualTo("$ 67.99")
        assertThat(annualPackage.product.period?.iso8601).isEqualTo("P1Y")
    }

    @Test
    fun `uses product overrides keyed by platform product identifier`() {
        val override = TestStoreProduct(
            id = "my_annual_product",
            name = "Annual",
            title = "Annual (My app)",
            price = Price(amountMicros = 129_990_000, currencyCode = "KRW", formatted = "₩129,990"),
            description = "Annual",
            period = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
        )

        val offerings = FixtureOfferingsFactory.createOfferings(
            offeringsJson("\$rc_annual" to "my_annual_product"),
            productOverridesByStoreId = mapOf("my_annual_product" to override),
        )

        val annualPackage = offerings.current!!.availablePackages.single()
        assertThat(annualPackage.product.price.formatted).isEqualTo("₩129,990")
        assertThat(annualPackage.product.price.currencyCode).isEqualTo("KRW")
    }

    @Test
    fun `falls back to a generic product for custom package identifiers`() {
        val offerings = FixtureOfferingsFactory.createOfferings(
            offeringsJson("my_custom_package" to "my_custom_product"),
        )

        val customPackage = offerings.current!!.availablePackages.single()
        assertThat(customPackage.product.id).isEqualTo("my_custom_product")
        assertThat(customPackage.product.price.formatted).isEqualTo("$ 9.99")
    }
}
