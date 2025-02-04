@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.translate
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toColorStyle

@JvmSynthetic
@Stable
internal fun Modifier.shadow(
    shadow: ShadowStyle,
    shape: Shape,
) = this.drawBehind {
    // Where to draw
    val offset = Offset(x = shadow.x.toPx(), y = shadow.y.toPx())
    val path = shape.toPath(size, layoutDirection, this, offset)
    // What to draw
    val paint = Paint().apply {
        when (shadow.color) {
            is ColorStyle.Solid -> color = shadow.color.color
            is ColorStyle.Gradient -> shadow.color.brush.applyTo(size = size, p = this, alpha = 1f)
        }

        if (shadow.radius != 0.dp) {
            asFrameworkPaint().maskFilter = BlurMaskFilter(shadow.radius.toPx(), BlurMaskFilter.Blur.NORMAL)
        }
    }

    // Actually drawing
    drawIntoCanvas { canvas ->
        canvas.save()
        // Make sure we don't draw inside our original shape. This would be visible if the composable is transparent.
        canvas.clipPath(shape.toPath(size, layoutDirection, this), ClipOp.Difference)
        canvas.drawPath(path, paint)
        canvas.restore()
    }
}

private fun Shape.toPath(size: Size, layoutDirection: LayoutDirection, density: Density, offset: Offset? = null): Path {
    val outline = createOutline(size, layoutDirection, density)

    return if (offset == null) {
        Path().apply { addOutline(outline) }
    } else {
        Path().apply { addOutline(outline, offset) }
    }
}

private fun Path.addOutline(outline: Outline, offset: Offset) {
    when (outline) {
        is Outline.Rectangle -> addRect(outline.rect.translate(offset))
        is Outline.Rounded -> addRoundRect(outline.roundRect.translate(offset))
        is Outline.Generic -> addPath(outline.path, offset)
    }
}

@Suppress("MagicNumber")
@Preview("Circle")
@Composable
private fun Shadow_Preview_Circle() {
    val shape = CircleShape
    Box(
        modifier = Modifier
            .requiredSize(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .requiredSize(100.dp)
                .shadow(
                    shadow = ShadowStyle(
                        color = ColorStyle.Solid(Color.Black),
                        x = 5.dp,
                        y = 5.dp,
                        radius = 0.dp,
                    ),
                    shape = shape,
                )
                .background(Color.Red, shape = shape),
        )
    }
}

@Suppress("MagicNumber")
@Preview("Square")
@Composable
private fun Shadow_Preview_Square() {
    val shape = RectangleShape
    Box(
        modifier = Modifier
            .requiredSize(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .requiredSize(100.dp)
                .shadow(
                    shadow = ShadowStyle(
                        color = ColorStyle.Solid(Color.Black),
                        x = 10.dp,
                        y = 5.dp,
                        radius = 20.dp,
                    ),
                    shape = shape,
                )
                .background(Color.Red, shape = shape),
        )
    }
}

@Preview
@Composable
private fun Shadow_Preview_CircleAlpha() {
    val shape = CircleShape
    Box(
        modifier = Modifier
            .requiredSize(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .requiredSize(100.dp)
                .shadow(
                    shadow = ShadowStyle(
                        color = ColorStyle.Solid(Color.Black),
                        x = 5.dp,
                        y = 5.dp,
                        radius = 0.dp,
                    ),
                    shape = shape,
                )
                .background(Color.Red.copy(alpha = 0.5f), shape = shape),
        )
    }
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun Shadow_Preview_SquareAlpha() {
    val shape = RectangleShape
    Box(
        modifier = Modifier
            .requiredSize(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .requiredSize(100.dp)
                .shadow(
                    shadow = ShadowStyle(
                        color = ColorStyle.Solid(Color.Black),
                        x = 10.dp,
                        y = 5.dp,
                        radius = 20.dp,
                    ),
                    shape = shape,
                )
                .background(Color.Red.copy(alpha = 0.5f), shape = shape),
        )
    }
}

@Suppress("MagicNumber")
@Preview("Gradient+CustomShape")
@Composable
private fun Shadow_Preview_Gradient_CustomShape() {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .requiredSize(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "GET UNLIMITED RGB",
            modifier = Modifier
                .shadow(
                    shadow = ShadowStyle(
                        color = ColorInfo.Gradient.Linear(
                            degrees = 0f,
                            points = listOf(
                                ColorInfo.Gradient.Point(
                                    color = Color.Red.toArgb(),
                                    percent = 10f,
                                ),
                                ColorInfo.Gradient.Point(
                                    color = Color.Green.toArgb(),
                                    percent = 50f,
                                ),
                                ColorInfo.Gradient.Point(
                                    color = Color.Blue.toArgb(),
                                    percent = 90f,
                                ),
                            ),
                        ).toColorStyle(),
                        x = 0.dp,
                        y = 6.dp,
                        radius = 9.5.dp,
                    ),
                    shape = shape,
                )
                .background(Color.Black, shape = shape)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Suppress("MagicNumber")
@Preview("Margin")
@Composable
private fun Shadow_Preview_Margin() {
    val margin = PaddingValues(start = 8.dp, top = 16.dp, end = 4.dp, bottom = 24.dp)
    val shape = RectangleShape
    Column(
        modifier = Modifier
            .requiredSize(width = 100.dp, height = 200.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .padding(margin)
                .requiredSize(width = 50.dp, height = 50.dp)
                .shadow(
                    shadow = ShadowStyle(
                        color = ColorStyle.Solid(Color.Black),
                        x = 0.dp,
                        y = 5.dp,
                        radius = 20.dp,
                    ),
                    shape = shape,
                )
                .background(Color.Red, shape)
                .border(width = 2.dp, Color.Blue, shape)
                .padding(all = 16.dp),
        )

        Box(
            modifier = Modifier
                .padding(margin)
                .requiredSize(width = 50.dp, height = 50.dp)
                .shadow(
                    shadow = ShadowStyle(
                        color = ColorStyle.Solid(Color.Black),
                        x = 0.dp,
                        y = 5.dp,
                        radius = 20.dp,
                    ),
                    shape = shape,
                )
                .background(Color.Red, shape)
                .border(width = 2.dp, Color.Blue, shape)
                .padding(all = 16.dp),
        )
    }
}
