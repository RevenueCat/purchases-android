package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Switch
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.border
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.SwitchTokens.TrackOutlineWidth
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.launch

/**
 * Identical to Material3's [Switch][androidx.compose.material3.Switch], but supporting gradients for its thumb, track,
 * and border colors.
 */
@Composable
@Suppress("LongParameterList")
internal fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

    val toggleableModifier =
        if (onCheckedChange != null) {
            Modifier
                .minimumInteractiveComponentSize()
                .toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    enabled = enabled,
                    role = Role.Switch,
                    interactionSource = interactionSource,
                    indication = null,
                )
        } else {
            Modifier
        }

    SwitchImpl(
        modifier =
        modifier
            .then(toggleableModifier)
            .wrapContentSize(Alignment.Center)
            .requiredSize(SwitchWidth, SwitchHeight),
        checked = checked,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        thumbShape = SwitchTokens.HandleShape.value,
        thumbContent = thumbContent,
    )
}

internal object SwitchDefaults {
    /**
     * Creates a [SwitchColors] that represents the different colors used in a [Switch] in different
     * states.
     *
     * @param checkedThumbColor the color used for the thumb when enabled and checked
     * @param checkedTrackColor the color used for the track when enabled and checked
     * @param checkedBorderColor the color used for the border when enabled and checked
     * @param checkedIconColor the color used for the icon when enabled and checked
     * @param uncheckedThumbColor the color used for the thumb when enabled and unchecked
     * @param uncheckedTrackColor the color used for the track when enabled and unchecked
     * @param uncheckedBorderColor the color used for the border when enabled and unchecked
     * @param uncheckedIconColor the color used for the icon when enabled and unchecked
     * @param disabledCheckedThumbColor the color used for the thumb when disabled and checked
     * @param disabledCheckedTrackColor the color used for the track when disabled and checked
     * @param disabledCheckedBorderColor the color used for the border when disabled and checked
     * @param disabledCheckedIconColor the color used for the icon when disabled and checked
     * @param disabledUncheckedThumbColor the color used for the thumb when disabled and unchecked
     * @param disabledUncheckedTrackColor the color used for the track when disabled and unchecked
     * @param disabledUncheckedBorderColor the color used for the border when disabled and unchecked
     * @param disabledUncheckedIconColor the color used for the icon when disabled and unchecked
     */
    @Suppress("LongParameterList")
    @Composable
    fun colors(
        checkedThumbColor: ColorStyle = ColorStyle.Solid(SwitchTokens.SelectedHandleColor.value),
        checkedTrackColor: ColorStyle = ColorStyle.Solid(SwitchTokens.SelectedTrackColor.value),
        checkedBorderColor: ColorStyle = ColorStyle.Solid(Color.Transparent),
        checkedIconColor: Color = SwitchTokens.SelectedIconColor.value,
        uncheckedThumbColor: ColorStyle = ColorStyle.Solid(SwitchTokens.UnselectedHandleColor.value),
        uncheckedTrackColor: ColorStyle = ColorStyle.Solid(SwitchTokens.UnselectedTrackColor.value),
        uncheckedBorderColor: ColorStyle = ColorStyle.Solid(SwitchTokens.UnselectedFocusTrackOutlineColor.value),
        uncheckedIconColor: Color = SwitchTokens.UnselectedIconColor.value,
        disabledCheckedThumbColor: ColorStyle =
            ColorStyle.Solid(
                SwitchTokens.DisabledSelectedHandleColor.value
                    .copy(alpha = SwitchTokens.DisabledSelectedHandleOpacity)
                    .compositeOver(MaterialTheme.colorScheme.surface),
            ),
        disabledCheckedTrackColor: ColorStyle =
            ColorStyle.Solid(
                SwitchTokens.DisabledSelectedTrackColor.value
                    .copy(alpha = SwitchTokens.DisabledTrackOpacity)
                    .compositeOver(MaterialTheme.colorScheme.surface),
            ),
        disabledCheckedBorderColor: ColorStyle = ColorStyle.Solid(Color.Transparent),
        disabledCheckedIconColor: Color =
            SwitchTokens.DisabledSelectedIconColor.value
                .copy(alpha = SwitchTokens.DisabledSelectedIconOpacity)
                .compositeOver(MaterialTheme.colorScheme.surface),
        disabledUncheckedThumbColor: ColorStyle =
            ColorStyle.Solid(
                SwitchTokens.DisabledUnselectedHandleColor.value
                    .copy(alpha = SwitchTokens.DisabledUnselectedHandleOpacity)
                    .compositeOver(MaterialTheme.colorScheme.surface),
            ),
        disabledUncheckedTrackColor: ColorStyle =
            ColorStyle.Solid(
                SwitchTokens.DisabledUnselectedTrackColor.value
                    .copy(alpha = SwitchTokens.DisabledTrackOpacity)
                    .compositeOver(MaterialTheme.colorScheme.surface),
            ),
        disabledUncheckedBorderColor: ColorStyle =
            ColorStyle.Solid(
                SwitchTokens.DisabledUnselectedTrackOutlineColor.value
                    .copy(alpha = SwitchTokens.DisabledTrackOpacity)
                    .compositeOver(MaterialTheme.colorScheme.surface),
            ),
        disabledUncheckedIconColor: Color =
            SwitchTokens.DisabledUnselectedIconColor.value
                .copy(alpha = SwitchTokens.DisabledUnselectedIconOpacity)
                .compositeOver(MaterialTheme.colorScheme.surface),
    ): SwitchColors =
        SwitchColors(
            checkedThumbColor = checkedThumbColor,
            checkedTrackColor = checkedTrackColor,
            checkedBorderColor = checkedBorderColor,
            checkedIconColor = checkedIconColor,
            uncheckedThumbColor = uncheckedThumbColor,
            uncheckedTrackColor = uncheckedTrackColor,
            uncheckedBorderColor = uncheckedBorderColor,
            uncheckedIconColor = uncheckedIconColor,
            disabledCheckedThumbColor = disabledCheckedThumbColor,
            disabledCheckedTrackColor = disabledCheckedTrackColor,
            disabledCheckedBorderColor = disabledCheckedBorderColor,
            disabledCheckedIconColor = disabledCheckedIconColor,
            disabledUncheckedThumbColor = disabledUncheckedThumbColor,
            disabledUncheckedTrackColor = disabledUncheckedTrackColor,
            disabledUncheckedBorderColor = disabledUncheckedBorderColor,
            disabledUncheckedIconColor = disabledUncheckedIconColor,
        )
}

