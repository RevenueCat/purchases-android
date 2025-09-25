package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.LayoutDirection

internal object PlaceholderDefaults {
    /**
     * The default [InfiniteRepeatableSpec] to use for [fade].
     */
    val fadeAnimationSpec: InfiniteRepeatableSpec<Float> by lazy {
        infiniteRepeatable(
            animation = tween(delayMillis = 200, durationMillis = 600),
            repeatMode = RepeatMode.Reverse,
        )
    }
}

/**
 * Internal custom placeholder Modifier.
 *
 * @param visible whether the placeholder should be visible or not.
 * @param color the color used to draw the placeholder UI.
 * @param shape desired shape of the placeholder. Defaults to [RectangleShape].
 * @param highlight optional highlight animation.
 * @param placeholderFadeTransitionSpec The transition spec to use when fading the placeholder
 * on/off screen. The boolean parameter defined for the transition is [visible].
 * @param contentFadeTransitionSpec The transition spec to use when fading the content
 * on/off screen. The boolean parameter defined for the transition is [visible].
 */
@Suppress("LongParameterList", "ModifierComposable")
@Composable
internal fun Modifier.placeholder(
    visible: Boolean,
    color: Color,
    shape: Shape = RectangleShape,
    highlight: PlaceholderHighlight? = null,
    placeholderFadeTransitionSpec: () -> FiniteAnimationSpec<Float> = { spring() },
    contentFadeTransitionSpec: () -> FiniteAnimationSpec<Float> = { spring() },
): Modifier {
    val placeholder = rememberPlaceholder(
        visible = visible,
        color = color,
        shape = shape,
        highlight = highlight,
        placeholderFadeTransitionSpec = placeholderFadeTransitionSpec,
        contentFadeTransitionSpec = contentFadeTransitionSpec,
    )

    return this then PlaceholderElement(placeholder = placeholder)
}

/**
 * Internal placeholder systems to remember placeholder and running & stopping the placeholder.
 *
 * @param visible whether the placeholder should be visible or not.
 * @param color the color used to draw the placeholder UI.
 * @param shape desired shape of the placeholder. Defaults to [RectangleShape].
 * @param highlight optional highlight animation.
 * @param placeholderFadeTransitionSpec The transition spec to use when fading the placeholder
 * on/off screen. The boolean parameter defined for the transition is [visible].
 * @param contentFadeTransitionSpec The transition spec to use when fading the content
 * on/off screen. The boolean parameter defined for the transition is [visible].
 */
@Suppress("LongParameterList")
@Composable
internal fun rememberPlaceholder(
    visible: Boolean,
    color: Color,
    shape: Shape = RectangleShape,
    highlight: PlaceholderHighlight? = null,
    placeholderFadeTransitionSpec: () -> FiniteAnimationSpec<Float> = { spring() },
    contentFadeTransitionSpec: () -> FiniteAnimationSpec<Float> = { spring() },
): Placeholder {
    val placeholder: Placeholder = remember(
        keys = arrayOf(visible, color, shape, highlight, placeholderFadeTransitionSpec, contentFadeTransitionSpec),
    ) {
        Placeholder(
            visible = visible,
            color = color,
            shape = shape,
            highlight = highlight,
            placeholderFadeTransitionSpec = placeholderFadeTransitionSpec,
            contentFadeTransitionSpec = contentFadeTransitionSpec,
        )
    }

    LaunchedEffect(key1 = placeholder) {
        if (visible) {
            placeholder.startAnimation()
        } else {
            placeholder.stopAnimation()
        }
    }

    return placeholder
}

/**
 * Internal placeholder data class for holding placeholder relevant data.
 *
 * @param visible whether the placeholder should be visible or not.
 * @param color the color used to draw the placeholder UI.
 * @param shape desired shape of the placeholder. Defaults to [RectangleShape].
 * @param highlight optional highlight animation.
 * @param placeholderFadeTransitionSpec The transition spec to use when fading the placeholder
 * on/off screen. The boolean parameter defined for the transition is [visible].
 * @param contentFadeTransitionSpec The transition spec to use when fading the content
 * on/off screen. The boolean parameter defined for the transition is [visible].
 */
