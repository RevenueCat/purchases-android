package com.revenuecat.purchases.common.remoteconfig

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.networking.RCContainer
import com.revenuecat.purchases.common.networking.RCContainerTestData
import com.revenuecat.purchases.common.networking.RCContentEncoding
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.util.Date

/**
 * Semi-integration tests for [RemoteConfigManager] that exercise the full sync flow against the **real**
 * [RemoteConfigDiskCache] and [RemoteConfigBlobStore] backed by a temp folder, with only the [Backend] mocked.
 *
 * Unlike [RemoteConfigManagerTest] (which mocks the cache/store and only verifies which methods are called),
 * these tests build genuine RC Container Format bytes (via [RCContainerTestData]), parse them with the real
 * [RCContainer] parser, run them through the manager, and read the result back off disk. The focus is the
 * end-to-end path: inline-blob extraction -> on-disk storage -> retrieval, plus pruning and the prefetch
 * report fed from the blobs actually held.
 */
@OptIn(InternalRevenueCatAPI::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RemoteConfigManagerIntegrationTest {

    private val testFolder = "temp_remote_config_integration_test_folder"

    private lateinit var applicationContext: Context
    private lateinit var backend: Backend
    private lateinit var diskCache: RemoteConfigDiskCache
    private lateinit var blobStore: RemoteConfigBlobStore
    private lateinit var manager: RemoteConfigManager

    private var capturedManifest: String? = null
    private var capturedPrefetchedBlobs: List<String>? = null
    private lateinit var onSuccess: (RCContainer?, VerificationResult) -> Unit

    @Before
    fun setup() {
        val tempTestFolder = File(testFolder)
        if (tempTestFolder.exists()) {
            error("Temp test folder should not exist before starting tests")
        }
        tempTestFolder.mkdirs()

        applicationContext = mockk()
        every { applicationContext.noBackupFilesDir } returns tempTestFolder

        backend = mockk()
        diskCache = RemoteConfigDiskCache(applicationContext)
        blobStore = RemoteConfigBlobStore(applicationContext)
        manager = RemoteConfigManager(backend, diskCache, blobStore, dateProvider = FixedDateProvider)

        every {
            backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any())
        } answers {
            capturedManifest = arg(3)
            capturedPrefetchedBlobs = arg(4)
            onSuccess = arg(5)
        }
    }

    @After
    fun tearDown() {
        File(testFolder).deleteRecursively()
    }

    @Test
    fun `first sync persists the config and stores a referenced inline blob, retrievable from the store`() {
        val blob = """{"workflow":"wf1"}""".toByteArray()
        val ref = RCContainerTestData.refOf(blob)
        val config = workflowsConfig(
            manifest = "v1.1.workflows:etag1",
            prefetchBlobs = listOf(ref),
            items = mapOf("wf1234" to ref),
        )

        sync(container(config, blob))

        val persisted = diskCache.read()!!
        assertThat(persisted.manifest).isEqualTo("v1.1.workflows:etag1")
        assertThat(persisted.activeTopics).containsExactly("workflows")
        assertThat(persisted.prefetchBlobs).containsExactly(ref)
        assertThat(persisted.topics).containsOnlyKeys("workflows")
        assertThat(persisted.topics["workflows"]!!.values.mapNotNull { it.blobRef }).containsExactly(ref)
        // The full config is the source of truth: the item's inline content is persisted too, not just its ref.
        assertThat(persisted.topics["workflows"]!!["wf1234"]!!.content).containsKey("offering_identifier")
        assertThat(blobStore.contains(ref)).isTrue
        assertThat(blobStore.read(ref)).isEqualTo(blob)
    }

    @Test
    fun `the committed single-element fixture round-trips its workflow blob into the store`() {
        val ref = RCContainerTestData.refOf(RCContainerTestData.WORKFLOW_BLOB)

        sync(parseFixture("v1_single_element.bin"))

        // The fixture inlines WORKFLOW_BLOB and references it from the workflows topic, so it lands in the store.
        assertThat(blobStore.read(ref)).isEqualTo(RCContainerTestData.WORKFLOW_BLOB)
        assertThat(diskCache.read()!!.activeTopics).containsExactly("workflows")
    }

    @Test
    fun `multiple referenced inline blobs are all stored and individually retrievable`() {
        val blobA = """{"workflow":"a"}""".toByteArray()
        val blobB = """{"workflow":"b"}""".toByteArray()
        val refA = RCContainerTestData.refOf(blobA)
        val refB = RCContainerTestData.refOf(blobB)
        val config = workflowsConfig(items = mapOf("wf1234" to refA, "wf5678" to refB))

        sync(container(config, blobA, blobB))

        assertThat(blobStore.cachedRefs()).containsExactlyInAnyOrder(refA, refB)
        assertThat(blobStore.read(refA)).isEqualTo(blobA)
        assertThat(blobStore.read(refB)).isEqualTo(blobB)
    }

    @Test
    fun `a second sync prunes the blob the new config no longer references and replays the stored manifest`() {
        val blobA = """{"workflow":"a"}""".toByteArray()
        val blobB = """{"workflow":"b"}""".toByteArray()
        val refA = RCContainerTestData.refOf(blobA)
        val refB = RCContainerTestData.refOf(blobB)

        sync(container(workflowsConfig(manifest = "v1.1.workflows:etag1", items = mapOf("wf1234" to refA)), blobA))
        assertThat(blobStore.contains(refA)).isTrue

        // Start the next sync: it should replay the manifest persisted by the first sync.
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        assertThat(capturedManifest).isEqualTo("v1.1.workflows:etag1")
        settle(container(workflowsConfig(manifest = "v1.2.workflows:etag2", items = mapOf("wf1234" to refB)), blobB))

        assertThat(blobStore.contains(refA)).isFalse
        assertThat(blobStore.contains(refB)).isTrue
        assertThat(diskCache.read()!!.topics["workflows"]!!.values.mapNotNull { it.blobRef }).containsExactly(refB)
    }

    @Test
    fun `a held prefetch blob is reported on the next request`() {
        val blob = """{"workflow":"wf1"}""".toByteArray()
        val ref = RCContainerTestData.refOf(blob)

        sync(container(workflowsConfig(prefetchBlobs = listOf(ref), items = mapOf("wf1234" to ref)), blob))
        assertThat(blobStore.contains(ref)).isTrue

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)

        // The blob is now cached on disk, so it is reported back so the server stops re-inlining it.
        assertThat(capturedPrefetchedBlobs).containsExactly(ref)
    }

    @Test
    fun `a tampered inline blob is not stored but the rest of the config still persists`() {
        val decoy = "decoy".toByteArray()
        val tamperedRef = RCContainerTestData.refOf(decoy)
        val realData = """{"workflow":"wf1"}""".toByteArray()
        val config = workflowsConfig(items = mapOf("wf1234" to tamperedRef))
        // The element is keyed by the decoy's content-address but carries different bytes, so its checksum fails.
        val tampered = container(
            config,
            blobs = listOf(realData),
            checksumOverride = { index, element ->
                if (index == 1) RCContainerTestData.sha256(decoy) else RCContainerTestData.sha256(element)
            },
        )

        sync(tampered)

        assertThat(blobStore.contains(tamperedRef)).isFalse
        assertThat(blobStore.cachedRefs()).isEmpty()
        // The configuration itself is the source of truth and persists regardless of the blob failing validation.
        assertThat(diskCache.read()!!.topics["workflows"]!!.values.mapNotNull { it.blobRef }).containsExactly(tamperedRef)
    }

    @Test
    fun `a gzipped inline blob is stored decompressed and retrievable by its uncompressed ref`() {
        val blob = """{"workflow":"wf1","note":"${"pad".repeat(50)}"}""".toByteArray()
        // The ref is the content-address of the uncompressed bytes, unchanged by compression.
        val ref = RCContainerTestData.refOf(blob)
        val config = workflowsConfig(prefetchBlobs = listOf(ref), items = mapOf("wf1234" to ref))

        // codec index 1 = the inline blob element is gzipped on the wire.
        sync(
            container(
                config,
                blobs = listOf(blob),
                codecForIndex = { index -> if (index == 1) RCContentEncoding.GZIP.id else RCContentEncoding.NONE.id },
            ),
        )

        assertThat(blobStore.contains(ref)).isTrue
        // The store holds the uncompressed payload.
        assertThat(blobStore.read(ref)).isEqualTo(blob)
    }

    @Test
    fun `an inline blob with an unsupported codec is skipped but the config still persists`() {
        val blob = """{"workflow":"wf1"}""".toByteArray()
        val ref = RCContainerTestData.refOf(blob)
        val config = workflowsConfig(items = mapOf("wf1234" to ref))

        // codec index 1 = brotli, which Android cannot decode: the blob is dropped, config is kept.
        sync(
            container(
                config,
                blobs = listOf(blob),
                codecForIndex = { index -> if (index == 1) RCContentEncoding.BROTLI.id else RCContentEncoding.NONE.id },
            ),
        )

        assertThat(blobStore.contains(ref)).isFalse
        assertThat(blobStore.cachedRefs()).isEmpty()
        assertThat(diskCache.read()!!.topicBlobRefs).containsExactlyEntriesOf(mapOf("workflows" to listOf(ref)))
    }

    @Test
    fun `a 204 after a prior sync leaves the disk cache and blobs untouched`() {
        val blob = """{"workflow":"wf1"}""".toByteArray()
        val ref = RCContainerTestData.refOf(blob)
        sync(container(workflowsConfig(manifest = "v1.1.workflows:etag1", items = mapOf("wf1234" to ref)), blob))

        // A 204 surfaces as a null container: nothing changed server-side.
        sync(null)

        assertThat(diskCache.read()!!.manifest).isEqualTo("v1.1.workflows:etag1")
        assertThat(blobStore.read(ref)).isEqualTo(blob)
    }

    @Test
    fun `an inline-only topic persists its item with no blob ref and writes no blobs`() {
        // wf1234 carries only inline content (offering_identifier), no blob_ref, and no element is inlined.
        val config = workflowsConfig(items = mapOf("wf1234" to null))

        sync(container(config))

        val topics = diskCache.read()!!.topics
        assertThat(topics).containsOnlyKeys("workflows")
        assertThat(topics["workflows"]!!["wf1234"]!!.blobRef).isNull()
        assertThat(topics["workflows"]!!.values.mapNotNull { it.blobRef }).isEmpty()
        assertThat(blobStore.cachedRefs()).isEmpty()
    }

    /** Builds a workflows-topic config. Each item maps an item key to its `blob_ref`, or `null` for inline-only. */
    private fun workflowsConfig(
        manifest: String = "v1.1.workflows:etag1",
        prefetchBlobs: List<String> = emptyList(),
        items: Map<String, String?>,
    ): String {
        val itemsJson = items.entries.joinToString(",\n") { (key, ref) ->
            val blobRef = ref?.let { """, "blob_ref": "$it"""" } ?: ""
            """          "$key": { "offering_identifier": "default"$blobRef }"""
        }
        val prefetchJson = prefetchBlobs.joinToString(",") { "\"$it\"" }
        return """
            {
              "domain": "app",
              "manifest": "$manifest",
              "active_topics": ["workflows"],
              "prefetch_blobs": [$prefetchJson],
              "topics": {
                "workflows": {
            $itemsJson
                }
              }
            }
        """.trimIndent()
    }

    private fun container(
        configJson: String,
        vararg blobs: ByteArray,
    ): RCContainer = container(configJson, blobs.toList())

    private fun container(
        configJson: String,
        blobs: List<ByteArray>,
        checksumOverride: ((index: Int, element: ByteArray) -> ByteArray)? = null,
        codecForIndex: (index: Int) -> Int = { RCContentEncoding.NONE.id },
    ): RCContainer = RCContainer.parse(
        RCContainerTestData.buildContainer(
            config = configJson.toByteArray(),
            elements = blobs,
            checksumOverride = checksumOverride,
            codecForIndex = codecForIndex,
        ),
    )

    private fun parseFixture(fileName: String): RCContainer {
        val bytes = javaClass.classLoader!!
            .getResource("${RCContainerTestData.FIXTURE_DIR}/$fileName")!!
            .openStream()
            .use { it.readBytes() }
        return RCContainer.parse(bytes)
    }

    /** Runs a full refresh and settles it with [container] (null = a 204). */
    private fun sync(container: RCContainer?) {
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        settle(container)
    }

    private fun settle(container: RCContainer?) {
        onSuccess.invoke(container, VerificationResult.VERIFIED)
    }

    private companion object {
        private const val TEST_APP_USER_ID = "test-app-user-id"
        private const val FIXED_MILLIS = 1_710_000_000_000L
        private val FixedDateProvider = object : DateProvider {
            override val now: Date get() = Date(FIXED_MILLIS)
        }
    }
}
