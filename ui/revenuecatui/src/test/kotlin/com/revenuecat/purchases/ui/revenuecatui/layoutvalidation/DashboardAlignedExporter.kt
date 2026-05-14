package com.revenuecat.purchases.ui.revenuecatui.layoutvalidation

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * Dashboard-aligned export — one entry per componentId declared in the paywall components
 * config, regardless of whether the rendered tree exposes it as its own semantics node.
 *
 * Conforms to `SCHEMA.md` v3:
 * - Every dashboard `id` is a key in `components`, on every platform.
 * - `rendered: true` carries the captured frame, `nativeType`, and `label` (text-bearing only).
 * - `rendered: false` carries the zero-frame sentinel `{0,0,0,0}` and omits `nativeType` /
 *   `label` — the renderer didn't run, so there's nothing to report. `type` is still emitted
 *   (it comes from the dashboard, not the renderer).
 * - No synthetic ids, no `_N` sub-element groupings, no platform-injected wrappers.
 *
 * Cross-platform comparison becomes a per-componentId walk: presence diff via `rendered`,
 * frame diff with tolerances, structural diff via `type` / `nativeType`.
 */
@Serializable
internal data class DashboardAlignedExport(
    val metadata: DashboardAlignedMetadata,
    val components: Map<String, DashboardAlignedComponent>,
)

@Serializable
internal data class DashboardAlignedMetadata(
    val extractorVersion: String,
    val locale: String,
    val offeringId: String?,
    val platform: String,
    val platformVersion: String,
    val timestamp: String,
    val viewport: Viewport,
    val rootFrame: Frame,
)

@Serializable
internal data class DashboardAlignedComponent(
    val componentId: String,
    /**
     * Cross-platform schema `type` enum: `text`, `image`, `icon`, `button`, `toggle`,
     * `input`, `scroll`, `container`. Always emitted; comes from the dashboard config,
     * not the renderer.
     */
    val type: String,
    /**
     * `true` when a SemanticsNode with a matching `testTag` was found in the rendered tree.
     * `false` means the component is in the dashboard but didn't surface as its own
     * SemanticsNode (merged with a parent, hidden, conditionally omitted, etc.).
     */
    val rendered: Boolean,
    /**
     * Cross-platform `nativeType` enum: `StaticText`, `Image`, `Icon`, `Button`, `Toggle`,
     * `Input`, `ScrollView`, `Container`, `Other`. Only emitted when `rendered: true`.
     */
    val nativeType: String? = null,
    /** Only emitted on text-bearing rendered components. */
    val label: String? = null,
    /** When `rendered: false`, exactly `{0,0,0,0}`. */
    val frame: Frame,
    val state: LayoutNodeState,
)

