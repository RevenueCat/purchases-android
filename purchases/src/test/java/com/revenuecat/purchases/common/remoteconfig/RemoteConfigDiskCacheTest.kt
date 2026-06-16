package com.revenuecat.purchases.common.remoteconfig

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
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

    private val testFolder = "temp_remote_config_test_folder"

    private lateinit var applicationContext: Context
    private lateinit var diskCache: RemoteConfigDiskCache

    @Before
    fun setup() {
        val tempTestFolder = File(testFolder)
        if (tempTestFolder.exists()) {
            error("Temp test folder should not exist before starting tests")
        }
        tempTestFolder.mkdirs()

        applicationContext = mockk()
        every { applicationContext.noBackupFilesDir } returns tempTestFolder

        diskCache = RemoteConfigDiskCache(applicationContext)
    }

    @After
    fun tearDown() {
        File(testFolder).deleteRecursively()
    }

    @Test
    fun `read returns null when nothing has been persisted`() {
        assertThat(diskCache.read()).isNull()
    }

    @Test
    fun `write then read round-trips the manifest and resolved topics`() {
        val manifest = RemoteConfiguration.Manifest(
            domain = "app",
            topics = mapOf("sources" to "etag1", "product_entitlement_mapping" to "etag2"),
            prefetchBlobs = listOf("blobRefA"),
            prefetchedBlobs = listOf("blobRefA"),
            lastRefreshAt = 1710000100L,
        )
        val topics = mapOf(
            "sources" to mapOf("default" to RemoteConfiguration.ConfigItem(blobRef = "blobRefA", prefetch = true)),
        )

        diskCache.write(manifest, topics)
        val read = diskCache.read()

        assertThat(read).isNotNull
        assertThat(read?.manifest).isEqualTo(manifest)
        assertThat(read?.topics).isEqualTo(topics)
    }

    @Test
    fun `write then read round-trips inline item content`() {
        val manifest = RemoteConfiguration.Manifest(domain = "app", topics = mapOf("sources" to "etag1"))
        val inlineItem = RemoteConfiguration.ConfigItem(
            content = buildJsonObject {
                put("id", "primary")
                put("url", "https://api.revenuecat.com")
                put("priority", 100)
                put("weight", 100)
            },
        )
        val topics = mapOf("sources" to mapOf("api" to inlineItem))

        diskCache.write(manifest, topics)
        val read = diskCache.read()

        val readItem = read?.topics?.getValue("sources")?.getValue("api")
        assertThat(readItem).isEqualTo(inlineItem)
        assertThat(readItem?.blobRef).isNull()
        assertThat(readItem?.content?.get("id")?.jsonPrimitive?.content).isEqualTo("primary")
        assertThat(readItem?.content?.get("priority")?.jsonPrimitive?.int).isEqualTo(100)
    }

    @Test
    fun `write creates the RevenueCat directory when absent`() {
        val manifest = RemoteConfiguration.Manifest(domain = "app")

        diskCache.write(manifest, emptyMap())

        assertThat(File(File(testFolder, "RevenueCat"), "remote_config.json").exists()).isTrue
    }

    @Test
    fun `write overwrites a previous snapshot`() {
        diskCache.write(RemoteConfiguration.Manifest(domain = "app", topics = mapOf("sources" to "old")), emptyMap())
        diskCache.write(RemoteConfiguration.Manifest(domain = "app", topics = mapOf("sources" to "new")), emptyMap())

        assertThat(diskCache.read()?.manifest?.topics).containsExactlyEntriesOf(mapOf("sources" to "new"))
    }

    @Test
    fun `read returns null when the persisted file is corrupt`() {
        val parent = File(testFolder, "RevenueCat").apply { mkdirs() }
        File(parent, "remote_config.json").writeText("{ this is not valid json")

        assertThat(diskCache.read()).isNull()
    }
}
