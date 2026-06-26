package com.revenuecat.purchases.common.remoteconfig

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
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
    fun `write then read round-trips the full persisted config`() {
        val config = PersistedRemoteConfigurationState(
            domain = "app",
            manifest = "v1.1710000100.sources:etag1,product_entitlement_mapping:etag2",
            activeTopics = listOf("sources", "product_entitlement_mapping"),
            prefetchBlobs = listOf("blobRefA"),
            topicBlobRefs = mapOf(
                "sources" to listOf("blobRefA"),
                "product_entitlement_mapping" to listOf("pemBlob"),
            ),
            lastRefreshAt = 1710000100L,
        )

        diskCache.write(config)
        val read = diskCache.read()

        assertThat(read).isEqualTo(config)
    }

    @Test
    fun `write returns true on a successful persist`() {
        val persisted = diskCache.write(PersistedRemoteConfigurationState(domain = "app", manifest = "v1.0."))

        assertThat(persisted).isTrue
    }

    @Test
    fun `inline-only topics persist with an empty blob ref list`() {
        val config = PersistedRemoteConfigurationState(
            domain = "app",
            manifest = "v1.1.sources:etag1",
            activeTopics = listOf("sources"),
            topicBlobRefs = mapOf("sources" to emptyList()),
        )

        diskCache.write(config)

        assertThat(diskCache.read()?.topicBlobRefs).isEqualTo(mapOf("sources" to emptyList<String>()))
    }

    @Test
    fun `read returns null for an incompatible old-format file`() {
        // The previous format stored "manifest" as an object; it is now an opaque string. An old file no longer
        // deserializes, so read returns null gracefully and the next sync rebuilds from scratch.
        val parent = File(File(testFolder, "RevenueCat"), "remote_config").apply { mkdirs() }
        File(parent, "remote_config.json").writeText(
            """{"manifest":{"domain":"app","topics":{"sources":"etag1"}},"topicBlobRefs":{}}""",
        )

        assertThat(diskCache.read()).isNull()
    }

    @Test
    fun `write creates the remote_config directory when absent`() {
        diskCache.write(PersistedRemoteConfigurationState(domain = "app", manifest = "v1.0."))

        assertThat(
            File(File(File(testFolder, "RevenueCat"), "remote_config"), "remote_config.json").exists(),
        ).isTrue
    }

    @Test
    fun `write overwrites a previous snapshot`() {
        diskCache.write(PersistedRemoteConfigurationState(domain = "app", manifest = "v1.1.sources:old"))
        diskCache.write(PersistedRemoteConfigurationState(domain = "app", manifest = "v1.2.sources:new"))

        assertThat(diskCache.read()?.manifest).isEqualTo("v1.2.sources:new")
    }

    @Test
    fun `read returns null when the persisted file is corrupt`() {
        val parent = File(File(testFolder, "RevenueCat"), "remote_config").apply { mkdirs() }
        File(parent, "remote_config.json").writeText("{ this is not valid json")

        assertThat(diskCache.read()).isNull()
    }
}