internal object DashboardAlignedExporter {

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        prettyPrint = true
        explicitNulls = false
    }

    /**
     * Bumped to `3.0.0` to match the cross-platform spec (see `SCHEMA.md`):
     * `dashboardType` → `type`, `dashboardName` dropped, `type` collapsed to a closed enum.
     */
    private const val EXTRACTOR_VERSION = "3.0.0"

    fun export(
        root: SemanticsNode,
        componentsConfig: JSONObject,
        metadata: DashboardAlignedMetadata,
    ): DashboardAlignedExport {
        val dashboardComponents = collectDashboardComponents(componentsConfig)
        val byTestTag = root.indexByTestTag()

        val components = dashboardComponents.associate { spec ->
            val rendered = byTestTag[spec.id]
            spec.id to buildComponent(spec, rendered)
        }.toSortedMap()

        return DashboardAlignedExport(metadata = metadata, components = components)
    }

    fun encodeToJson(export: DashboardAlignedExport): String =
        json.encodeToString(export)

    fun metadata(
        offeringId: String?,
        viewport: Viewport,
        rootFrame: Frame,
        platform: String,
        platformVersion: String,
        locale: String,
        timestamp: String,
    ): DashboardAlignedMetadata = DashboardAlignedMetadata(
        extractorVersion = EXTRACTOR_VERSION,
        locale = locale,
        offeringId = offeringId,
        platform = platform,
        platformVersion = platformVersion,
        timestamp = timestamp,
        viewport = viewport,
        rootFrame = rootFrame,
    )

    // ---- Dashboard walk ----

    private data class DashboardSpec(
        val id: String,
        /** Raw dashboard type — collapsed to the schema enum via [collapseDashboardType]. */
        val rawType: String,
    )

    /**
     * Walks [config] in document order and returns every `(id, rawType)` pair found. Each
     * dashboard component appears once; the cross-platform comparator keys by `id`.
     */
    private fun collectDashboardComponents(config: JSONObject): List<DashboardSpec> {
        val out = mutableListOf<DashboardSpec>()
        fun walk(value: Any?) {
            when (value) {
                is JSONObject -> {
                    val id = value.optString("id").takeIf { it.isNotBlank() }
                    val type = value.optString("type").takeIf { it.isNotBlank() }
                    if (id != null && type != null) {
                        out.add(DashboardSpec(id = id, rawType = type))
                    }
                    value.keys().forEach { walk(value.opt(it)) }
                }
                is JSONArray -> {
                    for (i in 0 until value.length()) walk(value.opt(i))
                }
            }
        }
        walk(config)
        return out
    }

    /**
     * Collapses raw dashboard `type` strings to the schema's closed enum (SCHEMA.md §7.1).
     * Unknown types fall through to `container` — the safest default for unmapped wrappers.
     */
    private fun collapseDashboardType(raw: String): String = when (raw) {
        "text" -> "text"
        "image" -> "image"
        "icon" -> "icon"
        "button",
        "purchase_button",
        "wallet_button",
        "redemption_button",
        "express_purchase_button",
        -> "button"
        "toggle" -> "toggle"
        "input_text",
        "input_single_choice",
        "input_multiple_choice",
        -> "input"
        "stack",
        "package",
        "tabs",
        "tab",
        "tab_control",
        "tab_control_button",
        "tab_control_toggle",
        "carousel",
        "timeline",
        "timeline_item",
        "badge",
        "footer",
        "header",
        "paywall",
        "sticky_footer",
        -> "container"
        else -> "container"
    }

    // ---- Compose tree walk ----

    private fun SemanticsNode.indexByTestTag(): Map<String, SemanticsNode> {
        val out = mutableMapOf<String, SemanticsNode>()
        fun walk(node: SemanticsNode) {
            val tag = node.config.getOrNull(SemanticsProperties.TestTag)
            if (tag != null && tag !in out) {
                out[tag] = node
            }
            node.children.forEach { walk(it) }
        }
        walk(this)
        return out
    }

    private fun buildComponent(
        spec: DashboardSpec,
        node: SemanticsNode?,
    ): DashboardAlignedComponent {
        val type = collapseDashboardType(spec.rawType)

        if (node == null) {
            return DashboardAlignedComponent(
                componentId = spec.id,
                type = type,
                rendered = false,
                frame = Frame(0.0, 0.0, 0.0, 0.0),
                state = LayoutNodeState(enabled = true, selected = false),
            )
        }

        val config = node.config
        val label = config.getOrNull(SemanticsProperties.Text)
            ?.joinToString(separator = "\n") { it.text }
            ?: config.getOrNull(SemanticsProperties.EditableText)?.text
            ?: config.getOrNull(SemanticsProperties.ContentDescription)
                ?.joinToString(separator = "\n")

        val role = config.getOrNull(SemanticsProperties.Role)
        val isScrollable = config.contains(SemanticsActions.ScrollBy) ||
            config.contains(SemanticsProperties.VerticalScrollAxisRange) ||
            config.contains(SemanticsProperties.HorizontalScrollAxisRange)

        val rect = node.boundsInRoot
        val frame = Frame(
            height = rect.height.roundToInt().toDouble(),
            width = rect.width.roundToInt().toDouble(),
            x = rect.left.roundToInt().toDouble(),
            y = rect.top.roundToInt().toDouble(),
        )

        return DashboardAlignedComponent(
            componentId = spec.id,
            type = type,
            rendered = true,
            nativeType = inferNativeType(
                type = type,
                role = role,
                label = label,
                hasChildren = node.children.isNotEmpty(),
                isScrollable = isScrollable,
            ),
            label = label,
            frame = frame,
            state = LayoutNodeState(
                enabled = !config.contains(SemanticsProperties.Disabled),
                selected = config.getOrNull(SemanticsProperties.Selected) ?: false,
            ),
        )
    }

    /**
     * Maps the rendered Compose semantics + the schema `type` to the closed `nativeType`
     * enum (SCHEMA.md §7.2). The schema `type` is the primary signal — Compose's role
     * isn't always present on every node — but role/scroll/label still help disambiguate
     * the `container`-typed wrappers that render as buttons or scroll views.
     */
    private fun inferNativeType(
        type: String,
        role: Role?,
        label: String?,
        hasChildren: Boolean,
        isScrollable: Boolean,
    ): String = when {
        // Strong signals from the rendered tree.
        role == Role.Button -> "Button"
        role.toString() == "Image" && type == "icon" -> "Icon"
        role.toString() == "Image" -> "Image"
        isScrollable -> "ScrollView"
        // Fall back to the dashboard type.
        type == "text" -> "StaticText"
        type == "image" -> "Image"
        type == "icon" -> "Icon"
        type == "button" -> "Button"
        type == "toggle" -> "Toggle"
        type == "input" -> "Input"
        type == "scroll" -> "ScrollView"
        type == "container" -> "Container"
        // Last-resort heuristics from the rendered tree.
        label != null -> "StaticText"
        hasChildren -> "Container"
        else -> "Other"
    }
}
