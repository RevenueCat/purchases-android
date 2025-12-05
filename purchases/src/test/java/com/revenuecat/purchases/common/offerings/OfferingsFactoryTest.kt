package com.revenuecat.purchases.common.offerings

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.APIKeyValidator
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.GoogleOfferingParser
import com.revenuecat.purchases.common.HTTPResponseOriginalSource
import com.revenuecat.purchases.common.OfferingParser
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.strings.OfferingStrings
import com.revenuecat.purchases.utils.ONE_OFFERINGS_INAPP_PRODUCT_RESPONSE
import com.revenuecat.purchases.utils.ONE_OFFERINGS_RESPONSE
import com.revenuecat.purchases.utils.STUB_OFFERING_IDENTIFIER
import com.revenuecat.purchases.utils.STUB_PRODUCT_IDENTIFIER
import com.revenuecat.purchases.utils.SyncDispatcher
import com.revenuecat.purchases.utils.stubINAPPStoreProduct
import com.revenuecat.purchases.utils.stubStoreProduct
import com.revenuecat.purchases.utils.stubSubscriptionOption
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.net.URL

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class OfferingsFactoryTest {

    // language=JSON
    private val oneOfferingWithNoProductsResponse = JSONObject(
        """
        {
            "offerings": [
                {
                    "identifier": "$STUB_OFFERING_IDENTIFIER",
                    "description": "This is the base offering",
                    "packages": []
                }
            ],
            "current_offering_id": "$STUB_OFFERING_IDENTIFIER"
        }
        """.trimIndent()
    )
    // language=JSON
    private val oneOfferingWithInvalidPaywallResponse = JSONObject(
        """
        {
            "offerings": [
                {
                    "identifier": "$STUB_OFFERING_IDENTIFIER",
                    "description": "This is the base offering",
                    "packages": [
                        {
                            "identifier": "${'$'}rc_monthly",
                            "platform_product_identifier": "$STUB_PRODUCT_IDENTIFIER"
                        }
                    ],
                    "paywall": "not a paywall"
                }
            ],
            "current_offering_id": "$STUB_OFFERING_IDENTIFIER"
        }
        """.trimIndent()
    )
    // language=JSON
    private val oneOfferingWithPaywall = JSONObject(
        """
        {
            "offerings": [
                {
                    "identifier": "$STUB_OFFERING_IDENTIFIER",
                    "description": "This is the base offering",
                    "packages": [
                        {
                            "identifier": "${'$'}rc_monthly",
                            "platform_product_identifier": "$STUB_PRODUCT_IDENTIFIER"
                        }
                    ],
                    "paywall": {
                        "template_name": "1",
                        "localized_strings": {
                            "en_US": {
                                "title": "Paywall",
                                "call_to_action": "Purchase",
                                "subtitle": "Description"
                            }
                        },
                        "config": {
                            "packages": ["${'$'}rc_monthly"],
                            "default_package": "${'$'}rc_monthly",
                            "images": {},
                            "colors": {
                                "light": {
                                    "background": "#FF00AA",
                                    "text_1": "#FF00AA22",
                                    "call_to_action_background": "#FF00AACC",
                                    "call_to_action_foreground": "#FF00AA"
                                }
                            }
                        },
                        "asset_base_url": "https://rc-paywalls.s3.amazonaws.com",
                        "zero_decimal_place_countries": {
                            "apple": ["TWA", "THA", "PHL", "MEX", "KAZ"],
                            "google": ["PH", "KZ", "TW", "MX", "TH"]
                        },
                        "revision": 7
                    }
                }
            ],
            "current_offering_id": "$STUB_OFFERING_IDENTIFIER"
        }
        """.trimIndent()
    )
    // language=JSON
    private val oneOfferingWithPlacement = JSONObject(
        """
        {
            "offerings": [
                {
                    "identifier": "$STUB_OFFERING_IDENTIFIER",
                    "description": "This is the base offering",
                    "packages": [
                        {
                            "identifier": "${'$'}rc_monthly",
                            "platform_product_identifier": "$STUB_PRODUCT_IDENTIFIER"
                        }
                    ]
                }
            ],
            "current_offering_id": "$STUB_OFFERING_IDENTIFIER",
            "placements": {
                "fallback_offering_id": "standard",
                "offering_ids_by_placement": {
                    "onboarding": null,
                    "gate": "big_feature"
                }
            }
        }
        """.trimIndent()
    )
    // language=JSON
    private val oneOfferingWithPlacementWithNullFallback = JSONObject(
        """
        {
            "offerings": [
                {
                    "identifier": "$STUB_OFFERING_IDENTIFIER",
                    "description": "This is the base offering",
                    "packages": [
                        {
                            "identifier": "${'$'}rc_monthly",
                            "platform_product_identifier": "$STUB_PRODUCT_IDENTIFIER"
                        }
                    ]
                }
            ],
            "current_offering_id": "$STUB_OFFERING_IDENTIFIER",
            "placements": {
                "fallback_offering_id": null,
                "offering_ids_by_placement": {
                    "onboarding": null,
                    "gate": "big_feature"
                }
            }
        }
        """.trimIndent()
    )
    // language=JSON
    private val oneOfferingWithTargeting = JSONObject(
        """
        {
            "offerings": [
                {
                    "identifier": "$STUB_OFFERING_IDENTIFIER",
                    "description": "This is the base offering",
                    "packages": [
                        {
                            "identifier": "${'$'}rc_monthly",
                            "platform_product_identifier": "$STUB_PRODUCT_IDENTIFIER"
                        }
                    ]
                }
            ],
            "current_offering_id": "$STUB_OFFERING_IDENTIFIER",
            "targeting": {
                "revision": 1,
                "rule_id": "abc123"
            }
        }
        """.trimIndent()
    )
    // language=JSON
    private val oneOfferingWithWPL = JSONObject(
        """
        {
            "offerings": [
                {
                    "identifier": "$STUB_OFFERING_IDENTIFIER",
                    "description": "This is the base offering",
                    "packages": [
                        {
                            "identifier": "${'$'}rc_monthly",
                            "platform_product_identifier": "$STUB_PRODUCT_IDENTIFIER",
                            "web_checkout_url": "http://revenuecat.com?package_id=${'$'}rc_monthly"
                        }
                    ],
                    "web_checkout_url": "http://revenuecat.com"
                }
            ],
            "current_offering_id": "$STUB_OFFERING_IDENTIFIER",
            "targeting": {
                "revision": 1,
                "rule_id": "abc123"
            }
        }
        """.trimIndent()
    )

    private val oneOfferingResponse = JSONObject(ONE_OFFERINGS_RESPONSE)
    private val oneOfferingInAppProductResponse = JSONObject(ONE_OFFERINGS_INAPP_PRODUCT_RESPONSE)

    private val productId = STUB_PRODUCT_IDENTIFIER

    private lateinit var appConfig: AppConfig
    private lateinit var billing: BillingAbstract
    private lateinit var offeringParser: OfferingParser
    private lateinit var dispatcher: Dispatcher
    private lateinit var offeringsFactory: OfferingsFactory

    @Before
    fun setUp() {
        appConfig = mockk<AppConfig>().apply {
            every { store } returns Store.PLAY_STORE
            every { apiKeyValidationResult } returns APIKeyValidator.ValidationResult.VALID
        }
        billing = mockk()
        offeringParser = GoogleOfferingParser()
        dispatcher = SyncDispatcher()
        offeringsFactory = OfferingsFactory(
            billing,
            offeringParser,
            dispatcher,
            appConfig,
        )
    }

    @Test
    fun `configuration error if no products configured in Play Store`() {
        every { appConfig.apiKeyValidationResult } returns APIKeyValidator.ValidationResult.VALID
        every { appConfig.store } returns Store.PLAY_STORE
        var purchasesError: PurchasesError? = null
        offeringsFactory.createOfferings(
            offeringsJSON = oneOfferingWithNoProductsResponse,
            originalDataSource = HTTPResponseOriginalSource.MAIN,
            loadedFromDiskCache = false,
            onError = { purchasesError = it },
            onSuccess = { fail("Expected error") }
        )
        assertThat(purchasesError).isNotNull
        assertThat(purchasesError!!.code).isEqualTo(PurchasesErrorCode.ConfigurationError)
        assertThat(purchasesError!!.underlyingErrorMessage).contains("Play Store")
        assertThat(purchasesError!!.underlyingErrorMessage).contains("a Play Store API key")
        assertThat(purchasesError!!.underlyingErrorMessage).contains("https://rev.cat/how-to-configure-offerings")
        assertThat(purchasesError!!.underlyingErrorMessage).contains("https://rev.cat/why-are-offerings-empty")
    }

    @Test
    fun `configuration error if no products configured with legacy API key`() {
        every { appConfig.apiKeyValidationResult } returns APIKeyValidator.ValidationResult.LEGACY
        every { appConfig.store } returns Store.PLAY_STORE
        offeringsFactory = OfferingsFactory(
            billing,
            offeringParser,
            dispatcher,
            appConfig,
        )

        var purchasesError: PurchasesError? = null
        offeringsFactory.createOfferings(
            offeringsJSON = oneOfferingWithNoProductsResponse,
            originalDataSource = HTTPResponseOriginalSource.MAIN,
            loadedFromDiskCache = false,
            onError = { purchasesError = it },
            onSuccess = { fail("Expected error") }
        )
        assertThat(purchasesError).isNotNull
        assertThat(purchasesError!!.code).isEqualTo(PurchasesErrorCode.ConfigurationError)
        assertThat(purchasesError!!.underlyingErrorMessage).contains("Play Store")
        assertThat(purchasesError!!.underlyingErrorMessage).contains("a Play Store API key")
        assertThat(purchasesError!!.underlyingErrorMessage).contains("https://rev.cat/how-to-configure-offerings")
        assertThat(purchasesError!!.underlyingErrorMessage).contains("https://rev.cat/why-are-offerings-empty")
    }

    @Test
    fun `configuration error if no products configured with Amazon store`() {
        every { appConfig.apiKeyValidationResult } returns APIKeyValidator.ValidationResult.VALID
        every { appConfig.store } returns Store.AMAZON
        offeringsFactory = OfferingsFactory(
            billing,
            offeringParser,
            dispatcher,
            appConfig,
        )

        var purchasesError: PurchasesError? = null
        offeringsFactory.createOfferings(
            offeringsJSON = oneOfferingWithNoProductsResponse,
            originalDataSource = HTTPResponseOriginalSource.MAIN,
            loadedFromDiskCache = false,
            onError = { purchasesError = it },
            onSuccess = { fail("Expected error") }
        )
        assertThat(purchasesError).isNotNull
        assertThat(purchasesError!!.code).isEqualTo(PurchasesErrorCode.ConfigurationError)
        assertThat(purchasesError!!.underlyingErrorMessage).contains("Amazon Appstore")
        assertThat(purchasesError!!.underlyingErrorMessage).contains("an Amazon Appstore API key")
        assertThat(purchasesError!!.underlyingErrorMessage).contains("https://rev.cat/how-to-configure-offerings")
        assertThat(purchasesError!!.underlyingErrorMessage).contains("https://rev.cat/why-are-offerings-empty")
    }

    @Test
    fun `configuration error if no products configured with simulated store`() {
        every { appConfig.apiKeyValidationResult } returns APIKeyValidator.ValidationResult.SIMULATED_STORE
        every { appConfig.store } returns Store.TEST_STORE
        offeringsFactory = OfferingsFactory(
            billing,
            offeringParser,
            dispatcher,
            appConfig,
        )

        var purchasesError: PurchasesError? = null
        offeringsFactory.createOfferings(
            offeringsJSON = oneOfferingWithNoProductsResponse,
            originalDataSource = HTTPResponseOriginalSource.MAIN,
            loadedFromDiskCache = false,
            onError = { purchasesError = it },
            onSuccess = { fail("Expected error") }
        )
        assertThat(purchasesError).isNotNull
        assertThat(purchasesError!!.code).isEqualTo(PurchasesErrorCode.ConfigurationError)
        assertThat(purchasesError!!.underlyingErrorMessage).contains("Test Store")
        assertThat(purchasesError!!.underlyingErrorMessage).contains("a Test Store API key")
        assertThat(purchasesError!!.underlyingErrorMessage).contains("https://rev.cat/how-to-configure-offerings")
        assertThat(purchasesError!!.underlyingErrorMessage).contains("https://rev.cat/why-are-offerings-empty")
    }

    @Test
    fun `configuration error if no products configured with other platform`() {
        every { appConfig.apiKeyValidationResult } returns APIKeyValidator.ValidationResult.OTHER_PLATFORM
        every { appConfig.store } returns Store.APP_STORE
        offeringsFactory = OfferingsFactory(
            billing,
            offeringParser,
            dispatcher,
            appConfig,
        )

        var purchasesError: PurchasesError? = null
        offeringsFactory.createOfferings(
            offeringsJSON = oneOfferingWithNoProductsResponse,
            originalDataSource = HTTPResponseOriginalSource.MAIN,
            loadedFromDiskCache = false,
            onError = { purchasesError = it },
            onSuccess = { fail("Expected error") }
        )
        assertThat(purchasesError).isNotNull
        assertThat(purchasesError!!.code).isEqualTo(PurchasesErrorCode.ConfigurationError)
        assertThat(purchasesError!!.underlyingErrorMessage).contains("an API key from a store that has no products")
        assertThat(purchasesError!!.underlyingErrorMessage).doesNotContain("Play Store")
        assertThat(purchasesError!!.underlyingErrorMessage).doesNotContain("Amazon Appstore")
        assertThat(purchasesError!!.underlyingErrorMessage).doesNotContain("Test Store")
        assertThat(purchasesError!!.underlyingErrorMessage).contains("https://rev.cat/how-to-configure-offerings")
        assertThat(purchasesError!!.underlyingErrorMessage).contains("https://rev.cat/why-are-offerings-empty")
    }

    @Test
    fun `createOfferings returns error if json with wrong format`() {
        var purchasesError: PurchasesError? = null
        offeringsFactory.createOfferings(
            offeringsJSON = JSONObject("{}"),
            originalDataSource = HTTPResponseOriginalSource.MAIN,
            loadedFromDiskCache = false,
            onError = { purchasesError = it },
            onSuccess = { fail("Expected error") }
        )
        assertThat(purchasesError).isNotNull
        assertThat(purchasesError!!.code).isEqualTo(PurchasesErrorCode.UnexpectedBackendResponseError)
    }

    @Test
    fun `configuration error if products are not set up when fetching offerings`() {
        val productIds = listOf(productId)
        mockStoreProduct(productIds, listOf(), ProductType.SUBS)
        mockStoreProduct(productIds, listOf(), ProductType.INAPP)

        var purchasesError: PurchasesError? = null
        offeringsFactory.createOfferings(
            offeringsJSON = oneOfferingResponse,
            originalDataSource = HTTPResponseOriginalSource.MAIN,
            loadedFromDiskCache = false,
            onError = { purchasesError = it },
            onSuccess = { fail("Expected error") }
        )

        assertThat(purchasesError).isNotNull
        assertThat(purchasesError!!.code).isEqualTo(PurchasesErrorCode.ConfigurationError)
        assertThat(purchasesError!!.underlyingErrorMessage).contains(
            OfferingStrings.CONFIGURATION_ERROR_PRODUCTS_NOT_FOUND
        )
    }

    @Test
    fun `returns offerings when products found as subs`() {
        val productIds = listOf(productId)
        mockStoreProduct(productIds, productIds, ProductType.SUBS)

        var offerings: Offerings? = null
        offeringsFactory.createOfferings(
            offeringsJSON = oneOfferingResponse,
            originalDataSource = HTTPResponseOriginalSource.MAIN,
            loadedFromDiskCache = false,
            onError = { fail("Expected success. Got error: $it") },
            onSuccess = { offerings = it.offerings }
        )

        assertThat(offerings).isNotNull
        assertThat(offerings!!.all.size).isEqualTo(1)
        assertThat(offerings!![STUB_OFFERING_IDENTIFIER]!!.monthly!!.product).isNotNull
    }

    @Test
    fun `returns offerings when products found as inapp`() {
        val productIds = listOf(productId)
        mockStoreProduct(productIds, emptyList(), ProductType.SUBS)
        mockStoreProduct(productIds, productIds, ProductType.INAPP)

        var offerings: Offerings? = null
        offeringsFactory.createOfferings(
            offeringsJSON = oneOfferingInAppProductResponse,
            originalDataSource = HTTPResponseOriginalSource.MAIN,
            loadedFromDiskCache = false,
            onError = { fail("Expected success. Got error: $it") },
            onSuccess = { offerings = it.offerings }
        )

        assertThat(offerings).isNotNull
        assertThat(offerings!!.all.size).isEqualTo(1)
        assertThat(offerings!![STUB_OFFERING_IDENTIFIER]!!.monthly!!.product).isNotNull
    }

    @Test
    fun `createOfferings with paywall`() {
        val productIds = listOf(productId)
        mockStoreProduct(productIds, emptyList(), ProductType.SUBS)
        mockStoreProduct(productIds, productIds, ProductType.INAPP)

        var offerings: Offerings? = null
        offeringsFactory.createOfferings(
            offeringsJSON = oneOfferingWithPaywall,
            originalDataSource = HTTPResponseOriginalSource.MAIN,
            loadedFromDiskCache = false,
            onError = { fail("Error: $it") },
            onSuccess = { offerings = it.offerings }
        )

        assertThat(offerings).isNotNull
        assertThat(offerings!!.current).isNotNull
        assertThat(offerings!!.current?.paywall).isNotNull
    }

    @Test
    fun `createOfferings does not fail if paywall is invalid`() {
        val productIds = listOf(productId)
        mockStoreProduct(productIds, emptyList(), ProductType.SUBS)
        mockStoreProduct(productIds, productIds, ProductType.INAPP)

        var offerings: Offerings? = null
        offeringsFactory.createOfferings(
            offeringsJSON = oneOfferingWithInvalidPaywallResponse,
            originalDataSource = HTTPResponseOriginalSource.MAIN,
            loadedFromDiskCache = false,
            onError = { fail("Error: $it") },
            onSuccess = { offerings = it.offerings }
        )

        assertThat(offerings).isNotNull
        assertThat(offerings!!.current).isNotNull
        assertThat(offerings!!.current?.paywall).isNull()
    }

    @Test
    fun `createOfferings with placement`() {
        val productIds = listOf(productId)
        mockStoreProduct(productIds, emptyList(), ProductType.SUBS)
        mockStoreProduct(productIds, productIds, ProductType.INAPP)

        var offerings: Offerings? = null
        offeringsFactory.createOfferings(
            offeringsJSON = oneOfferingWithPlacement,
            originalDataSource = HTTPResponseOriginalSource.MAIN,
            loadedFromDiskCache = false,
            onError = { fail("Error: $it") },
            onSuccess = { offerings = it.offerings }
        )

        assertThat(offerings).isNotNull
        assertThat(offerings!!.current).isNotNull
        assertThat(offerings!!.placements).isNotNull
        assertThat(offerings!!.placements!!.fallbackOfferingId).isEqualTo("standard")
        assertThat(offerings!!.placements!!.offeringIdsByPlacement).isEqualTo(mapOf(
            "onboarding" to null,
            "gate" to "big_feature"
        ))
    }

    @Test
    fun `createOfferings with placement with null fallback`() {
        val productIds = listOf(productId)
        mockStoreProduct(productIds, emptyList(), ProductType.SUBS)
        mockStoreProduct(productIds, productIds, ProductType.INAPP)

        var offerings: Offerings? = null
        offeringsFactory.createOfferings(
            offeringsJSON = oneOfferingWithPlacementWithNullFallback,
            originalDataSource = HTTPResponseOriginalSource.MAIN,
            loadedFromDiskCache = false,
            onError = { fail("Error: $it") },
            onSuccess = { offerings = it.offerings }
        )

        assertThat(offerings).isNotNull
        assertThat(offerings!!.current).isNotNull
        assertThat(offerings!!.placements).isNotNull
        assertThat(offerings!!.placements!!.fallbackOfferingId).isNull()
        assertThat(offerings!!.placements!!.offeringIdsByPlacement).isEqualTo(mapOf(
            "onboarding" to null,
            "gate" to "big_feature"
        ))
    }

    @Test
    fun `createOfferings with targeting`() {
        val productIds = listOf(productId)
        mockStoreProduct(productIds, emptyList(), ProductType.SUBS)
        mockStoreProduct(productIds, productIds, ProductType.INAPP)

        var offerings: Offerings? = null
        offeringsFactory.createOfferings(
            offeringsJSON = oneOfferingWithTargeting,
            originalDataSource = HTTPResponseOriginalSource.MAIN,
            loadedFromDiskCache = false,
            onError = { fail("Error: $it") },
            onSuccess = { offerings = it.offerings }
        )

        assertThat(offerings).isNotNull
        assertThat(offerings!!.targeting).isNotNull
        assertThat(offerings!!.targeting!!.revision).isEqualTo(1)
        assertThat(offerings!!.targeting!!.ruleId).isEqualTo("abc123")

        assertThat(
            offerings!!.current!!.availablePackages.first().presentedOfferingContext.targetingContext
        ).isNotNull()
        assertThat(
            offerings!!.current!!.availablePackages.first().presentedOfferingContext.targetingContext!!.revision
        ).isEqualTo(1)
        assertThat(
            offerings!!.current!!.availablePackages.first().presentedOfferingContext.targetingContext!!.ruleId
        ).isEqualTo("abc123")

        assertThat(
            offerings!!.all.values.first().availablePackages.first().presentedOfferingContext.targetingContext
        ).isNull()
    }

    @Test
    fun `createOfferings without WPL`() {
        val productIds = listOf(productId)
        mockStoreProduct(productIds, productIds, ProductType.SUBS)

        var offerings: Offerings? = null
        offeringsFactory.createOfferings(
            offeringsJSON = oneOfferingResponse,
            originalDataSource = HTTPResponseOriginalSource.MAIN,
            loadedFromDiskCache = false,
            onError = { fail("Expected success. Got error: $it") },
            onSuccess = { offerings = it.offerings }
        )

        assertThat(offerings).isNotNull
        val offering = offerings!!.current
        assertThat(offering).isNotNull
        assertThat(offering?.webCheckoutURL).isNull()
        val pkg = offering!!.availablePackages.first()
        assertThat(pkg.webCheckoutURL).isNull()
    }

    @Test
    fun `createOfferings with WPL`() {
        val productIds = listOf(productId)
        mockStoreProduct(productIds, emptyList(), ProductType.SUBS)
        mockStoreProduct(productIds, productIds, ProductType.INAPP)

        var offerings: Offerings? = null
        offeringsFactory.createOfferings(
            offeringsJSON = oneOfferingWithWPL,
            originalDataSource = HTTPResponseOriginalSource.MAIN,
            loadedFromDiskCache = false,
            onError = { fail("Error: $it") },
            onSuccess = { offerings = it.offerings }
        )

        assertThat(offerings).isNotNull
        val offering = offerings!!.current
        assertThat(offering).isNotNull
        assertThat(offering?.webCheckoutURL).isEqualTo(URL("http://revenuecat.com"))
        val pkg = offering!!.availablePackages.first()
        assertThat(pkg.webCheckoutURL).isEqualTo(URL("http://revenuecat.com?package_id=\$rc_monthly"))
    }

    @Test
    fun `createOfferings with invalid URL in WPL`() {
        val productIds = listOf(productId)
        mockStoreProduct(productIds, emptyList(), ProductType.SUBS)
        mockStoreProduct(productIds, productIds, ProductType.INAPP)

        val invalidUrlWPL = oneOfferingWithWPL.apply {
            val offering = getJSONArray("offerings").getJSONObject(0)
            offering.put("web_checkout_url", "ht!tp:/invalid-url")
            val pkg = offering.getJSONArray("packages").getJSONObject(0)
            pkg.put("web_checkout_url", "ht!tp:/invalid-url")
        }

        var offerings: Offerings? = null
        offeringsFactory.createOfferings(
            offeringsJSON = invalidUrlWPL,
            originalDataSource = HTTPResponseOriginalSource.MAIN,
            loadedFromDiskCache = false,
            onError = { fail("Error: $it") },
            onSuccess = { offerings = it.offerings }
        )

        assertThat(offerings).isNotNull
        val offering = offerings!!.current
        assertThat(offering).isNotNull
        assertThat(offering?.webCheckoutURL).isNull()
        val pkg = offering!!.availablePackages.first()
        assertThat(pkg.webCheckoutURL).isNull()
    }

    @Test
    fun `copy offering can create a copy with a different presented offering context`() {
        val productIds = listOf(productId)
        mockStoreProduct(productIds, productIds, ProductType.SUBS)

        var offerings: Offerings? = null
        offeringsFactory.createOfferings(
            offeringsJSON = oneOfferingResponse,
            originalDataSource = HTTPResponseOriginalSource.MAIN,
            loadedFromDiskCache = false,
            onError = { fail("Expected success. Got error: $it") },
            onSuccess = { offerings = it.offerings }
        )

        assertThat(offerings).isNotNull
        assertThat(offerings!!.all.size).isEqualTo(1)

        val offering = offerings!![STUB_OFFERING_IDENTIFIER]!!
        val originalPresentedOfferingContext = PresentedOfferingContext(
            offeringIdentifier = STUB_OFFERING_IDENTIFIER,
            placementIdentifier = null,
            targetingContext = null
        )
        assertThat(offering.availablePackages).allMatch {
            it.presentedOfferingContext == originalPresentedOfferingContext &&
                it.product.presentedOfferingContext == originalPresentedOfferingContext
        }
        val newPresentedOfferingContext = PresentedOfferingContext(
            offeringIdentifier = STUB_OFFERING_IDENTIFIER,
            placementIdentifier = "new_placement",
            targetingContext = PresentedOfferingContext.TargetingContext(1, "new_rule")
        )
        val modifiedOffering = offering.copy(newPresentedOfferingContext)
        assertThat(modifiedOffering.availablePackages).allMatch {
            it.presentedOfferingContext == newPresentedOfferingContext &&
                it.product.presentedOfferingContext == newPresentedOfferingContext
        }
    }

    // region helpers

    private fun mockStoreProduct(
        productIds: List<String> = listOf(productId),
        productIdsSuccessfullyFetched: List<String> = listOf(productId),
        type: ProductType = ProductType.SUBS
    ): List<StoreProduct> {
        val storeProducts = productIdsSuccessfullyFetched.map { productId ->
            if (type == ProductType.SUBS) stubStoreProduct(productId, stubSubscriptionOption("p1m", "P1M"))
            else stubINAPPStoreProduct(productId)
        }.map { it }

        every {
            billing.queryProductDetailsAsync(
                type,
                productIds.toSet(),
                captureLambda(),
                any(),
            )
        } answers {
            lambda<(List<StoreProduct>) -> Unit>().captured.invoke(storeProducts)
        }
        return storeProducts
    }

    // endregion helpers
}