@Suppress("LongParameterList")
@Poko
@Immutable
internal class SwitchColors(
    private val checkedThumbColor: ColorStyle,
    private val checkedTrackColor: ColorStyle,
    private val checkedBorderColor: ColorStyle,
    private val checkedIconColor: Color,
    private val uncheckedThumbColor: ColorStyle,
    private val uncheckedTrackColor: ColorStyle,
    private val uncheckedBorderColor: ColorStyle,
    private val uncheckedIconColor: Color,
    private val disabledCheckedThumbColor: ColorStyle,
    private val disabledCheckedTrackColor: ColorStyle,
    private val disabledCheckedBorderColor: ColorStyle,
    private val disabledCheckedIconColor: Color,
    private val disabledUncheckedThumbColor: ColorStyle,
    private val disabledUncheckedTrackColor: ColorStyle,
    private val disabledUncheckedBorderColor: ColorStyle,
    private val disabledUncheckedIconColor: Color,
) {
    @Stable
    internal fun thumbColor(enabled: Boolean, checked: Boolean): ColorStyle =
        if (enabled) {
            if (checked) checkedThumbColor else uncheckedThumbColor
        } else {
            if (checked) disabledCheckedThumbColor else disabledUncheckedThumbColor
        }

    @Stable
    internal fun trackColor(enabled: Boolean, checked: Boolean): ColorStyle =
        if (enabled) {
            if (checked) checkedTrackColor else uncheckedTrackColor
        } else {
            if (checked) disabledCheckedTrackColor else disabledUncheckedTrackColor
        }

    @Stable
    internal fun borderColor(enabled: Boolean, checked: Boolean): ColorStyle =
        if (enabled) {
            if (checked) checkedBorderColor else uncheckedBorderColor
        } else {
            if (checked) disabledCheckedBorderColor else disabledUncheckedBorderColor
        }

    @Stable
    internal fun iconColor(enabled: Boolean, checked: Boolean): Color =
        if (enabled) {
            if (checked) checkedIconColor else uncheckedIconColor
        } else {
            if (checked) disabledCheckedIconColor else disabledUncheckedIconColor
        }
}

