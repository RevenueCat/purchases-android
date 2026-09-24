package com.revenuecat.purchases.common

import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.RequestLane
import com.revenuecat.purchases.strings.NetworkStrings
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class BackendLanesTest {

    private val defaultDispatcher = mockk<Dispatcher>()
    private val remoteConfigDispatcher = mockk<Dispatcher>()

    private val warnings = mutableListOf<String>()
    private lateinit var previousLogHandler: LogHandler
    private lateinit var previousLogLevel: LogLevel

    @Before
    fun setUp() {
        previousLogHandler = currentLogHandler
        previousLogLevel = Config.logLevel
        Config.logLevel = LogLevel.VERBOSE
        currentLogHandler = object : LogHandler {
            override fun v(tag: String, msg: String) = Unit
            override fun d(tag: String, msg: String) = Unit
            override fun i(tag: String, msg: String) = Unit
            override fun w(tag: String, msg: String) {
                warnings.add(msg)
            }
            override fun e(tag: String, msg: String, throwable: Throwable?) = Unit
        }
    }

    @After
    fun tearDown() {
        currentLogHandler = previousLogHandler
        Config.logLevel = previousLogLevel
    }

    @Test
    fun `dedicated lane returns its own dispatcher without warning`() {
        val lanes = BackendLanes(defaultDispatcher, mapOf(RequestLane.REMOTE_CONFIG to remoteConfigDispatcher))

        assertThat(lanes[RequestLane.REMOTE_CONFIG]).isSameAs(remoteConfigDispatcher)
        assertThat(warnings).isEmpty()
    }

    @Test
    fun `default lane returns the default dispatcher without warning`() {
        val lanes = BackendLanes(defaultDispatcher, mapOf(RequestLane.REMOTE_CONFIG to remoteConfigDispatcher))

        assertThat(lanes[RequestLane.DEFAULT]).isSameAs(defaultDispatcher)
        assertThat(warnings).isEmpty()
    }

    @Test
    fun `lane without dedicated dispatcher falls back to default and warns when other lanes are dedicated`() {
        val lanes = BackendLanes(defaultDispatcher, mapOf(RequestLane.REMOTE_CONFIG to remoteConfigDispatcher))

        assertThat(lanes[RequestLane.EVENTS]).isSameAs(defaultDispatcher)
        assertThat(warnings).containsExactly(
            NetworkStrings.MISSING_DEDICATED_LANE_DISPATCHER.format(RequestLane.EVENTS.name),
        )
    }

    @Test
    fun `every lane falls back to default silently when no lane is dedicated`() {
        val lanes = BackendLanes(defaultDispatcher, emptyMap())

        for (lane in RequestLane.values()) {
            assertThat(lanes[lane]).isSameAs(defaultDispatcher)
        }
        assertThat(warnings).isEmpty()
    }

    @Test
    fun `endpoint resolves to the dispatcher of its lane`() {
        val lanes = BackendLanes(defaultDispatcher, mapOf(RequestLane.REMOTE_CONFIG to remoteConfigDispatcher))

        assertThat(lanes[Endpoint.GetRemoteConfig("app")]).isSameAs(remoteConfigDispatcher)
        assertThat(lanes[Endpoint.PostReceipt]).isSameAs(defaultDispatcher)
    }
}
