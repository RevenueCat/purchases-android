package com.revenuecat.purchases.ui.revenuecatui.layoutvalidation

import android.os.Build
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale
import kotlin.math.roundToInt

@Serializable
internal data class PaywallTreeExport(
    val metadata: PaywallTreeMetadata,
    val root: LayoutNode,
)

@Serializable
internal data class PaywallTreeMetadata(
    val extractorVersion: String,
    val locale: String,
    val offeringId: String?,
    val platform: String,
    val platformVersion: String,
    val timestamp: String,
    val viewport: Viewport,
)

@Serializable
internal data class Viewport(
    val height: Int,
    val scale: Double,
    val width: Int,
)

@Serializable
internal data class LayoutNode(
    val children: List<LayoutNode>,
    val componentId: String? = null,
    val frame: Frame,
    val label: String? = null,
    val nativeType: String,
    val state: LayoutNodeState,
    val type: String,
)

@Serializable
internal data class Frame(
    val height: Double,
    val width: Double,
    val x: Double,
    val y: Double,
)

@Serializable
internal data class LayoutNodeState(
    val enabled: Boolean,
    val selected: Boolean,
)

internal object SemanticsLayoutExporter {

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        prettyPrint = true
        explicitNulls = false
    }

    fun export(
        root: SemanticsNode,
        metadata: PaywallTreeMetadata,
    ): PaywallTreeExport {
        return PaywallTreeExport(
            metadata = metadata,
            root = root.toLayoutNode(),
        )
    }

    fun metadata(
        viewportWidth: Int,
        viewportHeight: Int,
        scale: Double,
        offeringId: String?,
        timestamp: String,
        locale: Locale = Locale.getDefault(),
    ): PaywallTreeMetadata {
        return PaywallTreeMetadata(
            extractorVersion = "1.0.0",
            locale = locale.toString(),
            offeringId = offeringId,
            platform = "android",
            platformVersion = Build.VERSION.RELEASE,
            timestamp = timestamp,
            viewport = Viewport(
                height = viewportHeight,
                scale = scale,
                width = viewportWidth,
            ),
        )
    }

    fun encodeToJson(export: PaywallTreeExport): String {
        return json.encodeToString(export)
    }

    private fun SemanticsNode.toLayoutNode(): LayoutNode {
        val config = config
        val label = config.getOrNull(SemanticsProperties.Text)
            ?.joinToString(separator = "\n") { it.text }
            ?: config.getOrNull(SemanticsProperties.EditableText)?.text
            ?: config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(separator = "\n")

        val frame = boundsInRoot.toFrame()
        val role = config.getOrNull(SemanticsProperties.Role)
        val children = children
            .map { it.toLayoutNode() }
            .sortedWith(
                compareBy(
                    { it.frame.y },
                    { it.frame.x },
                    { it.componentId.orEmpty() },
                    { it.type.orEmpty() },
                ),
            )

        return LayoutNode(
            children = children,
            componentId = config.getOrNull(SemanticsProperties.TestTag),
            frame = frame,
            label = label,
            nativeType = inferNativeType(role = role, label = label, hasChildren = children.isNotEmpty()),
            state = LayoutNodeState(
                enabled = !config.contains(SemanticsProperties.Disabled),
                selected = config.getOrNull(SemanticsProperties.Selected) ?: false,
            ),
            type = inferType(role = role, label = label, hasChildren = children.isNotEmpty()),
        )
    }

    private fun Rect.toFrame(): Frame {
        return Frame(
            height = height.roundToInt().toDouble(),
            width = width.roundToInt().toDouble(),
            x = left.roundToInt().toDouble(),
            y = top.roundToInt().toDouble(),
        )
    }

    private fun inferType(role: Role?, label: String?, hasChildren: Boolean): String {
        return when {
            role == Role.Button -> "button"
            role.toString() == "Image" -> "image"
            label != null -> "text"
            hasChildren -> "container"
            else -> "container"
        }
    }

    private fun inferNativeType(role: Role?, label: String?, hasChildren: Boolean): String {
        return when {
            role == Role.Button -> "Button"
            role.toString() == "Image" -> "Image"
            label != null -> "StaticText"
            hasChildren -> "Other"
            else -> "Other"
        }
    }
}
