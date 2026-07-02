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
