package com.revenuecat.purchases.common.networking

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.utils.TestUrlConnection
import com.revenuecat.purchases.utils.TestUrlConnectionFactory
import com.revenuecat.purchases.utils.UrlConnection
import com.revenuecat.purchases.utils.UrlConnectionFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class SourceHealthCheckerTest {

    private class FakeDateProvider(private val currentTime: AtomicLong = AtomicLong(1_000_000L)) : DateProvider {
        override val now: Date
            get() = Date(currentTime.get())

        fun advanceTime(millis: Long) {
            currentTime.addAndGet(millis)
        }
    }

    private val sourceBaseUrl = "https://api.revenuecat.com/"
    private val healthUrl = "https://api.revenuecat.com/v1/health/connectivity"

    private fun connection(responseCode: Int) = TestUrlConnection(responseCode, ByteArrayInputStream(ByteArray(0)))

    private fun checker(
        factory: UrlConnectionFactory,
        dateProvider: DateProvider = FakeDateProvider(),
    ) = SourceHealthChecker(factory, dateProvider)

    @Test
    fun `2xx responses are healthy`() {
        listOf(200, 204, 299).forEach { responseCode ->
            val factory = TestUrlConnectionFactory(mapOf(healthUrl to connection(responseCode)))
            assertThat(checker(factory).isHealthy(sourceBaseUrl))
                .withFailMessage("Expected $responseCode to be healthy")
                .isTrue()
        }
    }

    @Test
    fun `non-2xx responses are not healthy`() {
        listOf(301, 404, 500, 503).forEach { responseCode ->
            val factory = TestUrlConnectionFactory(mapOf(healthUrl to connection(responseCode)))
            assertThat(checker(factory).isHealthy(sourceBaseUrl))
                .withFailMessage("Expected $responseCode to not be healthy")
                .isFalse()
        }
    }

    @Test
    fun `connection failure is not healthy`() {
        val factory = TestUrlConnectionFactory(connectionProvider = { throw IOException("no route to host") })
        assertThat(checker(factory).isHealthy(sourceBaseUrl)).isFalse()
    }

    @Test
    fun `checks the health connectivity path of the source`() {
        val factory = TestUrlConnectionFactory(mapOf(healthUrl to connection(200)))
        checker(factory).isHealthy(sourceBaseUrl)
        assertThat(factory.createdConnections).containsExactly(healthUrl)
    }

    @Test
    fun `builds the health url for a base url without a trailing slash`() {
        val factory = TestUrlConnectionFactory(mapOf(healthUrl to connection(200)))
        checker(factory).isHealthy("https://api.revenuecat.com")
        assertThat(factory.createdConnections).containsExactly(healthUrl)
    }

    @Test
    fun `disconnects the connection`() {
        val connection = connection(200)
        val factory = TestUrlConnectionFactory(mapOf(healthUrl to connection))
        checker(factory).isHealthy(sourceBaseUrl)
        assertThat(connection.isDisconnected).isTrue()
    }

    @Test
    fun `disconnects the connection when reading the response fails`() {
        val connection = object : UrlConnection {
            var isDisconnected = false
            override val responseCode: Int
                get() = throw IOException("connection reset")
            override val inputStream = ByteArrayInputStream(ByteArray(0))
            override fun disconnect() {
                isDisconnected = true
            }
        }
        val factory = object : UrlConnectionFactory {
            override fun createConnection(url: String, requestMethod: String): UrlConnection = connection
        }
        assertThat(SourceHealthChecker(factory, FakeDateProvider()).isHealthy(sourceBaseUrl)).isFalse()
        assertThat(connection.isDisconnected).isTrue()
    }

    @Test
    fun `caches the result within its validity window`() {
        val factory = TestUrlConnectionFactory(connectionProvider = { connection(200) })
        val checker = checker(factory)
        assertThat(checker.isHealthy(sourceBaseUrl)).isTrue()
        assertThat(checker.isHealthy(sourceBaseUrl)).isTrue()
        assertThat(factory.createdConnections).hasSize(1)
    }

    @Test
    fun `caches unhealthy results too`() {
        val factory = TestUrlConnectionFactory(connectionProvider = { connection(500) })
        val checker = checker(factory)
        assertThat(checker.isHealthy(sourceBaseUrl)).isFalse()
        assertThat(checker.isHealthy(sourceBaseUrl)).isFalse()
        assertThat(factory.createdConnections).hasSize(1)
    }

    @Test
    fun `checks again once the cached result expires`() {
        val dateProvider = FakeDateProvider()
        val factory = TestUrlConnectionFactory(connectionProvider = { connection(200) })
        val checker = checker(factory, dateProvider)
        checker.isHealthy(sourceBaseUrl)
        dateProvider.advanceTime(10_000L)
        checker.isHealthy(sourceBaseUrl)
        assertThat(factory.createdConnections).hasSize(2)
    }

    @Test
    fun `caches results per source`() {
        val otherHealthUrl = "https://api.rc-backup.com/v1/health/connectivity"
        val factory = TestUrlConnectionFactory(
            mapOf(
                healthUrl to connection(200),
                otherHealthUrl to connection(503),
            ),
        )
        val checker = checker(factory)
        assertThat(checker.isHealthy(sourceBaseUrl)).isTrue()
        assertThat(checker.isHealthy("https://api.rc-backup.com/")).isFalse()
        assertThat(factory.createdConnections).containsExactly(healthUrl, otherHealthUrl)
    }

    @Test
    fun `concurrent checks for the same source share one request`() {
        val checkStarted = CountDownLatch(1)
        val releaseCheck = CountDownLatch(1)
        val factory = TestUrlConnectionFactory(
            connectionProvider = {
                checkStarted.countDown()
                assertThat(releaseCheck.await(5, TimeUnit.SECONDS)).isTrue()
                connection(200)
            },
        )
        val checker = checker(factory)
        var firstResult = false
        var secondResult = false
        val firstCheck = thread { firstResult = checker.isHealthy(sourceBaseUrl) }
        assertThat(checkStarted.await(5, TimeUnit.SECONDS)).isTrue()
        val secondCheck = thread { secondResult = checker.isHealthy(sourceBaseUrl) }
        releaseCheck.countDown()
        firstCheck.join(5_000)
        secondCheck.join(5_000)
        assertThat(firstResult).isTrue()
        assertThat(secondResult).isTrue()
        assertThat(factory.createdConnections).hasSize(1)
    }
}