@Composable
@Suppress("LongParameterList", "ModifierWithoutDefault")
private fun SwitchImpl(
    modifier: Modifier,
    checked: Boolean,
    enabled: Boolean,
    colors: SwitchColors,
    thumbContent: (@Composable () -> Unit)?,
    interactionSource: InteractionSource,
    thumbShape: Shape,
) {
    val trackColor = colors.trackColor(enabled, checked)
    val resolvedThumbColor = colors.thumbColor(enabled, checked)
    val trackShape = SwitchTokens.TrackShape.value

    Box(
        modifier
            .border(TrackOutlineWidth, colors.borderColor(enabled, checked), trackShape)
            .background(trackColor, trackShape),
    ) {
        Box(
            modifier =
            Modifier
                .align(Alignment.CenterStart)
                .then(ThumbElement(interactionSource, checked))
                .indication(
                    interactionSource = interactionSource,
                    indication =
                    ripple(
                        bounded = false,
                        radius = SwitchTokens.StateLayerSize / 2,
                    ),
                )
                .background(resolvedThumbColor, thumbShape),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbContent != null) {
                val iconColor = colors.iconColor(enabled, checked)
                CompositionLocalProvider(
                    LocalContentColor provides iconColor,
                    content = thumbContent,
                )
            }
        }
    }
}

private data class ThumbElement(
    val interactionSource: InteractionSource,
    val checked: Boolean,
) : ModifierNodeElement<ThumbNode>() {
    override fun create() = ThumbNode(interactionSource, checked)

    override fun update(node: ThumbNode) {
        node.interactionSource = interactionSource
        if (node.checked != checked) {
            node.invalidateMeasurement()
        }
        node.checked = checked
        node.update()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "switchThumb"
        properties["interactionSource"] = interactionSource
        properties["checked"] = checked
    }
}

private class ThumbNode(
    var interactionSource: InteractionSource,
    var checked: Boolean,
) : Modifier.Node(), LayoutModifierNode {

    override val shouldAutoInvalidate: Boolean
        get() = false

    private var isPressed = false
    private var offsetAnim: Animatable<Float, AnimationVector1D>? = null
    private var sizeAnim: Animatable<Float, AnimationVector1D>? = null
    private var initialOffset: Float = Float.NaN
    private var initialSize: Float = Float.NaN

    override fun onAttach() {
        coroutineScope.launch {
            var pressCount = 0
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> pressCount++
                    is PressInteraction.Release -> pressCount--
                    is PressInteraction.Cancel -> pressCount--
                }
                val pressed = pressCount > 0
                if (isPressed != pressed) {
                    isPressed = pressed
                    invalidateMeasurement()
                }
            }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val hasContent =
            measurable.maxIntrinsicHeight(constraints.maxWidth) != 0 &&
                measurable.maxIntrinsicWidth(constraints.maxHeight) != 0
        val size =
            when {
                isPressed -> SwitchTokens.PressedHandleWidth
                hasContent || checked -> ThumbDiameter
                else -> UncheckedThumbDiameter
            }.toPx()

        val actualSize = (sizeAnim?.value ?: size).toInt()
        val placeable = measurable.measure(Constraints.fixed(actualSize, actualSize))
        val thumbPaddingStart = (SwitchHeight - size.toDp()) / 2f
        val minBound = thumbPaddingStart.toPx()
        val thumbPathLength = (SwitchWidth - ThumbDiameter) - ThumbPadding
        val maxBound = thumbPathLength.toPx()
        val offset =
            when {
                isPressed && checked -> maxBound - TrackOutlineWidth.toPx()
                isPressed && !checked -> TrackOutlineWidth.toPx()
                checked -> maxBound
                else -> minBound
            }

        if (sizeAnim?.targetValue != size) {
            coroutineScope.launch {
                sizeAnim?.animateTo(size, if (isPressed) SnapSpec else AnimationSpec)
            }
        }

        if (offsetAnim?.targetValue != offset) {
            coroutineScope.launch {
                offsetAnim?.animateTo(offset, if (isPressed) SnapSpec else AnimationSpec)
            }
        }

        if (initialSize.isNaN() && initialOffset.isNaN()) {
            initialSize = size
            initialOffset = offset
        }

        return layout(actualSize, actualSize) {
            placeable.placeRelative(offsetAnim?.value?.toInt() ?: offset.toInt(), 0)
        }
    }

    fun update() {
        if (sizeAnim == null && !initialSize.isNaN()) {
            sizeAnim = Animatable(initialSize)
        }

        if (offsetAnim == null && !initialOffset.isNaN()) offsetAnim = Animatable(initialOffset)
    }
}

