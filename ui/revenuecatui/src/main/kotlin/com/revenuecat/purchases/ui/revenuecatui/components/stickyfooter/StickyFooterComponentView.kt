@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stickyfooter

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.stack.rememberUpdatedStackComponentState
import com.revenuecat.purchases.ui.revenuecatui.components.style.StickyFooterComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker

@Suppress("LongParameterList")
@Composable
internal fun StickyFooterComponentView(
    style: StickyFooterComponentStyle,
    state: PaywallState.Loaded.Components,
    clickHandler: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
    componentInteractionTracker: PaywallComponentInteractionTracker = PaywallComponentInteractionTracker { _ -> },
    ancestorScrollOrientations: Set<Orientation> = emptySet(),
) {
    val stack = style.stackComponentStyle
    val stackState = rememberUpdatedStackComponentState(stack, state)
    // A stack with its own vertical scroll already measures its children at their natural height.
    val measureWithUnboundedHeight = stackState.size.height is Fit &&
        stackState.scrollOrientation != Orientation.Vertical
    // To find the content's natural height, we measure it without a maximum height.
    // Tell nested stacks about that measurement constraint before we know whether the content fits.
    val unboundedContentAxes = if (measureWithUnboundedHeight) {
        ancestorScrollOrientations + Orientation.Vertical
    } else {
        ancestorScrollOrientations
    }
    BoxWithConstraints(modifier = modifier, propagateMinConstraints = true) {
        val viewport = if (measureWithUnboundedHeight) {
            val viewportHeight = constraints.maxHeight
            var overflows by remember { mutableStateOf(false) }
            val scrollState = rememberScrollState()
            val scroll = if (overflows && !stackState.scrollExplicitlyDisabled && constraints.hasBoundedHeight) {
                Modifier.verticalScroll(scrollState)
            } else {
                Modifier
            }
            scroll.measureFooterContent(viewportHeight) { overflows = it }
        } else {
            Modifier
        }
        Box(modifier = viewport, propagateMinConstraints = true) {
            StackComponentView(
                style = stack,
                state = state,
                clickHandler = clickHandler,
                componentInteractionTracker = componentInteractionTracker,
                ancestorScrollOrientations = unboundedContentAxes,
            )
        }
    }
}

/**
 * A Fit footer has no height for its descendants to fill. Measure their content first. A fixed
 * row inside the footer can still give its own children a definite height.
 *
 * Clip and scroll only when the content exceeds the viewport, so compact footers keep their
 * shadows and badges outside their bounds and expose no scroll action.
 */
private fun Modifier.measureFooterContent(viewportHeight: Int, onOverflowChanged: (Boolean) -> Unit): Modifier =
    layout { measurable, constraints ->
        val content = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        onOverflowChanged(content.height > viewportHeight)
        layout(content.width, constraints.constrainHeight(content.height)) { content.placeRelative(0, 0) }
    }
