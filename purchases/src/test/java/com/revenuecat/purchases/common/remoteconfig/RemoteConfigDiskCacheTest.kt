package com.revenuecat.purchases.common.remoteconfig

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RemoteConfigDiskCacheTest {

    private val testFolder = "temp_remote_config_disk_cache_test_folder"

    private lateinit var rootDir: File
    private lateinit var applicationContext: Context
    private lateinit var diskCache: RemoteConfigDiskCache
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        rootDir = File(testFolder)
        if (rootDir.exists()) {
            error("Temp test folder should not exist before starting tests")
        }
        rootDir.mkdirs()

        applicationContext = mockk()
        every { applicationContext.noBackupFilesDir } returns rootDir

        diskCache = RemoteConfigDiskCache(applicationContext, json)
    }

    @After
    fun tearDown() {
        rootDir.deleteRecursively()
    }

    @Test
    fun `write persists JSON file at expected path`() {
        val response = sampleResponse()

        diskCache.write(response)

        val target = File(File(rootDir, "RevenueCat"), "remote_config.json")
        assertThat(target.exists()).isTrue
        val roundTripped = json.decodeFromString<RemoteConfigResponse>(target.readText())
        assertThat(roundTripped).isEqualTo(response)
    }

    @Test
    fun `write creates parent RevenueCat directory if missing`() {
        val parent = File(rootDir, "RevenueCat")
        assertThat(parent.exists()).isFalse

        diskCache.write(sampleResponse())

        assertThat(parent.exists()).isTrue
        assertThat(parent.isDirectory).isTrue
    }

    @Test
    fun `subsequent writes overwrite the previous snapshot`() {
        val first = sampleResponse(blobRef = "blob-1")
        val second = sampleResponse(blobRef = "blob-2")

        diskCache.write(first)
        diskCache.write(second)

        val target = File(File(rootDir, "RevenueCat"), "remote_config.json")
        val roundTripped = json.decodeFromString<RemoteConfigResponse>(target.readText())
        assertThat(roundTripped).isEqualTo(second)
    }

    @Test
    fun `write does not leave temp files behind on success`() {
        diskCache.write(sampleResponse())

        val parent = File(rootDir, "RevenueCat")
        val tempFiles = parent.listFiles { _, name -> name.startsWith("rc_remote_config_") }
        assertThat(tempFiles).isNotNull
        assertThat(tempFiles!!).isEmpty()
    }

    @Test
    fun `write swallows IOException when noBackupFilesDir is not writable`() {
        // Replace rootDir with a regular file so mkdirs / createTempFile inside it fails.
        rootDir.deleteRecursively()
        rootDir.writeText("not a directory")

        // Should not throw.
        diskCache.write(sampleResponse())
    }

    private fun sampleResponse(blobRef: String = "blob-default"): RemoteConfigResponse =
        RemoteConfigResponse(
            blobSources = listOf(
                BlobSource(
                    id = "primary",
                    urlFormat = "https://assets.example/{blob_ref}",
                    priority = 0,
                    weight = 100,
                ),
            ),
            manifest = Manifest(
                topics = mapOf(
                    Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf(
                        "default" to TopicEntry(blobRef = blobRef),
                    ),
                ),
            ),
        )
}
