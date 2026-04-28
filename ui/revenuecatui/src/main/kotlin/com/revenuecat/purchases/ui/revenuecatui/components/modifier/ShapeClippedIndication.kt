@file:JvmSynthetic
@file:Suppress("DEPRECATION_ERROR")

package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

/**
 * Wraps [LocalIndication.current] and clips its drawing to [shape]. Use this in
 * `Modifier.clickable(indication = ...)` when the surrounding layout would otherwise need a
 * graphics-layer `Modifier.clip(shape)` to bound the ripple — that approach also clips
 * children that intentionally overflow the parent (badges with offsets, shadows). The clip
 * here applies only to the indication's own draw, not to children rendered earlier via
 * `drawContent`.
 */
@Composable
@JvmSynthetic
internal fun rememberShapeClippedIndication(shape: Shape): Indication {
    val base = LocalIndication.current
    return remember(base, shape) { ShapeClippedIndication(base, shape) }
}

private class ShapeClippedIndication(
    private val base: Indication,
    private val shape: Shape,
) : Indication {
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        val baseInstance = base.rememberUpdatedInstance(interactionSource)
        return remember(baseInstance, shape) { ShapeClippedIndicationInstance(baseInstance, shape) }
    }
}

private class ShapeClippedIndicationInstance(
    private val baseInstance: IndicationInstance,
    private val shape: Shape,
) : IndicationInstance {
    override fun ContentDrawScope.drawIndication() {
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = Path().apply { addOutline(outline) }
        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.clipPath(path)
            with(baseInstance) { drawIndication() }
            canvas.restore()
        }
    }
}
