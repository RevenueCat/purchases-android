@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode

/**
 * Wraps [LocalIndication.current] and clips its drawing to [shape]. Use this in
 * `Modifier.clickable(indication = ...)` when the surrounding layout would otherwise need a
 * graphics-layer `Modifier.clip(shape)` to bound the ripple — that approach also clips
 * children that intentionally overflow the parent (badges with offsets, shadows). The clip
 * here applies only to the base indication's own draw, not to children rendered earlier via
 * `drawContent`.
 *
 * Implemented as an [IndicationNodeFactory] so it routes through Compose's stable
 * `Modifier.Node` indication path. Earlier prototypes used the legacy `Indication` /
 * `IndicationInstance` interface; that path goes through `composed { … drawWithContent { … } }`,
 * which interferes with `BlurMaskFilter`-based shadows on descendants (children's overflowing
 * shadows fail to render).
 */
@Composable
@JvmSynthetic
internal fun rememberShapeClippedIndication(shape: Shape): Indication {
    val base = LocalIndication.current
    return remember(base, shape) {
        if (base is IndicationNodeFactory) {
            ShapeClippedIndication(base, shape)
        } else {
            // Should never happen with Material3's default ripple, but fall back to the base
            // unmodified rather than fail. Caller-side ripple won't be shape-clipped in this
            // case; that's acceptable given it's a non-default theming setup.
            base
        }
    }
}

private class ShapeClippedIndication(
    private val base: IndicationNodeFactory,
    private val shape: Shape,
) : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode =
        ShapeClippedIndicationNode(base, shape, interactionSource)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ShapeClippedIndication) return false
        return base == other.base && shape == other.shape
    }

    override fun hashCode(): Int {
        var result = base.hashCode()
        result = 31 * result + shape.hashCode()
        return result
    }
}

private class ShapeClippedIndicationNode(
    private val base: IndicationNodeFactory,
    private val shape: Shape,
    private val interactionSource: InteractionSource,
) : DelegatingNode(), DrawModifierNode {

    private var baseDelegate: DelegatableNode? = null

    override fun onAttach() {
        super.onAttach()
        baseDelegate = delegate(base.create(interactionSource))
    }

    override fun ContentDrawScope.draw() {
        // 1. Draw the layout's own content (children + earlier modifiers in the chain) with
        //    NO shape clip — this is what lets overflowing children (badges, shadows) render.
        drawContent()
        // 2. Then invoke the delegated indication's draw inside a shape-clipped canvas
        //    transaction. Because we override `draw()` on this DelegatingNode, the delegate's
        //    own draw() is NOT auto-called by the chain — we must call it here.
        val node = baseDelegate as? DrawModifierNode ?: return
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = Path().apply { addOutline(outline) }
        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.clipPath(path)
            with(node) { draw() }
            canvas.restore()
        }
    }
}