@Suppress("LongParameterList", "MagicNumber")
@Stable
internal data class Placeholder(
    private val visible: Boolean,
    private val color: Color,
    private val shape: Shape = RectangleShape,
    private val highlight: PlaceholderHighlight? = null,
    private val placeholderFadeTransitionSpec: () -> FiniteAnimationSpec<Float> = { spring() },
    private val contentFadeTransitionSpec: () -> FiniteAnimationSpec<Float> = { spring() },
) {
    private var lastSize: Size? = null
    private var lastLayoutDirection: LayoutDirection? = null
    private var lastOutline: Outline? = null

    private val placeholderAlpha = Animatable(if (visible) 1f else 0f)
    private val contentAlpha = Animatable(if (visible) 0f else 1f)
    private val highlightProgress = Animatable(0f)
    private val paint = Paint().apply {
        isAntiAlias = true
        style = PaintingStyle.Fill
        blendMode = this.blendMode
    }

    internal suspend fun startAnimation() {
        placeholderAlpha.animateTo(
            targetValue = if (visible) 1f else 0f,
            animationSpec = placeholderFadeTransitionSpec(),
        )
        contentAlpha.animateTo(
            targetValue = if (visible) 0f else 1f,
            animationSpec = contentFadeTransitionSpec(),
        )

        // Coroutine for the infinite highlight (shimmer) animation
        val shouldAnimateHighlight = visible && highlight?.animationSpec != null
        highlightProgress.stop()
        if (shouldAnimateHighlight) {
            highlightProgress.animateTo(
                targetValue = 1f,
                animationSpec = highlight!!.animationSpec!!,
            )
        } else {
            highlightProgress.snapTo(0f)
        }
    }

    internal suspend fun stopAnimation() {
        placeholderAlpha.stop()
        contentAlpha.stop()
        highlightProgress.stop()
    }

    internal fun ContentDrawScope.draw() {
        val pAlpha = placeholderAlpha.value
        val cAlpha = contentAlpha.value

        // Draw content
        if (cAlpha > 0.01f) {
            paint.alpha = cAlpha
            withLayer(paint) {
                with(this@draw) {
                    drawContent()
                }
            }
        }

        // Draw placeholder
        if (pAlpha > 0.01f) {
            paint.alpha = pAlpha
            withLayer(paint) {
                lastOutline = drawPlaceholder(
                    shape = shape,
                    color = color,
                    highlight = highlight,
                    progress = highlightProgress.value,
                    lastOutline = lastOutline,
                    lastLayoutDirection = lastLayoutDirection,
                    lastSize = lastSize,
                )
            }
        }

        // Cache size and direction
        lastSize = size
        lastLayoutDirection = layoutDirection
    }
}

private class PlaceholderNode(
    var placeholder: Placeholder,
) : Modifier.Node(), DrawModifierNode {

    override fun ContentDrawScope.draw() {
        with(placeholder) {
            draw()
        }
    }
}

// The factory for our PlaceholderNode
private data class PlaceholderElement(
    var placeholder: Placeholder,
) : ModifierNodeElement<PlaceholderNode>() {
    override fun create(): PlaceholderNode {
        return PlaceholderNode(placeholder = placeholder)
    }

    override fun update(node: PlaceholderNode) {
        node.placeholder = placeholder
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "placeholder"
        properties["placeholder"] = placeholder
        properties["loadingDescription"] = "Loading.."
    }
}

@Suppress("LongParameterList")
private fun DrawScope.drawPlaceholder(
    shape: Shape,
    color: Color,
    highlight: PlaceholderHighlight?,
    progress: Float,
    lastOutline: Outline?,
    lastLayoutDirection: LayoutDirection?,
    lastSize: Size?,
): Outline? {
    if (shape === RectangleShape) {
        drawRect(color = color)
        if (highlight != null) {
            drawRect(
                brush = highlight.brush(progress, size),
                alpha = highlight.alpha(progress),
            )
        }
        return null
    }

    val outline = lastOutline.takeIf {
        size == lastSize && layoutDirection == lastLayoutDirection
    } ?: shape.createOutline(size, layoutDirection, this)

    drawOutline(outline = outline, color = color)
    if (highlight != null) {
        drawOutline(
            outline = outline,
            brush = highlight.brush(progress, size),
            alpha = highlight.alpha(progress),
        )
    }
    return outline
}

private inline fun DrawScope.withLayer(
    paint: Paint,
    drawBlock: DrawScope.() -> Unit,
) = drawIntoCanvas { canvas ->
    canvas.saveLayer(size.toRect(), paint)
    drawBlock()
    canvas.restore()
}

internal interface PlaceholderHighlight {
    val animationSpec: InfiniteRepeatableSpec<Float>?
    fun brush(@FloatRange(from = 0.0, to = 1.0) progress: Float, size: Size): Brush

    @FloatRange(from = 0.0, to = 1.0)
    fun alpha(progress: Float): Float
}

// region highlights

internal data class Fade(
    private val highlightColor: Color,
    override val animationSpec: InfiniteRepeatableSpec<Float>,
) : PlaceholderHighlight {
    private val brush = SolidColor(highlightColor)

    /**
     * Return a [Brush] to draw for the given [progress] and [size].
     *
     * @param progress the current animated progress in the range of 0f..1f.
     * @param size The size of the current layout to draw in.
     */
    override fun brush(progress: Float, size: Size): Brush = brush

    /**
     * Return the desired alpha value used for drawing the [Brush] returned from [brush].
     *
     * @param progress the current animated progress in the range of 0f..1f.
     */
    override fun alpha(progress: Float): Float = progress
}
