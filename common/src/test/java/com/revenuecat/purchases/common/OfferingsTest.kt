//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.models.ProductDetails
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class OfferingsTest {

    private val monthlyProductIdentifier = "com.myproduct.monthly"

    @Test
    fun `Package is not created if there are no valid products`() {
        val packageToTest = getPackageJSON().createPackage(
            getProducts("com.myproduct.annual"),
            "offering"
        )
        assertThat(packageToTest).isNull()
    }

    @Test
    fun `Package is created if there are valid products`() {
        val products = getProducts(monthlyProductIdentifier)
        val packageToTest =
            getPackageJSON(PackageType.MONTHLY.identifier!!, monthlyProductIdentifier)
                .createPackage(
                    products,
                    "offering"
                )
        assertThat(packageToTest).isNotNull
        assertThat(packageToTest!!.product).isEqualTo(products[monthlyProductIdentifier])
        assertThat(packageToTest.identifier).isEqualTo(PackageType.MONTHLY.identifier)
        assertThat(packageToTest.packageType).isEqualTo(PackageType.MONTHLY)
    }

    @Test
    fun `Offering is not created if there are no valid packages`() {
        val products = getProducts("com.myproduct.bad")
        val offering = getOfferingJSON().createOffering(products)
        assertThat(offering).isNull()
    }

    @Test
    fun `Offering is created if there are valid packages`() {
        val products = getProducts()
        val offering = getOfferingJSON().createOffering(products)
        assertThat(offering).isNotNull
    }

    @Test
    fun `List of offerings is empty if there are no valid offerings`() {
        val products = getProducts("com.myproduct.bad")
        val offerings = getOfferingsJSON().createOfferings(products)
        assertThat(offerings).isNotNull
        assertThat(offerings.current).isNull()
        assertThat(offerings["offering_a"]).isNull()
        assertThat(offerings["offering_b"]).isNull()
    }

    @Test
    fun `Offerings can be created`() {
        val products = getProducts("com.myproduct.sixMonth", "com.myproduct.monthly")
        val offerings = getOfferingsJSON().createOfferings(products)
        assertThat(offerings).isNotNull
        assertThat(offerings.current!!.identifier).isEqualTo("offering_a")
        assertThat(offerings["offering_a"]).isNotNull
        assertThat(offerings["offering_b"]).isNotNull
    }

    @Test
    fun `Lifetime package`() {
        testPackageType(PackageType.LIFETIME)
    }

    @Test
    fun `Annual package`() {
        testPackageType(PackageType.ANNUAL)
    }

    @Test
    fun `Six months package`() {
        testPackageType(PackageType.SIX_MONTH)
    }

    @Test
    fun `Three months package`() {
        testPackageType(PackageType.THREE_MONTH)
    }

    @Test
    fun `Two months package`() {
        testPackageType(PackageType.TWO_MONTH)
    }

    @Test
    fun `Monthly package`() {
        testPackageType(PackageType.MONTHLY)
    }

    @Test
    fun `Weekly package`() {
        testPackageType(PackageType.WEEKLY)
    }

    @Test
    fun `Custom package`() {
        testPackageType(PackageType.CUSTOM)
    }

    @Test
    fun `Unknown package`() {
        testPackageType(PackageType.UNKNOWN)
    }

    @Test
    fun `No offerings`() {
        val offerings =
            JSONObject("{'offerings': [], 'current_offering_id': null}").createOfferings(emptyMap())

        assertThat(offerings).isNotNull
        assertThat(offerings.current).isNull()
    }

    @Test
    fun `Current offering is a broken product`() {
        val offerings =
            JSONObject("{'offerings': [], 'current_offering_id': 'offering_with_broken_product'}")
                .createOfferings(emptyMap())

        assertThat(offerings).isNotNull
        assertThat(offerings.current).isNull()
    }

    private fun testPackageType(packageType: PackageType) {
        var identifier = packageType.identifier
        if (identifier == null) {
            identifier = if (packageType == PackageType.UNKNOWN) {
                "\$rc_a_future_package_type"
            } else {
                "custom"
            }
        }
        val productIdentifier = "com.myproduct"
        val products = getProducts("com.myproduct")
        val offerings = JSONObject(
            "{'offerings': [" +
                "{'identifier': 'offering_a', " +
                "'description': 'This is the base offering'," +
                "'packages':[{'identifier': '$identifier', 'platform_product_identifier':'$productIdentifier'}]}], " +
                "'current_offering_id': 'offering_a'}"
        ).createOfferings(products)

        assertThat(offerings).isNotNull
        assertThat(offerings.current)
        assertPackage(packageType, PackageType.LIFETIME, offerings.current!!.lifetime)
        assertPackage(packageType, PackageType.ANNUAL, offerings.current!!.annual)
        assertPackage(packageType, PackageType.SIX_MONTH, offerings.current!!.sixMonth)
        assertPackage(packageType, PackageType.THREE_MONTH, offerings.current!!.threeMonth)
        assertPackage(packageType, PackageType.TWO_MONTH, offerings.current!!.twoMonth)
        assertPackage(packageType, PackageType.MONTHLY, offerings.current!!.monthly)
        assertPackage(packageType, PackageType.WEEKLY, offerings.current!!.weekly)

        assertThat(offerings["offering_a"]!!.getPackage(identifier).packageType).isEqualTo(packageType)
    }

    private fun assertPackage(
        packageType: PackageType,
        expectedPackageType: PackageType,
        packageToCheck: Package?
    ) {
        if (packageType == expectedPackageType) {
            assertThat(packageToCheck).isNotNull
        } else {
            assertThat(packageToCheck).isNull()
        }
    }

    private fun getOfferingsJSON() =
        JSONObject(
            "{'offerings': [" +
                "${getOfferingJSON(
                    "offering_a",
                    PackageType.SIX_MONTH.identifier!!,
                    "com.myproduct.sixMonth"
                )}, " +
                "${getOfferingJSON(
                    "offering_b",
                    PackageType.MONTHLY.identifier!!,
                    "com.myproduct.monthly"
                )}]" +
                ", 'current_offering_id': 'offering_a'}"
        )

    private fun getOfferingJSON(
        offeringIdentifier: String = "offering_a",
        packageIdentifier: String = PackageType.MONTHLY.identifier!!,
        productIdentifier: String = monthlyProductIdentifier
    ) = JSONObject(
        "{'identifier': '$offeringIdentifier', " +
            "'description': 'This is the base offering'," +
            "'packages':[${getPackageJSON(packageIdentifier, productIdentifier)}]}"
    )

    private fun getPackageJSON(
        packageIdentifier: String = PackageType.MONTHLY.identifier!!,
        productIdentifier: String = monthlyProductIdentifier
    ) =
        JSONObject(
            "{'identifier': '$packageIdentifier', " +
                "'platform_product_identifier': '$productIdentifier'}"
        )

    private fun getProducts(
        vararg productIdentifiers: String = arrayOf(monthlyProductIdentifier)
    ): Map<String, ProductDetails> =
        productIdentifiers.map { productIdentifier ->
            productIdentifier to mockk<ProductDetails>().also {
                every { it.sku } returns productIdentifier
            }
        }.toMap()
}
