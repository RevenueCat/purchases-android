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
    fun `returns high-res when both high and low succeed`() = runTest {
        val highUrl = URL("https://example.com/high.mp4")
        val lowUrl = URL("https://example.com/low.mp4")
        val highUri = URI("file:///tmp/high.mp4")
        val lowUri = URI("file:///tmp/low.mp4")
        val repository = FakeFileRepository(
            generated = mapOf(
                highUrl to highUri,
                lowUrl to lowUri,
            ),
        )

        val resolved = resolveVideoUrlWithFallback(
            videoUrls = videoUrls(highUrl = highUrl, lowUrl = lowUrl),
            repository = repository,
        )

        assertThat(resolved).isEqualTo(highUri)
    }

    @Test
    fun `returns low-res when high-res fails and low-res succeeds`() = runTest {
        val highUrl = URL("https://example.com/high.mp4")
        val lowUrl = URL("https://example.com/low.mp4")
        val lowUri = URI("file:///tmp/low.mp4")
        val repository = FakeFileRepository(
            generated = mapOf(lowUrl to lowUri),
            failingUrls = setOf(highUrl),
        )

        val resolved = resolveVideoUrlWithFallback(
            videoUrls = videoUrls(highUrl = highUrl, lowUrl = lowUrl),
            repository = repository,
        )

        assertThat(resolved).isEqualTo(lowUri)
    }

    @Test
    fun `returns null when both high-res and low-res fail`() = runTest {
        val highUrl = URL("https://example.com/high.mp4")
        val lowUrl = URL("https://example.com/low.mp4")
        val repository = FakeFileRepository(
            failingUrls = setOf(highUrl, lowUrl),
        )

        val resolved = resolveVideoUrlWithFallback(
            videoUrls = videoUrls(highUrl = highUrl, lowUrl = lowUrl),
            repository = repository,
        )

        assertThat(resolved).isNull()
    }

    @Test
    fun `returns null when high-res fails and there is no low-res URL`() = runTest {
        val highUrl = URL("https://example.com/high.mp4")
        val repository = FakeFileRepository(
            failingUrls = setOf(highUrl),
        )

        val resolved = resolveVideoUrlWithFallback(
            videoUrls = videoUrls(highUrl = highUrl, lowUrl = null),
            repository = repository,
        )

        assertThat(resolved).isNull()
    }

    @Test
    fun `does not attempt low-res when low-res URL equals high-res URL`() = runTest {
        val highUrl = URL("https://example.com/video.mp4")
        val repository = FakeFileRepository(
            failingUrls = setOf(highUrl),
        )

        val resolved = resolveVideoUrlWithFallback(
            videoUrls = videoUrls(highUrl = highUrl, lowUrl = highUrl),
            repository = repository,
        )

        assertThat(resolved).isNull()
        assertThat(repository.requestedUrls).containsExactly(highUrl)
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
        private val generated: Map<URL, URI> = emptyMap(),
        private val failingUrls: Set<URL> = emptySet(),
    ) : FileRepository {

        val requestedUrls: MutableList<URL> = mutableListOf()

        override fun prefetch(urls: List<Pair<URL, Checksum?>>) = Unit

        override suspend fun generateOrGetCachedFileURL(url: URL, checksum: Checksum?): URI {
            requestedUrls += url
            if (url in failingUrls) {
                throw RuntimeException("Simulated failure for $url")
            }
            return generated[url] ?: throw RuntimeException("Missing fake URI for $url")
        }

        override fun getFile(url: URL, checksum: Checksum?): URI? = null
    }
}
