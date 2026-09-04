package com.revenuecat.purchases.ui.revenuecatui.components.webview

import com.revenuecat.purchases.Store
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.data.WorkflowScreenContext
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

private val prettyJson = Json { prettyPrint = true }

internal class WebViewContextSnapshotTest {

    @Test
    fun `contains every section with its empty shape and no workflow key`() {
        val snapshot = testContextSnapshot(darkMode = true)

        assertThat(snapshot.keys).containsExactly(
            "custom",
            "offering",
            "packages",
            "package",
            "selected_package",
            "inputs",
            "device_meta",
        )
        assertThat(snapshot.getValue("custom").jsonObject).isEmpty()
        assertThat(snapshot.getValue("offering")).isEqualTo(JsonNull)
        assertThat(snapshot.getValue("packages").jsonArray).isEmpty()
        assertThat(snapshot.getValue("package")).isEqualTo(JsonNull)
        assertThat(snapshot.getValue("selected_package")).isEqualTo(JsonNull)
        assertThat(snapshot.getValue("inputs").jsonObject).isEmpty()
    }

    @Test
    fun `serializes the whole payload in contract order`() {
        val prepaid = prepaidPackage()

        val snapshot = testContextSnapshot(
            customVariables = mapOf("org" to CustomVariableValue.String("RevenueCat")),
            offering = offeringOf(prepaid),
            componentPackage = prepaid,
            storefrontCountryCode = "ES",
            workflowScreen = WorkflowScreenContext(
                workflowId = "wf_123",
                stepId = "step_paywall",
                stepType = "screen",
                screenType = listOf("paywall"),
            ),
        ).withFixedTimestamp()

        assertThat(prettyJson.encodeToString(JsonObject.serializer(), snapshot)).isEqualTo(
            """
            {
                "custom": {
                    "org": "RevenueCat"
                },
                "offering": {
                    "identifier": "promo",
                    "display_name": "Promo offering"
                },
                "packages": [
                    {
                        "identifier": "prepaid",
                        "products": [
                            {
                                "identifier": "prepaid_monthly",
                                "store": {
                                    "store_type": "play_store",
                                    "country": "ES"
                                },
                                "display_name": "Prepaid Monthly",
                                "is_subscription": true,
                                "period": "P1M",
                                "is_auto_renewing": false,
                                "price": {
                                    "amount": 2.99,
                                    "currency": "USD"
                                }
                            }
                        ]
                    }
                ],
                "package": {
                    "identifier": "prepaid",
                    "products": [
                        {
                            "identifier": "prepaid_monthly",
                            "store": {
                                "store_type": "play_store",
                                "country": "ES"
                            },
                            "display_name": "Prepaid Monthly",
                            "is_subscription": true,
                            "period": "P1M",
                            "is_auto_renewing": false,
                            "price": {
                                "amount": 2.99,
                                "currency": "USD"
                            }
                        }
                    ]
                },
                "selected_package": null,
                "inputs": {
                },
                "workflow": {
                    "workflow_id": "wf_123",
                    "step_id": "step_paywall",
                    "step_type": "screen",
                    "screen_type": [
                        "paywall"
                    ]
                },
                "device_meta": {
                    "is_preview": false,
                    "locale": "en-US",
                    "dark_mode": false,
                    "updated_at": 0
                }
            }
            """.trimIndent(),
        )
    }

    // --- custom ---

    @Test
    fun `custom carries every variable with its type intact`() {
        val custom = snapshotWith(
            "org" to CustomVariableValue.String("RevenueCat"),
            "is_premium" to CustomVariableValue.Boolean(true),
            "rating" to CustomVariableValue.Number(4.5),
        )

        assertThat(Json.encodeToString(JsonObject.serializer(), custom))
            .isEqualTo("""{"org":"RevenueCat","is_premium":true,"rating":4.5}""")
    }

    @Test
    fun `whole numbers keep no decimal part`() {
        // The contract's example is `"streak_days": 12`.
        val custom = snapshotWith("streak_days" to CustomVariableValue.Number(12))

        assertThat(Json.encodeToString(JsonObject.serializer(), custom)).isEqualTo("""{"streak_days":12}""")
    }

    @Test
    fun `non-finite numbers stay encodable`() {
        val custom = snapshotWith("broken" to CustomVariableValue.Number(Double.NaN))

        assertThat(Json.encodeToString(JsonObject.serializer(), custom)).isEqualTo("""{"broken":"NaN"}""")
    }

    // --- offering, packages, selection ---

    @Test
    fun `offering carries its identifier and display name`() {
        val offering = TestData.template7CustomPackageOffering

        val snapshot = testContextSnapshot(offering = offering).getValue("offering").jsonObject

        assertThat(snapshot.getValue("identifier").jsonPrimitive.content).isEqualTo(offering.identifier)
        assertThat(snapshot.getValue("display_name").jsonPrimitive.content)
            .isEqualTo(offering.serverDescription)
    }

