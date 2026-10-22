package com.revenuecat.purchases

import android.content.Context
import android.os.Parcel
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.CustomerInfoFactory
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.createCustomerInfo
import com.revenuecat.purchases.common.networking.ETagManager
import com.revenuecat.purchases.common.networking.ETagPayloadStore
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.common.networking.buildPostReceiptResponse
import com.revenuecat.purchases.common.verification.SignatureVerificationResult
import com.revenuecat.purchases.subscriberattributes.SubscriberAttribute
import com.revenuecat.purchases.subscriberattributes.remoteSubscriberAttributes
import com.revenuecat.purchases.utils.Responses
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

/**
 * The `subscriber.subscriber_attributes` object of a CustomerInfo response, read the way the SDK reads it, from
 * each place a CustomerInfo comes from: a network response (main, load shedder, post receipt, ETag replay), the
 * on-device cache, and a parcel.
 *
 * Wire format, as the backend sends it and iOS decodes it:
 * `"subscriber_attributes": {"<key>": {"value": "<string or null>", "updated_at_ms": <epoch ms>}}`
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class CustomerInfoSubscriberAttributesTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val responseWithAttributes: String = JSONObject(Responses.validFullPurchaserResponse).apply {
        getJSONObject("subscriber").put(
            "subscriber_attributes",
            JSONObject(
                """
                {
                  "${'$'}email": {"value": "user@example.com", "updated_at_ms": 1600000000000},
                  "${'$'}displayName": {"value": "Dave", "updated_at_ms": 1600000000123},
                  "${'$'}mediaSource": {"value": "Organic", "updated_at_ms": 1650000000000},
                  "custom_key": {"value": "custom value", "updated_at_ms": 1650000000000},
                  "myCustomAttribute": {"value": "camel", "updated_at_ms": 1650000000000},
                  "deleted": {"value": null, "updated_at_ms": 1700000000000}
                }
                """.trimIndent(),
            ),
        )
    }.toString()

    private val expectedAttributes = mapOf(
        "\$email" to synced("\$email", "user@example.com", 1_600_000_000_000),
        "\$displayName" to synced("\$displayName", "Dave", 1_600_000_000_123),
        "\$mediaSource" to synced("\$mediaSource", "Organic", 1_650_000_000_000),
        "custom_key" to synced("custom_key", "custom value", 1_650_000_000_000),
        "myCustomAttribute" to synced("myCustomAttribute", "camel", 1_650_000_000_000),
        "deleted" to synced("deleted", null, 1_700_000_000_000),
    )

    // region The response itself

    @Test
    fun `a full response's subscriber attributes are parsed`() {
        val remote = createCustomerInfo(responseWithAttributes).remoteSubscriberAttributes()!!

        assertThat(remote.attributes).isEqualTo(expectedAttributes)
        assertThat(remote.unreadableKeys).isEmpty()
    }

    @Test
    fun `subscriber attributes don't change how the rest of the response is parsed`() {
        val with = createCustomerInfo(responseWithAttributes)
        val without = createCustomerInfo(Responses.validFullPurchaserResponse)

        assertThat(with).isEqualTo(without)
        assertThat(with.entitlements.all.keys).isEqualTo(without.entitlements.all.keys)
        assertThat(with.activeSubscriptions).isEqualTo(without.activeSubscriptions)
        assertThat(with.allPurchasedProductIds).isEqualTo(without.allPurchasedProductIds)
        assertThat(with.originalAppUserId).isEqualTo(without.originalAppUserId)
    }

    @Test
    fun `a response without subscriber attributes has none to merge`() {
        assertThat(createCustomerInfo(Responses.validFullPurchaserResponse).remoteSubscriberAttributes()).isNull()
        assertThat(createCustomerInfo(Responses.validEmptyPurchaserResponse).remoteSubscriberAttributes()).isNull()
    }

    @Test
    fun `subscriber attributes are only read from inside subscriber`() {
        val misplaced = JSONObject(Responses.validFullPurchaserResponse)
            .put("subscriber_attributes", JSONObject().put("band", entry("Rush", 1_600_000_000_000)))

        assertThat(createCustomerInfo(misplaced).remoteSubscriberAttributes()).isNull()
    }

    @Test
    fun `attribute keys are kept exactly as sent`() {
        val keys = listOf(
            "\$email",
            "custom_key",
            "myCustomAttribute",
            "UPPER_CASE",
            "with.dot",
            "with space",
            "with-dash",
            "ünïcödé_🎸",
            "123",
        )
        val info = withAttributes(*keys.map { it to entry("v", 1_600_000_000_000) }.toTypedArray())

        val attributes = info.remoteSubscriberAttributes()!!.attributes

        assertThat(attributes.keys).containsExactlyInAnyOrderElementsOf(keys)
        attributes.forEach { (key, attribute) -> assertThat(attribute.key.backendKey).isEqualTo(key) }
    }

    @Test
    fun `attribute values are kept exactly as sent`() {
        val values = mapOf(
            "empty" to "",
            "whitespace" to "  padded  ",
            "unicode" to "Ünïcödé 🎸 日本語",
            "json_looking" to """{"nested": [1, 2]}""",
            "newlines" to "line 1\nline 2",
            "long" to "x".repeat(10_000),
        )
        val info = withAttributes(*values.map { (key, value) -> key to entry(value, 1_600_000_000_000) }.toTypedArray())

        val parsed = info.remoteSubscriberAttributes()!!.attributes.mapValues { it.value.value }

        assertThat(parsed).isEqualTo(values)
    }

    @Test
    fun `updated_at_ms keeps millisecond precision`() {
        val info = withAttributes(
            "ms" to entry("a", 1_600_000_000_123),
            "epoch" to entry("b", 0),
            "far_future" to entry("c", 4_102_444_800_000),
            "fractional" to entry("d", 1_600_000_000_123.9),
        )

        val setTimes = info.remoteSubscriberAttributes()!!.attributes.mapValues { it.value.setTime }

        assertThat(setTimes).isEqualTo(
            mapOf(
                "ms" to Date(1_600_000_000_123),
                "epoch" to Date(0),
                "far_future" to Date(4_102_444_800_000),
                "fractional" to Date(1_600_000_000_123),
            ),
        )
    }

    @Test
    fun `the timestamp is read from snake_case updated_at_ms only`() {
        val info = withAttributes(
            "camel" to JSONObject().put("value", "v").put("updatedAtMs", 1_600_000_000_000),
        )

        val remote = info.remoteSubscriberAttributes()!!

        assertThat(remote.attributes).isEmpty()
        assertThat(remote.unreadableKeys).containsExactly("camel")
    }

    @Test
    fun `every parsed attribute is marked synced`() {
        val remote = createCustomerInfo(responseWithAttributes).remoteSubscriberAttributes()!!

        assertThat(remote.attributes.values).allMatch { it.isSynced }
    }

    // endregion The response itself

    // region Where CustomerInfo comes from

    @Test
    fun `parsed from a network response`() {
        val info = CustomerInfoFactory.buildCustomerInfo(httpResult(responseWithAttributes))

        assertThat(info.remoteSubscriberAttributes()!!.attributes).isEqualTo(expectedAttributes)
    }

    @Test
    fun `parsed from a load shedder response`() {
        val info = CustomerInfoFactory.buildCustomerInfo(httpResult(responseWithAttributes, isLoadShedder = true))

        assertThat(info.originalSource).isEqualTo(CustomerInfoOriginalSource.LOAD_SHEDDER)
        assertThat(info.remoteSubscriberAttributes()!!.attributes).isEqualTo(expectedAttributes)
    }

    @Test
    fun `parsed from a post receipt response`() {
        val response = buildPostReceiptResponse(httpResult(responseWithAttributes))

        assertThat(response.customerInfo.remoteSubscriberAttributes()!!.attributes).isEqualTo(expectedAttributes)
    }

    @Test
    fun `parsed from a response replayed from the ETag cache on a 304`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences("customer_info_attributes_etag_test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        val eTagManager = ETagManager(
            mockk(),
            lazy { preferences },
            payloadStore = ETagPayloadStore(temporaryFolder.newFolder()),
        )
        val url = "https://api.revenuecat.com/v1/subscribers/waldo"
        eTagManager.getHTTPResultFromCacheOrBackend(
            RCHTTPStatusCodes.SUCCESS,
            responseWithAttributes,
            eTagHeader = "etag-1",
            urlString = url,
            refreshETag = false,
            requestDate = null,
            verificationResult = SignatureVerificationResult.NotRequested,
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )

        val replayed = eTagManager.getHTTPResultFromCacheOrBackend(
            RCHTTPStatusCodes.NOT_MODIFIED,
            "",
            eTagHeader = "etag-1",
            urlString = url,
            refreshETag = false,
            requestDate = null,
            verificationResult = SignatureVerificationResult.NotRequested,
            isLoadShedderResponse = false,
            isFallbackURL = false,
        )!!

        assertThat(replayed.responseCode).isEqualTo(RCHTTPStatusCodes.SUCCESS)
        val info = CustomerInfoFactory.buildCustomerInfo(replayed)
        assertThat(info.remoteSubscriberAttributes()!!.attributes).isEqualTo(expectedAttributes)
    }

    @Test
    fun `still parsed after the raw data round trips through a string`() {
        val roundTripped = createCustomerInfo(createCustomerInfo(responseWithAttributes).rawData.toString())

        assertThat(roundTripped.remoteSubscriberAttributes()!!.attributes).isEqualTo(expectedAttributes)
    }

    @Test
    fun `still parsed from the customer info cache`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences("customer_info_attributes_cache_test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        val deviceCache = DeviceCache(preferences, apiKey = "api_key")

        val fetched = CustomerInfoFactory.buildCustomerInfo(httpResult(responseWithAttributes))
        deviceCache.cacheCustomerInfo("waldo", fetched)
        val cached = deviceCache.getCachedCustomerInfo("waldo")!!

        assertThat(cached.remoteSubscriberAttributes()!!.attributes).isEqualTo(expectedAttributes)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `still parsed after the customer info is parcelled`() {
        val info = createCustomerInfo(responseWithAttributes)
        val parcel = Parcel.obtain()
        val restored = try {
            parcel.writeParcelable(info, 0)
            parcel.setDataPosition(0)
            parcel.readParcelable<CustomerInfo>(CustomerInfo::class.java.classLoader)!!
        } finally {
            parcel.recycle()
        }

        assertThat(restored.remoteSubscriberAttributes()!!.attributes).isEqualTo(expectedAttributes)
    }

    // endregion Where CustomerInfo comes from

    private fun withAttributes(vararg entries: Pair<String, JSONObject>): CustomerInfo {
        val attributes = JSONObject().apply { entries.forEach { (key, entry) -> put(key, entry) } }
        return createCustomerInfo(
            JSONObject(Responses.validFullPurchaserResponse).apply {
                getJSONObject("subscriber").put("subscriber_attributes", attributes)
            },
        )
    }

    private fun entry(value: String?, updatedAtMs: Number): JSONObject =
        JSONObject().put("value", value ?: JSONObject.NULL).put("updated_at_ms", updatedAtMs)

    private fun httpResult(body: String, isLoadShedder: Boolean = false) = HTTPResult(
        RCHTTPStatusCodes.SUCCESS,
        body,
        HTTPResult.Origin.BACKEND,
        null,
        SignatureVerificationResult.NotRequested,
        isLoadShedderResponse = isLoadShedder,
        isFallbackURL = false,
    )

    private fun synced(key: String, value: String?, updatedAtMs: Long) =
        SubscriberAttribute(key, value, setTime = Date(updatedAtMs), isSynced = true)
}
