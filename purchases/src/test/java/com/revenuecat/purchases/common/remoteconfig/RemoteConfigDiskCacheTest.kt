package com.revenuecat.purchases.common.remoteconfig

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.buildJsonObject
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
    fun `write then read round-trips the full persisted config`() {
        val config = PersistedRemoteConfigurationState(
            domain = "app",
            manifest = "v1.1710000100.sources:etag1,product_entitlement_mapping:etag2",
            activeTopics = listOf("sources", "product_entitlement_mapping"),
            prefetchBlobs = listOf("blobRefA"),
            topics = mapOf(
                "sources" to ConfigTopic(mapOf("default" to RemoteConfiguration.ConfigItem(blobRef = "blobRefA"))),
                "product_entitlement_mapping" to ConfigTopic(
                    mapOf("pem" to RemoteConfiguration.ConfigItem(blobRef = "pemBlob")),
                ),
            ),
        )

        diskCache.write(config)
        val read = diskCache.read()

        assertThat(read).isEqualTo(config)
    }

    @Test
    fun `read serves the in-memory snapshot without re-reading the file`() {
        val config = PersistedRemoteConfigurationState(domain = "app", manifest = "v1.0.")
        diskCache.write(config)

        // Remove the backing file: a read must still answer from the snapshot, proving no file re-read.
        File(File(File(testFolder, "RevenueCat"), "remote_config"), "remote_config.json").delete()

        assertThat(diskCache.read()).isEqualTo(config)
    }

    @Test
    fun `a fresh instance reads the state from disk`() {
        val config = PersistedRemoteConfigurationState(domain = "app", manifest = "v1.0.")
        diskCache.write(config)

        // A new instance has no snapshot, so this proves the write actually reached the file.
        assertThat(RemoteConfigDiskCache(applicationContext).read()).isEqualTo(config)
    }

    @Test
    fun `read caches a miss so the file is not re-checked every call`() {
        assertThat(diskCache.read()).isNull()

        // A file appearing behind the cache's back is not picked up: the cache is the file's sole writer,
        // and the cached miss stands until a write() or clear() through this instance.
        RemoteConfigDiskCache(applicationContext)
            .write(PersistedRemoteConfigurationState(domain = "app", manifest = "v1.0."))

        assertThat(diskCache.read()).isNull()
    }

    @Test
    fun `write returns true on a successful persist`() {
        val persisted = diskCache.write(PersistedRemoteConfigurationState(domain = "app", manifest = "v1.0."))

        assertThat(persisted).isTrue
    }

    @Test
    fun `a topic with inline-only items round-trips its content with no blob ref`() {
        val config = PersistedRemoteConfigurationState(
            domain = "app",
            manifest = "v1.1.sources:etag1",
            activeTopics = listOf("sources"),
            topics = mapOf(
                "sources" to ConfigTopic(
                    mapOf(
                        "api" to RemoteConfiguration.ConfigItem(
                            metadata = buildJsonObject { put("url", "https://api.revenuecat.com") },
                        ),
                    ),
                ),
            ),
        )

        diskCache.write(config)

        val read = diskCache.read()!!
        assertThat(read.topics["sources"]!!["api"]!!.blobRef).isNull()
        assertThat(read).isEqualTo(config)
    }

    @Test
    fun `an old-format file with topicBlobRefs is read with empty topics and self-heals`() {
        // The previous format stored "topicBlobRefs"; it is now "topics". With a string manifest the file still
        // deserializes — the unknown key is ignored and topics defaults empty, so the next sync rebuilds the index.
        val parent = File(File(testFolder, "RevenueCat"), "remote_config").apply { mkdirs() }
        File(parent, "remote_config.json").writeText(
            """{"domain":"app","manifest":"v1.1.sources:etag1","topicBlobRefs":{"sources":["a"]}}""",
        )

        val read = diskCache.read()
        assertThat(read).isNotNull
        assertThat(read!!.manifest).isEqualTo("v1.1.sources:etag1")
        assertThat(read.topics).isEmpty()
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
    fun `clear deletes the persisted state so read returns null`() {
        diskCache.write(PersistedRemoteConfigurationState(domain = "app", manifest = "v1.1.sources:etag1"))

        diskCache.clear()

        assertThat(diskCache.read()).isNull()
        assertThat(
            File(File(File(testFolder, "RevenueCat"), "remote_config"), "remote_config.json").exists(),
        ).isFalse
    }

    @Test
    fun `clear is a no-op when nothing has been persisted`() {
        diskCache.clear()

        assertThat(diskCache.read()).isNull()
    }

    @Test
    fun `read tolerates unknown keys in the persisted file for forward-compatibility`() {
        // A file written by a future SDK version may carry extra fields; they must not fail the read.
        val file = File(File(File(testFolder, "RevenueCat"), "remote_config"), "remote_config.json")
        file.parentFile!!.mkdirs()
        // The persisted format uses the Kotlin property names (e.g. `activeTopics`), unlike the wire format.
        // language=json
        file.writeText(
            """
            {
              "domain": "app",
              "manifest": "v1.0.",
              "activeTopics": ["sources"],
              "future_field": { "nested": true },
              "topics": { "sources": { "api": { "url": "https://api.revenuecat.com", "future_key": 1 } } }
            }
            """.trimIndent(),
        )

        val read = diskCache.read()

        assertThat(read).isNotNull
        assertThat(read!!.domain).isEqualTo("app")
        assertThat(read.manifest).isEqualTo("v1.0.")
        assertThat(read.activeTopics).containsExactly("sources")
        // Unknown item keys survive as metadata (the ConfigItem serializer keeps non-reserved keys).
        assertThat(read.topics.getValue("sources").getValue("api").metadata).containsKey("future_key")
    }

    @Test
    fun `read returns null when the persisted file is corrupt`() {
        val parent = File(File(testFolder, "RevenueCat"), "remote_config").apply { mkdirs() }
        File(parent, "remote_config.json").writeText("{ this is not valid json")

        assertThat(diskCache.read()).isNull()
    }
}
