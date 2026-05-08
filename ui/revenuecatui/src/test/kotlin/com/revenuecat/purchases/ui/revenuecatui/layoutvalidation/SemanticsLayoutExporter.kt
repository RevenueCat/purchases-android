package com.revenuecat.purchases.ui.revenuecatui.layoutvalidation

import android.os.Build
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
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
internal data class FlattenedPaywallTreeExport(
    val components: Map<String, FlattenedLayoutNode>,
    val metadata: FlattenedPaywallTreeMetadata,
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
internal data class FlattenedPaywallTreeMetadata(
    val extractorVersion: String,
    val locale: String,
    val offeringId: String?,
    val platform: String,
    val platformVersion: String,
    val rootFrame: Frame,
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
internal data class FlattenedLayoutNode(
    val componentId: String,
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

    fun flattenAndNormalize(
        export: PaywallTreeExport,
        componentNames: Map<String, String> = emptyMap(),
        paywallSyntheticIds: Set<String> = emptySet(),
    ): FlattenedPaywallTreeExport {
        // Re-write componentIds for top-level structural buckets to the synthetic `paywall`
        // prefix (matches iOS's `paywall_0..N`). The original id is kept for label lookup,
        // but the dedup key uses the synthetic value.
        data class Mapped(val original: String, val exported: String, val node: LayoutNode)

        val mapped = export.root.flattenForComparison().map { fn ->
            val exported = if (fn.componentId in paywallSyntheticIds) {
                SYNTHETIC_PAYWALL_ID
            } else {
                fn.componentId
            }
            Mapped(original = fn.componentId, exported = exported, node = fn.node)
        }.sortedWith(
            compareBy(
                { it.node.frame.y },
                { it.node.frame.x },
                { it.exported },
                { it.node.type },
                { it.node.label.orEmpty() },
            ),
        )

        val componentIdCounts = mutableMapOf<String, Int>()
        val duplicateComponentIds = mapped
            .groupingBy { it.exported }
            .eachCount()
            .filterValues { it > 1 }
            .keys

        val components = mapped.associate { entry ->
            val exportedId = entry.exported
            val node = entry.node
            val index = componentIdCounts.getOrDefault(exportedId, 0)
            componentIdCounts[exportedId] = index + 1
            val key = if (exportedId in duplicateComponentIds) {
                "${exportedId}_$index"
            } else {
                exportedId
            }

            // Fall back to the component's `name` from the components config when the rendered tree
            // didn't surface a label (mirrors the iOS extractor, which labels containers like
            // "Feature list", "Package list", "Button stack").
            val effectiveLabel = node.label ?: componentNames[entry.original]

            key to FlattenedLayoutNode(
                componentId = exportedId,
                frame = node.frame.normalized(),
                label = effectiveLabel,
                nativeType = node.nativeType,
                state = node.state,
                type = node.type,
            )
        }.toSortedMap()

        return FlattenedPaywallTreeExport(
            components = components,
            metadata = export.metadata.toFlattenedMetadata(rootFrame = export.root.frame.normalized()),
        )
    }

    fun encodeToJson(export: FlattenedPaywallTreeExport): String {
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
        // Compose marks scrollable containers with the ScrollBy action and either a vertical or
        // horizontal ScrollAxisRange.
        val isScrollable = config.contains(SemanticsActions.ScrollBy) ||
            config.contains(SemanticsProperties.VerticalScrollAxisRange) ||
            config.contains(SemanticsProperties.HorizontalScrollAxisRange)
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
            nativeType = inferNativeType(
                role = role,
                label = label,
                hasChildren = children.isNotEmpty(),
                isScrollable = isScrollable,
            ),
            state = LayoutNodeState(
                enabled = !config.contains(SemanticsProperties.Disabled),
                selected = config.getOrNull(SemanticsProperties.Selected) ?: false,
            ),
            type = inferType(
                role = role,
                label = label,
                hasChildren = children.isNotEmpty(),
                isScrollable = isScrollable,
            ),
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

    private fun inferType(
        role: Role?,
        label: String?,
        hasChildren: Boolean,
        isScrollable: Boolean,
    ): String {
        return when {
            role == Role.Button -> "button"
            role.toString() == "Image" -> "image"
            isScrollable -> "scroll"
            label != null -> "text"
            hasChildren -> "container"
            else -> "container"
        }
    }

    private fun inferNativeType(
        role: Role?,
        label: String?,
        hasChildren: Boolean,
        isScrollable: Boolean,
    ): String {
        return when {
            role == Role.Button -> "Button"
            role.toString() == "Image" -> "Image"
            isScrollable -> "ScrollView"
            label != null -> "StaticText"
            hasChildren -> "Other"
            else -> "Other"
        }
    }

    private data class FlattenedNode(
        val componentId: String,
        val node: LayoutNode,
    )

    private fun LayoutNode.flattenForComparison(inheritedComponentId: String? = null): List<FlattenedNode> {
        val isInsideBundle = inheritedComponentId != null
        // Only the OUTERMOST eligible stack becomes an anchor; nested stacks inside an active
        // bundle keep inheriting the bundle's id rather than re-anchoring.
        val isAnchor = !isInsideBundle && isBundleAnchor
        val effectiveComponentId = inheritedComponentId ?: componentId
        val childInheritedId = when {
            isAnchor -> componentId
            else -> inheritedComponentId
        }
        // While inside a bundle, descend until we hit a renderable visual unit (image / text
        // leaf) — that unit emits the entry; we don't keep walking past it.
        val isVisualUnit = isInsideBundle && isLeafRenderable
        val children = if (isVisualUnit) {
            emptyList()
        } else {
            children.flatMap { it.flattenForComparison(childInheritedId) }
        }
        val isLeafComponent = componentId != null &&
            children.none { it.componentId == effectiveComponentId } &&
            !hasDescendantComponent
        val shouldIncludeCurrent = effectiveComponentId != null &&
            !isAnchor && // anchors emit their descendants but not themselves
            if (isInsideBundle) {
                // Only emit visual units inside a bundle so we don't double-count wrappers.
                isVisualUnit
            } else {
                isRenderable || (isLeafComponent && hasMeasuredArea)
            }

        return if (shouldIncludeCurrent) {
            listOf(FlattenedNode(componentId = requireNotNull(effectiveComponentId), node = this)) + children
        } else {
            children
        }
    }

    /**
     * `true` when this node should bundle its renderable descendants under its own componentId.
     */
    private val LayoutNode.isBundleAnchor: Boolean
        get() = componentId != null &&
            // The anchor must be a generic structural stack — not a button (which keeps its
            // own entry on iOS), not a scroll view, not a renderable leaf.
            nativeType != "Button" &&
            nativeType != "ScrollView" &&
            !isLeafRenderable &&
            children.isNotEmpty() &&
            // Every renderable descendant must be a simple text/image leaf for the bundle to
            // make sense; presence of a button or scroll surface means we have a real
            // structural boundary that should keep its own id.
            allRenderableDescendantsAreSimpleLeaves

    private val LayoutNode.isLeafRenderable: Boolean
        get() = label != null || nativeType == "Image"

    private val LayoutNode.allRenderableDescendantsAreSimpleLeaves: Boolean
        get() = children.all { child ->
            when {
                // A simple text or image leaf is fine.
                child.isLeafRenderable && child.children.isEmpty() -> true
                // A pass-through stack (no renderable content of its own) is fine if its
                // descendants are also simple leaves — this handles wrappers
                // that hold a single icon+text pair inside the Feature list.
                !child.isLeafRenderable && child.nativeType != "Button" &&
                    child.nativeType != "ScrollView" && child.allRenderableDescendantsAreSimpleLeaves -> true
                else -> false
            }
        }

    private val LayoutNode.hasMeasuredArea: Boolean
        get() = frame.width > 0.0 && frame.height > 0.0

    private val LayoutNode.hasDescendantComponent: Boolean
        get() = children.any { it.componentId != null || it.hasDescendantComponent }

    private val LayoutNode.isRenderable: Boolean
        get() {
            // A node is renderable when it has a measured frame AND either:
            // - it carries visible content (label or a non-generic native type), OR
            // - it has its own componentId (a structural bucket worth surfacing on its own,
            //   e.g. the sticky-footer rows that get renamed to paywall_N).
            // The hasMeasuredArea check still prunes the empty stack/separator placeholders
            // that have no real layout.
            val hasContent = label != null || nativeType != "Other"
            val hasOwnIdentity = componentId != null
            return (hasContent || hasOwnIdentity) && hasMeasuredArea
        }

    private fun Frame.normalized(): Frame {
        return Frame(
            height = height.normalizeCoordinate(),
            width = width.normalizeCoordinate(),
            x = x.normalizeCoordinate(),
            y = y.normalizeCoordinate(),
        )
    }

    private fun Double.normalizeCoordinate(): Double {
        return (this * COORDINATE_PRECISION).roundToInt() / COORDINATE_PRECISION
    }

    private fun PaywallTreeMetadata.toFlattenedMetadata(rootFrame: Frame): FlattenedPaywallTreeMetadata {
        return FlattenedPaywallTreeMetadata(
            extractorVersion = "2.0.0",
            locale = locale,
            offeringId = offeringId,
            platform = platform,
            platformVersion = platformVersion,
            rootFrame = rootFrame,
            timestamp = timestamp,
            viewport = viewport,
        )
    }

    private const val COORDINATE_PRECISION = 1000.0

    /**
     * Synthetic componentId used for top-level structural buckets (the scroll view, package list
     * containers, button rows, footer button row).
     */
    private const val SYNTHETIC_PAYWALL_ID = "paywall"
}
