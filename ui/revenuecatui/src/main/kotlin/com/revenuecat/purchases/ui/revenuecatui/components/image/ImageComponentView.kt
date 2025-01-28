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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
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
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.CornerRadiuses
import com.revenuecat.purchases.paywalls.components.properties.FitMode
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.MaskShape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toContentScale
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toShape
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.urlsForCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.aspectRatio
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.border
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.overlay
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.shadow
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.previewEmptyState
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.forCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberBorderStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberShadowStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ImageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.RemoteImage
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import java.net.URL
import androidx.compose.ui.graphics.Color as ComposeColor

@JvmSynthetic
@Composable
internal fun ImageComponentView(
    style: ImageComponentStyle,
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
    previewImageLoader: ImageLoader? = null,
) {
    // Get an ImageComponentState that calculates the overridden properties we should use.
    val imageState = rememberUpdatedImageComponentState(
        style = style,
        paywallState = state,
    )

    if (imageState.visible) {
        val overlay = imageState.overlay?.forCurrentTheme
        val borderStyle = imageState.border?.let { rememberBorderStyle(border = it) }
        val shadowStyle = imageState.shadow?.let { rememberShadowStyle(shadow = it) }
        val composeShape by remember(imageState.shape) { derivedStateOf { imageState.shape ?: RectangleShape } }

        RemoteImage(
            urlString = imageState.imageUrls.webp.toString(),
            modifier = modifier
                .size(imageState.size)
                .applyIfNotNull(imageState.aspectRatio) { aspectRatio(it) }
                .padding(imageState.margin)
                .applyIfNotNull(shadowStyle) { shadow(it, composeShape) }
                .applyIfNotNull(overlay) { overlay(it, composeShape) }
                .clip(composeShape)
                .applyIfNotNull(borderStyle) { border(it, composeShape) }
                .padding(imageState.padding),
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
                shape = MaskShape.Rectangle(
                    corners = CornerRadiuses.Dp(
                        topLeading = 20.0,
                        topTrailing = 20.0,
                        bottomLeading = 20.0,
                        bottomTrailing = 20.0,
                    ),
                ),
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
                shape = MaskShape.Rectangle(
                    corners = CornerRadiuses.Dp(
                        topLeading = 20.0,
                        topTrailing = 20.0,
                        bottomLeading = 20.0,
                        bottomTrailing = 20.0,
                    ),
                ),
            ),
            state = previewEmptyState(),
            previewImageLoader = previewImageLoader(themeImageUrls),
        )
    }
}

