package com.revenuecat.purchases.customercenter

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
public class ScreenOfferingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `ScreenOffering serialization - CURRENT type`() {
        val screenOffering = CustomerCenterConfigData.ScreenOffering(
            type = CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.CURRENT
        )

        val serialized = json.encodeToString(screenOffering)
        val deserialized = json.decodeFromString<CustomerCenterConfigData.ScreenOffering>(serialized)

        assertThat(deserialized.type).isEqualTo(CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.CURRENT)
        assertThat(deserialized.offeringId).isNull()
    }

    @Test
    fun `ScreenOffering serialization - SPECIFIC type with offering ID`() {
        val screenOffering = CustomerCenterConfigData.ScreenOffering(
            type = CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.SPECIFIC,
            offeringId = "premium_monthly_plan"
        )

        val serialized = json.encodeToString(screenOffering)
        val deserialized = json.decodeFromString<CustomerCenterConfigData.ScreenOffering>(serialized)

        assertThat(deserialized.type).isEqualTo(CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.SPECIFIC)
        assertThat(deserialized.offeringId).isEqualTo("premium_monthly_plan")
    }

    @Test
    fun `Screen with new offering field - JSON parsing`() {
        val jsonString = """
        {
            "type": "NO_ACTIVE",
            "title": "No Active Screen",
            "subtitle": "No active subscriptions",
            "paths": [],
            "offering": {
                "type": "CURRENT"
            }
        }
        """.trimIndent()

        val screen = json.decodeFromString<CustomerCenterConfigData.Screen>(jsonString)

        assertThat(screen.type).isEqualTo(CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE)
        assertThat(screen.offering).isNotNull
        assertThat(screen.offering?.type).isEqualTo(CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.CURRENT)
        assertThat(screen.offering?.offeringId).isNull()
    }

    @Test
    fun `Screen with specific offering - JSON parsing`() {
        val jsonString = """
        {
            "type": "NO_ACTIVE",
            "title": "No Active Screen", 
            "paths": [],
            "offering": {
                "type": "SPECIFIC",
                "offering_id": "premium_monthly_plan"
            }
        }
        """.trimIndent()

        val screen = json.decodeFromString<CustomerCenterConfigData.Screen>(jsonString)

        assertThat(screen.offering).isNotNull
        assertThat(screen.offering?.type).isEqualTo(CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.SPECIFIC)
        assertThat(screen.offering?.offeringId).isEqualTo("premium_monthly_plan")
    }


    @Test
    fun `Screen no offering specified - JSON parsing`() {
        val jsonString = """
        {
            "type": "MANAGEMENT",
            "title": "Management Screen",
            "paths": []
        }
        """.trimIndent()

        val screen = json.decodeFromString<CustomerCenterConfigData.Screen>(jsonString)

        assertThat(screen.offering).isNull()
    }


    @Test
    fun `Screen with invalid offering type - throws exception`() {
        val jsonString = """
        {
            "type": "NO_ACTIVE",
            "title": "No Active Screen",
            "paths": [],
            "offering": {
                "type": "INVALID_TYPE"
            }
        }
        """.trimIndent()

        try {
            json.decodeFromString<CustomerCenterConfigData.Screen>(jsonString)
            assertThat(false).describedAs("Expected exception to be thrown").isTrue()
        } catch (e: Exception) {
            assertThat(e).isInstanceOf(kotlinx.serialization.SerializationException::class.java)
        }
    }

    @Test
    fun `Screen with malformed JSON - throws exception`() {
        val jsonString = """
        {
            "type": "NO_ACTIVE",
            "title": "No Active Screen", 
            "paths": [],
            "offering": "invalid_offering_format"
        }
        """.trimIndent()

        try {
            json.decodeFromString<CustomerCenterConfigData.Screen>(jsonString)
            assertThat(false).describedAs("Expected exception to be thrown").isTrue()
        } catch (e: Exception) {
            // Expected - malformed JSON should throw an exception
            assertThat(e).isNotNull()
        }
    }

    @Test
    fun `ScreenOfferingType enum values`() {
        assertThat(CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.CURRENT.value).isEqualTo("CURRENT")
        assertThat(CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.SPECIFIC.value).isEqualTo("SPECIFIC")
    }

    @Test
    fun `ScreenOffering with buttonText - serialization and deserialization`() {
        val screenOffering = CustomerCenterConfigData.ScreenOffering(
            type = CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.CURRENT,
            buttonText = "Get Started"
        )

        val serialized = json.encodeToString(screenOffering)
        val deserialized = json.decodeFromString<CustomerCenterConfigData.ScreenOffering>(serialized)

        assertThat(deserialized.type).isEqualTo(CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.CURRENT)
        assertThat(deserialized.offeringId).isNull()
        assertThat(deserialized.buttonText).isEqualTo("Get Started")
    }

    @Test
    fun `ScreenOffering without buttonText - backward compatibility`() {
        val screenOffering = CustomerCenterConfigData.ScreenOffering(
            type = CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.SPECIFIC,
            offeringId = "premium_plan"
        )

        val serialized = json.encodeToString(screenOffering)
        val deserialized = json.decodeFromString<CustomerCenterConfigData.ScreenOffering>(serialized)

        assertThat(deserialized.type).isEqualTo(CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.SPECIFIC)
        assertThat(deserialized.offeringId).isEqualTo("premium_plan")
        assertThat(deserialized.buttonText).isNull()
    }

    @Test
    fun `ScreenOffering JSON parsing with button_text`() {
        val jsonString = """
        {
            "type": "CURRENT",
            "button_text": "Subscribe Now"
        }
        """.trimIndent()

        val screenOffering = json.decodeFromString<CustomerCenterConfigData.ScreenOffering>(jsonString)

        assertThat(screenOffering.type).isEqualTo(CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.CURRENT)
        assertThat(screenOffering.offeringId).isNull()
        assertThat(screenOffering.buttonText).isEqualTo("Subscribe Now")
    }

    @Test
    fun `ScreenOffering JSON parsing without button_text - backward compatibility`() {
        val jsonString = """
        {
            "type": "SPECIFIC",
            "offering_id": "monthly_plan"
        }
        """.trimIndent()

        val screenOffering = json.decodeFromString<CustomerCenterConfigData.ScreenOffering>(jsonString)

        assertThat(screenOffering.type).isEqualTo(CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.SPECIFIC)
        assertThat(screenOffering.offeringId).isEqualTo("monthly_plan")
        assertThat(screenOffering.buttonText).isNull()
    }

    @Test
    fun `Screen with offering containing buttonText - JSON parsing`() {
        val jsonString = """
        {
            "type": "NO_ACTIVE",
            "title": "No Active Screen",
            "subtitle": "No active subscriptions",
            "paths": [],
            "offering": {
                "type": "CURRENT",
                "button_text": "Start Subscription"
            }
        }
        """.trimIndent()

        val screen = json.decodeFromString<CustomerCenterConfigData.Screen>(jsonString)

        assertThat(screen.type).isEqualTo(CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE)
        assertThat(screen.offering).isNotNull
        assertThat(screen.offering?.type).isEqualTo(CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.CURRENT)
        assertThat(screen.offering?.offeringId).isNull()
        assertThat(screen.offering?.buttonText).isEqualTo("Start Subscription")
    }

    @Test
    fun `ScreenOffering with all fields - serialization and deserialization`() {
        val screenOffering = CustomerCenterConfigData.ScreenOffering(
            type = CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.SPECIFIC,
            offeringId = "premium_yearly",
            buttonText = "Upgrade to Premium"
        )

        val serialized = json.encodeToString(screenOffering)
        val deserialized = json.decodeFromString<CustomerCenterConfigData.ScreenOffering>(serialized)

        assertThat(deserialized.type).isEqualTo(CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.SPECIFIC)
        assertThat(deserialized.offeringId).isEqualTo("premium_yearly")
        assertThat(deserialized.buttonText).isEqualTo("Upgrade to Premium")
    }
}