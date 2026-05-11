package com.revenuecat.purchases.common.remoteconfig

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.utils.UrlConnection
import com.revenuecat.purchases.utils.UrlConnectionFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Fail.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.security.MessageDigest

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class TopicFetcherTest {

    private val testFolder = "temp_topic_fetcher_test_folder"

    private lateinit var rootDir: File
    private lateinit var applicationContext: Context
    private lateinit var urlConnectionFactory: UrlConnectionFactory

    @Before
    fun setUp() {
        rootDir = File(testFolder)
        if (rootDir.exists()) {
            error("Temp test folder should not exist before starting tests")
        }
        rootDir.mkdirs()

        applicationContext = mockk()
        every { applicationContext.noBackupFilesDir } returns rootDir

        urlConnectionFactory = mockk()
    }

    private fun TestScope.fetcher() = TopicFetcher(
        applicationContext = applicationContext,
        urlConnectionFactory = urlConnectionFactory,
        downloadDispatcher = UnconfinedTestDispatcher(testScheduler),
    )

    @After
    fun tearDown() {
        rootDir.deleteRecursively()
    }

    @Test
    fun `returns success synchronously when target file already exists`() = runTest {
        val payload = """{"cached":true}""".toByteArray()
        val blobRef = sha256Hex(payload)
        val target = topicFile(Topic.PRODUCT_ENTITLEMENT_MAPPING, blobRef)
        target.parentFile?.mkdirs()
        target.writeBytes(payload)

        val error = fetcher().fetchTopicIfNeeded(
            topic = Topic.PRODUCT_ENTITLEMENT_MAPPING,
            entryId = "default",
            topicEntry = topicEntry(blobRef),
            source = source("https://assets.example/{blob_ref}"),
        )

        assertThat(error).isNull()
        verify(exactly = 0) { urlConnectionFactory.createConnection(any(), any()) }
    }

    @Test
    fun `downloads, verifies SHA-256, and stores at the expected target path`() = runTest {
        val payload = """{"hello":"world"}""".toByteArray()
        val blobRef = sha256Hex(payload)
        mockSuccessfulDownload("https://assets.example/$blobRef", payload)

        val error = fetcher().fetchTopicIfNeeded(
            topic = Topic.PRODUCT_ENTITLEMENT_MAPPING,
            entryId = "default",
            topicEntry = topicEntry(blobRef),
            source = source("https://assets.example/{blob_ref}"),
        )

        assertThat(error).isNull()
        val target = topicFile(Topic.PRODUCT_ENTITLEMENT_MAPPING, blobRef)
        assertThat(target).exists()
        assertThat(target.readBytes()).isEqualTo(payload)
        assertThat(leftoverTempFiles(target.parentFile)).isEmpty()
    }

    @Test
    fun `substitutes blob_ref placeholder in the asset source url format`() = runTest {
        val payload = """{}""".toByteArray()
        val blobRef = sha256Hex(payload)
        val expectedUrl = "https://cdn.example/topics/$blobRef"
        mockSuccessfulDownload(expectedUrl, payload)

        val error = fetcher().fetchTopicIfNeeded(
            topic = Topic.PRODUCT_ENTITLEMENT_MAPPING,
            entryId = "default",
            topicEntry = topicEntry(blobRef),
            source = source("https://cdn.example/topics/{blob_ref}"),
        )

        if (error != null) fail<Unit>("Expected success, got error: $error")
        verify(exactly = 1) { urlConnectionFactory.createConnection(expectedUrl, any()) }
    }

    @Test
    fun `surfaces error and writes no target file when HTTP response is non-200`() = runTest {
        val payload = """{}""".toByteArray()
        val blobRef = sha256Hex(payload)
        val url = "https://assets.example/$blobRef"
        val connection = mockk<UrlConnection>(relaxed = true).also {
            every { it.responseCode } returns HttpURLConnection.HTTP_NOT_FOUND
        }
        every { urlConnectionFactory.createConnection(url, any()) } returns connection

        val error = fetcher().fetchTopicIfNeeded(
            topic = Topic.PRODUCT_ENTITLEMENT_MAPPING,
            entryId = "default",
            topicEntry = topicEntry(blobRef),
            source = source("https://assets.example/{blob_ref}"),
        )

        assertThat(error).isNotNull
        assertThat(topicFile(Topic.PRODUCT_ENTITLEMENT_MAPPING, blobRef)).doesNotExist()
        verify { connection.disconnect() }
    }

    @Test
    fun `surfaces error and leaves no temp files when reading the input stream throws`() = runTest {
        val payload = """{}""".toByteArray()
        val blobRef = sha256Hex(payload)
        val url = "https://assets.example/$blobRef"
        val throwingStream = object : java.io.InputStream() {
            override fun read(): Int = throw IOException("simulated read failure")
        }
        val connection = mockk<UrlConnection>(relaxed = true).also {
            every { it.responseCode } returns HttpURLConnection.HTTP_OK
            every { it.inputStream } returns throwingStream
        }
        every { urlConnectionFactory.createConnection(url, any()) } returns connection

        val error: PurchasesError? = fetcher().fetchTopicIfNeeded(
            topic = Topic.PRODUCT_ENTITLEMENT_MAPPING,
            entryId = "default",
            topicEntry = topicEntry(blobRef),
            source = source("https://assets.example/{blob_ref}"),
        )

        assertThat(error).isNotNull
        val target = topicFile(Topic.PRODUCT_ENTITLEMENT_MAPPING, blobRef)
        assertThat(target).doesNotExist()
        assertThat(leftoverTempFiles(target.parentFile)).isEmpty()
    }

    @Test
    fun `surfaces error when downloaded bytes don't match the expected SHA-256`() = runTest {
        val payload = """{"actual":"contents"}""".toByteArray()
        val wrongBlobRef = "0".repeat(64)
        val url = "https://assets.example/$wrongBlobRef"
        mockSuccessfulDownload(url, payload)

        val error = fetcher().fetchTopicIfNeeded(
            topic = Topic.PRODUCT_ENTITLEMENT_MAPPING,
            entryId = "default",
            topicEntry = topicEntry(wrongBlobRef),
            source = source("https://assets.example/{blob_ref}"),
        )

        assertThat(error).isNotNull
        assertThat(error?.code).isEqualTo(PurchasesErrorCode.NetworkError)
        val target = topicFile(Topic.PRODUCT_ENTITLEMENT_MAPPING, wrongBlobRef)
        assertThat(target).doesNotExist()
        assertThat(leftoverTempFiles(target.parentFile)).isEmpty()
    }

    @Test
    fun `multiple entryIds for the same topic write to distinct file paths`() = runTest {
        val payloadA = """{"entryId":"A"}""".toByteArray()
        val payloadB = """{"entryId":"B"}""".toByteArray()
        val blobRefA = sha256Hex(payloadA)
        val blobRefB = sha256Hex(payloadB)
        mockSuccessfulDownload("https://assets.example/$blobRefA", payloadA)
        mockSuccessfulDownload("https://assets.example/$blobRefB", payloadB)

        val errorA = fetcher().fetchTopicIfNeeded(
            topic = Topic.PRODUCT_ENTITLEMENT_MAPPING,
            entryId = "default",
            topicEntry = topicEntry(blobRefA),
            source = source("https://assets.example/{blob_ref}"),
        )
        if (errorA != null) fail<Unit>("Expected success, got error: $errorA")
        val errorB = fetcher().fetchTopicIfNeeded(
            topic = Topic.PRODUCT_ENTITLEMENT_MAPPING,
            entryId = "EXPERIMENT_A",
            topicEntry = topicEntry(blobRefB),
            source = source("https://assets.example/{blob_ref}"),
        )
        if (errorB != null) fail<Unit>("Expected success, got error: $errorB")

        val targetA = topicFile(Topic.PRODUCT_ENTITLEMENT_MAPPING, blobRefA)
        val targetB = topicFile(Topic.PRODUCT_ENTITLEMENT_MAPPING, blobRefB)
        assertThat(targetA).exists()
        assertThat(targetB).exists()
        assertThat(targetA).isNotEqualTo(targetB)
        assertThat(targetA.readBytes()).isEqualTo(payloadA)
        assertThat(targetB.readBytes()).isEqualTo(payloadB)
    }

    @Test
    fun `rejects malformed blobRef before any IO`() = runTest {
        val error = fetcher().fetchTopicIfNeeded(
            topic = Topic.PRODUCT_ENTITLEMENT_MAPPING,
            entryId = "default",
            topicEntry = topicEntry("not-hex"),
            source = source("https://assets.example/{blob_ref}"),
        )

        assertThat(error).isNotNull
        assertThat(error?.code).isEqualTo(PurchasesErrorCode.UnexpectedBackendResponseError)
        verify(exactly = 0) { urlConnectionFactory.createConnection(any(), any()) }
        val topicDir = File(File(rootDir, "RevenueCat/topics"), Topic.PRODUCT_ENTITLEMENT_MAPPING.key)
        assertThat(topicDir.exists() && topicDir.listFiles()?.isNotEmpty() == true).isFalse
    }

    @Test
    fun `rejects blobRef containing path separators`() = runTest {
        val malicious = "../../escape"
        val error = fetcher().fetchTopicIfNeeded(
            topic = Topic.PRODUCT_ENTITLEMENT_MAPPING,
            entryId = "default",
            topicEntry = topicEntry(malicious),
            source = source("https://assets.example/{blob_ref}"),
        )

        assertThat(error).isNotNull
        assertThat(error?.code).isEqualTo(PurchasesErrorCode.UnexpectedBackendResponseError)
        verify(exactly = 0) { urlConnectionFactory.createConnection(any(), any()) }
        val outsideTarget = File(rootDir.parentFile, "escape")
        assertThat(outsideTarget).doesNotExist()
    }

    @Test
    fun `rejects blobRef shorter than 64 hex chars`() = runTest {
        val error = fetcher().fetchTopicIfNeeded(
            topic = Topic.PRODUCT_ENTITLEMENT_MAPPING,
            entryId = "default",
            topicEntry = topicEntry("abc123"),
            source = source("https://assets.example/{blob_ref}"),
        )

        assertThat(error).isNotNull
        assertThat(error?.code).isEqualTo(PurchasesErrorCode.UnexpectedBackendResponseError)
        verify(exactly = 0) { urlConnectionFactory.createConnection(any(), any()) }
    }

    @Test
    fun `rejects blobRef longer than 64 hex chars`() = runTest {
        val tooLong = "a".repeat(65)
        val error = fetcher().fetchTopicIfNeeded(
            topic = Topic.PRODUCT_ENTITLEMENT_MAPPING,
            entryId = "default",
            topicEntry = topicEntry(tooLong),
            source = source("https://assets.example/{blob_ref}"),
        )

        assertThat(error).isNotNull
        assertThat(error?.code).isEqualTo(PurchasesErrorCode.UnexpectedBackendResponseError)
        verify(exactly = 0) { urlConnectionFactory.createConnection(any(), any()) }
    }

    @Test
    fun `rejects 64-char blobRef containing non-hex letters`() = runTest {
        val nonHex = "g".repeat(64)
        val error = fetcher().fetchTopicIfNeeded(
            topic = Topic.PRODUCT_ENTITLEMENT_MAPPING,
            entryId = "default",
            topicEntry = topicEntry(nonHex),
            source = source("https://assets.example/{blob_ref}"),
        )

        assertThat(error).isNotNull
        assertThat(error?.code).isEqualTo(PurchasesErrorCode.UnexpectedBackendResponseError)
        verify(exactly = 0) { urlConnectionFactory.createConnection(any(), any()) }
    }

    @Test
    fun `accepts mixed-case 64-char hex blobRef`() = runTest {
        val mixedCaseHex = "ABCDEFabcdef" + "0".repeat(52)
        val url = "https://assets.example/$mixedCaseHex"
        mockSuccessfulDownload(url, "ignored".toByteArray())

        val error = fetcher().fetchTopicIfNeeded(
            topic = Topic.PRODUCT_ENTITLEMENT_MAPPING,
            entryId = "default",
            topicEntry = topicEntry(mixedCaseHex),
            source = source("https://assets.example/{blob_ref}"),
        )

        // Validation passes; download is attempted (checksum fails because mixedCaseHex is not the real SHA-256
        // of "ignored", which is expected — the point is that the request was made).
        assertThat(error).isNotNull
        assertThat(error?.code).isEqualTo(PurchasesErrorCode.NetworkError)
        verify(exactly = 1) { urlConnectionFactory.createConnection(url, any()) }
    }

    private fun mockSuccessfulDownload(url: String, payload: ByteArray) {
        val connection = mockk<UrlConnection>(relaxed = true).also {
            every { it.responseCode } returns HttpURLConnection.HTTP_OK
            every { it.inputStream } returns ByteArrayInputStream(payload)
        }
        every { urlConnectionFactory.createConnection(url, any()) } returns connection
    }

    private fun topicFile(topic: Topic, blobRef: String): File =
        File(File(File(rootDir, "RevenueCat/topics"), topic.key), blobRef)

    private fun leftoverTempFiles(dir: File?): List<File> =
        dir?.listFiles()?.filter { it.name.startsWith("rc_topic_") }.orEmpty()

    private fun topicEntry(blobRef: String) = TopicEntry(blobRef = blobRef)

    private fun source(urlFormat: String) =
        BlobSource(
            id = "primary",
            urlFormat = urlFormat,
            priority = 0,
            weight = 100,
        )

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
