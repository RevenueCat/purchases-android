//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.utils.stubPricingPhase
import com.revenuecat.purchases.utils.stubPurchaseOption
import com.revenuecat.purchases.utils.stubStoreProduct
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class OfferingsTest {

    private val monthlyProductGroupIdentifier = "com.myproduct"
    private val monthlyProductIdentifier = "com.myproduct.monthly"

    @Test
    fun `Package is not created if there are no valid products`() {
        val packageToTest = getPackageJSON().createPackage(
            getProductsByGroupId("com.myproduct.annual" to "P1Y"),
            "offering"
        )
        assertThat(packageToTest).isNull()
    }

    @Test
    fun `Package is created if there are valid products`() {
        val products: Map<String, List<StoreProduct>> = getProductsByGroupId()
        val packageJSON = getPackageJSON(
            packageIdentifier = PackageType.MONTHLY.identifier!!,
            productIdentifier = monthlyProductIdentifier,
            productGroupIdentifier = monthlyProductGroupIdentifier
        )
        val packageToTest = packageJSON.createPackage(products, "offering")
        assertThat(packageToTest).isNotNull
        assertThat(packageToTest!!.product).isEqualTo(products[monthlyProductGroupIdentifier]?.get(0))
        assertThat(packageToTest.identifier).isEqualTo(PackageType.MONTHLY.identifier)
        assertThat(packageToTest.packageType).isEqualTo(PackageType.MONTHLY)
    }

    @Test
    fun `Package is created for migrated products with same product identifier and group`() {
        val productIdPreBC5 = "onemonth_freetrial"
        val products: Map<String, List<StoreProduct>> = getProductsByGroupId(productIdPreBC5 to "P1M")
        val packageJSON = getPackageJSON(
            packageIdentifier = PackageType.MONTHLY.identifier!!,
            productIdentifier = productIdPreBC5,
            productGroupIdentifier = productIdPreBC5
        )
        val packageToTest = packageJSON.createPackage(products, "offering")
        assertThat(packageToTest).isNotNull
        assertThat(packageToTest!!.product).isEqualTo(products[productIdPreBC5]?.get(0))
        assertThat(packageToTest.identifier).isEqualTo(PackageType.MONTHLY.identifier)
        assertThat(packageToTest.packageType).isEqualTo(PackageType.MONTHLY)
    }

    @Test
    fun `Offering is not created if there are no valid packages`() {
        val products = getProductsByGroupId("com.myproduct.bad" to "P1M")
        val offeringJSON = getOfferingJSON()
        val offering = offeringJSON.createOffering(products)
        assertThat(offering).isNull()
    }

    @Test
    fun `Offering is created if there are valid packages`() {
        val products = getProductsByGroupId()
        val offering = getOfferingJSON().createOffering(products)
        assertThat(offering).isNotNull
    }

    @Test
    fun `List of offerings is empty if there are no valid offerings`() {
        val products = getProductsByGroupId("com.myproduct.bad" to "P1M")
        val offerings = getOfferingsJSON().createOfferings(products)
        assertThat(offerings).isNotNull
        assertThat(offerings.current).isNull()
        assertThat(offerings["offering_a"]).isNull()
        assertThat(offerings["offering_b"]).isNull()
    }

    @Test
    fun `Offerings can be created`() {
        val products = getProductsByGroupId("com.myproduct" to "P6M", "com.myproduct" to "P1M")
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

    @Test
    fun `multiple offerings with packages of the same duration`() {
        // TODOBC5 https://github.com/RevenueCat/purchases-android/pull/674#discussion_r1013951751
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
        val productGroupIdentifier = "com.revenuecat.premium"
        val productIdentifier = "com.revenuecat.premium.monthly"
        val duration = "P1M"
        val products = getProductsByGroupId(productGroupIdentifier to duration)
        val offeringJSON = getOfferingJSON(
            offeringIdentifier = "offering_a",
            packageIdentifier = identifier,
            productIdentifier = productIdentifier,
            productGroupIdentifier = productGroupIdentifier
        )
        val offerings = JSONObject(
            """
                {
                  'offerings': [
                    $offeringJSON
                  ], 
                  'current_offering_id': 'offering_a'
               }""".trimIndent()
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
                    offeringIdentifier = "offering_a",
                    packageIdentifier = PackageType.SIX_MONTH.identifier!!,
                    productIdentifier = "com.myproduct.sixMonth",
                    productGroupIdentifier = "com.myproduct"
                )}, " +
                "${getOfferingJSON(
                    offeringIdentifier = "offering_b",
                    packageIdentifier = PackageType.MONTHLY.identifier!!,
                    productIdentifier = "com.myproduct.monthly",
                    productGroupIdentifier = "com.myproduct"
                )}]" +
                ", 'current_offering_id': 'offering_a'}"
        )

    private fun getOfferingJSON(
        offeringIdentifier: String = "offering_a",
        packageIdentifier: String = PackageType.MONTHLY.identifier!!,
        productIdentifier: String = monthlyProductIdentifier,
        productGroupIdentifier: String = monthlyProductGroupIdentifier
    ) = JSONObject(
        """
            {
                'description': 'This is the base offering',
                'identifier': '$offeringIdentifier',
                'packages':[${getPackageJSON(packageIdentifier, productIdentifier, productGroupIdentifier)}]
            }
        """.trimIndent()
    )

    private fun getPackageJSON(
        packageIdentifier: String = PackageType.MONTHLY.identifier!!,
        productIdentifier: String = monthlyProductIdentifier,
        productGroupIdentifier: String = monthlyProductGroupIdentifier,
        duration: String = "P1M"
    ) =
        JSONObject(
            """
                {
                    'identifier': '$packageIdentifier',
                    'platform_product_group_identifier': '$productGroupIdentifier',
                    'platform_product_identifier': '$productIdentifier',
                    'product_duration': '$duration'
                }
            """.trimIndent()
        )

    private fun getProductsByGroupId(
        vararg productGroupsAndDurations: Pair<String, String> = arrayOf(monthlyProductGroupIdentifier to "P1M")
    ): Map<String, List<StoreProduct>> =
        productGroupsAndDurations.associate { (productGroupIdentifier, duration) ->
            val pricingPhases = listOf(
                stubPricingPhase(
                    price = 1.99,
                    billingPeriod = duration,
                    recurrenceMode = ProductDetails.RecurrenceMode.INFINITE_RECURRING
                )
            )
            val purchaseOption = stubPurchaseOption(pricingPhases, listOf("tag"))
            val storeProduct = stubStoreProduct(productId = productGroupIdentifier, duration, listOf(purchaseOption))
            productGroupIdentifier to listOf(storeProduct)
        }

}
