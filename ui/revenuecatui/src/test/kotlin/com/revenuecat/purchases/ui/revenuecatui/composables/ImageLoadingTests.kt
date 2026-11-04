package com.revenuecat.purchases.ui.revenuecatui.composables

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import coil.ImageLoader
import coil.decode.DataSource
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.BuildConfig
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberBackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.helpers.LocalPreviewImageLoader
import com.revenuecat.purchases.ui.revenuecatui.helpers.captureToImageCompat
import com.revenuecat.purchases.ui.revenuecatui.paywalls.PaywallImageLoader
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CompletableDeferred
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowPixelCopy
import java.net.URL
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(shadows = [ShadowPixelCopy::class], sdk = [26])
internal class ImageLoadingTests(private val imageType: ImageType) {

    enum class ImageType { COMPONENT, BACKGROUND }

    @get:Rule
    val composeTestRule = createComposeRule()

    private val requests = CopyOnWriteArrayList<ImageRequest>()
    private val failedUrl = "https://assets.pawwalls.com/failed.png"
    private val imageUrl = "https://assets.pawwalls.com/image.png"
    private var alwaysFail = false
    private var delayedUrl: String? = null
    private var responseGate: CompletableDeferred<Unit>? = null
    private var retryResponseGate: CompletableDeferred<Unit>? = null
    private lateinit var imageLoader: ImageLoader

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.Blue.toArgb())
        }
        imageLoader = ImageLoader.Builder(context)
            .components {
                add { chain ->
                    val request = chain.request
                    requests.add(request)
                    if (request.data == delayedUrl) responseGate?.await()
                    if (request.data == failedUrl && !request.memoryCachePolicy.readEnabled) retryResponseGate?.await()
                    if (request.data == failedUrl && (alwaysFail || request.memoryCachePolicy.readEnabled)) {
                        ErrorResult(
                            drawable = null,
                            request = request,
                            throwable = IllegalStateException("Unable to decode test image"),
                        )
                    } else {
                        SuccessResult(
                            drawable = BitmapDrawable(context.resources, bitmap),
                            request = request,
                            dataSource = DataSource.MEMORY,
                        )
                    }
                }
            }
            .build()
        mockkObject(PaywallImageLoader)
        every { PaywallImageLoader.get(any()) } returns imageLoader
    }

    @After
    fun teardown() {
        unmockkObject(PaywallImageLoader)
        imageLoader.shutdown()
    }

    @Test
    fun `Identical full and placeholder URLs use one image request`() {
        delayedUrl = imageUrl
        responseGate = CompletableDeferred()
        render(mutableStateOf(Sources(imageUrl, imageUrl)))

        waitForRequests(imageUrl, count = 1)

        assertThat(requests.map { it.data }).containsExactly(imageUrl)
        assertCacheEnabled(requests.single())
        responseGate?.complete(Unit)
        composeTestRule.waitForIdle()
        assertImageIsBlue()
    }

    @Test
    fun `Preview shows the blocking image when full and placeholder URLs match`() {
        assumeTrue("Preview image loading is intentionally disabled in release builds", BuildConfig.DEBUG)
        render(mutableStateOf(Sources(imageUrl, imageUrl)), inPreviewMode = true)

        assertImageIsBlue()
        assertThat(requests.map { it.data }).containsOnly(imageUrl)
    }

    @Test
    fun `Different placeholder URL still loads separately`() {
        val placeholderUrl = "https://assets.pawwalls.com/image_low_res.webp"
        delayedUrl = imageUrl
        responseGate = CompletableDeferred()
        render(mutableStateOf(Sources(imageUrl, placeholderUrl)))

        waitForRequests(imageUrl, count = 1)
        waitForRequests(placeholderUrl, count = 1)

        assertThat(requests.map { it.data }).containsExactlyInAnyOrder(imageUrl, placeholderUrl)
        responseGate?.complete(Unit)
    }

    @Test
    fun `Changing identical placeholder URL while full image loads shows new placeholder`() {
        val placeholderUrl = "https://assets.pawwalls.com/image_low_res.webp"
        val sources = mutableStateOf(Sources(imageUrl, imageUrl))
        delayedUrl = imageUrl
        responseGate = CompletableDeferred()
        render(sources)
        waitForRequests(imageUrl, count = 1)

        try {
            composeTestRule.runOnIdle { sources.value = Sources(imageUrl, placeholderUrl) }
            waitForRequests(placeholderUrl, count = 1)

            assertImageIsBlue()
            assertThat(requests.map { it.data }).containsExactlyInAnyOrder(imageUrl, placeholderUrl)
            assertCacheEnabled(requests.single { it.data == placeholderUrl })
        } finally {
            responseGate?.complete(Unit)
        }
    }

    @Test
    fun `Full image retry keeps separate placeholder cached`() {
        val placeholderUrl = "https://assets.pawwalls.com/image_low_res.webp"
        delayedUrl = failedUrl
        responseGate = CompletableDeferred()
        retryResponseGate = CompletableDeferred()
        render(mutableStateOf(Sources(failedUrl, placeholderUrl)))
        waitForRequests(placeholderUrl, count = 1)

        responseGate?.complete(Unit)
        waitForRequests(failedUrl, count = 2)

        val placeholderRequests = requests.filter { it.data == placeholderUrl }
        assertThat(placeholderRequests).isNotEmpty()
        placeholderRequests.forEach(::assertCacheEnabled)
        retryResponseGate?.complete(Unit)
    }

    @Test
    fun `Failed image retries once without reading caches and can recover`() {
        render(mutableStateOf(Sources(failedUrl, failedUrl)))

        waitForRequests(failedUrl, count = 2)

        assertThat(requests).hasSize(2)
        assertCacheEnabled(requests.first())
        assertThat(requests.last().memoryCachePolicy).isEqualTo(CachePolicy.WRITE_ONLY)
        assertThat(requests.last().diskCachePolicy).isEqualTo(CachePolicy.WRITE_ONLY)
        assertImageIsBlue()
    }

    @Test
    fun `Repeated failure does not trigger more retries`() {
        alwaysFail = true
        render(mutableStateOf(Sources(failedUrl, failedUrl)))

        waitForRequests(failedUrl, count = 2)

        assertThat(requests).hasSize(2)
    }

    @Test
    fun `Changing image after a failure enables cache reads`() {
        val sources = mutableStateOf(Sources(failedUrl, failedUrl))
        render(sources)
        waitForRequests(failedUrl, count = 2)

        composeTestRule.runOnIdle { sources.value = Sources(imageUrl, imageUrl) }
        waitForRequests(imageUrl, count = 1)

        assertCacheEnabled(requests.single { it.data == imageUrl })
    }

    @Test
    fun `Changing only query after a failure enables cache reads`() {
        val sources = mutableStateOf(Sources(failedUrl, failedUrl))
        render(sources)
        waitForRequests(failedUrl, count = 2)
        val updatedUrl = "$failedUrl?revision=2"

        composeTestRule.runOnIdle { sources.value = Sources(updatedUrl, updatedUrl) }
        waitForRequests(updatedUrl, count = 1)

        assertCacheEnabled(requests.single { it.data == updatedUrl })
    }

    private fun render(sources: MutableState<Sources>, inPreviewMode: Boolean = false) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalInspectionMode provides inPreviewMode,
                LocalPreviewImageLoader provides imageLoader.takeIf { inPreviewMode },
            ) {
                val source = sources.value
                when (imageType) {
                    ImageType.COMPONENT -> RemoteImage(
                        urlString = source.full,
                        placeholderUrlString = source.placeholder,
                        modifier = Modifier.requiredSize(100.dp).testTag("image"),
                    )
                    ImageType.BACKGROUND -> BackgroundImage(source)
                }
            }
        }
    }

    @Composable
    private fun BackgroundImage(sources: Sources) {
        val background = rememberBackgroundStyle(
            BackgroundStyles.Image(
                sources = ThemeImageUrls(
                    light = ImageUrls(
                        original = URL(sources.full),
                        webp = URL(sources.full),
                        webpLowRes = URL(sources.placeholder),
                        width = 100u,
                        height = 100u,
                    ),
                ),
                contentScale = ContentScale.Fit,
                colorOverlay = null,
            ),
        )
        Box(Modifier.requiredSize(100.dp).testTag("image").background(background))
    }

    private fun waitForRequests(url: String, count: Int) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            // Robolectric only draws these painters when PixelCopy captures the view.
            composeTestRule.onNodeWithTag("image").captureToImageCompat()
            requests.count { it.data == url } >= count
        }
        composeTestRule.waitForIdle()
    }

    private fun assertCacheEnabled(request: ImageRequest) {
        assertThat(request.memoryCachePolicy).isEqualTo(CachePolicy.ENABLED)
        assertThat(request.diskCachePolicy).isEqualTo(CachePolicy.ENABLED)
    }

    private fun assertImageIsBlue() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            val bitmap = composeTestRule.onNodeWithTag("image").captureToImageCompat().asAndroidBitmap()
            bitmap.getPixel(bitmap.width / 2, bitmap.height / 2) == Color.Blue.toArgb()
        }
    }

    private data class Sources(val full: String, val placeholder: String)

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun parameters(): Collection<Array<ImageType>> = ImageType.values().map { arrayOf(it) }
    }
}
