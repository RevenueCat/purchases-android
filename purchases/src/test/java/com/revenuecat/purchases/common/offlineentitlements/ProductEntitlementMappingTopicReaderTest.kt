package com.revenuecat.purchases.common.offlineentitlements

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.remoteconfig.Topic
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ProductEntitlementMappingTopicReaderTest {

    private val testFolder = "temp_topic_reader_test_folder"
    private val topicDirRelative = "RevenueCat/topics/${Topic.PRODUCT_ENTITLEMENT_MAPPING.key}"

    private lateinit var rootDir: File
    private lateinit var topicDir: File
    private lateinit var applicationContext: Context
    private var dirAccessCount: Int = 0

    @Before
    fun setUp() {
        rootDir = File(testFolder)
        if (rootDir.exists()) {
            error("Temp test folder should not exist before starting tests")
        }
        rootDir.mkdirs()
        topicDir = File(rootDir, topicDirRelative)

        applicationContext = mockk()
        dirAccessCount = 0
        every { applicationContext.noBackupFilesDir } answers {
            dirAccessCount++
            rootDir
        }
    }

    @After
    fun tearDown() {
        rootDir.deleteRecursively()
    }

    @Test
    fun `read returns null when the topic directory does not exist`() = runTest {
        val reader = ProductEntitlementMappingTopicReader(applicationContext, UnconfinedTestDispatcher(testScheduler))
        val mapping = reader.read()
        assertThat(mapping).isNull()
    }

    @Test
    fun `read returns null when the topic directory is empty`() = runTest {
        topicDir.mkdirs()
        val reader = ProductEntitlementMappingTopicReader(applicationContext, UnconfinedTestDispatcher(testScheduler))
        val mapping = reader.read()
        assertThat(mapping).isNull()
    }

    @Test
    fun `read parses the blob and exposes the mapping`() = runTest {
        writeBlob("abc", validMappingJson("monthly", "pro"))
        val reader = ProductEntitlementMappingTopicReader(applicationContext, UnconfinedTestDispatcher(testScheduler))

        val mapping = reader.read()!!

        assertThat(mapping.mappings).containsKey("monthly")
        assertThat(mapping.mappings["monthly"]?.entitlements).containsExactly("pro")
        assertThat(mapping.loadedFromCache).isTrue
    }

    @Test
    fun `second read after a successful load returns the cached mapping without re-reading from disk`() = runTest {
        writeBlob("abc", validMappingJson("monthly", "pro"))
        val reader = ProductEntitlementMappingTopicReader(applicationContext, UnconfinedTestDispatcher(testScheduler))

        val firstMapping = reader.read()!!
        val accessesAfterFirst = dirAccessCount

        // Replace the on-disk blob: cached read should return the original, not the new contents.
        topicDir.listFiles()?.forEach { it.delete() }
        writeBlob("def", validMappingJson("annual", "premium"))

        val second = reader.read()
        assertThat(second).isSameAs(firstMapping)
        assertThat(dirAccessCount).isEqualTo(accessesAfterFirst)
    }

    @Test
    fun `read ignores files starting with the rc_topic_ temp prefix`() = runTest {
        topicDir.mkdirs()
        File(topicDir, "rc_topic_inflight.tmp").writeText("not a real blob")
        val reader = ProductEntitlementMappingTopicReader(applicationContext, UnconfinedTestDispatcher(testScheduler))

        val mapping = reader.read()
        assertThat(mapping).isNull()
    }

    @Test
    fun `read returns null when the blob payload is not valid JSON`() = runTest {
        writeBlob("abc", "not-json")
        val reader = ProductEntitlementMappingTopicReader(applicationContext, UnconfinedTestDispatcher(testScheduler))

        val mapping = reader.read()
        assertThat(mapping).isNull()
    }

    @Test
    fun `invalidate clears the in-memory cache so the next read re-loads from disk`() = runTest {
        writeBlob("abc", validMappingJson("monthly", "pro"))
        val reader = ProductEntitlementMappingTopicReader(applicationContext, UnconfinedTestDispatcher(testScheduler))

        val firstMapping = reader.read()!!
        assertThat(firstMapping.mappings).containsKey("monthly")

        topicDir.listFiles()?.forEach { it.delete() }
        writeBlob("def", validMappingJson("annual", "premium"))

        reader.invalidate()
        val mapping = reader.read()!!
        assertThat(mapping.mappings).containsKey("annual")
        assertThat(mapping.mappings).doesNotContainKey("monthly")
    }

    @Test
    fun `concurrent reads coalesce into a single disk access`() = runTest {
        writeBlob("abc", validMappingJson("monthly", "pro"))
        // StandardTestDispatcher: scheduled work waits for advanceUntilIdle, so we can interleave callers.
        val reader = ProductEntitlementMappingTopicReader(applicationContext, StandardTestDispatcher(testScheduler))

        val first = async { reader.read() }
        val second = async { reader.read() }

        testScheduler.advanceUntilIdle()

        assertThat(first.await()?.mappings).containsKey("monthly")
        assertThat(second.await()?.mappings).containsKey("monthly")
        assertThat(first.await()).isSameAs(second.await())
        // Disk read happens once even though two callers raced.
        assertThat(dirAccessCount).isEqualTo(1)
    }

    private fun writeBlob(blobRef: String, json: String) {
        topicDir.mkdirs()
        File(topicDir, blobRef).writeText(json)
    }

    private fun validMappingJson(productId: String, entitlement: String): String =
        """
        {
          "product_entitlement_mapping": {
            "$productId": {
              "product_identifier": "$productId",
              "entitlements": ["$entitlement"]
            }
          }
        }
        """.trimIndent()
}