    @Test
    fun `packages carries every available package in order`() {
        val offering = TestData.template7CustomPackageOffering

        val packages = testContextSnapshot(offering = offering).getValue("packages").jsonArray

        assertThat(packages.map { it.jsonObject.getValue("identifier").jsonPrimitive.content })
            .isEqualTo(offering.availablePackages.map { it.identifier })
    }

    @Test
    fun `packages omits display_name, which the offerings endpoint does not serve`() {
        val packages = testContextSnapshot(offering = TestData.template7CustomPackageOffering)
            .getValue("packages").jsonArray

        assertThat(packages.first().jsonObject.keys).containsExactly("identifier", "products")
    }

    @Test
    fun `package and selected_package differ when the component sits inside a package`() {
        val offering = TestData.template7CustomPackageOffering
        val componentPackage = offering.availablePackages.first()
        val selected = offering.availablePackages.last()

        val snapshot = testContextSnapshot(
            offering = offering,
            componentPackage = componentPackage,
            selectedPackage = selected,
        )

        assertThat(identifierOf(snapshot, "package")).isEqualTo(componentPackage.identifier)
        assertThat(identifierOf(snapshot, "selected_package")).isEqualTo(selected.identifier)
    }

    @Test
    fun `selected_package is null when nothing is selected`() {
        val snapshot = testContextSnapshot(offering = TestData.template7CustomPackageOffering)

        assertThat(snapshot.getValue("selected_package")).isEqualTo(JsonNull)
    }

    // --- products ---

    @Test
    fun `product carries store, period and price`() {
        val offering = TestData.template7CustomPackageOffering
        val monthly = offering.monthly!!

        val product = testContextSnapshot(
            offering = offering,
            componentPackage = monthly,
            storefrontCountryCode = "ES",
        ).productOfPackage()

        assertThat(product.getValue("identifier").jsonPrimitive.content).isEqualTo(monthly.product.id)
        assertThat(product.getValue("display_name").jsonPrimitive.content).isEqualTo(monthly.product.name)
        assertThat(product.getValue("is_subscription").jsonPrimitive.boolean).isTrue()
        assertThat(product.getValue("period").jsonPrimitive.content).isEqualTo(monthly.product.period!!.iso8601)
        val store = product.getValue("store").jsonObject
        assertThat(store.getValue("store_type").jsonPrimitive.content).isEqualTo("play_store")
        assertThat(store.getValue("country").jsonPrimitive.content).isEqualTo("ES")
        val price = product.getValue("price").jsonObject
        assertThat(price.getValue("currency").jsonPrimitive.content).isEqualTo(monthly.product.price.currencyCode)
        assertThat(price.getValue("amount").jsonPrimitive.double)
            .isEqualTo(monthly.product.price.amountMicros / 1_000_000.0)
    }

    @Test
    fun `store country is omitted until the storefront is known`() {
        val offering = TestData.template7CustomPackageOffering

        val store = testContextSnapshot(
            offering = offering,
            componentPackage = offering.monthly,
            storefrontCountryCode = null,
        ).productOfPackage().getValue("store").jsonObject

        assertThat(store.keys).containsExactly("store_type")
    }

    @Test
    fun `store type follows the configured store`() {
        val offering = TestData.template7CustomPackageOffering

        val store = testContextSnapshot(
            offering = offering,
            componentPackage = offering.monthly,
            store = Store.AMAZON,
        ).productOfPackage().getValue("store").jsonObject

        assertThat(store.getValue("store_type").jsonPrimitive.content).isEqualTo("amazon")
    }

    @Test
    fun `is_auto_renewing follows the base plan's recurrence mode`() {
        val offering = TestData.template7CustomPackageOffering

        val product = testContextSnapshot(
            offering = offering,
            componentPackage = offering.monthly,
        ).productOfPackage()

        assertThat(product.getValue("is_auto_renewing").jsonPrimitive.boolean).isTrue()
    }

    @Test
    fun `is_auto_renewing is false for a prepaid base plan`() {
        val product = testContextSnapshot(
            componentPackage = prepaidPackage(),
        ).productOfPackage()

        assertThat(product.getValue("is_auto_renewing").jsonPrimitive.boolean).isFalse()
    }

    @Test
    fun `a non-subscription reports no period and no renewal`() {
        val offering = TestData.template7CustomPackageOffering

        val product = testContextSnapshot(
            offering = offering,
            componentPackage = offering.lifetime,
        ).productOfPackage()

        assertThat(product.getValue("is_subscription").jsonPrimitive.boolean).isFalse()
        assertThat(product).doesNotContainKey("period")
        assertThat(product).doesNotContainKey("is_auto_renewing")
    }

    // --- workflow ---

