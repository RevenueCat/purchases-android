package com.revenuecat.purchases

import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.utils.UrlConnection
import com.revenuecat.purchases.utils.UrlConnectionFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.net.URL

@RunWith(RobolectricTestRunner::class)
class ForcedFailureUrlConnectionFactoryTest {

    private companion object {
        const val BLOB_URL = "https://config.revenuecat-static.com/project/blob-ref"
    }

    private lateinit var delegate: UrlConnectionFactory
    private lateinit var delegateConnection: UrlConnection

    @Before
    fun setUp() {
        delegateConnection = mockk(relaxed = true)
        delegate = mockk<UrlConnectionFactory>().apply {
            every { createConnection(any(), any(), any(), any()) } returns delegateConnection
        }
    }

    @Test
    fun `delegates when the strategy does not force a connection failure`() {
        val factory = ForcedFailureUrlConnectionFactory(delegate, strategy(forceConnectionFailure = false))

        assertThat(factory.createConnection(BLOB_URL, 1, 2, "GET")).isSameAs(delegateConnection)
        verify(exactly = 1) { delegate.createConnection(BLOB_URL, 1, 2, "GET") }
    }

    @Test
    fun `throws without connecting when the strategy forces a connection failure`() {
        val factory = ForcedFailureUrlConnectionFactory(delegate, strategy(forceConnectionFailure = true))

        assertThatThrownBy { factory.createConnection(BLOB_URL, 1, 2, "GET") }
            .isInstanceOf(IOException::class.java)
            .hasMessageContaining(BLOB_URL)
        verify(exactly = 0) { delegate.createConnection(any(), any(), any(), any()) }
    }

    @Test
    fun `forcing server errors does not force connection failures by default`() {
        val factory = ForcedFailureUrlConnectionFactory(delegate, ForceServerErrorStrategy.failAll)

        assertThat(factory.createConnection(BLOB_URL, 1, 2, "GET")).isSameAs(delegateConnection)
    }

    private fun strategy(forceConnectionFailure: Boolean) = object : ForceServerErrorStrategy {
        override fun shouldForceServerError(baseURL: URL, endpoint: Endpoint): Boolean = true
        override fun shouldForceConnectionFailure(url: String): Boolean = forceConnectionFailure
    }
}