private val ThumbDiameter = SwitchTokens.SelectedHandleWidth
private val UncheckedThumbDiameter = SwitchTokens.UnselectedHandleWidth

private val SwitchWidth = SwitchTokens.TrackWidth
private val SwitchHeight = SwitchTokens.TrackHeight
private val ThumbPadding = (SwitchHeight - ThumbDiameter) / 2
private val SnapSpec = SnapSpec<Float>()
private val AnimationSpec = TweenSpec<Float>(durationMillis = 100)

@Suppress("ConstPropertyName")
private object SwitchTokens {
    val DisabledSelectedHandleColor = ColorSchemeKeyTokens.Surface
    const val DisabledSelectedHandleOpacity = 1.0f
    val DisabledSelectedIconColor = ColorSchemeKeyTokens.OnSurface
    const val DisabledSelectedIconOpacity = 0.38f
    val DisabledSelectedTrackColor = ColorSchemeKeyTokens.OnSurface
    const val DisabledTrackOpacity = 0.12f
    val DisabledUnselectedHandleColor = ColorSchemeKeyTokens.OnSurface
    const val DisabledUnselectedHandleOpacity = 0.38f
    val DisabledUnselectedIconColor = ColorSchemeKeyTokens.SurfaceContainerHighest
    const val DisabledUnselectedIconOpacity = 0.38f
    val DisabledUnselectedTrackColor = ColorSchemeKeyTokens.SurfaceContainerHighest
    val DisabledUnselectedTrackOutlineColor = ColorSchemeKeyTokens.OnSurface
    val HandleShape = ShapeKeyTokens.CornerFull
    val PressedHandleWidth = 28.0.dp
    val SelectedHandleColor = ColorSchemeKeyTokens.OnPrimary
    val SelectedHandleWidth = 24.0.dp
    val SelectedIconColor = ColorSchemeKeyTokens.OnPrimaryContainer
    val SelectedTrackColor = ColorSchemeKeyTokens.Primary
    val StateLayerSize = 40.0.dp
    val TrackHeight = 32.0.dp
    val TrackOutlineWidth = 2.0.dp
    val TrackShape = ShapeKeyTokens.CornerFull
    val TrackWidth = 52.0.dp
    val UnselectedFocusTrackOutlineColor = ColorSchemeKeyTokens.Outline
    val UnselectedHandleColor = ColorSchemeKeyTokens.Outline
    val UnselectedHandleWidth = 16.0.dp
    val UnselectedIconColor = ColorSchemeKeyTokens.SurfaceContainerHighest
    val UnselectedTrackColor = ColorSchemeKeyTokens.SurfaceContainerHighest
}

private enum class ColorSchemeKeyTokens {
    OnPrimary,
    OnPrimaryContainer,
    OnSurface,
    Outline,
    Primary,
    Surface,
    SurfaceContainerHighest,
}

private enum class ShapeKeyTokens {
    CornerFull,
}

private val ShapeKeyTokens.value: Shape
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.shapes.fromToken(this)

private val ColorSchemeKeyTokens.value: Color
    @ReadOnlyComposable @Composable
    get() = MaterialTheme.colorScheme.fromToken(this)

@Suppress("UnusedReceiverParameter")
private fun Shapes.fromToken(value: ShapeKeyTokens): Shape {
    return when (value) {
        ShapeKeyTokens.CornerFull -> CircleShape
    }
}

@Stable
private fun ColorScheme.fromToken(value: ColorSchemeKeyTokens): Color {
    return when (value) {
        ColorSchemeKeyTokens.OnPrimary -> onPrimary
        ColorSchemeKeyTokens.OnPrimaryContainer -> onPrimaryContainer
        ColorSchemeKeyTokens.OnSurface -> onSurface
        ColorSchemeKeyTokens.Outline -> outline
        ColorSchemeKeyTokens.Primary -> primary
        ColorSchemeKeyTokens.Surface -> surface
        ColorSchemeKeyTokens.SurfaceContainerHighest -> surfaceContainerHighest
    }
}
