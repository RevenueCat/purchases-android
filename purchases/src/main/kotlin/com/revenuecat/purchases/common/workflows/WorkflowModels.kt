@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.utils.serializers.JsonObjectToMapSerializer
import com.revenuecat.purchases.utils.serializers.URLSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.net.URL

@Serializable
internal data class WorkflowSummary(
    val id: String,
    @SerialName("display_name") val displayName: String,
)

@Serializable
internal data class WorkflowsListResponse(
    val workflows: List<WorkflowSummary> = emptyList(),
)

@Serializable
internal data class WorkflowTriggerAction(
    val type: String,
    val value: String? = null,
    @SerialName("step_id") val stepId: String? = null,
) {
    val resolvedTargetStepId: String?
        get() = value ?: stepId
}

@Serializable
internal data class WorkflowTrigger(
    val name: String,
    val type: String,
    @SerialName("action_id") val actionId: String,
    @SerialName("component_id") val componentId: String,
)

@Serializable
internal data class WorkflowStep(
    val id: String,
    val type: String,
    @SerialName("screen_id") val screenId: String? = null,
    @SerialName("param_values") val paramValues: Map<String, JsonElement> = emptyMap(),
    val triggers: List<WorkflowTrigger> = emptyList(),
    val outputs: Map<String, JsonElement> = emptyMap(),
    @SerialName("trigger_actions") val triggerActions: Map<String, WorkflowTriggerAction> = emptyMap(),
    val metadata: JsonElement? = null,
)

@Serializable
internal data class WorkflowScreen(
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
    @SerialName("offering_id") val offeringId: String? = null,
)

/**
 * Published workflow document returned inline or via CDN (same JSON shape).
 */
@Serializable
internal data class PublishedWorkflow(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("initial_step_id") val initialStepId: String,
    val steps: Map<String, WorkflowStep>,
    val screens: Map<String, WorkflowScreen>,
    @SerialName("ui_config") val uiConfig: UiConfig,
    @Serializable(with = JsonObjectToMapSerializer::class)
    val metadata: Map<String, @Contextual Any> = emptyMap(),
)

internal data class WorkflowFetchResult(
    val workflow: PublishedWorkflow,
    val enrolledVariants: Map<String, String>?,
)

internal enum class WorkflowResponseAction(val value: String) {
    INLINE("inline"),
    USE_CDN("use_cdn"),
    ;

    internal companion object {
        fun fromValue(value: String): WorkflowResponseAction? =
            values().firstOrNull { it.value == value }
    }
}
