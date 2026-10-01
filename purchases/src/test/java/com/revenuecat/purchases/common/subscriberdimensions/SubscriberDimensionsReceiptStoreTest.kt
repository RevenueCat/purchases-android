@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.common.subscriberdimensions

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.localrules.RulesDimensionValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Date

internal class SubscriberDimensionsReceiptStoreTest {

    private val deviceCache = mockk<DeviceCache>()
    private val store = SubscriberDimensionsReceiptStore(deviceCache)
    private val errorsLogged = mutableListOf<String>()
    private val originalLogHandler = currentLogHandler

    private var storedJson: String? = null

    @Before
    fun setUp() {
        currentLogHandler = object : LogHandler {
            override fun v(tag: String, msg: String) {}
            override fun d(tag: String, msg: String) {}
            override fun i(tag: String, msg: String) {}
            override fun w(tag: String, msg: String) {}
            override fun e(tag: String, msg: String, throwable: Throwable?) {
                errorsLogged.add(msg)
            }
        }
        every { deviceCache.getCachedSubscriberDimensionsJson(any()) } returns null
        every { deviceCache.getCachedSubscriberDimensionsJson(USER) } answers { storedJson }
        every { deviceCache.cacheSubscriberDimensions(USER, any()) } answers { storedJson = secondArg() }
        every { deviceCache.clearSubscriberDimensions(USER) } answers { storedJson = null }
    }

    @After
    fun tearDown() {
        currentLogHandler = originalLogHandler
    }

    @Test
    fun `nothing stored gives nothing`() {
        assertThat(store.get(USER)).isNull()
    }

    @Test
    fun `stores the response's timestamped dimensions and serves them parsed`() {
        store.store(USER, response(dimensions = """{"country":"ES","subscription_status":null}""", asOf = 100))

        assertThat(JSONObject(storedJson!!).getLong("as_of")).isEqualTo(100)
        assertThat(JSONObject(storedJson!!).getJSONObject("dimensions").getString("country")).isEqualTo("ES")
        assertThat(store.get(USER)).isEqualTo(
            SubscriberDimensions(
                values = mapOf(
                    "country" to RulesDimensionValue.StringValue("ES"),
                    "subscription_status" to RulesDimensionValue.NullValue,
                ),
                asOf = Date(100),
            ),
        )
    }

    @Test
    fun `an empty dimensions object with a timestamp is stored`() {
        store.store(USER, response(dimensions = "{}", asOf = 100))

        assertThat(store.get(USER)).isEqualTo(SubscriberDimensions(values = emptyMap(), asOf = Date(100)))
    }

    @Test
    fun `a response without a timestamp keeps the stored dimensions`() {
        store.store(USER, response(dimensions = """{"country":"ES"}""", asOf = 100))

        store.store(USER, JSONObject().put("dimensions", JSONObject("""{"country":"FR"}""")))

        assertThat(store.get(USER)?.values).containsEntry("country", RulesDimensionValue.StringValue("ES"))
        verify(exactly = 1) { deviceCache.cacheSubscriberDimensions(USER, any()) }
    }

    @Test
    fun `a response without dimensions keeps the stored dimensions`() {
        store.store(USER, response(dimensions = """{"country":"ES"}""", asOf = 100))

        store.store(USER, JSONObject().put("as_of", 200))
        store.store(USER, JSONObject().put("dimensions", "ES").put("as_of", 200))
        store.store(USER, JSONObject())

        assertThat(store.get(USER)?.asOf).isEqualTo(Date(100))
        verify(exactly = 1) { deviceCache.cacheSubscriberDimensions(USER, any()) }
    }

    @Test
    fun `the stored dimensions are parsed once while they are unchanged`() {
        storedJson = """{"dimensions":{"country":"ES"},"as_of":100}"""

        val first = store.get(USER)
        val second = store.get(USER)
        storedJson = """{"dimensions":{"country":"FR"},"as_of":200}"""
        val third = store.get(USER)

        assertThat(second).isSameAs(first)
        assertThat(third?.values).containsEntry("country", RulesDimensionValue.StringValue("FR"))
    }

    @Test
    fun `a different user has no dimensions`() {
        store.store(USER, response(dimensions = """{"country":"ES"}""", asOf = 100))

        assertThat(store.get("someone-else")).isNull()
    }

    @Test
    fun `dimensions stored without a timestamp by an earlier version are ignored without an error`() {
        storedJson = """{"country":"ES"}"""

        assertThat(store.get(USER)).isNull()
        assertThat(errorsLogged).isEmpty()
    }

    @Test
    fun `stored dimensions that can't be read are ignored`() {
        storedJson = "not json"
        assertThat(store.get(USER)).isNull()

        storedJson = """["an", "array"]"""
        assertThat(store.get(USER)).isNull()
    }

    @Test
    fun `discardAsync removes the stored copy once the config copy is newer`() = runTest {
        val store = SubscriberDimensionsReceiptStore(deviceCache, scope = this)
        store.store(USER, response(dimensions = """{"country":"ES"}""", asOf = 100))
        val superseded = store.get(USER)!!

        store.discardAsync(USER, superseded)
        verify(exactly = 0) { deviceCache.clearSubscriberDimensions(any()) }
        advanceUntilIdle()

        verify(exactly = 1) { deviceCache.clearSubscriberDimensions(USER) }
        assertThat(store.get(USER)).isNull()
    }

    @Test
    fun `discardAsync keeps a newer copy stored in the meantime`() = runTest {
        val store = SubscriberDimensionsReceiptStore(deviceCache, scope = this)
        store.store(USER, response(dimensions = """{"country":"ES"}""", asOf = 100))
        val superseded = store.get(USER)!!
        store.store(USER, response(dimensions = """{"country":"FR"}""", asOf = 300))

        store.discardAsync(USER, superseded)
        advanceUntilIdle()

        verify(exactly = 0) { deviceCache.clearSubscriberDimensions(any()) }
        assertThat(store.get(USER)?.asOf).isEqualTo(Date(300))
    }

    @Test
    fun `discardAsync does nothing when nothing is stored`() = runTest {
        val store = SubscriberDimensionsReceiptStore(deviceCache, scope = this)

        store.discardAsync(USER, SubscriberDimensions(emptyMap(), Date(100)))
        advanceUntilIdle()

        verify(exactly = 0) { deviceCache.clearSubscriberDimensions(any()) }
    }

    private fun response(dimensions: String, asOf: Long) = JSONObject()
        .put("request_date", "2019-08-16T10:30:42Z")
        .put("subscriber", JSONObject())
        .put("dimensions", JSONObject(dimensions))
        .put("as_of", asOf)

    private companion object {
        const val USER = "user"
    }
}
