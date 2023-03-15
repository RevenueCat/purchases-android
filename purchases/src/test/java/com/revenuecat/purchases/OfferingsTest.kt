//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.utils.stubINAPPStoreProduct
import com.revenuecat.purchases.utils.stubPricingPhase
import com.revenuecat.purchases.utils.stubStoreProduct
import com.revenuecat.purchases.utils.stubSubscriptionOption
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class OfferingsTest {

    private val productIdentifier = "com.myproduct"
    private val inAppProductIdentifier = "com.myproduct.lifetime"
    private val monthlyBasePlanId = "monthly_base_plan"
    private val annualBasePlanId = "annual_base_plan"
    private val monthlyPeriod = Period.create("P1M")
    private val annualPeriod = Period.create("P1Y")

    private val offeringsParser = OfferingParserFactory.createOfferingParser(Store.PLAY_STORE)

    @Test
    fun `Package is not created if planIdentifier doesnt match any base plan ids`() {
        val storeProductAnnual = getStoreProduct(productIdentifier, annualPeriod, annualBasePlanId)
        val productWithSingleAnnualBasePlan = mapOf(productIdentifier to listOf(storeProductAnnual))

        val packageWithSingleMonthlyBasePlanJson = getPackageJSON(
            packageIdentifier = "monthly",
            productIdentifier = productIdentifier,
            basePlanId = monthlyBasePlanId
        )
        val packageToTest = offeringsParser.createPackage(
            packageWithSingleMonthlyBasePlanJson,
            productWithSingleAnnualBasePlan,
            "offering"
        )
        assertThat(packageToTest).isNull()
    }

    @Test
    fun `Package is created if there are valid products`() {
        val storeProductMonthly = getStoreProduct(productIdentifier, monthlyPeriod, monthlyBasePlanId)
        val storeProductAnnual = getStoreProduct(productIdentifier, annualPeriod, annualBasePlanId)
        val storeProductInApp = stubINAPPStoreProduct(inAppProductIdentifier)

        val products = mapOf(
            productIdentifier to listOf(storeProductMonthly, storeProductAnnual),
            inAppProductIdentifier to listOf(storeProductInApp)
        )

        val monthlyPackageJSON = getPackageJSON(
            packageIdentifier = PackageType.MONTHLY.identifier!!,
            productIdentifier = storeProductMonthly.id,
            basePlanId = storeProductMonthly.subscriptionOptions!!.basePlan!!.id
        )
        val monthlyPackageToTest = offeringsParser.createPackage(
            monthlyPackageJSON,
            products,
            "offering"
        )
        assertThat(monthlyPackageToTest).isNotNull
        assertThat(monthlyPackageToTest!!.product).isEqualTo(products[productIdentifier]?.get(0))
        assertThat(monthlyPackageToTest.identifier).isEqualTo(PackageType.MONTHLY.identifier)
        assertThat(monthlyPackageToTest.packageType).isEqualTo(PackageType.MONTHLY)

        val annualPackageJSON = getPackageJSON(
            packageIdentifier = PackageType.ANNUAL.identifier!!,
            productIdentifier = storeProductAnnual.id,
            basePlanId = storeProductAnnual.subscriptionOptions!!.basePlan!!.id
        )
        val annualPackageToTest = offeringsParser.createPackage(annualPackageJSON, products, "offering")
        assertThat(annualPackageToTest).isNotNull
        assertThat(annualPackageToTest!!.product).isEqualTo(products[productIdentifier]?.get(1))
        assertThat(annualPackageToTest.identifier).isEqualTo(PackageType.ANNUAL.identifier)
        assertThat(annualPackageToTest.packageType).isEqualTo(PackageType.ANNUAL)

        val inAppPackageJson = getLifetimePackageJSON()
        val inAppPackageToTest = offeringsParser.createPackage(
            inAppPackageJson,
            products,
            "offering",
        )
        assertThat(inAppPackageToTest).isNotNull
        assertThat(inAppPackageToTest!!.product).isEqualTo(products[inAppProductIdentifier]?.get(0))
        assertThat(inAppPackageToTest.identifier).isEqualTo(PackageType.LIFETIME.identifier)
        assertThat(inAppPackageToTest.packageType).isEqualTo(PackageType.LIFETIME)
    }

    @Test
    fun `Package is not created if subscription is missing plan identifier`() {
        val storeProductMonthly = getStoreProduct(productIdentifier, monthlyPeriod, monthlyBasePlanId)
        val storeProductAnnual = getStoreProduct(productIdentifier, annualPeriod, annualBasePlanId)

        val products = mapOf(productIdentifier to listOf(storeProductMonthly, storeProductAnnual))

        val monthlyPackageJSON = getPackageJSON(
            packageIdentifier = PackageType.MONTHLY.identifier!!,
            productIdentifier = productIdentifier,
            basePlanId = monthlyBasePlanId
        )

        val annualPackageJSON = JSONObject(
            """
                {
                    'identifier': '${PackageType.ANNUAL.identifier}',
                    'platform_product_identifier': '$productIdentifier'
                }
            """.trimIndent()
        )

        val monthlyPackageToTest = offeringsParser.createPackage(
            monthlyPackageJSON,
            products,
            "offering",
        )
        assertThat(monthlyPackageToTest).isNotNull
        assertThat(monthlyPackageToTest!!.product).isEqualTo(products[productIdentifier]?.get(0))
        assertThat(monthlyPackageToTest.identifier).isEqualTo(PackageType.MONTHLY.identifier)
        assertThat(monthlyPackageToTest.packageType).isEqualTo(PackageType.MONTHLY)

        val annualPackageToTest = offeringsParser.createPackage(
            annualPackageJSON,
            products,
            "offering",
        )
        assertThat(annualPackageToTest).isNull()
    }

    @Test
    fun `Offering is not created if there are no valid packages`() {
        val productId = "com.myproduct.bad"
        val storeProductAnnual = getStoreProduct(productId, annualPeriod, annualBasePlanId)
        val products = mapOf(productId to listOf(storeProductAnnual))

        val offeringJSON = getOfferingJSON()
        val offering = offeringsParser.createOffering(offeringJSON, products)
        assertThat(offering).isNull()
    }

    @Test
    fun `Offering is created if there are valid packages`() {
        val storeProductMonthly = getStoreProduct(productIdentifier, monthlyPeriod, monthlyBasePlanId)
        val storeProductAnnual = getStoreProduct(productIdentifier, annualPeriod, annualBasePlanId)

        val products = mapOf(productIdentifier to listOf(storeProductMonthly, storeProductAnnual))

        val monthlyPackageJSON = getPackageJSON(
            PackageType.MONTHLY.identifier!!,
            productIdentifier,
            monthlyBasePlanId
        )
        val annualPackageJSON = getPackageJSON(
            PackageType.ANNUAL.identifier!!,
            productIdentifier,
            annualBasePlanId
        )
        val offeringJSON = getOfferingJSON(
            offeringIdentifier = "offering_a",
            packagesJSON = listOf(monthlyPackageJSON, annualPackageJSON)
        )

        val offering = offeringsParser.createOffering(offeringJSON, products)
        assertThat(offering).isNotNull
    }

    @Test
    fun `List of offerings is empty if there are no valid offerings`() {
        val productId = "com.myproduct.bad"
        val storeProductAnnual = getStoreProduct(productId, annualPeriod, annualBasePlanId)
        val products = mapOf(productId to listOf(storeProductAnnual))

        val offerings = offeringsParser.createOfferings(getOfferingsJSON(), products)
        assertThat(offerings).isNotNull
        assertThat(offerings.current).isNull()
        assertThat(offerings["offering_a"]).isNull()
        assertThat(offerings["offering_b"]).isNull()
    }

    @Test
    fun `Offerings can be created`() {
        val storeProductMonthly = getStoreProduct(productIdentifier, monthlyPeriod, monthlyBasePlanId)
        val storeProductAnnual = getStoreProduct(productIdentifier, annualPeriod, annualBasePlanId)

        val products = mapOf(productIdentifier to listOf(storeProductMonthly, storeProductAnnual))

        val offerings = offeringsParser.createOfferings(getOfferingsJSON(), products)
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
            offeringsParser.createOfferings(
                JSONObject("{'offerings': [], 'current_offering_id': null}"),
                emptyMap()
            )

        assertThat(offerings).isNotNull
        assertThat(offerings.current).isNull()
    }

    @Test
    fun `Current offering is a broken product`() {
        val offerings =
            offeringsParser.createOfferings(
                JSONObject("{'offerings': [], 'current_offering_id': 'offering_with_broken_product'}"),
                emptyMap()
            )

        assertThat(offerings).isNotNull
        assertThat(offerings.current).isNull()
    }

    @Test
    fun `multiple offerings with packages of the same duration`() {
        val monthlyProduct2ID = productIdentifier + "monthly2"
        val annualProductID = productIdentifier + "annual"

        val monthlyProduct = getStoreProduct(productIdentifier, monthlyPeriod, monthlyBasePlanId)
        val monthlyProduct2 = getStoreProduct(monthlyProduct2ID, monthlyPeriod, monthlyBasePlanId)
        val annualProduct = getStoreProduct(annualProductID, annualPeriod, annualBasePlanId)

        // TODO fix - there would never be two monthly products with the same identifiers
//        val products = mapOf(productIdentifier to listOf(monthlyProduct, annualProduct), monthlyProduct2ID to listOf(monthlyProduct2))

        val products = mapOf(productIdentifier to listOf(monthlyProduct, monthlyProduct2, annualProduct))


        val monthlyPackageID = "monthly"
        val monthlyPackage2ID = "monthly_@"
        val annualPackageID = "annual"
        val offeringID = "offering_a"

        val offering = offeringsParser.createOffering(
            getOfferingJSON(
                offeringID,
                listOf(
                    getPackageJSON(monthlyPackageID, productIdentifier),
                    getPackageJSON(monthlyPackage2ID, monthlyProduct2ID),
                    getPackageJSON(annualPackageID, annualProductID)
                )
            ),
            products
        )

        val packages = offering!!.availablePackages
        val monthlyPackages = packages.filter { it.packageType == PackageType.MONTHLY }
        val yearlyPackages = packages.filter { it.packageType == PackageType.ANNUAL }

        assertThat(offering).isNotNull
        assertThat(offering.identifier).isEqualTo(offeringID)
        assertThat(packages.size).isEqualTo(3)
        assertThat(monthlyPackages.size).isEqualTo(2)
        assertThat(yearlyPackages.size).isEqualTo(1)
        assertThat(monthlyPackages.filter { it.identifier == monthlyPackageID }).isNotNull
        assertThat(monthlyPackages.filter { it.identifier == monthlyPackage2ID }).isNotNull
        assertThat(yearlyPackages.filter { it.identifier == annualPackageID }).isNotNull
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
        val offerings = offeringsParser.createOfferings(
            JSONObject(
                """
                {
                  'offerings': [
                    $offeringJSON
                  ],
                  'current_offering_id': 'offering_a'
               }""".trimIndent()
            ),
            products
        )

        assertThat(offerings).isNotNull
        assertThat(offerings.current).isNotNull
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
                        packagesJSON = listOf(
                            getPackageJSON(
                                PackageType.ANNUAL.identifier!!,
                                productIdentifier,
                                annualBasePlanId
                            )
                        )
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
                monthlyBasePlanId
            )
        )
    ) = JSONObject(
        """
            {
                'description': 'This is the base offering',
                'identifier': '$offeringIdentifier',
                'packages': $packagesJSON
            }
        """.trimIndent()
    )

    private fun getPackageJSON(
        packageIdentifier: String = PackageType.MONTHLY.identifier!!,
        productIdentifier: String = this.productIdentifier,
        basePlanId: String = monthlyBasePlanId,
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
                    'platform_product_identifier': '$inAppProductIdentifier'
                }
            """.trimIndent()
        )

    private fun getStoreProduct(
        productId: String = productIdentifier,
        period: Period =  monthlyPeriod,
        basePlanId: String = monthlyBasePlanId,
    ): StoreProduct {
        val basePlanPricingPhase = stubPricingPhase(
            price = 1.99,
            billingPeriod = period,
            recurrenceMode = ProductDetails.RecurrenceMode.INFINITE_RECURRING
        )
        val basePlanSubscriptionOption =
            stubSubscriptionOption(basePlanId, productId, period, pricingPhases = listOf(basePlanPricingPhase))

        val offerPricingPhases = listOf(
            stubPricingPhase(
                price = 0.0,
                billingPeriod = period,
                recurrenceMode = ProductDetails.RecurrenceMode.NON_RECURRING
            ),
            basePlanPricingPhase
        )
        val offerSubscriptionOption = stubSubscriptionOption(basePlanId, productId, period, offerPricingPhases)
        return stubStoreProduct(
            productId,
            basePlanSubscriptionOption,
            listOf(basePlanSubscriptionOption, offerSubscriptionOption)
        )
    }
}
