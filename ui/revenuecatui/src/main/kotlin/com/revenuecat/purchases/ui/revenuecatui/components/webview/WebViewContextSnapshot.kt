@file:JvmSynthetic
@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.common.SharedConstants.MICRO_MULTIPLIER
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.components.style.WebViewComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.WorkflowScreenContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

@JvmSynthetic
internal fun webViewContextSnapshot(
    state: PaywallState.Loaded.Components,
    style: WebViewComponentStyle,
    darkMode: Boolean,
): JsonObject {
    val selectedPackage = state.selectedPackageInfo?.rcPackage
    return webViewContextSnapshot(
        WebViewContextInput(
            customVariables = state.mergedCustomVariables,
            offering = state.offering,
            // Inside a package component the values describe that package; elsewhere they follow
            // the selection, matching what a text component resolves.
            componentPackage = style.rcPackage ?: selectedPackage,
            selectedPackage = selectedPackage,
            store = state.store,
            storefrontCountryCode = state.storefrontCountryCode,
            workflowScreen = state.workflowScreen,
            locale = state.locale.toLanguageTag(),
            darkMode = darkMode,
        ),
    )
}

/**
 * Builds the payload the handshake seeds and later `context` pushes replace, per the
 * custom component variables contract (RevenueCat/docs#1874): absent structured sections are
 * literal `null`, empty maps are `{}`, and `workflow` is omitted entirely outside a funnel.
 *
 * `packages[].display_name` is omitted: the offerings endpoint does not serve package display names.
 */
@JvmSynthetic
internal fun webViewContextSnapshot(input: WebViewContextInput): JsonObject = buildJsonObject {
    putJsonObject(Keys.CUSTOM) {
        input.customVariables.forEach { (name, value) -> put(name, value.asJsonPrimitive) }
    }
    put(Keys.OFFERING, input.offering?.asJson() ?: JsonNull)
    putJsonArray(Keys.PACKAGES) {
        input.offering?.availablePackages?.forEach { add(it.asJson(input)) }
    }
    put(Keys.PACKAGE, input.componentPackage?.asJson(input) ?: JsonNull)
    put(Keys.SELECTED_PACKAGE, input.selectedPackage?.asJson(input) ?: JsonNull)
    putJsonObject(Keys.INPUTS) {}
    input.workflowScreen?.let { putWorkflow(it) }
    putJsonObject(Keys.DEVICE_META) {
        put(Keys.IS_PREVIEW, false)
        put(Keys.LOCALE, input.locale)
        put(Keys.DARK_MODE, input.darkMode)
        put(Keys.UPDATED_AT, System.currentTimeMillis())
    }
}

private fun JsonObjectBuilder.putWorkflow(workflowScreen: WorkflowScreenContext) {
    putJsonObject(Keys.WORKFLOW) {
        put(Keys.WORKFLOW_ID, workflowScreen.workflowId)
        put(Keys.STEP_ID, workflowScreen.stepId)
        put(Keys.STEP_TYPE, workflowScreen.stepType)
        // The wire type has no null: an untagged step and one tagged with nothing look the same.
        putJsonArray(Keys.SCREEN_TYPE) { workflowScreen.screenType?.forEach { add(it) } }
    }
}

private fun Offering.asJson(): JsonObject = buildJsonObject {
    put(Keys.IDENTIFIER, identifier)
    // The offerings endpoint serves the dashboard display name under the `description` key.
    put(Keys.DISPLAY_NAME, serverDescription)
}

private fun Package.asJson(input: WebViewContextInput): JsonObject = buildJsonObject {
    put(Keys.IDENTIFIER, identifier)
    // One product per package on Android; the contract carries a list for stores serving more.
    putJsonArray(Keys.PRODUCTS) { add(product.asJson(input)) }
}

private fun StoreProduct.asJson(input: WebViewContextInput): JsonObject = buildJsonObject {
    put(Keys.IDENTIFIER, id)
    putJsonObject(Keys.STORE) {
        put(Keys.STORE_TYPE, input.store.stringValue)
        input.storefrontCountryCode?.let { put(Keys.COUNTRY, it) }
    }
    put(Keys.DISPLAY_NAME, name)
    put(Keys.IS_SUBSCRIPTION, period != null)
    period?.let { put(Keys.PERIOD, it.iso8601) }
    isAutoRenewing?.let { put(Keys.IS_AUTO_RENEWING, it) }
    putJsonObject(Keys.PRICE) {
        put(Keys.AMOUNT, price.amountMicros / MICRO_MULTIPLIER)
        put(Keys.CURRENCY, price.currencyCode)
    }
}

/** Google reports renewal on the base pricing phase, which trial and intro phases precede. */
private val StoreProduct.isAutoRenewing: Boolean?
    get() = when (defaultOption?.fullPricePhase?.recurrenceMode) {
        RecurrenceMode.INFINITE_RECURRING -> true
        RecurrenceMode.NON_RECURRING -> false
        else -> null
    }

/** Wire keys from the contract (RevenueCat/docs#1874), in the order its table lists them. */
private object Keys {
    const val CUSTOM = "custom"
    const val OFFERING = "offering"
    const val PACKAGES = "packages"
    const val PACKAGE = "package"
    const val SELECTED_PACKAGE = "selected_package"
    const val INPUTS = "inputs"
    const val DEVICE_META = "device_meta"

    const val IS_PREVIEW = "is_preview"
    const val LOCALE = "locale"
    const val DARK_MODE = "dark_mode"
    const val UPDATED_AT = "updated_at"

    const val IDENTIFIER = "identifier"
    const val DISPLAY_NAME = "display_name"
    const val PRODUCTS = "products"
    const val STORE = "store"
    const val STORE_TYPE = "store_type"
    const val COUNTRY = "country"
    const val IS_SUBSCRIPTION = "is_subscription"
    const val PERIOD = "period"
    const val IS_AUTO_RENEWING = "is_auto_renewing"
    const val PRICE = "price"
    const val AMOUNT = "amount"
    const val CURRENCY = "currency"

    const val WORKFLOW = "workflow"
    const val WORKFLOW_ID = "workflow_id"
    const val STEP_ID = "step_id"
    const val STEP_TYPE = "step_type"
    const val SCREEN_TYPE = "screen_type"
}

private val CustomVariableValue.asJsonPrimitive: JsonPrimitive
    get() = map(
        string = { JsonPrimitive(it) },
        number = { number ->
            // A bare NaN or Infinity is not valid JSON.
            if (!number.isFinite()) {
                JsonPrimitive(number.toString())
            } else {
                val whole = number.toLong()
                if (number == whole.toDouble()) JsonPrimitive(whole) else JsonPrimitive(number)
            }
        },
        boolean = { JsonPrimitive(it) },
    )
