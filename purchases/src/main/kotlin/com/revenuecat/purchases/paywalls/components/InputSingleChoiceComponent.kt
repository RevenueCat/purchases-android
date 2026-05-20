@file:Suppress("LongParameterList")

package com.revenuecat.purchases.paywalls.components

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("input_single_choice")
@Immutable
public class InputSingleChoiceComponent(
    @get:JvmSynthetic
    @SerialName("field_id")
    public val fieldId: String,
    @get:JvmSynthetic
    public val required: Boolean = false,
    @get:JvmSynthetic
    public val stack: StackComponent,
    @get:JvmSynthetic
    public val overrides: List<ComponentOverride<PartialInputSingleChoiceComponent>> = emptyList(),
) : PaywallComponent

@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("input_option")
@Immutable
public class InputOptionComponent(
    @get:JvmSynthetic
    @SerialName("option_id")
    public val optionId: String,
    @get:JvmSynthetic
    @SerialName("option_value")
    public val optionValue: String,
    @get:JvmSynthetic
    public val stack: StackComponent,
    @get:JvmSynthetic
    public val triggers: Map<String, String>? = null,
    @get:JvmSynthetic
    public val overrides: List<ComponentOverride<PartialInputOptionComponent>> = emptyList(),
) : PaywallComponent

@InternalRevenueCatAPI
@Poko
@Serializable
@Immutable
public class PartialInputSingleChoiceComponent(
    @get:JvmSynthetic
    @SerialName("field_id")
    public val fieldId: String? = null,
    @get:JvmSynthetic
    public val required: Boolean? = null,
) : PartialComponent

@InternalRevenueCatAPI
@Poko
@Serializable
@Immutable
public class PartialInputOptionComponent(
    @get:JvmSynthetic
    @SerialName("option_id")
    public val optionId: String? = null,
    @get:JvmSynthetic
    @SerialName("option_value")
    public val optionValue: String? = null,
    @get:JvmSynthetic
    public val triggers: Map<String, String>? = null,
) : PartialComponent
