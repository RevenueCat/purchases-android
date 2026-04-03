package com.revenuecat.purchases.paywalls.components.common.serializers

import com.revenuecat.purchases.models.StoreReplacementMode
import com.revenuecat.purchases.utils.serializers.EnumDeserializerWithDefault

internal object UpgradeReplacementModeDeserializer : EnumDeserializerWithDefault<StoreReplacementMode>(
    defaultValue = StoreReplacementMode.CHARGE_PRORATED_PRICE,
    typeForValue = { value -> value.name.lowercase() },
)

internal object DowngradeReplacementModeDeserializer : EnumDeserializerWithDefault<StoreReplacementMode>(
    defaultValue = StoreReplacementMode.DEFERRED,
    typeForValue = { value -> value.name.lowercase() },
)
