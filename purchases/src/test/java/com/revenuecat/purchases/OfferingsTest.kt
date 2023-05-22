//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.utils.getLifetimePackageJSON
import com.revenuecat.purchases.utils.stubINAPPStoreProduct
import com.revenuecat.purchases.utils.stubPricingPhase
import com.revenuecat.purchases.utils.stubStoreProduct
import com.revenuecat.purchases.utils.stubSubscriptionOption
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
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
    private val monthlyPackageID = PackageType.MONTHLY.identifier!!
    private val annualPackageID = PackageType.ANNUAL.identifier!!

    private val offeringsParser = OfferingParserFactory.createOfferingParser(Store.PLAY_STORE)

    @Test
    fun `createPackage returns null if packageJson planIdentifier doesnt match any sub StoreProduct base plan ids`() {
        val storeProductAnnual = getStoreProduct(productIdentifier, annualPeriod, annualBasePlanId)
        val storeProductInApp = stubINAPPStoreProduct(inAppProductIdentifier)
        val singleAnnualBasePlanProductAndInAppProduct = mapOf(
            productIdentifier to listOf(storeProductAnnual),
            inAppProductIdentifier to listOf(storeProductInApp)
        )

        val packageWithSingleMonthlyBasePlanJson = getPackageJSON(
            packageIdentifier = "monthly",
            productIdentifier = productIdentifier,
            basePlanId = monthlyBasePlanId
        )
        val packageToTest = offeringsParser.createPackage(
            packageWithSingleMonthlyBasePlanJson,
            singleAnnualBasePlanProductAndInAppProduct,
            "offering"
        )
        assertThat(packageToTest).isNull()
    }

    @Test
    fun `createPackage for sub sets presentedOfferingId on Package, product, product's defaultOption, and product's subscriptionOptions`() {
        val storeProductMonthly = getStoreProduct(productIdentifier, monthlyPeriod, monthlyBasePlanId)
        val storeProductAnnual = getStoreProduct(productIdentifier, annualPeriod, annualBasePlanId)

        val products = mapOf(
            productIdentifier to listOf(storeProductMonthly, storeProductAnnual)
        )

        val monthlyPackageJSON = getPackageJSON(
            packageIdentifier = PackageType.MONTHLY.identifier!!,
            productIdentifier = storeProductMonthly.id,
            basePlanId = storeProductMonthly.subscriptionOptions!!.basePlan!!.id
        )

        val expectedOfferingIdentifier = "offering"
        val monthlyPackageToTest = offeringsParser.createPackage(
            monthlyPackageJSON,
            products,
            expectedOfferingIdentifier
        )!!

        assertThat(monthlyPackageToTest.offering).isEqualTo(expectedOfferingIdentifier)

        val packageProduct = monthlyPackageToTest.product
        assertThat(packageProduct.presentedOfferingIdentifier).isEqualTo(expectedOfferingIdentifier)

        val defaultOption = packageProduct.defaultOption!!
        assertThat(defaultOption.presentedOfferingIdentifier).isEqualTo(expectedOfferingIdentifier)

        val allOptions = packageProduct.subscriptionOptions!!
        allOptions.forEach {
            assertThat(it.presentedOfferingIdentifier).isEqualTo(expectedOfferingIdentifier)
        }
    }

    @Test
    fun `createPackage for OTP sets offeringId on Package and OTP Product`() {
        val storeProductInApp = stubINAPPStoreProduct(inAppProductIdentifier)
        val products = mapOf(
            inAppProductIdentifier to listOf(storeProductInApp)
        )
        val expectedOfferingIdentifier = "OTP_offering"
        val inAppPackageJson = getLifetimePackageJSON()
        val inAppPackageToTest = offeringsParser.createPackage(
            inAppPackageJson,
            products,
            expectedOfferingIdentifier
        )

        assertThat(inAppPackageToTest!!.offering).isEqualTo(expectedOfferingIdentifier)

        val packageProduct = inAppPackageToTest!!.product
        assertThat(packageProduct.presentedOfferingIdentifier).isEqualTo(expectedOfferingIdentifier)
    }

    @Test
    fun `createPackage creates a Package if package json matches subscription store products`() {
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
        assertThat(monthlyPackageToTest!!.product).usingRecursiveComparison()
            .isEqualTo(products[productIdentifier]?.get(0))
        assertThat(monthlyPackageToTest.identifier).isEqualTo(PackageType.MONTHLY.identifier)
        assertThat(monthlyPackageToTest.packageType).isEqualTo(PackageType.MONTHLY)

        val annualPackageJSON = getPackageJSON(
            packageIdentifier = annualPackageID,
            productIdentifier = storeProductAnnual.id,
            basePlanId = storeProductAnnual.subscriptionOptions!!.basePlan!!.id
        )
        val annualPackageToTest = offeringsParser.createPackage(annualPackageJSON, products, "offering")
        assertThat(annualPackageToTest).isNotNull
        assertThat(annualPackageToTest!!.product).usingRecursiveComparison()
            .isEqualTo(products[productIdentifier]?.get(1))
        assertThat(annualPackageToTest.identifier).isEqualTo(annualPackageID)
        assertThat(annualPackageToTest.packageType).isEqualTo(PackageType.ANNUAL)
    }

    fun `createPackage creates a Package if packageJson matches INAPP StoreProduct`() {
        val storeProductMonthly = getStoreProduct(productIdentifier, monthlyPeriod, monthlyBasePlanId)
        val storeProductAnnual = getStoreProduct(productIdentifier, annualPeriod, annualBasePlanId)
        val storeProductInApp = stubINAPPStoreProduct(inAppProductIdentifier)

        val products = mapOf(
            productIdentifier to listOf(storeProductMonthly, storeProductAnnual),
            inAppProductIdentifier to listOf(storeProductInApp)
        )

        val inAppPackageJson = getLifetimePackageJSON()
        val inAppPackageToTest = offeringsParser.createPackage(
            inAppPackageJson,
            products,
            "offering",
        )
        assertThat(inAppPackageToTest).isNotNull
        assertThat(inAppPackageToTest!!.product).usingRecursiveComparison()
            .isEqualTo(products[inAppProductIdentifier]?.get(0))
        assertThat(inAppPackageToTest.identifier).isEqualTo(PackageType.LIFETIME.identifier)
        assertThat(inAppPackageToTest.packageType).isEqualTo(PackageType.LIFETIME)
    }

    @Test
    fun `createPackage returns null if packageJson is missing plan_identifier`() {
        val storeProductMonthly = getStoreProduct(productIdentifier, monthlyPeriod, monthlyBasePlanId)
        val storeProductAnnual = getStoreProduct(productIdentifier, annualPeriod, annualBasePlanId)

        val products = mapOf(productIdentifier to listOf(storeProductMonthly, storeProductAnnual))

        val annualPackageJSON = JSONObject(
            """
                {
                    'identifier': '${annualPackageID}',
                    'platform_product_identifier': '$productIdentifier'
                }
            """.trimIndent()
        )

        val annualPackageToTest = offeringsParser.createPackage(
            annualPackageJSON,
            products,
            "offering",
        )
        assertThat(annualPackageToTest).isNull()
    }

    @Test
    fun `createOffering returns null if offeringJson contains no packages matching StoreProducts`() {
        val productId = "com.myproduct.bad"
        val storeProductAnnual = getStoreProduct(productId, annualPeriod, annualBasePlanId)
        val products = mapOf(productId to listOf(storeProductAnnual))

        val offeringWithOneMonthlyPackageJson = getOfferingJSON()
        val offering = offeringsParser.createOffering(offeringWithOneMonthlyPackageJson, products)
        assertThat(offering).isNull()
    }

    @Test
    fun `createOffering returns Offering with offeringJson packages that match StoreProducts`() {
        val storeProductMonthly = getStoreProduct(productIdentifier, monthlyPeriod, monthlyBasePlanId)
        val storeProductAnnual = getStoreProduct(productIdentifier, annualPeriod, annualBasePlanId)

        val products = mapOf(productIdentifier to listOf(storeProductMonthly, storeProductAnnual))

        val monthlyPackageID = PackageType.MONTHLY.identifier!!
        val monthlyPackageJSON = getPackageJSON(
            monthlyPackageID,
            storeProductMonthly.id,
            storeProductMonthly.subscriptionOptions!!.basePlan!!.id
        )

        val annualPackageID = annualPackageID
        val annualPackageJSON = getPackageJSON(
            annualPackageID,
            storeProductAnnual.id,
            storeProductAnnual.subscriptionOptions!!.basePlan!!.id
        )
        val offeringId = "offering_a"
        val metadata = mapOf(
            "int" to 5,
            "double" to 5.5,
            "boolean" to true,
            "string" to "five",
            "array" to arrayOf("five"),
            "dictionary" to mapOf(
                "string" to "five"
            )
        )
        val offeringJSON = getOfferingJSON(
            offeringIdentifier = offeringId,
            packagesJSON = listOf(monthlyPackageJSON, annualPackageJSON),
            metadata = metadata
        )

        val offering = offeringsParser.createOffering(offeringJSON, products)
        assertThat(offering).isNotNull
        assertThat(offering!!.identifier).isEqualTo(offeringId)
        assertThat(offering!!.metadata).isEqualTo(metadata)

        val packages = offering.availablePackages
        assertThat(packages.size).isEqualTo(2)

        val monthlyPackages = packages.filter { it.packageType == PackageType.MONTHLY }
        val yearlyPackages = packages.filter { it.packageType == PackageType.ANNUAL }
        assertThat(monthlyPackages.size).isEqualTo(1)
        assertThat(yearlyPackages.size).isEqualTo(1)

        assertThat(monthlyPackages.filter { it.identifier == monthlyPackageID }.size).isEqualTo(1)
        assertThat(yearlyPackages.filter { it.identifier == annualPackageID }.size).isEqualTo(1)
    }

    @Test
    fun `createOfferings returns empty list if offeringsJson contains no matching StoreProducts`() {
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
    fun `createOfferings creates offerings`() {
        val storeProductMonthly = getStoreProduct(productIdentifier, monthlyPeriod, monthlyBasePlanId)
        val storeProductAnnual = getStoreProduct(productIdentifier, annualPeriod, annualBasePlanId)

        val products = mapOf(productIdentifier to listOf(storeProductMonthly, storeProductAnnual))
        val offeringsJson = getOfferingsJSON()

        val offerings = offeringsParser.createOfferings(offeringsJson, products)
        assertThat(offerings).isNotNull
        assertThat(offerings.all.size).isEqualTo(2)
        assertThat(offerings.current!!.identifier).isEqualTo(offeringsJson.getString("current_offering_id"))
        assertThat(offerings["offering_a"]).isNotNull
        assertThat(offerings["offering_b"]).isNotNull
    }

    @Test
    fun `createOfferings does not set currentOffering if current_offering_id is null`() {
        val storeProductMonthly = getStoreProduct(productIdentifier, monthlyPeriod, monthlyBasePlanId)
        val storeProductAnnual = getStoreProduct(productIdentifier, annualPeriod, annualBasePlanId)

        val products = mapOf(productIdentifier to listOf(storeProductMonthly, storeProductAnnual))
        val offeringsJson = getOfferingsJSON("")

        val offerings = offeringsParser.createOfferings(offeringsJson, products)
        assertThat(offerings).isNotNull
        assertThat(offerings.all.size).isEqualTo(2)
        assertThat(offerings.current).isNull()
        assertThat(offerings["offering_a"]).isNotNull
        assertThat(offerings["offering_b"]).isNotNull
    }

    @Test
    fun `Lifetime package type set properly`() {
        testPackageType(PackageType.LIFETIME)
    }

    @Test
    fun `Annual package type set properly`() {
        testPackageType(PackageType.ANNUAL)
    }

    @Test
    fun `Six months package type set properly`() {
        testPackageType(PackageType.SIX_MONTH)
    }

    @Test
    fun `Three months package type set properly`() {
        testPackageType(PackageType.THREE_MONTH)
    }

    @Test
    fun `Two months package type set properly`() {
        testPackageType(PackageType.TWO_MONTH)
    }

    @Test
    fun `Monthly package type set properly`() {
        testPackageType(PackageType.MONTHLY)
    }

    @Test
    fun `Weekly package type set properly`() {
        testPackageType(PackageType.WEEKLY)
    }

    @Test
    fun `Custom package type set properly`() {
        testPackageType(PackageType.CUSTOM)
    }

    @Test
    fun `Unknown package type set properly`() {
        testPackageType(PackageType.UNKNOWN)
    }

    @Test
    fun `createOfferings returns empty list if no offerings in json`() {
        val offerings =
            offeringsParser.createOfferings(
                JSONObject("{'offerings': [], 'current_offering_id': null}"),
                emptyMap()
            )

        assertThat(offerings).isNotNull
        assertThat(offerings.all.size).isEqualTo(0)
        assertThat(offerings.current).isNull()
    }

    @Test
    fun `createOfferings returns empty list if no StoreProducts fetched`() {
        val offerings =
            offeringsParser.createOfferings(
                JSONObject("{'offerings': [], 'current_offering_id': 'offering_with_broken_product'}"),
                emptyMap()
            )

        assertThat(offerings).isNotNull
        assertThat(offerings.all.size).isEqualTo(0)
        assertThat(offerings.current).isNull()
    }

    @Test
    fun `Can create multiple offerings with packages of the same duration`() {
        val productId2 = productIdentifier + "2"

        val monthlyProduct = getStoreProduct(productIdentifier, monthlyPeriod, monthlyBasePlanId)
        val annualProduct = getStoreProduct(productIdentifier, annualPeriod, annualBasePlanId)

        val monthlyProduct2 = getStoreProduct(productId2, monthlyPeriod, monthlyBasePlanId)

        val products = mapOf(
            productIdentifier to listOf(monthlyProduct, annualProduct),
            productId2 to listOf(monthlyProduct2)
        )

        val offeringID = "offering_a"
        val offeringID2 = "offering_b"

        val offerings = offeringsParser.createOfferings(
            getOfferingsJSON(
                offeringID,
                mapOf(
                    offeringID to listOf(
                        getPackageJSON(monthlyPackageID, productIdentifier),
                        getPackageJSON(annualPackageID, productIdentifier)
                    ),
                    offeringID2 to listOf(
                        getPackageJSON(monthlyPackageID, productId2)
                    )
                )
            ),
            products
        )

        val offering1 = offerings[offeringID]
        val offering1Packages = offering1!!.availablePackages
        val offering1MonthlyPackages = offering1Packages.filter { it.packageType == PackageType.MONTHLY }
        val offering1YearlyPackages = offering1Packages.filter { it.packageType == PackageType.ANNUAL }

        assertThat(offering1).isNotNull
        assertThat(offering1.identifier).isEqualTo(offeringID)
        assertThat(offering1Packages.size).isEqualTo(2)
        assertThat(offering1MonthlyPackages.size).isEqualTo(1)
        assertThat(offering1YearlyPackages.size).isEqualTo(1)
        assertThat(offering1MonthlyPackages.filter { it.identifier == monthlyPackageID }).isNotNull
        assertThat(offering1YearlyPackages.filter { it.identifier == annualPackageID }).isNotNull

        val offering2 = offerings[offeringID2]
        val offering2Packages = offering2!!.availablePackages
        val offering2MonthlyPackages = offering2Packages.filter { it.packageType == PackageType.MONTHLY }
        val offering2YearlyPackages = offering2Packages.filter { it.packageType == PackageType.ANNUAL }

        assertThat(offering2).isNotNull
        assertThat(offering2.identifier).isEqualTo(offeringID2)
        assertThat(offering2Packages.size).isEqualTo(1)
        assertThat(offering2MonthlyPackages.size).isEqualTo(1)
        assertThat(offering2YearlyPackages.size).isEqualTo(0)
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
            ),
            null
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

    private fun getOfferingsJSON(
        currentOfferingId: String = "offering_a",
        offeringPackagesById: Map<String, List<JSONObject>> =
            mapOf(
                "offering_a" to listOf(
                    getPackageJSON(
                        annualPackageID,
                        productIdentifier,
                        annualBasePlanId
                    )
                ),
                "offering_b" to listOf(
                    getPackageJSON(
                        monthlyPackageID,
                        this.productIdentifier,
                        monthlyBasePlanId
                    )
                )
            )
    ): JSONObject {
        val offeringJsons = mutableListOf<JSONObject>()
        offeringPackagesById.forEach { (offeringId, packages) ->
            offeringJsons.add(
                getOfferingJSON(
                    offeringId,
                    packages,
                    null
                )
            )
        }

        val offeringsJsonArray = JSONArray(offeringJsons)

        return JSONObject().apply {
            put("offerings", offeringsJsonArray)
            put("current_offering_id", currentOfferingId)
        }
    }

    private fun getOfferingJSON(
        offeringIdentifier: String = "offering_a",
        packagesJSON: List<JSONObject> = listOf(
            getPackageJSON(
                monthlyPackageID,
                this.productIdentifier,
                monthlyBasePlanId
            ),
        ),
        metadata: Map<String, Any>? = null
    ) = JSONObject(
        """
            {
                'description': 'This is the base offering',
                'identifier': '$offeringIdentifier',
                'packages': $packagesJSON,
                'metadata': ${if (metadata != null) JSONObject(metadata).toString() else "null"}
            }
        """.trimIndent()
    )

    private fun getOfferingJSONWithoutMetadata(
        offeringIdentifier: String = "offering_a",
        packagesJSON: List<JSONObject> = listOf(
            getPackageJSON(
                monthlyPackageID,
                this.productIdentifier,
                monthlyBasePlanId
            ),
        ),
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
        packageIdentifier: String = monthlyPackageID,
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

    private fun getStoreProduct(
        productId: String = productIdentifier,
        period: Period = monthlyPeriod,
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
