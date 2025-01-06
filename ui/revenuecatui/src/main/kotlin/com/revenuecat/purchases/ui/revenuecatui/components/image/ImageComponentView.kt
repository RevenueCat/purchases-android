@file:JvmSynthetic
@file:Suppress("TooManyFunctions")

package com.revenuecat.purchases.ui.revenuecatui.components.image

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.core.graphics.component3
import androidx.core.graphics.component4
import coil.ImageLoader
import coil.decode.DataSource
import coil.request.SuccessResult
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FitMode
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toContentScale
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.urlsForCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.aspectRatio
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.overlay
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ImageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.RemoteImage
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatePaywallComponentsDataOrNull
import java.net.URL
import androidx.compose.ui.graphics.Color as ComposeColor

@JvmSynthetic
@Composable
internal fun ImageComponentView(
    style: ImageComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    previewImageLoader: ImageLoader? = null,
) {
    // Get an ImageComponentState that calculates the overridden properties we should use.
    val imageState = rememberUpdatedImageComponentState(
        style = style,
        paywallState = state,
        selected = selected,
    )

    if (imageState.visible) {
        val overlay = imageState.overlay?.let { rememberColorStyle(it) }
        RemoteImage(
            urlString = imageState.imageUrls.webp.toString(),
            modifier = modifier
                .size(imageState.size)
                .applyIfNotNull(imageState.aspectRatio) { aspectRatio(it) }
                .applyIfNotNull(overlay) { overlay(it, imageState.shape ?: RectangleShape) }
                .applyIfNotNull(imageState.shape) { clip(it) },
            placeholderUrlString = imageState.imageUrls.webpLowRes.toString(),
            contentScale = imageState.contentScale,
            previewImageLoader = previewImageLoader,
        )
    }
}

private class PreviewParameters(
    @Px val imageWidth: UInt,
    @Px val imageHeight: UInt,
    val viewSize: Size,
    val fitMode: FitMode,
)

private class PreviewParametersProvider : PreviewParameterProvider<PreviewParameters> {
    override val values: Sequence<PreviewParameters> = sequenceOf(
        PreviewParameters(
            imageWidth = 100u,
            imageHeight = 100u,
            viewSize = Size(width = Fixed(200u), height = Fixed(200u)),
            fitMode = FitMode.FILL,
        ),
        PreviewParameters(
            imageWidth = 100u,
            imageHeight = 100u,
            viewSize = Size(width = Fixed(200u), height = Fixed(200u)),
            fitMode = FitMode.FIT,
        ),
        PreviewParameters(
            imageWidth = 100u,
            imageHeight = 100u,
            viewSize = Size(width = Fixed(200u), height = Fixed(50u)),
            fitMode = FitMode.FILL,
        ),
        PreviewParameters(
            imageWidth = 100u,
            imageHeight = 100u,
            viewSize = Size(width = Fixed(200u), height = Fixed(50u)),
            fitMode = FitMode.FIT,
        ),
        PreviewParameters(
            imageWidth = 100u,
            imageHeight = 100u,
            viewSize = Size(width = Fixed(50u), height = Fixed(200u)),
            fitMode = FitMode.FILL,
        ),
        PreviewParameters(
            imageWidth = 100u,
            imageHeight = 100u,
            viewSize = Size(width = Fixed(50u), height = Fixed(200u)),
            fitMode = FitMode.FIT,
        ),
        PreviewParameters(
            imageWidth = 100u,
            imageHeight = 100u,
            viewSize = Size(width = Fixed(72u), height = Fit),
            fitMode = FitMode.FILL,
        ),
        PreviewParameters(
            imageWidth = 100u,
            imageHeight = 100u,
            viewSize = Size(width = Fit, height = Fixed(72u)),
            fitMode = FitMode.FILL,
        ),
        PreviewParameters(
            imageWidth = 1909u,
            imageHeight = 1306u,
            viewSize = Size(width = Fill, height = Fit),
            fitMode = FitMode.FIT,
        ),
        PreviewParameters(
            imageWidth = 1306u,
            imageHeight = 1909u,
            viewSize = Size(width = Fit, height = Fill),
            fitMode = FitMode.FIT,
        ),
    )
}

@Preview
@Composable
private fun ImageComponentView_Preview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    val themeImageUrls = previewThemeImageUrls(widthPx = parameters.imageWidth, heightPx = parameters.imageHeight)
    Box(modifier = Modifier.background(ComposeColor.Red)) {
        ImageComponentView(
            style = previewImageComponentStyle(
                themeImageUrls = themeImageUrls,
                size = parameters.viewSize,
                fitMode = parameters.fitMode,
            ),
            state = previewEmptyState(),
            previewImageLoader = previewImageLoader(themeImageUrls),
        )
    }
}