@Preview
@Composable
private fun ImageComponentView_Preview_Margin_Padding() {
    val themeImageUrls = previewThemeImageUrls(widthPx = 400u, heightPx = 400u)
    Box(
        modifier = Modifier
            .height(200.dp)
            .background(ComposeColor.Gray),
    ) {
        ImageComponentView(
            style = previewImageComponentStyle(
                themeImageUrls = themeImageUrls,
                size = Size(width = Fixed(400u), height = Fixed(400u)),
                paddingValues = PaddingValues(20.dp),
                marginValues = PaddingValues(20.dp),
                fitMode = FitMode.FIT,
                shape = MaskShape.Rectangle(
                    corners = CornerRadiuses.Dp(
                        topLeading = 20.0,
                        topTrailing = 20.0,
                        bottomLeading = 20.0,
                        bottomTrailing = 20.0,
                    ),
                ),
                shadow = null,
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
    val themeImageUrls = previewThemeImageUrls(widthPx = 400u, heightPx = 400u)
    Box(modifier = Modifier.background(ComposeColor.Red)) {
        ImageComponentView(
            style = previewImageComponentStyle(
                themeImageUrls = themeImageUrls,
                size = Size(width = Fixed(400u), height = Fit),
                fitMode = FitMode.FIT,
                shape = MaskShape.Rectangle(
                    corners = CornerRadiuses.Dp(
                        topLeading = 20.0,
                        topTrailing = 20.0,
                        bottomLeading = 20.0,
                        bottomTrailing = 20.0,
                    ),
                ),
                overlay = ColorStyles(
                    light = ColorInfo.Gradient.Linear(
                        degrees = -90f,
                        points = listOf(
                            ColorInfo.Gradient.Point(
                                color = Color.parseColor("#88FF0000"),
                                percent = 0f,
                            ),
                            ColorInfo.Gradient.Point(
                                color = Color.parseColor("#8800FF00"),
                                percent = 50f,
                            ),
                            ColorInfo.Gradient.Point(
                                color = Color.parseColor("#880000FF"),
                                percent = 100f,
                            ),
                        ),
                    ).toColorStyle(),
                ),
            ),
            state = previewEmptyState(),
            previewImageLoader = previewImageLoader(themeImageUrls),
        )
    }
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun ImageComponentView_Preview_RadialGradient() {
    val themeImageUrls = previewThemeImageUrls(widthPx = 400u, heightPx = 400u)
    Box(modifier = Modifier.background(ComposeColor.Red)) {
        ImageComponentView(
            style = previewImageComponentStyle(
                themeImageUrls = themeImageUrls,
                size = Size(width = Fixed(400u), height = Fit),
                fitMode = FitMode.FIT,
                shape = MaskShape.Rectangle(
                    corners = CornerRadiuses.Dp(
                        topLeading = 20.0,
                        topTrailing = 20.0,
                        bottomLeading = 20.0,
                        bottomTrailing = 20.0,
                    ),
                ),
                overlay = ColorStyles(
                    light = ColorInfo.Gradient.Radial(
                        listOf(
                            ColorInfo.Gradient.Point(
                                color = Color.parseColor("#88FF0000"),
                                percent = 0f,
                            ),
                            ColorInfo.Gradient.Point(
                                color = Color.parseColor("#8800FF00"),
                                percent = 50f,
                            ),
                            ColorInfo.Gradient.Point(
                                color = Color.parseColor("#880000FF"),
                                percent = 100f,
                            ),
                        ),
                    ).toColorStyle(),
                ),
            ),
            state = previewEmptyState(),
            previewImageLoader = previewImageLoader(themeImageUrls),
        )
    }
}

private class MaskShapeProvider : PreviewParameterProvider<MaskShape> {
    override val values: Sequence<MaskShape> = sequenceOf(
        MaskShape.Rectangle(
            corners = CornerRadiuses.Dp(
                topLeading = 30.0,
                topTrailing = 50.0,
                bottomLeading = 20.0,
                bottomTrailing = 40.0,
            ),
        ),
        MaskShape.Concave,
        MaskShape.Convex,
        MaskShape.Circle,
    )
}

@Preview
@Composable
private fun ImageComponentView_Preview_MaskShape(
    @PreviewParameter(MaskShapeProvider::class) maskShape: MaskShape,
) {
    val themeImageUrls = previewThemeImageUrls(widthPx = 400u, heightPx = 200u)
    Box(modifier = Modifier.background(ComposeColor.Blue)) {
        ImageComponentView(
            style = previewImageComponentStyle(
                themeImageUrls = themeImageUrls,
                size = Size(width = Fixed(400u), height = Fixed(200u)),
                fitMode = FitMode.FIT,
                shape = maskShape,
            ),
            state = previewEmptyState(),
            previewImageLoader = previewImageLoader(themeImageUrls),
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun previewImageComponentStyle(
    themeImageUrls: ThemeImageUrls,
    size: Size,
    fitMode: FitMode,
    shape: MaskShape,
    overlay: ColorStyles? = null,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    marginValues: PaddingValues = PaddingValues(0.dp),
    border: BorderStyles? = BorderStyles(
        width = 2.dp,
        colors = ColorStyles(light = ColorStyle.Solid(ComposeColor.Cyan)),
    ),
    shadow: ShadowStyles? = ShadowStyles(
        colors = ColorStyles(ColorStyle.Solid(ComposeColor.Black)),
        radius = 10.dp,
        x = 0.dp,
        y = 3.dp,
    ),
) = ImageComponentStyle(
    sources = nonEmptyMapOf(LocaleId("en_US") to themeImageUrls),
    size = size,
    shape = shape.toShape(),
    padding = paddingValues,
    margin = marginValues,
    border = border,
    shadow = shadow,
    overlay = overlay,
    contentScale = fitMode.toContentScale(),
    rcPackage = null,
    overrides = null,
)

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
