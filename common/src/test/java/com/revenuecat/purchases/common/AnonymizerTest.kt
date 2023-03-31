package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class AnonymizerTest {
    private lateinit var anonymizer: Anonymizer

    @Before
    fun setup() {
        anonymizer = Anonymizer()
    }

    // region anonymizeString

    @Test
    fun `anonymizeString removes emails`() {
        val originalString = "Some random text with an sample.123+34@revenuecat.com and test.1@gmail.com email."
        val expectedString = "Some random text with an ***** and ***** email."
        assertThat(anonymizer.anonymizedString(originalString)).isEqualTo(expectedString)
    }

    @Test
    fun `anonymizeString removes UUIDs`() {
        val originalString = "Some random text with a 2c5e8760-a864-11ed-afa1-0242ac120002 uuid."
        val expectedString = "Some random text with a ***** uuid."
        assertThat(anonymizer.anonymizedString(originalString)).isEqualTo(expectedString)
    }

    @Test
    fun `anonymizeString removes IPs`() {
        val originalString = "Some random text with a 192.168.1.1 ip."
        val expectedString = "Some random text with a ***** ip."
        assertThat(anonymizer.anonymizedString(originalString)).isEqualTo(expectedString)
    }

    @Test
    fun `anonymizeString removes multiple pieces of PII`() {
        val originalString = "Some random text with a 685a5091-7e0b-44c0-a948-61ce324477c4 uuid and a " +
            "random.test@revenuecat.com email and a random 1.1.1.1 ip"
        val expectedString = "Some random text with a ***** uuid and a ***** email and a random ***** ip"
        assertThat(anonymizer.anonymizedString(originalString)).isEqualTo(expectedString)
    }

    // endregion

    // region anonymizeStringMap

    @Test
    fun `anonymizedStringMap anonymizes string fields if needed`() {
        val originalMap = mapOf(
            "key-1" to "string with some.pii@revenuecat.com and 192.168.1.1.",
            "key-2" to "string without pii",
            "key-3" to "string with other.pii@revenuecat.com"
        )
        val expectedMap = mapOf(
            "key-1" to "string with ***** and *****.",
            "key-2" to "string without pii",
            "key-3" to "string with *****"
        )
        assertThat(anonymizer.anonymizedStringMap(originalMap)).isEqualTo(expectedMap)
    }

    // endregion

    // region anonymizedMap

    @Test
    fun `anonymizedMap anonymizes all string fields if needed`() {
        val originalMap = mapOf(
            "key-1" to 1234,
            "key-2" to "string with some.pii@revenuecat.com and 192.168.1.1.",
            "key-3" to true,
            "key-4" to "string without pii",
            "key-5" to "string with other.pii@revenuecat.com"
        )
        val expectedMap = mapOf(
            "key-1" to 1234,
            "key-2" to "string with ***** and *****.",
            "key-3" to true,
            "key-4" to "string without pii",
            "key-5" to "string with *****"
        )
        assertThat(anonymizer.anonymizedMap(originalMap)).isEqualTo(expectedMap)
    }

    @Test
    fun `anonymizedMap anonymizes lists`() {
        val originalMap = mapOf(
            "key-1" to 1234,
            "key-2" to "string with some.pii@revenuecat.com and 192.168.1.1.",
            "key-3" to listOf("some string with some.pii@revenuecat.com", "and some without", 1234),
            "key-4" to "string without pii",
            "key-5" to "string with other.pii@revenuecat.com"
        )
        val expectedMap = mapOf(
            "key-1" to 1234,
            "key-2" to "string with ***** and *****.",
            "key-3" to listOf("some string with *****", "and some without", 1234),
            "key-4" to "string without pii",
            "key-5" to "string with *****"
        )
        assertThat(anonymizer.anonymizedMap(originalMap)).isEqualTo(expectedMap)
    }

    @Test
    fun `anonymizedMap anonymizes nested maps`() {
        val originalMap = mapOf(
            "key-1" to 1234,
            "key-2" to "string with some.pii@revenuecat.com and 192.168.1.1.",
            "key-3" to mapOf(
                "nested-key-1" to "some random.email@revenuecat.com",
                "nested-key-2" to 4321,
                "nested-key-3" to "string without PII"
            ),
            "key-4" to "string without pii",
            "key-5" to "string with other.pii@revenuecat.com"
        )
        val expectedMap = mapOf(
            "key-1" to 1234,
            "key-2" to "string with ***** and *****.",
            "key-3" to mapOf(
                "nested-key-1" to "some *****",
                "nested-key-2" to 4321,
                "nested-key-3" to "string without PII"
            ),
            "key-4" to "string without pii",
            "key-5" to "string with *****"
        )
        assertThat(anonymizer.anonymizedMap(originalMap)).isEqualTo(expectedMap)
    }

    // endregion
}
