package com.revenuecat.purchases.ui.revenuecatui.extensions

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallValidationResult
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import java.util.Date
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState as actualToComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatePaywallComponentsDataOrNull as actualValidatePaywallComponentsDataOrNull

/**
 * Same as the production-code namesake, but with some parameters made optional as a convenience for testing code.
 */
internal fun Offering.toComponentsPaywallState(
    validationResult: PaywallValidationResult.Components,
    storefrontCountryCode: String? = null,
    dateProvider: () -> Date = { Date() },
    purchases: PurchasesType = MockPurchasesType(),
    customVariables: Map<String, CustomVariableValue> = emptyMap(),
): PaywallState.Loaded.Components =
    actualToComponentsPaywallState(
        validationResult = validationResult,
        storefrontCountryCode = storefrontCountryCode,
        dateProvider = dateProvider,
        purchases = purchases,
        customVariables = customVariables,
    )

/**
 * Same as the production-code namesake, but with some parameters made optional as a convenience for testing code.
 */
internal fun Offering.validatePaywallComponentsDataOrNull(
    resourceProvider: ResourceProvider = MockResourceProvider(),
): Result<PaywallValidationResult.Components, NonEmptyList<PaywallValidationError>>? =
    actualValidatePaywallComponentsDataOrNull(
        resourceProvider = resourceProvider
    )
