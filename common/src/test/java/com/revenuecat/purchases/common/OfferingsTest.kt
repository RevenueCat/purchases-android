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
import com.revenuecat.purchases.utils.stubINAPPStoreProduct
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

    private val productIdentifier = "com.myproduct"
    private val lifetimeProductIdentifier = "com.myproduct.lifetime"
    private val monthlyBasePlan = "monthly_base_plan"
    private val annualBasePlan = "annual_base_plan"
    private val monthlyDuration = "P1M"
    private val annualDuration = "P1Y"

    @Test
    fun `Package is not created if there are no valid products`() {
        val storeProductAnnual = getStoreProduct(productIdentifier, annualDuration, annualBasePlan)
        val products = mapOf(productIdentifier to listOf(storeProductAnnual))

        val packageToTest = getPackageJSON().createPackage(
            products,
            "offering"
        )
        assertThat(packageToTest).isNull()
    }

    @Test
    fun `Package is created if there are valid products`() {
        val storeProductMonthly = getStoreProduct(productIdentifier, monthlyDuration, monthlyBasePlan)
        val storeProductAnnual = getStoreProduct(productIdentifier, annualDuration, annualBasePlan)
        val storeProductLifetime = stubINAPPStoreProduct(lifetimeProductIdentifier)

        val products = mapOf(
            productIdentifier to listOf(storeProductMonthly, storeProductAnnual),
            lifetimeProductIdentifier to listOf(storeProductLifetime)
        )

        val monthlyPackageJSON = getPackageJSON(
            packageIdentifier = PackageType.MONTHLY.identifier!!,
            productIdentifier = productIdentifier,
            basePlanId = monthlyBasePlan
        )
        val annualPackageJSON = getPackageJSON(
            packageIdentifier = PackageType.ANNUAL.identifier!!,
            productIdentifier = productIdentifier,
            basePlanId = annualBasePlan
        )
        val lifetimePackageJSON = getLifetimePackageJSON()

        val monthlyPackageToTest = monthlyPackageJSON.createPackage(products, "offering")
        assertThat(monthlyPackageToTest).isNotNull
        assertThat(monthlyPackageToTest!!.product).isEqualTo(products[productIdentifier]?.get(0))
        assertThat(monthlyPackageToTest.identifier).isEqualTo(PackageType.MONTHLY.identifier)
        assertThat(monthlyPackageToTest.packageType).isEqualTo(PackageType.MONTHLY)

        val annualPackageToTest = annualPackageJSON.createPackage(products, "offering")
        assertThat(annualPackageToTest).isNotNull
        assertThat(annualPackageToTest!!.product).isEqualTo(products[productIdentifier]?.get(1))
        assertThat(annualPackageToTest.identifier).isEqualTo(PackageType.ANNUAL.identifier)
        assertThat(annualPackageToTest.packageType).isEqualTo(PackageType.ANNUAL)

        val lifetimePackageToTest = lifetimePackageJSON.createPackage(products, "offering")
        assertThat(lifetimePackageToTest).isNotNull
        assertThat(lifetimePackageToTest!!.product).isEqualTo(products[lifetimeProductIdentifier]?.get(0))
        assertThat(lifetimePackageToTest.identifier).isEqualTo(PackageType.LIFETIME.identifier)
        assertThat(lifetimePackageToTest.packageType).isEqualTo(PackageType.LIFETIME)
    }

    @Test
    fun `Package is not created if subscription is missing plan identifier`() {
        val storeProductMonthly = getStoreProduct(productIdentifier, monthlyDuration, monthlyBasePlan)
        val storeProductAnnual = getStoreProduct(productIdentifier, annualDuration, annualBasePlan)

        val products = mapOf(productIdentifier to listOf(storeProductMonthly, storeProductAnnual))

        val monthlyPackageJSON = getPackageJSON(
            packageIdentifier = PackageType.MONTHLY.identifier!!,
            productIdentifier = productIdentifier,
            basePlanId = monthlyBasePlan
        )

        val annualPackageJSON = JSONObject(
            """
                {
                    'identifier': '${PackageType.ANNUAL.identifier}',
                    'platform_product_identifier': '$productIdentifier'
                }
            """.trimIndent()
        )

        val monthlyPackageToTest = monthlyPackageJSON.createPackage(products, "offering")
        assertThat(monthlyPackageToTest).isNotNull
        assertThat(monthlyPackageToTest!!.product).isEqualTo(products[productIdentifier]?.get(0))
        assertThat(monthlyPackageToTest.identifier).isEqualTo(PackageType.MONTHLY.identifier)
        assertThat(monthlyPackageToTest.packageType).isEqualTo(PackageType.MONTHLY)

        val annualPackageToTest = annualPackageJSON.createPackage(products, "offering")
        assertThat(annualPackageToTest).isNull()
    }

    @Test
    fun `Offering is not created if there are no valid packages`() {
        val productId = "com.myproduct.bad"
        val storeProductAnnual = getStoreProduct(productId, annualDuration, annualBasePlan)
        val products = mapOf(productId to listOf(storeProductAnnual))

        val offeringJSON = getOfferingJSON()
        val offering = offeringJSON.createOffering(products)
        assertThat(offering).isNull()
    }

    @Test
    fun `Offering is created if there are valid packages`() {
        val storeProductMonthly = getStoreProduct(productIdentifier, monthlyDuration, monthlyBasePlan)
        val storeProductAnnual = getStoreProduct(productIdentifier, annualDuration, annualBasePlan)

        val products = mapOf(productIdentifier to listOf(storeProductMonthly, storeProductAnnual))

        val monthlyPackageJSON = getPackageJSON(
            PackageType.MONTHLY.identifier!!,
            productIdentifier,
            monthlyBasePlan
        )
        val annualPackageJSON = getPackageJSON(
            PackageType.ANNUAL.identifier!!,
            productIdentifier,
            annualBasePlan
        )
        val offeringJSON = getOfferingJSON(
            offeringIdentifier = "offering_a",
            packagesJSON = listOf(monthlyPackageJSON, annualPackageJSON)
        )

        val offering = offeringJSON.createOffering(products)
        assertThat(offering).isNotNull
    }

    @Test
    fun `List of offerings is empty if there are no valid offerings`() {
        val productId = "com.myproduct.bad"
        val storeProductAnnual = getStoreProduct(productId, annualDuration, annualBasePlan)
        val products = mapOf(productId to listOf(storeProductAnnual))

        val offerings = getOfferingsJSON().createOfferings(products)
        assertThat(offerings).isNotNull
        assertThat(offerings.current).isNull()
        assertThat(offerings["offering_a"]).isNull()
        assertThat(offerings["offering_b"]).isNull()
    }

    @Test
    fun `Offerings can be created`() {
        val storeProductMonthly = getStoreProduct(productIdentifier, monthlyDuration, monthlyBasePlan)
        val storeProductAnnual = getStoreProduct(productIdentifier, annualDuration, annualBasePlan)

        val products = mapOf(productIdentifier to listOf(storeProductMonthly, storeProductAnnual))

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
        val storeProductMonthly = getStoreProduct()
        val products = mapOf(productIdentifier to listOf(storeProductMonthly))
        val offeringJSON = getOfferingJSON(
            offeringIdentifier = "offering_a",
            listOf(
                getPackageJSON(packageIdentifier = identifier)
            )
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
                "${
                    getOfferingJSON(
                        offeringIdentifier = "offering_a",
                        packagesJSON = listOf(getPackageJSON(
                            PackageType.ANNUAL.identifier!!,
                            productIdentifier,
                            annualBasePlan
                        ))
                    )
                }, " +
                "${
                    getOfferingJSON(offeringIdentifier = "offering_b")
                }]" +
                ", 'current_offering_id': 'offering_a'}"
        )

    private fun getOfferingJSON(
        offeringIdentifier: String = "offering_a",
        packagesJSON: List<JSONObject> = listOf(
            getPackageJSON(
                PackageType.MONTHLY.identifier!!,
                this.productIdentifier,
                monthlyBasePlan
            )
        )
    ) = JSONObject(
        """
            {
                'description': 'This is the base offering',
                'identifier': '$offeringIdentifier',
                'packages': ${packagesJSON}
            }
        """.trimIndent()
    )

    private fun getPackageJSON(
        packageIdentifier: String = PackageType.MONTHLY.identifier!!,
        productIdentifier: String = this.productIdentifier,
        basePlanId: String = monthlyBasePlan,
    ) =
        JSONObject(
            """
                {
                    'identifier': '$packageIdentifier',
                    'platform_product_identifier': '$productIdentifier',
                    'platform_product_plan_identifier': '$basePlanId'
                }
            """.trimIndent()
        )

    private fun getLifetimePackageJSON() =
        JSONObject(
            """
                {
                    'identifier': '${PackageType.LIFETIME.identifier}',
                    'platform_product_identifier': '$lifetimeProductIdentifier'
                }
            """.trimIndent()
        )

    private fun getStoreProduct(
        productId: String = productIdentifier,
        duration: String = monthlyDuration,
        basePlanId: String = monthlyBasePlan,
    ): StoreProduct {
        val basePlanPricingPhase = stubPricingPhase(
            price = 1.99,
            billingPeriod = duration,
            recurrenceMode = ProductDetails.RecurrenceMode.INFINITE_RECURRING
        )
        val basePlanPurchaseOption = stubPurchaseOption(basePlanId, duration, listOf(basePlanPricingPhase))

        val offerPricingPhases = listOf(
            stubPricingPhase(
                price = 0.0,
                billingPeriod = duration,
                recurrenceMode = ProductDetails.RecurrenceMode.NON_RECURRING
            ),
            basePlanPricingPhase
        )
        val offerPurchaseOption = stubPurchaseOption(basePlanId, duration, offerPricingPhases)
        return stubStoreProduct(productId, listOf(basePlanPurchaseOption, offerPurchaseOption))
    }
}