    @Test
    fun `workflow is omitted on a standalone paywall`() {
        assertThat(testContextSnapshot()).doesNotContainKey("workflow")
    }

    @Test
    fun `workflow carries the step this state renders`() {
        val workflow = testContextSnapshot(
            workflowScreen = WorkflowScreenContext(
                workflowId = "wf_123",
                stepId = "step_paywall",
                stepType = "screen",
                screenType = listOf("paywall"),
            ),
        ).getValue("workflow").jsonObject

        assertThat(Json.encodeToString(JsonObject.serializer(), workflow)).isEqualTo(
            """{"workflow_id":"wf_123","step_id":"step_paywall","step_type":"screen","screen_type":["paywall"]}""",
        )
    }

    @Test
    fun `step_type is null for an untyped step`() {
        val workflow = testContextSnapshot(
            workflowScreen = WorkflowScreenContext(
                workflowId = "wf_123",
                stepId = "step_paywall",
                stepType = null,
                screenType = listOf("paywall"),
            ),
        ).getValue("workflow").jsonObject

        assertThat(workflow.getValue("step_type")).isEqualTo(JsonNull)
    }

    @Test
    fun `screen_type is an empty array for an untagged step`() {
        // The wire type has no null; the untagged-vs-tagged-empty distinction is native-only.
        val workflow = testContextSnapshot(
            workflowScreen = WorkflowScreenContext(
                workflowId = "wf_123",
                stepId = "step_paywall",
                stepType = "screen",
                screenType = null,
            ),
        ).getValue("workflow").jsonObject

        assertThat(workflow.getValue("screen_type").jsonArray).isEmpty()
    }

    // --- device_meta ---

    @Test
    fun `device_meta carries host details`() {
        val before = System.currentTimeMillis()

        val deviceMeta = testContextSnapshot(darkMode = true).getValue("device_meta").jsonObject

        assertThat(deviceMeta.getValue("is_preview").jsonPrimitive.boolean).isFalse()
        assertThat(deviceMeta.getValue("locale").jsonPrimitive.content).isEqualTo("en-US")
        assertThat(deviceMeta.getValue("dark_mode").jsonPrimitive.boolean).isTrue()
        assertThat(deviceMeta.getValue("updated_at").jsonPrimitive.long)
            .isBetween(before, System.currentTimeMillis())
    }

    @Test
    fun `package is the component's own when it sits inside a package, ignoring the selection`() {
        val offering = TestData.template7CustomPackageOffering
        val own = offering.availablePackages.first()
        val selected = offering.availablePackages.last()
        val state = FakePaywallState(packages = offering.availablePackages)
            .apply { update(selectedPackageUniqueId = selected.identifier) }

        val snapshot = webViewContextSnapshot(state, testWebViewStyle(rcPackage = own), darkMode = false)

        assertThat(identifierOf(snapshot, "package")).isEqualTo(own.identifier)
        assertThat(identifierOf(snapshot, "selected_package")).isEqualTo(selected.identifier)
    }

    @Test
    fun `package follows the selection when the component is outside a package`() {
        val offering = TestData.template7CustomPackageOffering
        val selected = offering.availablePackages.last()
        val state = FakePaywallState(packages = offering.availablePackages)
            .apply { update(selectedPackageUniqueId = selected.identifier) }

        val snapshot = webViewContextSnapshot(state, testWebViewStyle(rcPackage = null), darkMode = false)

        assertThat(identifierOf(snapshot, "package")).isEqualTo(selected.identifier)
    }

    @Test
    fun `derives the custom variables from the paywall state`() {
        val state = FakePaywallState(
            components = emptyList(),
            customVariables = mapOf("org" to CustomVariableValue.String("RevenueCat")),
        )

        val custom = webViewContextSnapshot(state, testWebViewStyle(), darkMode = false).getValue("custom").jsonObject

        assertThat(custom.getValue("org").jsonPrimitive.content).isEqualTo("RevenueCat")
    }

    @Test
    fun `derives the locale from the paywall state as a BCP-47 tag`() {
        // The state carries the locale as an underscored `LocaleId`; the wire needs a tag.
        val deviceMeta = webViewContextSnapshot(FakePaywallState(components = emptyList()), testWebViewStyle(), darkMode = false)
            .getValue("device_meta").jsonObject

        assertThat(deviceMeta.getValue("locale").jsonPrimitive.content).isEqualTo("en-US")
    }

    private fun snapshotWith(vararg variables: Pair<String, CustomVariableValue>) =
        testContextSnapshot(customVariables = variables.toMap()).getValue("custom").jsonObject

    private fun identifierOf(snapshot: JsonObject, key: String) =
        snapshot.getValue(key).jsonObject.getValue("identifier").jsonPrimitive.content

    private fun JsonObject.productOfPackage(): JsonObject =
        getValue("package").jsonObject.getValue("products").jsonArray.single().jsonObject
}
