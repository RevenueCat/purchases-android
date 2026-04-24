@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.utils.serializers.EnumDeserializerWithDefault
import com.revenuecat.purchases.utils.serializers.JsonObjectToMapSerializer
import com.revenuecat.purchases.utils.serializers.URLSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
    public data class Step(val stepId: String) : WorkflowTriggerAction()

    @InternalRevenueCatAPI
    public object Unknown : WorkflowTriggerAction()
}

internal object WorkflowTriggerActionSerializer : KSerializer<WorkflowTriggerAction> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("WorkflowTriggerAction")

    override fun deserialize(decoder: Decoder): WorkflowTriggerAction {
        val element = (decoder as JsonDecoder).decodeJsonElement().jsonObject
        return when (element["type"]?.jsonPrimitive?.content) {
            "step" -> {
                val stepId = element["step_id"]?.jsonPrimitive?.content
                    ?: return WorkflowTriggerAction.Unknown
                WorkflowTriggerAction.Step(stepId = stepId)
            }
            else -> WorkflowTriggerAction.Unknown
        }
    }

    override fun serialize(encoder: Encoder, value: WorkflowTriggerAction) {
        throw NotImplementedError("Serialization is not implemented because it is not needed.")
    }
}

@InternalRevenueCatAPI
@Serializable
public data class WorkflowTrigger(
    val name: String,
    val type: WorkflowTriggerType,
    @SerialName("action_id") val actionId: String,
    @SerialName("component_id") val componentId: String,
)

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
)

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
)

@InternalRevenueCatAPI
public data class WorkflowDataResult(
    val workflow: PublishedWorkflow,
    val enrolledVariants: Map<String, String>?,
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
    @SerialName("enrolled_variants")
    val enrolledVariants: Map<String, String>? = null,
)
