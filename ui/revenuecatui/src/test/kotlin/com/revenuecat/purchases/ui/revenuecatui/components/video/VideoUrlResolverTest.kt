package com.revenuecat.purchases.ui.revenuecatui.components.video

import com.revenuecat.purchases.models.Checksum
import com.revenuecat.purchases.paywalls.components.properties.VideoUrls
import com.revenuecat.purchases.storage.FileRepository
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.net.URI
import java.net.URL

class VideoUrlResolverTest {

    @Test
    fun `returns cached high-res URI when high-res is cached`() {
        val highUrl = URL("https://example.com/high.mp4")
        val lowUrl = URL("https://example.com/low.mp4")
        val highUri = URI("file:///tmp/high.mp4")
        val repository = FakeFileRepository(
            cachedFiles = mapOf(highUrl to highUri),
        )

        val resolved = resolveVideoUrl(
            videoUrls = videoUrls(highUrl = highUrl, lowUrl = lowUrl),
            repository = repository,
        )

        assertThat(resolved).isEqualTo(highUri)
    }

    @Test
    fun `returns cached low-res URI when only low-res is cached`() {
        val highUrl = URL("https://example.com/high.mp4")
        val lowUrl = URL("https://example.com/low.mp4")
        val lowUri = URI("file:///tmp/low.mp4")
        val repository = FakeFileRepository(
            cachedFiles = mapOf(lowUrl to lowUri),
        )

        val resolved = resolveVideoUrl(
            videoUrls = videoUrls(highUrl = highUrl, lowUrl = lowUrl),
            repository = repository,
        )

        assertThat(resolved).isEqualTo(lowUri)
    }

    @Test
    fun `returns remote URL as URI when nothing is cached`() {
        val highUrl = URL("https://example.com/high.mp4")
        val lowUrl = URL("https://example.com/low.mp4")
        val repository = FakeFileRepository()

        val resolved = resolveVideoUrl(
            videoUrls = videoUrls(highUrl = highUrl, lowUrl = lowUrl),
            repository = repository,
        )

        assertThat(resolved).isEqualTo(highUrl.toURI())
    }

    @Test
    fun `returns remote URL when high-res not cached and no low-res URL`() {
        val highUrl = URL("https://example.com/high.mp4")
        val repository = FakeFileRepository()

        val resolved = resolveVideoUrl(
            videoUrls = videoUrls(highUrl = highUrl, lowUrl = null),
            repository = repository,
        )

        assertThat(resolved).isEqualTo(highUrl.toURI())
    }

    @Test
    fun `does not check low-res cache when low-res URL equals high-res URL`() {
        val url = URL("https://example.com/video.mp4")
        val repository = FakeFileRepository()

        val resolved = resolveVideoUrl(
            videoUrls = videoUrls(highUrl = url, lowUrl = url),
            repository = repository,
        )

        assertThat(resolved).isEqualTo(url.toURI())
        // getFile should only be called once (for high-res), not for low-res since URLs are equal
        assertThat(repository.getFileRequests).containsExactly(url)
    }

    @Test
    fun `cacheVideo calls generateOrGetCachedFileURL`() = runTest {
        val url = URL("https://example.com/video.mp4")
        val checksum = Checksum(Checksum.Algorithm.SHA256, "abc123")
        val uri = URI("file:///tmp/video.mp4")
        val repository = FakeFileRepository(
            cachedFiles = mapOf(url to uri),
        )

        cacheVideo(url, checksum, repository)

        assertThat(repository.generateRequests).containsExactly(url)
    }

    private fun videoUrls(
        highUrl: URL,
        lowUrl: URL?,
    ): VideoUrls = VideoUrls(
        width = 1920u,
        height = 1080u,
        url = highUrl,
        checksum = Checksum(Checksum.Algorithm.SHA256, "high"),
        urlLowRes = lowUrl,
        checksumLowRes = lowUrl?.let { Checksum(Checksum.Algorithm.SHA256, "low") },
    )

    private class FakeFileRepository(
        private val cachedFiles: Map<URL, URI> = emptyMap(),
        private val failingUrls: Set<URL> = emptySet(),
    ) : FileRepository {

        val getFileRequests: MutableList<URL> = mutableListOf()
        val generateRequests: MutableList<URL> = mutableListOf()

        override fun prefetch(urls: List<Pair<URL, Checksum?>>) = Unit

        override fun getFile(url: URL, checksum: Checksum?): URI? {
            getFileRequests += url
            return cachedFiles[url]
        }

        @Suppress("TooGenericExceptionCaught")
        override suspend fun generateOrGetCachedFileURL(url: URL, checksum: Checksum?): URI {
            generateRequests += url
            if (url in failingUrls) {
                throw RuntimeException("Simulated failure for $url")
            }
            return cachedFiles[url] ?: throw RuntimeException("Missing fake URI for $url")
        }
    }
}
