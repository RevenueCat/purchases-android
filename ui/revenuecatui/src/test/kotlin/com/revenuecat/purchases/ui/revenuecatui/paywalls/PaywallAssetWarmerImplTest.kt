package com.revenuecat.purchases.ui.revenuecatui.paywalls

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.request.Disposable
import coil.request.ImageRequest
import com.revenuecat.purchases.paywalls.PaywallAssetWarmer
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.ServiceLoader
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
internal class PaywallAssetWarmerImplTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val imageLoader = mockk<ImageLoader>()
    private val requests = CopyOnWriteArrayList<ImageRequest>()
    private val shared = Uri.parse("https://assets.pawwalls.com/shared.webp")
    private val other = Uri.parse("https://assets.pawwalls.com/other.webp")
    private val warmer = PaywallAssetWarmerImpl()

    @Before
    fun setUp() {
        mockkObject(PaywallImageLoader)
        every { PaywallImageLoader.get(any()) } returns imageLoader
        every { imageLoader.enqueue(any()) } answers {
            requests.add(firstArg())
            mockk<Disposable>(relaxed = true)
        }
    }

    @After
    fun tearDown() {
        unmockkObject(PaywallImageLoader)
    }

    // Guards the META-INF/services descriptor against the impl being moved or renamed.
    @Test
    fun `warmer is discoverable through ServiceLoader`() {
        val loadedWarmer = ServiceLoader.load(
            PaywallAssetWarmer::class.java,
            PaywallAssetWarmer::class.java.classLoader,
        ).firstOrNull()

        assertThat(loadedWarmer).isInstanceOf(PaywallAssetWarmerImpl::class.java)
    }

    @Test
    fun `shared images are warmed once across batches while unrelated images still warm`() {
        warmer.warmImages(context, listOf(shared, shared, other))
        warmer.warmImages(context, listOf(shared, other))

        assertThat(requests.map { it.data }).containsExactly(shared, other)
    }

    @Test
    fun `concurrent workflows share one warming request`() {
        val executor = Executors.newFixedThreadPool(2)
        val start = CyclicBarrier(2)
        try {
            val calls = (1..2).map {
                executor.submit {
                    start.await(5, TimeUnit.SECONDS)
                    warmer.warmImages(context, listOf(shared))
                }
            }
            calls.forEach { it.get(5, TimeUnit.SECONDS) }
            assertThat(requests.map { it.data }).containsExactly(shared)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `successful images can be warmed again through the normal cache`() {
        assertCanWarmAgain { request -> request.listener!!.onSuccess(request, mockk()) }
    }

    @Test
    fun `failed images can be warmed again`() {
        assertCanWarmAgain { request -> request.listener!!.onError(request, mockk()) }
    }

    @Test
    fun `cancelled images can be warmed again`() {
        assertCanWarmAgain { request -> request.listener!!.onCancel(request) }
    }

    @Test
    fun `enqueue exceptions release the image and do not stop the batch`() {
        every { imageLoader.enqueue(any()) } answers {
            val request = firstArg<ImageRequest>()
            if (request.data == shared) throw IllegalStateException("enqueue failed")
            requests.add(request)
            mockk<Disposable>(relaxed = true)
        }
        warmer.warmImages(context, listOf(shared, other))
        assertThat(requests.map { it.data }).containsExactly(other)

        every { imageLoader.enqueue(any()) } answers {
            requests.add(firstArg())
            mockk<Disposable>(relaxed = true)
        }
        warmer.warmImages(context, listOf(shared))
        assertThat(requests.map { it.data }).containsExactly(other, shared)
    }

    @Test
    fun `an old completion cannot release a newer warming request`() {
        warmer.warmImages(context, listOf(shared))
        val first = requests.single()
        first.listener!!.onError(first, mockk())
        warmer.warmImages(context, listOf(shared))

        first.listener!!.onCancel(first)
        warmer.warmImages(context, listOf(shared))
        assertThat(requests).hasSize(2)
    }

    private fun assertCanWarmAgain(complete: (ImageRequest) -> Unit) {
        warmer.warmImages(context, listOf(shared))
        complete(requests.single())
        warmer.warmImages(context, listOf(shared))
        assertThat(requests.map { it.data }).containsExactly(shared, shared)
    }
}
