package com.revenuecat.purchases.paywalls.components.common.serializers

import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.utils.serializers.EnumDeserializerWithDefault

internal object UpgradeReplacementModeDeserializer : EnumDeserializerWithDefault<GoogleReplacementMode>(
    defaultValue = GoogleReplacementMode.CHARGE_PRORATED_PRICE,
    typeForValue = { value -> value.name.lowercase() },
)

internal object DowngradeReplacementModeDeserializer : EnumDeserializerWithDefault<GoogleReplacementMode>(
    defaultValue = GoogleReplacementMode.DEFERRED,
    typeForValue = { value -> value.name.lowercase() },
)