@Preview
@Composable
private fun ImageComponentView_Preview_SmallerContainer() {
    val themeImageUrls = previewThemeImageUrls(widthPx = 400u, heightPx = 400u)
    Box(
        modifier = Modifier
            .height(200.dp)
            .background(ComposeColor.Blue),
    ) {
        ImageComponentView(
            style = previewImageComponentStyle(
                themeImageUrls = themeImageUrls,
                size = Size(width = Fixed(400u), height = Fixed(400u)),
                fitMode = FitMode.FIT,
            ),
            state = previewEmptyState(),
            previewImageLoader = previewImageLoader(themeImageUrls),
        )
    }
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun ImageComponentView_Preview_LinearGradient() {
    val themeImageUrls = previewThemeImageUrls(widthPx = 100u, heightPx = 100u)
    Box(modifier = Modifier.background(ComposeColor.Red)) {
        ImageComponentView(
            style = previewImageComponentStyle(
                themeImageUrls = themeImageUrls,
                size = Size(width = Fixed(400u), height = Fit),
                fitMode = FitMode.FIT,
                overlay = ColorScheme(
                    light = ColorInfo.Gradient.Linear(
                        degrees = -90f,
                        points = listOf(
                            ColorInfo.Gradient.Point(
                                color = Color.parseColor("#88FF0000"),
                                percent = 0f,
                            ),
                            ColorInfo.Gradient.Point(
                                color = Color.parseColor("#8800FF00"),
                                percent = 0.5f,
                            ),
                            ColorInfo.Gradient.Point(
                                color = Color.parseColor("#880000FF"),
                                percent = 1f,
                            ),
                        ),
                    ),
                ),
            ),
            state = previewEmptyState(),
        )
    }
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun ImageComponentView_Preview_RadialGradient() {
    val themeImageUrls = previewThemeImageUrls(widthPx = 100u, heightPx = 100u)
    Box(modifier = Modifier.background(ComposeColor.Red)) {
        ImageComponentView(
            style = previewImageComponentStyle(
                themeImageUrls = themeImageUrls,
                size = Size(width = Fixed(400u), height = Fit),
                fitMode = FitMode.FIT,
                overlay = ColorScheme(
                    light = ColorInfo.Gradient.Radial(
                        listOf(
                            ColorInfo.Gradient.Point(
                                color = Color.parseColor("#88FF0000"),
                                percent = 0f,
                            ),
                            ColorInfo.Gradient.Point(
                                color = Color.parseColor("#8800FF00"),
                                percent = 0.5f,
                            ),
                            ColorInfo.Gradient.Point(
                                color = Color.parseColor("#880000FF"),
                                percent = 1f,
                            ),
                        ),
                    ),
                ),
            ),
            state = previewEmptyState(),
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun previewImageComponentStyle(
    themeImageUrls: ThemeImageUrls,
    size: Size,
    fitMode: FitMode,
    overlay: ColorScheme? = null,
) = ImageComponentStyle(
    sources = nonEmptyMapOf(LocaleId("en_US") to themeImageUrls),
    size = size,
    shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 20.dp),
    overlay = overlay,
    contentScale = fitMode.toContentScale(),
    overrides = null,
)

private fun previewEmptyState(): PaywallState.Loaded.Components {
    val data = PaywallComponentsData(
        templateName = "template",
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = ComponentsConfig(
            base = PaywallComponentsConfig(
                // This would normally contain at least one ImageComponent, but that's not needed for previews.
                stack = StackComponent(components = emptyList()),
                background = Background.Color(
                    ColorScheme(light = ColorInfo.Hex(androidx.compose.ui.graphics.Color.White.toArgb())),
                ),
                stickyFooter = null,
            ),
        ),
        componentsLocalizations = nonEmptyMapOf(
            LocaleId("en_US") to nonEmptyMapOf(LocalizationKey("dummy") to LocalizationData.Text("dummy")),
        ),
        defaultLocaleIdentifier = LocaleId("en_US"),
    )
    val offering = Offering(
        identifier = "identifier",
        serverDescription = "serverDescription",
        metadata = emptyMap(),
        availablePackages = emptyList(),
        paywallComponents = data,
    )
    val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
    return offering.toComponentsPaywallState(validated)
}

@Composable
private fun previewImageLoader(themeImageUrls: ThemeImageUrls) =
    previewImageLoader(imageUrls = themeImageUrls.urlsForCurrentTheme)

@Composable
private fun previewImageLoader(
    imageUrls: ImageUrls,
    @DrawableRes resource: Int = R.drawable.android,
): ImageLoader {
    val context = LocalContext.current
    return ImageLoader.Builder(context)
        .components {
            add { chain ->
                SuccessResult(
                    drawable = BitmapDrawable(
                        chain.request.context.resources,
                        context.getDrawable(resource)!!.toBitmap(
                            width = imageUrls.width,
                            height = imageUrls.height,
                            // Create a deterministic color from the URL and size.
                            background = with(imageUrls) { "$original:$width$height".toRgbColor() },
                        ),
                    ),
                    request = chain.request,
                    dataSource = DataSource.MEMORY,
                )
            }
        }
        .build()
}

private fun previewThemeImageUrls(widthPx: UInt, heightPx: UInt): ThemeImageUrls =
    ThemeImageUrls(
        light = ImageUrls(
            original = URL("https://preview"),
            webp = URL("https://preview"),
            webpLowRes = URL("https://preview"),
            width = widthPx,
            height = heightPx,
        ),
    )

/**
 * Converts this drawable to a bitmap with a [background].
 */
@Suppress("DestructuringDeclarationWithTooManyEntries")
fun Drawable.toBitmap(
    @Px width: UInt,
    @Px height: UInt,
    @ColorInt background: Int,
): Bitmap {
    val (oldLeft, oldTop, oldRight, oldBottom) = bounds

    val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    canvas.drawColor(background)

    setBounds(0, 0, width.toInt(), height.toInt())
    draw(canvas)
    setBounds(oldLeft, oldTop, oldRight, oldBottom)

    return bitmap
}

@Suppress("MagicNumber")
private fun String.toRgbColor(): Int {
    val hash = hashCode()
    // Use the hash to generate ARGB color components
    val r = (hash shr 16 and 0xFF)
    val g = (hash shr 8 and 0xFF)
    val b = (hash and 0xFF)

    // Combine the components into a color integer with full opacity (alpha = 255)
    return 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
}
