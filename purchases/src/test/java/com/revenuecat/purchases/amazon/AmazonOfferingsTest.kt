//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.amazon

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.OfferingParserFactory
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.amazon.helpers.dummyAmazonProduct
import com.revenuecat.purchases.amazon.helpers.stubStoreProductForAmazon
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.utils.getAmazonPackageJSON
import com.revenuecat.purchases.utils.getLifetimePackageJSON
import com.revenuecat.purchases.utils.stubINAPPStoreProduct
import org.assertj.core.api.Assertions
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class AmazonOfferingsTest {

    private val productSku = "com.myproduct"
    private val monthlyTermSku = "$productSku.monthly"
    private val annualTermSku = "$productSku.annual"
    private val inAppProductIdentifier = "com.myproduct.lifetime"
    private val monthlyPeriod = Period.create("P1M")
    private val annualPeriod = Period.create("P1Y")
    private val monthlyPackageID = PackageType.MONTHLY.identifier!!
    private val annualPackageID = PackageType.ANNUAL.identifier!!

    private val storeProductMonthly = stubStoreProductForAmazon(monthlyTermSku, period = monthlyPeriod)
    private  val storeProductAnnual = stubStoreProductForAmazon(annualTermSku, period = annualPeriod)

    private val offeringsParser = OfferingParserFactory.createOfferingParser(Store.AMAZON)

    @Test
    fun `createPackage returns null if packageJson productIdentifier doesnt match any sub StoreProduct id`() {
        val storeProductAnnual = dummyAmazonProduct(annualTermSku).toStoreProduct("US")
        val storeProductMap = mapOf(annualTermSku to listOf(storeProductAnnual!!))

        val packageWithMonthlyProduct = getAmazonPackageJSON(
            packageIdentifier = monthlyPackageID,
            productIdentifier = monthlyTermSku
        )
        val packageToTest = offeringsParser.createPackage(
            packageWithMonthlyProduct,
            storeProductMap,
            PresentedOfferingContext("offering"),
        )
        Assertions.assertThat(packageToTest).isNull()
    }

    @Test
    fun `createPackage for sub sets presentedOfferingId on Package and product`() {
        val storeProductMonthly = dummyAmazonProduct(monthlyTermSku).toStoreProduct("US")
        val storeProductMap = mapOf(monthlyTermSku to listOf(storeProductMonthly!!))

        val packageWithMonthlyProduct = getAmazonPackageJSON(
            packageIdentifier = monthlyPackageID,
            productIdentifier = monthlyTermSku
        )

        val expectedOfferingIdentifier = "offering"
        val monthlyPackageToTest = offeringsParser.createPackage(
            packageWithMonthlyProduct,
            storeProductMap,
            PresentedOfferingContext(expectedOfferingIdentifier),
        )!!

        Assertions.assertThat(monthlyPackageToTest.offering).isEqualTo(expectedOfferingIdentifier)
        Assertions.assertThat(monthlyPackageToTest.presentedOfferingContext.offeringIdentifier).isEqualTo(expectedOfferingIdentifier)

        val packageProduct = monthlyPackageToTest.product
        Assertions.assertThat(packageProduct.presentedOfferingIdentifier).isEqualTo(expectedOfferingIdentifier)
        Assertions.assertThat(packageProduct.presentedOfferingContext.offeringIdentifier).isEqualTo(expectedOfferingIdentifier)

        Assertions.assertThat(packageProduct.defaultOption).isNull()
        Assertions.assertThat(packageProduct.subscriptionOptions).isNull()
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
            PresentedOfferingContext(expectedOfferingIdentifier)
        )

        Assertions.assertThat(inAppPackageToTest!!.offering).isEqualTo(expectedOfferingIdentifier)
        Assertions.assertThat(inAppPackageToTest!!.presentedOfferingContext.offeringIdentifier).isEqualTo(expectedOfferingIdentifier)

        val packageProduct = inAppPackageToTest!!.product
        Assertions.assertThat(packageProduct.presentedOfferingIdentifier).isEqualTo(expectedOfferingIdentifier)
        Assertions.assertThat(packageProduct.presentedOfferingContext.offeringIdentifier).isEqualTo(expectedOfferingIdentifier)
    }

    @Test
    fun `createPackage creates a Package if package json matches subscription store products`() {
        val monthlyPackageJSON = getAmazonPackageJSON(
            packageIdentifier = PackageType.MONTHLY.identifier!!,
            productIdentifier = storeProductMonthly.id,
        )
        val productsMap = mapOf(
            monthlyTermSku to listOf(storeProductMonthly),
            annualTermSku to listOf(storeProductAnnual)
        )
        val monthlyPackageToTest = offeringsParser.createPackage(
            monthlyPackageJSON,
            productsMap,
            PresentedOfferingContext("offering")
        )
        Assertions.assertThat(monthlyPackageToTest).isNotNull
        Assertions.assertThat(monthlyPackageToTest!!.product).usingRecursiveComparison()
            .isEqualTo(productsMap[monthlyTermSku]?.get(0))
        Assertions.assertThat(monthlyPackageToTest.identifier).isEqualTo(PackageType.MONTHLY.identifier)
        Assertions.assertThat(monthlyPackageToTest.packageType).isEqualTo(PackageType.MONTHLY)

        val annualPackageJSON = getAmazonPackageJSON(
            packageIdentifier = annualPackageID,
            productIdentifier = storeProductAnnual.id
        )
        val annualPackageToTest = offeringsParser.createPackage(annualPackageJSON, productsMap, PresentedOfferingContext("offering"))
        Assertions.assertThat(annualPackageToTest).isNotNull
        Assertions.assertThat(annualPackageToTest!!.product).usingRecursiveComparison()
            .isEqualTo(productsMap[storeProductAnnual.id]?.get(0))
        Assertions.assertThat(annualPackageToTest.identifier).isEqualTo(annualPackageID)
        Assertions.assertThat(annualPackageToTest.packageType).isEqualTo(PackageType.ANNUAL)
    }

    fun `createPackage creates a Package if packageJson matches INAPP StoreProduct`() {
        val storeProductInApp = stubINAPPStoreProduct(inAppProductIdentifier)

        val inAppProductMap = mapOf(inAppProductIdentifier to listOf(storeProductInApp))

        val inAppPackageJson = getLifetimePackageJSON()
        val inAppPackageToTest = offeringsParser.createPackage(
            inAppPackageJson,
            inAppProductMap,
            PresentedOfferingContext("offering"),
        )
        Assertions.assertThat(inAppPackageToTest).isNotNull
        Assertions.assertThat(inAppPackageToTest!!.product).isEqualTo(inAppProductMap[inAppProductIdentifier]?.get(0))
        Assertions.assertThat(inAppPackageToTest.identifier).isEqualTo(PackageType.LIFETIME.identifier)
        Assertions.assertThat(inAppPackageToTest.packageType).isEqualTo(PackageType.LIFETIME)
    }

    @Test
    fun `createOffering returns null if offeringJson contains no packages matching StoreProducts`() {
        val productId = "com.myproduct.bad"
        val products = mapOf(productId to listOf(storeProductAnnual))

        val offeringWithOneMonthlyPackageJson = getAmazonOfferingJSON()
        val offering = offeringsParser.createOffering(offeringWithOneMonthlyPackageJson, products)
        Assertions.assertThat(offering).isNull()
    }

    @Test
    fun `createOffering returns Offering with offeringJson packages that match StoreProducts`() {
        val products = mapOf(
            monthlyTermSku to listOf(storeProductMonthly),
            annualTermSku to listOf(storeProductAnnual)
        )

        val monthlyPackageID = PackageType.MONTHLY.identifier!!
        val monthlyPackageJSON = getAmazonPackageJSON(
            monthlyPackageID,
            storeProductMonthly.id
        )

        val annualPackageID = annualPackageID
        val annualPackageJSON = getAmazonPackageJSON(
            annualPackageID,
            storeProductAnnual.id
        )
        val offeringId = "offering_a"
        val offeringJSON = getAmazonOfferingJSON(
            offeringIdentifier = offeringId,
            packagesJSON = listOf(monthlyPackageJSON, annualPackageJSON)
        )

        val offering = offeringsParser.createOffering(offeringJSON, products)
        Assertions.assertThat(offering).isNotNull
        Assertions.assertThat(offering!!.identifier).isEqualTo(offeringId)

        val packages = offering.availablePackages
        Assertions.assertThat(packages.size).isEqualTo(2)

        val monthlyPackages = packages.filter { it.packageType == PackageType.MONTHLY }
        val yearlyPackages = packages.filter { it.packageType == PackageType.ANNUAL }
        Assertions.assertThat(monthlyPackages.size).isEqualTo(1)
        Assertions.assertThat(yearlyPackages.size).isEqualTo(1)

        Assertions.assertThat(monthlyPackages.filter { it.identifier == monthlyPackageID }.size).isEqualTo(1)
        Assertions.assertThat(yearlyPackages.filter { it.identifier == annualPackageID }.size).isEqualTo(1)
    }

    @Test
    fun `createOfferings returns empty list if offeringsJson contains no matching StoreProducts`() {
        val productId = "com.myproduct.bad"
        val products = mapOf(productId to listOf(storeProductAnnual))

        val offerings = offeringsParser.createOfferings(getAmazonOfferingsJSON(), products)
        Assertions.assertThat(offerings).isNotNull
        Assertions.assertThat(offerings.current).isNull()
        Assertions.assertThat(offerings["offering_a"]).isNull()
        Assertions.assertThat(offerings["offering_b"]).isNull()
    }

    @Test
    fun `createOfferings creates offerings`() {
        val products = mapOf(
            monthlyTermSku to listOf(storeProductMonthly),
            annualTermSku to listOf(storeProductAnnual)
        )
        val offeringsJson = getAmazonOfferingsJSON()

        val offerings = offeringsParser.createOfferings(offeringsJson, products)
        Assertions.assertThat(offerings).isNotNull
        Assertions.assertThat(offerings.all.size).isEqualTo(2)
        Assertions.assertThat(offerings.current!!.identifier).isEqualTo(offeringsJson.getString("current_offering_id"))
        Assertions.assertThat(offerings["offering_a"]).isNotNull
        Assertions.assertThat(offerings["offering_b"]).isNotNull
    }

    @Test
    fun `createOfferings does not set currentOffering if current_offering_id is null`() {
        val products = mapOf(
            monthlyTermSku to listOf(storeProductMonthly),
            annualTermSku to listOf(storeProductAnnual)
        )
        val offeringsJson = getAmazonOfferingsJSON("")

        val offerings = offeringsParser.createOfferings(offeringsJson, products)
        Assertions.assertThat(offerings).isNotNull
        Assertions.assertThat(offerings.all.size).isEqualTo(2)
        Assertions.assertThat(offerings.current).isNull()
        Assertions.assertThat(offerings["offering_a"]).isNotNull
        Assertions.assertThat(offerings["offering_b"]).isNotNull
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

        Assertions.assertThat(offerings).isNotNull
        Assertions.assertThat(offerings.all.size).isEqualTo(0)
        Assertions.assertThat(offerings.current).isNull()
    }

    @Test
    fun `createOfferings returns empty list if no StoreProducts fetched`() {
        val offerings =
            offeringsParser.createOfferings(
                JSONObject("{'offerings': [], 'current_offering_id': 'offering_with_broken_product'}"),
                emptyMap()
            )

        Assertions.assertThat(offerings).isNotNull
        Assertions.assertThat(offerings.all.size).isEqualTo(0)
        Assertions.assertThat(offerings.current).isNull()
    }

    @Test
    fun `Can create multiple offerings with packages of the same duration`() {
        val monthlyTermSku2 = monthlyTermSku + "2"
        val monthlyProduct2 = stubStoreProductForAmazon(monthlyTermSku2, period = monthlyPeriod)

        val products = mapOf(
            monthlyTermSku to listOf(storeProductMonthly),
            annualTermSku to listOf(storeProductAnnual),
            monthlyTermSku2 to listOf(monthlyProduct2)
        )

        val offeringID = "offering_a"
        val offeringID2 = "offering_b"

        val offerings = offeringsParser.createOfferings(
            getAmazonOfferingsJSON(
                offeringID,
                mapOf(
                    offeringID to listOf(
                        getAmazonPackageJSON(monthlyPackageID, monthlyTermSku),
                        getAmazonPackageJSON(annualPackageID, annualTermSku)
                    ),
                    offeringID2 to listOf(
                        getAmazonPackageJSON(monthlyPackageID, monthlyTermSku2)
                    )
                )
            ),
            products
        )

        val offering1 = offerings[offeringID]
        val offering1Packages = offering1!!.availablePackages
        val offering1MonthlyPackages = offering1Packages.filter { it.packageType == PackageType.MONTHLY }
        val offering1YearlyPackages = offering1Packages.filter { it.packageType == PackageType.ANNUAL }

        Assertions.assertThat(offering1).isNotNull
        Assertions.assertThat(offering1.identifier).isEqualTo(offeringID)
        Assertions.assertThat(offering1Packages.size).isEqualTo(2)
        Assertions.assertThat(offering1MonthlyPackages.size).isEqualTo(1)
        Assertions.assertThat(offering1YearlyPackages.size).isEqualTo(1)
        Assertions.assertThat(offering1MonthlyPackages.filter { it.identifier == monthlyPackageID }).isNotNull
        Assertions.assertThat(offering1YearlyPackages.filter { it.identifier == annualPackageID }).isNotNull

        val offering2 = offerings[offeringID2]
        val offering2Packages = offering2!!.availablePackages
        val offering2MonthlyPackages = offering2Packages.filter { it.packageType == PackageType.MONTHLY }
        val offering2YearlyPackages = offering2Packages.filter { it.packageType == PackageType.ANNUAL }

        Assertions.assertThat(offering2).isNotNull
        Assertions.assertThat(offering2.identifier).isEqualTo(offeringID2)
        Assertions.assertThat(offering2Packages.size).isEqualTo(1)
        Assertions.assertThat(offering2MonthlyPackages.size).isEqualTo(1)
        Assertions.assertThat(offering2YearlyPackages.size).isEqualTo(0)
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
        val products = mapOf(monthlyTermSku to listOf(storeProductMonthly))
        val offeringJSON = getAmazonOfferingJSON(
            offeringIdentifier = "offering_a",
            listOf(
                getAmazonPackageJSON(packageIdentifier = identifier)
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

        Assertions.assertThat(offerings).isNotNull
        Assertions.assertThat(offerings.current).isNotNull
        assertPackage(packageType, PackageType.LIFETIME, offerings.current!!.lifetime)
        assertPackage(packageType, PackageType.ANNUAL, offerings.current!!.annual)
        assertPackage(packageType, PackageType.SIX_MONTH, offerings.current!!.sixMonth)
        assertPackage(packageType, PackageType.THREE_MONTH, offerings.current!!.threeMonth)
        assertPackage(packageType, PackageType.TWO_MONTH, offerings.current!!.twoMonth)
        assertPackage(packageType, PackageType.MONTHLY, offerings.current!!.monthly)
        assertPackage(packageType, PackageType.WEEKLY, offerings.current!!.weekly)

        Assertions.assertThat(offerings["offering_a"]!!.getPackage(identifier).packageType).isEqualTo(packageType)
    }

    private fun assertPackage(
        packageType: PackageType,
        expectedPackageType: PackageType,
        packageToCheck: Package?
    ) {
        if (packageType == expectedPackageType) {
            Assertions.assertThat(packageToCheck).isNotNull
        } else {
            Assertions.assertThat(packageToCheck).isNull()
        }
    }
    private fun getAmazonOfferingsJSON(
        currentOfferingId: String = "offering_a",
        offeringPackagesById: Map<String, List<JSONObject>> =
            mapOf(
                "offering_a" to listOf(
                    getAmazonPackageJSON(
                        annualPackageID,
                        annualTermSku
                    )
                ),
                "offering_b" to listOf(
                    getAmazonPackageJSON(
                        monthlyPackageID,
                        monthlyTermSku
                    )
                )
            )
    ): JSONObject {
        val offeringJsons = mutableListOf<JSONObject>()
        offeringPackagesById.forEach { (offeringId, packages) ->
            offeringJsons.add(
                getAmazonOfferingJSON(
                    offeringId,
                    packages
                )
            )
        }

        val offeringsJsonArray = JSONArray(offeringJsons)

        return JSONObject().apply {
            put("offerings", offeringsJsonArray)
            put("current_offering_id", currentOfferingId)
        }
    }

    private fun getAmazonOfferingJSON(
        offeringIdentifier: String = "offering_a",
        packagesJSON: List<JSONObject> = listOf(
            getAmazonPackageJSON(
                monthlyPackageID,
                monthlyTermSku
            ),
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
}
