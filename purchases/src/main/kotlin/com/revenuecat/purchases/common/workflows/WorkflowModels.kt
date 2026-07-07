@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.ExitOffers
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.utils.serializers.EnumDeserializerWithDefault
import com.revenuecat.purchases.utils.serializers.JsonObjectToMapSerializer
import com.revenuecat.purchases.utils.serializers.SealedDeserializerWithDefault
import com.revenuecat.purchases.utils.serializers.URLSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.net.URL

@InternalRevenueCatAPI
@Serializable(with = WorkflowTriggerTypeDeserializer::class)
public enum class WorkflowTriggerType {
    @SerialName("on_press")
    ON_PRESS,
    UNKNOWN,
}

internal object WorkflowTriggerTypeDeserializer : EnumDeserializerWithDefault<WorkflowTriggerType>(
    defaultValue = WorkflowTriggerType.UNKNOWN,
)

@InternalRevenueCatAPI
@Serializable(with = WorkflowTriggerActionSerializer::class)
public sealed class WorkflowTriggerAction {
    @InternalRevenueCatAPI
    @Serializable
    public data class Step(@SerialName("step_id") val stepId: String) : WorkflowTriggerAction()

    @InternalRevenueCatAPI
    @Serializable
    public object Unknown : WorkflowTriggerAction()
}

internal object WorkflowTriggerActionSerializer : SealedDeserializerWithDefault<WorkflowTriggerAction>(
    serialName = "WorkflowTriggerAction",
    serializerByType = mapOf("step" to { WorkflowTriggerAction.Step.serializer() }),
    defaultValue = { WorkflowTriggerAction.Unknown },
)

@InternalRevenueCatAPI
@Serializable
public data class WorkflowTrigger(
    val name: String,
    val type: WorkflowTriggerType,
    @SerialName("action_id") val actionId: String,
    @SerialName("component_id") val componentId: String,
)

/**
 * Step `screen_type` classification values and the metadata key they are returned under.
 *
 * The backend tags each step with its screen classification inside `metadata` (`metadata.screen_type`),
 * e.g. `["paywall"]`. The SDK uses this to decide which workflow steps report paywall events.
 */
@InternalRevenueCatAPI
public object WorkflowScreenType {

    /** The key the classification list is stored under inside a step's `metadata`. */
    public const val METADATA_KEY: String = "screen_type"

    /** A step whose screen is a paywall; it should report paywall impression/close events. */
    public const val PAYWALL: String = "paywall"
}

@InternalRevenueCatAPI
@Serializable
public data class WorkflowStep(
    val id: String,
    val type: String,
    @SerialName("screen_id") val screenId: String? = null,
    @SerialName("param_values") val paramValues: Map<String, JsonElement> = emptyMap(),
    val triggers: List<WorkflowTrigger> = emptyList(),
    val outputs: Map<String, JsonElement> = emptyMap(),
    @SerialName("trigger_actions") val triggerActions: Map<String, WorkflowTriggerAction> = emptyMap(),
    val metadata: JsonElement? = null,
) {
    /**
     * The step's screen classification, as reported by the backend under `metadata.screen_type`.
     *
     * Returns `null` when the step was not tagged (older workflows, or workflows served before the
     * screen-analytics rollout) and an empty list when it was tagged with no known type. The
     * null-vs-empty distinction matters for paywall-event gating: an untagged step falls back to the
     * prior structural inference (`id == singleStepFallbackId`), while a step explicitly tagged without
     * `paywall` suppresses paywall events. See `tracksPaywallEvents` in the UI layer.
     */
    @InternalRevenueCatAPI
    public val stepScreenType: List<String>?
        get() {
            // A present-but-non-array `screen_type` (null, scalar, object) is treated the same as an
            // absent key: untagged (returns null), so events keep reporting. This matches the iOS
            // contract and is the conservative default; the backend only ever ships `screen_type` as a
            // JSON array, so suppressing on a malformed shape would risk muting real events.
            val metadataObject = metadata as? JsonObject ?: return null
            val screenType = metadataObject[WorkflowScreenType.METADATA_KEY] as? JsonArray ?: return null
            return screenType.mapNotNull { element ->
                (element as? JsonPrimitive)?.takeIf { it.isString }?.content
            }
        }
}

@InternalRevenueCatAPI
@Serializable
public data class WorkflowScreen(
    val name: String? = null,
    @SerialName("template_name") val templateName: String,
    val revision: Int = 0,
    @Serializable(with = URLSerializer::class)
    @SerialName("asset_base_url") val assetBaseURL: URL,
    @SerialName("components_config") val componentsConfig: ComponentsConfig,
    @SerialName("components_localizations")
    val componentsLocalizations: Map<LocaleId, Map<LocalizationKey, LocalizationData>>,
    @SerialName("default_locale") val defaultLocaleIdentifier: LocaleId,
    @SerialName("config") val config: JsonObject = JsonObject(emptyMap()),
    @SerialName("offering_identifier") val offeringIdentifier: String? = null,
    @SerialName("exit_offers") val exitOffers: ExitOffers? = null,
)

/**
 * Published workflow document returned inline or via CDN (same JSON shape).
 */
@InternalRevenueCatAPI
@Serializable
public data class PublishedWorkflow(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("initial_step_id") val initialStepId: String,
    val steps: Map<String, WorkflowStep>,
    val screens: Map<String, WorkflowScreen>,
    @SerialName("ui_config") val uiConfig: UiConfig,
    @Serializable(with = JsonObjectToMapSerializer::class)
    val metadata: Map<String, @Contextual Any> = emptyMap(),
    val hash: String? = null,
    @SerialName("single_step_fallback_id") val singleStepFallbackId: String? = null,
) {
    @InternalRevenueCatAPI
    public val dismissExitOffer: WorkflowExitOffer?
        get() {
            val stepId = singleStepFallbackId ?: return null
            val step = steps[stepId] ?: return null
            val screenId = step.screenId ?: return null
            val offeringId = screens[screenId]?.exitOffers?.dismiss?.offeringId ?: return null
            return WorkflowExitOffer(offeringId = offeringId, stepId = step.id)
        }
}

@InternalRevenueCatAPI
public data class WorkflowExitOffer(
    val offeringId: String,
    val stepId: String,
)

@InternalRevenueCatAPI
public data class WorkflowDataResult(
    val workflow: PublishedWorkflow,
)

@Serializable
internal enum class WorkflowResponseAction {
    @SerialName("inline")
    INLINE,

    @SerialName("use_cdn")
    USE_CDN,
}

@Serializable
internal data class WorkflowDetailResponse(
    val action: WorkflowResponseAction,
    val data: PublishedWorkflow? = null,
    val url: String? = null,
    val hash: String? = null,
)

@Serializable
internal data class WorkflowSummary(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("offering_id") val offeringId: String? = null,
    val prefetch: Boolean = false,
)

@Serializable
internal data class WorkflowsListResponse(
    val workflows: List<WorkflowSummary>,
)
