//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.PresentedOfferingContextSerializer
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.ReplacementMode
import com.revenuecat.purchases.TargetingContextSerializer
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.PeriodSerializer
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PriceSerializer
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PricingPhaseSerializer
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.stubGooglePurchase
import com.revenuecat.purchases.utils.stubStoreProduct
import com.revenuecat.purchases.utils.stubSubscriptionOption
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalSerializationApi::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ReceiptInfoTest {

    private val productIdentifier = "com.myproduct"
    private val json = Json.Default

    private val mockGooglePurchase = stubGooglePurchase(
        productIds = listOf("productIdentifier")
    )

    @Test
    fun `ReceiptInfo defaults price and currency from a INAPP StoreProduct`() {
        val mockStoreTransaction = makeMockStoreTransaction(
            purchaseState = PurchaseState.PURCHASED,
            subscriptionOptionId = null
        )
        val mockStoreProduct = stubStoreProduct(
            productId = productIdentifier,
            defaultOption = null,
            subscriptionOptions = emptyList(),
            price = Price(
                formatted = "$0.99",
                amountMicros = 990000,
                currencyCode = "USD"
            )
        )

        val receiptInfo = ReceiptInfo.from(
            storeTransaction = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionsForProductIDs = null,
        )

        assertThat(receiptInfo.price).isEqualTo(0.99)
        assertThat(receiptInfo.currency).isEqualTo("USD")
        assertThat(receiptInfo.duration).isNull()
        assertThat(receiptInfo.pricingPhases).isNull()
    }

    @Test
    fun `ReceiptInfo sets duration and pricingPhases from a StoreProduct with a subscription period and subscription options`() {
        val subscriptionOptionId = "option-id"

        val mockStoreTransaction = makeMockStoreTransaction(
            purchaseState = PurchaseState.PURCHASED,
            subscriptionOptionId = subscriptionOptionId
        )

        val subscriptionOption = stubSubscriptionOption(subscriptionOptionId, productIdentifier)

        val mockStoreProduct = stubStoreProduct(
            productId = productIdentifier,
            defaultOption = subscriptionOption,
            subscriptionOptions = listOf(subscriptionOption)
        )

        val receiptInfo = ReceiptInfo.from(
            storeTransaction = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionsForProductIDs = null,
        )

        assertThat(receiptInfo.price).isEqualTo(4.99)
        assertThat(receiptInfo.currency).isEqualTo("USD")
        assertThat(receiptInfo.duration).isEqualTo("P1M")
        assertThat(receiptInfo.pricingPhases?.size).isEqualTo(1)
    }

    @Test
    fun `ReceiptInfo allows price and currency to be set manually`() {
        val price = 0.99
        val currency = "USD"

        val receiptInfo = ReceiptInfo(
            productIDs = listOf(productIdentifier),
            price = price,
            currency = currency
        )

        assertThat(receiptInfo.price).isEqualTo(0.99)
        assertThat(receiptInfo.currency).isEqualTo("USD")
        assertThat(receiptInfo.duration).isNull()
        assertThat(receiptInfo.pricingPhases).isNull()
    }

    @Test
    fun `platformProductIDs maintains the same order as productIDs`() {
        val product1 = "product1"
        val product2 = "product2"
        val product3 = "product3"
        val productIDs = listOf(product1, product2, product3)

        val receiptInfo = ReceiptInfo(
            productIDs = productIDs,
            platformProductIds = listOf(
                mapOf("product_id" to product1),
                mapOf("product_id" to product2),
                mapOf("product_id" to product3)
            )
        )

        val platformProductIDs = receiptInfo.platformProductIds
        assertThat(platformProductIDs).isNotNull
        assertThat(platformProductIDs!!.size).isEqualTo(3)
        assertThat(platformProductIDs[0]["product_id"]).isEqualTo(product1)
        assertThat(platformProductIDs[1]["product_id"]).isEqualTo(product2)
        assertThat(platformProductIDs[2]["product_id"]).isEqualTo(product3)
    }

    private fun makeMockStoreTransaction(purchaseState: PurchaseState, subscriptionOptionId: String?): StoreTransaction {
        return StoreTransaction(
            orderId = mockGooglePurchase.orderId,
            productIds =  mockGooglePurchase.products,
            type = ProductType.INAPP,
            purchaseTime = mockGooglePurchase.purchaseTime,
            purchaseToken = mockGooglePurchase.purchaseToken,
            purchaseState = purchaseState,
            isAutoRenewing = mockGooglePurchase.isAutoRenewing,
            signature = mockGooglePurchase.signature,
            originalJson = JSONObject(mockGooglePurchase.originalJson),
            presentedOfferingIdentifier = null,
            storeUserID = null,
            purchaseType = PurchaseType.GOOGLE_PURCHASE,
            marketplace = null,
            subscriptionOptionId = subscriptionOptionId,
            replacementMode = null
        )
    }

    @Test
    fun `ReceiptInfo with Period can be serialized and deserialized`() {
        val period = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M")
        val receiptInfo = ReceiptInfo(
            productIDs = listOf(productIdentifier),
            price = 4.99,
            currency = "USD",
            period = period
        )

        val encoded = json.encodeToString(receiptInfo)
        val decoded = json.decodeFromString<ReceiptInfo>(encoded)

        // language=JSON
        val expectedJson = """
            {
                "productIDs":["com.myproduct"],
                "price":4.99,
                "currency":"USD",
                "period":{
                    "value":1,
                    "unit":"MONTH",
                    "iso8601":"P1M"
                }
            }
        """.trimIndent().lines().joinToString("") { it.trim() }
        assertThat(encoded).isEqualTo(expectedJson)
        assertThat(decoded).isEqualTo(receiptInfo)
    }

    @Test
    fun `ReceiptInfo with PricingPhase can be serialized and deserialized`() {
        val period = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M")
        val price = Price(formatted = "$4.99", amountMicros = 4990000, currencyCode = "USD")
        val pricingPhase = PricingPhase(
            billingPeriod = period,
            recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
            billingCycleCount = null,
            price = price
        )

        val receiptInfo = ReceiptInfo(
            productIDs = listOf(productIdentifier),
            price = 4.99,
            currency = "USD",
            period = period,
            pricingPhases = listOf(pricingPhase)
        )

        val encoded = json.encodeToString(receiptInfo)
        val decoded = json.decodeFromString<ReceiptInfo>(encoded)

        // language=JSON
        val expectedJson = """
            {
                "productIDs":["com.myproduct"],
                "price":4.99,
                "currency":"USD",
                "period":{
                    "value":1,
                    "unit":"MONTH",
                    "iso8601":"P1M"
                },
                "pricingPhases":[
                    {
                        "billing_period":{
                            "value":1,
                            "unit":"MONTH",
                            "iso8601":"P1M"
                        },
                        "recurrence_mode":{
                            "name":"INFINITE_RECURRING"
                        },
                        "billing_cycle_count":null,
                        "price":{
                            "formatted":"$4.99",
                            "amount_micros":4990000,
                            "currency_code":"USD"
                        }
                    }
                ]
            }
        """.trimIndent().lines().joinToString("") { it.trim() }

        assertThat(encoded).isEqualTo(expectedJson)
        assertThat(decoded).isEqualTo(receiptInfo)
    }

    @Test
    fun `ReceiptInfo with PricingPhase and billingCycleCount can be serialized`() {
        val period = Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W")
        val price = Price(formatted = "$0.99", amountMicros = 990000, currencyCode = "USD")
        val pricingPhase = PricingPhase(
            billingPeriod = period,
            recurrenceMode = RecurrenceMode.FINITE_RECURRING,
            billingCycleCount = 3,
            price = price
        )

        val receiptInfo = ReceiptInfo(
            productIDs = listOf(productIdentifier),
            price = 0.99,
            currency = "USD",
            pricingPhases = listOf(pricingPhase)
        )

        val encoded = json.encodeToString(receiptInfo)
        val decoded = json.decodeFromString<ReceiptInfo>(encoded)

        // language=JSON
        val expectedJson = """
            {
                "productIDs":["com.myproduct"],
                "price":0.99,
                "currency":"USD",
                "pricingPhases":[
                    {
                        "billing_period":{
                            "value":1,
                            "unit":"WEEK",
                            "iso8601":"P1W"
                        },
                        "recurrence_mode":{
                            "name":"FINITE_RECURRING"
                        },
                        "billing_cycle_count":3,
                        "price":{
                            "formatted":"$0.99",
                            "amount_micros":990000,
                            "currency_code":"USD"
                        }
                    }
                ]
            }
        """.trimIndent().lines().joinToString("") { it.trim() }

        assertThat(encoded).isEqualTo(expectedJson)

        assertThat(decoded.pricingPhases).isNotNull
        assertThat(decoded.pricingPhases!!.size).isEqualTo(1)
        val decodedPricingPhase = decoded.pricingPhases[0]
        assertThat(decodedPricingPhase.billingCycleCount).isEqualTo(3)
    }

    @Test
    fun `ReceiptInfo with null period and pricingPhases can be serialized and deserialized`() {
        val original = ReceiptInfo(
            productIDs = listOf(productIdentifier),
            price = 0.99,
            currency = "USD",
            period = null,
            pricingPhases = null
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<ReceiptInfo>(encoded)

        assertThat(decoded.productIDs).isEqualTo(original.productIDs)
        assertThat(decoded.price).isEqualTo(original.price)
        assertThat(decoded.currency).isEqualTo(original.currency)
        assertThat(decoded.period).isNull()
        assertThat(decoded.pricingPhases).isNull()
    }

    @Test
    fun `ReceiptInfo with replacement mode can be serialized and deserialized`() {
        val expectedReplacementMode = GoogleReplacementMode.WITH_TIME_PRORATION
        val original = ReceiptInfo(
            productIDs = listOf(productIdentifier),
            price = 0.99,
            currency = "USD",
            period = null,
            pricingPhases = null,
            replacementMode = expectedReplacementMode,
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<ReceiptInfo>(encoded)

        // language=JSON
        val expectedJson = """
            {
                "productIDs":["com.myproduct"],
                "price":0.99,
                "currency":"USD",
                "replacementMode":{
                    "type":"GoogleReplacementMode",
                    "name":"WITH_TIME_PRORATION"
                }
            }
        """.trimIndent().lines().joinToString("") { it.trim() }

        assertThat(decoded.productIDs).isEqualTo(original.productIDs)
        assertThat(decoded.replacementMode).isEqualTo(expectedReplacementMode)

        assertThat(encoded).isEqualTo(expectedJson)
    }

    @Test
    fun `ReceiptInfo with unknown replacement mode type fails serializing and deserializing`() {
        val unknownReplacementMode: ReplacementMode = object : ReplacementMode {
            override val name: String
                get() = "SOME_NEW_MODE"

            override fun describeContents(): Int {
                return 0
            }

            override fun writeToParcel(dest: Parcel, flags: Int) {
                // No-op
            }
        }
        val original = ReceiptInfo(
            productIDs = listOf(productIdentifier),
            price = 0.99,
            currency = "USD",
            period = null,
            pricingPhases = null,
            replacementMode = unknownReplacementMode,
        )

        assertThatExceptionOfType(SerializationException::class.java).isThrownBy { json.encodeToString(original) }

        // language=JSON
        val unknownReplacementModeJson = """
            {
                "productIDs":["com.myproduct"],
                "price":0.99,
                "currency":"USD",
                "replacementMode":{
                    "type":"UnknownReplcementMode",
                    "name":"WITH_TIME_PRORATION"
                }
            }
        """.trimIndent().lines().joinToString("") { it.trim() }
        assertThatExceptionOfType(SerializationException::class.java).isThrownBy { json.decodeFromString<ReceiptInfo>(unknownReplacementModeJson) }
    }

    @Test
    fun `ReceiptInfo with PresentedOfferingContext can be serialized and deserialized`() {
        val targetingContext = PresentedOfferingContext.TargetingContext(
            revision = 5,
            ruleId = "rule123"
        )
        val presentedContext = PresentedOfferingContext(
            offeringIdentifier = "offering1",
            placementIdentifier = "placement1",
            targetingContext = targetingContext
        )

        val receiptInfo = ReceiptInfo(
            productIDs = listOf(productIdentifier),
            presentedOfferingContext = presentedContext,
            price = 4.99,
            currency = "USD"
        )

        val encoded = json.encodeToString(receiptInfo)
        val decoded = json.decodeFromString<ReceiptInfo>(encoded)

        // language=JSON
        val expectedJson = """
            {
                "productIDs":["com.myproduct"],
                "presentedOfferingContext":{
                    "offeringIdentifier":"offering1",
                    "placementIdentifier":"placement1",
                    "targetingContext":{
                        "revision":5,
                        "ruleId":"rule123"
                    }
                },
                "price":4.99,
                "currency":"USD"
            }
        """.trimIndent().lines().joinToString("") { it.trim() }

        assertThat(decoded).isEqualTo(receiptInfo)
        assertThat(encoded).isEqualTo(expectedJson)
    }

    @Test
    fun `ReceiptInfo with PresentedOfferingContext without targeting context can be serialized`() {
        val presentedContext = PresentedOfferingContext(
            offeringIdentifier = "offering1",
            placementIdentifier = "placement1",
            targetingContext = null
        )

        val receiptInfo = ReceiptInfo(
            productIDs = listOf(productIdentifier),
            presentedOfferingContext = presentedContext,
            price = 4.99,
            currency = "USD"
        )

        val encoded = json.encodeToString(receiptInfo)
        val decoded = json.decodeFromString<ReceiptInfo>(encoded)

        assertThat(decoded).isEqualTo(receiptInfo)
        assertThat(decoded.presentedOfferingContext?.targetingContext).isNull()
    }

    @Test
    fun `ReceiptInfo with null PresentedOfferingContext can be serialized and deserialized`() {
        val original = ReceiptInfo(
            productIDs = listOf(productIdentifier),
            presentedOfferingContext = null,
            price = 1.99,
            currency = "USD"
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<ReceiptInfo>(encoded)

        assertThat(decoded).isEqualTo(original)
        assertThat(decoded.presentedOfferingContext).isNull()
    }

    @Test
    fun `PresentedOfferingContextSerializer includes all constructor parameters`() {
        // Get all constructors from PresentedOfferingContext using Java reflection
        val constructors = PresentedOfferingContext::class.java.constructors
        // Find the constructor with the most parameters (the primary constructor)
        val primaryConstructor = constructors.maxByOrNull { it.parameterCount }
            ?: error("PresentedOfferingContext must have at least one constructor")

        val constructorParamCount = primaryConstructor.parameterCount

        // Get the descriptor from the serializer
        val descriptor = PresentedOfferingContextSerializer.descriptor

        // Verify the number of fields matches
        assertThat(descriptor.elementsCount)
            .withFailMessage(
                "PresentedOfferingContextSerializer has ${descriptor.elementsCount} fields, " +
                    "but PresentedOfferingContext constructor has $constructorParamCount parameters. " +
                    "Did you forget to update the serializer?"
            )
            .isEqualTo(constructorParamCount)

        // Get expected field names from the descriptor
        val expectedFields = setOf("offeringIdentifier", "placementIdentifier", "targetingContext")

        // Verify each expected field is in the descriptor
        expectedFields.forEach { fieldName ->
            val hasField = (0 until descriptor.elementsCount).any { index ->
                descriptor.getElementName(index) == fieldName
            }
            assertThat(hasField)
                .withFailMessage(
                    "PresentedOfferingContextSerializer is missing field '$fieldName'. " +
                        "Please add it to the serializer."
                )
                .isTrue()
        }
    }

    @Test
    fun `TargetingContextSerializer includes all constructor parameters`() {
        // Get all constructors from TargetingContext using Java reflection
        val constructors = PresentedOfferingContext.TargetingContext::class.java.constructors
        // Find the constructor with the most parameters (the primary constructor)
        val primaryConstructor = constructors.maxByOrNull { it.parameterCount }
            ?: error("TargetingContext must have at least one constructor")

        val constructorParamCount = primaryConstructor.parameterCount

        // Get the descriptor from the serializer
        val descriptor = TargetingContextSerializer.descriptor

        // Verify the number of fields matches
        assertThat(descriptor.elementsCount)
            .withFailMessage(
                "TargetingContextSerializer has ${descriptor.elementsCount} fields, " +
                    "but TargetingContext constructor has $constructorParamCount parameters. " +
                    "Did you forget to update the serializer?"
            )
            .isEqualTo(constructorParamCount)

        // Get expected field names from the descriptor
        val expectedFields = setOf("revision", "ruleId")

        // Verify each expected field is in the descriptor
        expectedFields.forEach { fieldName ->
            val hasField = (0 until descriptor.elementsCount).any { index ->
                descriptor.getElementName(index) == fieldName
            }
            assertThat(hasField)
                .withFailMessage(
                    "TargetingContextSerializer is missing field '$fieldName'. " +
                        "Please add it to the serializer."
                )
                .isTrue()
        }
    }

    @Test
    fun `PeriodSerializer includes all constructor parameters`() {
        // Get all constructors from Period using Java reflection
        val constructors = Period::class.java.constructors
        // Find the constructor with the most parameters (the primary constructor)
        val primaryConstructor = constructors.maxByOrNull { it.parameterCount }
            ?: error("Period must have at least one constructor")

        val constructorParamCount = primaryConstructor.parameterCount

        // Get the descriptor from the serializer
        val descriptor = PeriodSerializer.descriptor

        // Verify the number of fields matches
        assertThat(descriptor.elementsCount)
            .withFailMessage(
                "PeriodSerializer has ${descriptor.elementsCount} fields, " +
                    "but Period constructor has $constructorParamCount parameters. " +
                    "Did you forget to update the serializer?"
            )
            .isEqualTo(constructorParamCount)

        // Get expected field names from the descriptor
        val expectedFields = setOf("value", "unit", "iso8601")

        // Verify each expected field is in the descriptor
        expectedFields.forEach { fieldName ->
            val hasField = (0 until descriptor.elementsCount).any { index ->
                descriptor.getElementName(index) == fieldName
            }
            assertThat(hasField)
                .withFailMessage(
                    "PeriodSerializer is missing field '$fieldName'. " +
                        "Please add it to the serializer."
                )
                .isTrue()
        }
    }

    @Test
    fun `PriceSerializer includes all constructor parameters`() {
        // Get all constructors from Price using Java reflection
        val constructors = Price::class.java.constructors
        // Find the constructor with the most parameters (the primary constructor)
        val primaryConstructor = constructors.maxByOrNull { it.parameterCount }
            ?: error("Price must have at least one constructor")

        val constructorParamCount = primaryConstructor.parameterCount

        // Get the descriptor from the serializer
        val descriptor = PriceSerializer.descriptor

        // Verify the number of fields matches
        assertThat(descriptor.elementsCount)
            .withFailMessage(
                "PriceSerializer has ${descriptor.elementsCount} fields, " +
                    "but Price constructor has $constructorParamCount parameters. " +
                    "Did you forget to update the serializer?"
            )
            .isEqualTo(constructorParamCount)

        // Get expected field names from the descriptor
        val expectedFields = setOf("formatted", "amount_micros", "currency_code")

        // Verify each expected field is in the descriptor
        expectedFields.forEach { fieldName ->
            val hasField = (0 until descriptor.elementsCount).any { index ->
                descriptor.getElementName(index) == fieldName
            }
            assertThat(hasField)
                .withFailMessage(
                    "PriceSerializer is missing field '$fieldName'. " +
                        "Please add it to the serializer."
                )
                .isTrue()
        }
    }

    @Test
    fun `PricingPhaseSerializer includes all constructor parameters`() {
        // Get all constructors from PricingPhase using Java reflection
        val constructors = PricingPhase::class.java.constructors
        // Find the constructor with the most parameters (the primary constructor)
        val primaryConstructor = constructors.maxByOrNull { it.parameterCount }
            ?: error("PricingPhase must have at least one constructor")

        val constructorParamCount = primaryConstructor.parameterCount

        // Get the descriptor from the serializer
        val descriptor = PricingPhaseSerializer.descriptor

        // Verify the number of fields matches
        assertThat(descriptor.elementsCount)
            .withFailMessage(
                "PricingPhaseSerializer has ${descriptor.elementsCount} fields, " +
                    "but PricingPhase constructor has $constructorParamCount parameters. " +
                    "Did you forget to update the serializer?"
            )
            .isEqualTo(constructorParamCount)

        // Get expected field names from the descriptor
        val expectedFields = setOf("billing_period", "recurrence_mode", "billing_cycle_count", "price")

        // Verify each expected field is in the descriptor
        expectedFields.forEach { fieldName ->
            val hasField = (0 until descriptor.elementsCount).any { index ->
                descriptor.getElementName(index) == fieldName
            }
            assertThat(hasField)
                .withFailMessage(
                    "PricingPhaseSerializer is missing field '$fieldName'. " +
                        "Please add it to the serializer."
                )
                .isTrue()
        }
    }

    @Test
    fun `ReceiptInfo serializes with sdkOriginated true`() {
        val receiptInfo = ReceiptInfo(
            productIDs = listOf(productIdentifier),
            price = 4.99,
            currency = "USD",
            sdkOriginated = true
        )

        val encoded = json.encodeToString(receiptInfo)
        val decoded = json.decodeFromString<ReceiptInfo>(encoded)

        // language=JSON
        val expectedJson = """
            {
                "productIDs":["com.myproduct"],
                "price":4.99,
                "currency":"USD",
                "sdkOriginated":true
            }
        """.trimIndent().lines().joinToString("") { it.trim() }

        assertThat(encoded).isEqualTo(expectedJson)
        assertThat(decoded).isEqualTo(receiptInfo)
        assertThat(decoded.sdkOriginated).isTrue
    }
}
