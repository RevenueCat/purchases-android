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
import java.nio.ByteBuffer

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RemoteConfigBlobStoreTest {

    private val testFolder = "temp_remote_config_blob_test_folder"

    // Two valid 32-char URL-safe base64 refs (24-byte hash encoding).
    private val refA = "AAAABBBBCCCCDDDDEEEEFFFFGGGGHHHH"
    private val refB = "IIIIJJJJKKKKLLLLMMMMNNNNOOOOPPPP"

    private lateinit var applicationContext: Context
    private lateinit var blobStore: RemoteConfigBlobStore

    @Before
    fun setup() {
        val tempTestFolder = File(testFolder)
        if (tempTestFolder.exists()) {
            error("Temp test folder should not exist before starting tests")
        }
        tempTestFolder.mkdirs()

        applicationContext = mockk()
        every { applicationContext.noBackupFilesDir } returns tempTestFolder

        blobStore = RemoteConfigBlobStore(applicationContext)
    }

    @After
    fun tearDown() {
        File(testFolder).deleteRecursively()
    }

    @Test
    fun `write then read round-trips the blob bytes`() {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)

        blobStore.write(refA, ByteBuffer.wrap(bytes))

        assertThat(blobStore.read(refA)).isEqualTo(bytes)
    }

    @Test
    fun `write returns true when the blob is persisted`() {
        assertThat(blobStore.write(refA, ByteBuffer.wrap(byteArrayOf(1, 2, 3)))).isTrue
    }

    @Test
    fun `write returns false for a malformed ref`() {
        assertThat(blobStore.write("../escape", ByteBuffer.wrap(byteArrayOf(6, 6, 6)))).isFalse
    }

    @Test
    fun `write does not consume the caller's buffer`() {
        val buffer = ByteBuffer.wrap(byteArrayOf(9, 8, 7))

        blobStore.write(refA, buffer)

        assertThat(buffer.position()).isEqualTo(0)
    }

    @Test
    fun `contains reflects whether a blob has been written`() {
        assertThat(blobStore.contains(refA)).isFalse

        blobStore.write(refA, ByteBuffer.wrap(byteArrayOf(1)))

        assertThat(blobStore.contains(refA)).isTrue
    }

    @Test
    fun `read returns null for an absent blob`() {
        assertThat(blobStore.read(refA)).isNull()
    }

    @Test
    fun `contains and cachedRefs load blobs already on disk from a previous instance`() {
        blobStore.write(refA, ByteBuffer.wrap(byteArrayOf(1)))

        // A fresh instance over the same directory must discover the existing blob via its one-time disk scan.
        val reopened = RemoteConfigBlobStore(applicationContext)

        assertThat(reopened.contains(refA)).isTrue
        assertThat(reopened.cachedRefs()).containsExactly(refA)
    }

    @Test
    fun `cachedRefs reflects the written blobs`() {
        blobStore.write(refA, ByteBuffer.wrap(byteArrayOf(1)))
        blobStore.write(refB, ByteBuffer.wrap(byteArrayOf(2)))

        assertThat(blobStore.cachedRefs()).containsExactlyInAnyOrder(refA, refB)
    }

    @Test
    fun `cachedRefs is empty when nothing has been written`() {
        assertThat(blobStore.cachedRefs()).isEmpty()
    }

    @Test
    fun `retainOnly deletes unreferenced blobs and keeps referenced ones`() {
        blobStore.write(refA, ByteBuffer.wrap(byteArrayOf(1)))
        blobStore.write(refB, ByteBuffer.wrap(byteArrayOf(2)))

        blobStore.retainOnly(setOf(refA))

        assertThat(blobStore.contains(refA)).isTrue
        assertThat(blobStore.contains(refB)).isFalse
    }

    @Test
    fun `retainOnly prunes orphan side files and invalid-named files the index never tracks`() {
        blobStore.write(refA, ByteBuffer.wrap(byteArrayOf(1)))
        // Simulate the AtomicFile side file left by a write interrupted mid-flight, plus a stray
        // invalid-named file.
        val blobsDir = File(File(testFolder, "RevenueCat"), "blobs")
        val orphanSideFile = File(blobsDir, "$refB.new").apply { writeBytes(byteArrayOf(9)) }
        val invalidNamed = File(blobsDir, "not-a-valid-ref").apply { writeBytes(byteArrayOf(9)) }

        blobStore.retainOnly(setOf(refA))

        assertThat(orphanSideFile.exists()).isFalse
        assertThat(invalidNamed.exists()).isFalse
        assertThat(blobStore.contains(refA)).isTrue
        assertThat(blobStore.cachedRefs()).containsExactly(refA)
    }

    @Test
    fun `a write interrupted mid-flight leaves no readable blob`() {
        // An interrupted AtomicFile write leaves only the `<ref>.new` side file: the blob file itself is
        // renamed into place only on a successful finish, so it can never exist half-written.
        val blobsDir = File(File(testFolder, "RevenueCat"), "blobs").apply { mkdirs() }
        File(blobsDir, "$refA.new").writeBytes(byteArrayOf(1, 2))

        // Neither the store nor a fresh instance's disk scan sees the interrupted write as a cached blob.
        assertThat(blobStore.contains(refA)).isFalse
        assertThat(blobStore.read(refA)).isNull()
        val reopened = RemoteConfigBlobStore(applicationContext)
        assertThat(reopened.contains(refA)).isFalse
        assertThat(reopened.cachedRefs()).isEmpty()

        // A retry completes normally, replacing the leftover side file.
        assertThat(blobStore.write(refA, ByteBuffer.wrap(byteArrayOf(3, 4)))).isTrue
        assertThat(blobStore.read(refA)).isEqualTo(byteArrayOf(3, 4))
    }

    @Test
    fun `read self-heals the index when the underlying file is gone`() {
        blobStore.write(refA, ByteBuffer.wrap(byteArrayOf(1)))
        assertThat(blobStore.contains(refA)).isTrue

        // The file disappears from under us (e.g. external removal); the index still claims it.
        File(File(File(testFolder, "RevenueCat"), "blobs"), refA).delete()

        assertThat(blobStore.read(refA)).isNull()
        // The read miss corrected the index, so a later ensureDownloaded/prefetch would re-fetch it.
        assertThat(blobStore.contains(refA)).isFalse
    }

    @Test
    fun `retainOnly with an empty set clears the cache`() {
        blobStore.write(refA, ByteBuffer.wrap(byteArrayOf(1)))

        blobStore.retainOnly(emptySet())

        assertThat(blobStore.cachedRefs()).isEmpty()
    }

    @Test
    fun `clear deletes every cached blob`() {
        blobStore.write(refA, ByteBuffer.wrap(byteArrayOf(1)))
        blobStore.write(refB, ByteBuffer.wrap(byteArrayOf(2)))

        blobStore.clear()

        assertThat(blobStore.cachedRefs()).isEmpty()
        assertThat(blobStore.contains(refA)).isFalse
        assertThat(blobStore.contains(refB)).isFalse
    }

    @Test
    fun `clear is a no-op when nothing has been written`() {
        blobStore.clear()

        assertThat(blobStore.cachedRefs()).isEmpty()
    }

    @Test
    fun `a malformed ref is rejected and cannot escape the blobs directory`() {
        val malformed = "../escape"

        blobStore.write(malformed, ByteBuffer.wrap(byteArrayOf(6, 6, 6)))

        assertThat(blobStore.contains(malformed)).isFalse
        assertThat(blobStore.read(malformed)).isNull()
        // Nothing leaked outside the blobs directory.
        assertThat(File(testFolder, "escape").exists()).isFalse
        assertThat(File(File(testFolder, "RevenueCat"), "escape").exists()).isFalse
    }
}
