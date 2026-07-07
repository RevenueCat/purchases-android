package com.revenuecat.purchases.ui.revenuecatui

import dev.drewhamilton.poko.Poko

/**
 * A validated message received from a Paywalls V2 `web_view` component.
 *
 * Messages follow the RevenueCat web view postMessage envelope. Known [type]s include:
 * - `rc:step-loaded`
 * - `rc:step-complete` (carries [responses])
 * - `rc:request-variables`
 * - `rc:error` (carries [error])
 *
 * @property componentId The canonical component id (the `web_view.id` from the paywall schema). This
 * matches the id API consumers see in the paywall configuration.
 * @property type The message type, e.g. `rc:step-complete`.
 * @property responses The structured responses for a `rc:step-complete` message, if any.
 * @property error The error description for a `rc:error` message, if any.
 */
@Poko
public class PaywallWebViewMessage(
    public val componentId: String,
    public val type: String,
    public val responses: Map<String, PaywallWebViewValue>? = null,
    public val error: String? = null,
)

/**
 * Lets app code send messages back into a Paywalls V2 `web_view` component, for example in response to
 * a `rc:request-variables` message.
 *
 * Outgoing messages preserve the RevenueCat web view postMessage envelope and are validated before
 * being delivered to the web view.
 */
public interface PaywallWebViewController {

    /**
     * Sends a `rc:variables` message into the web view targeting [componentId]. The SDK already replies
     * with SDK-managed variables (such as `locale`); use this to provide additional values. The flat
     * [variables] map is sent as the transport envelope's `payload` field (not nested under a
     * `variables` key). Reserved SDK-managed top-level keys cannot be overwritten; provide
     * app-specific values under `custom`.
     */
    public fun postVariables(
        componentId: String,
        variables: Map<String, PaywallWebViewValue>,
    )

    /**
     * Sends a message of the given [type] into the web view targeting [componentId]. The flat
     * [variables] map is sent as the transport envelope's `payload` field (not nested under a
     * `variables` key). Use [postVariables] for the common `rc:variables` case.
     */
    public fun postMessage(
        componentId: String,
        type: String,
        variables: Map<String, PaywallWebViewValue>,
    )
}

/**
 * Receives validated messages from Paywalls V2 `web_view` components.
 *
 * Set this on [PaywallOptions.Builder.setWebViewMessageHandler]. The handler is always invoked on the
 * main thread. The app decides what to do with each message: the SDK does not automatically dismiss the
 * paywall or trigger a purchase in response to `rc:step-complete`.
 *
 * ### Usage
 * ```kotlin
 * PaywallOptions.Builder { /* dismiss */ }
 *     .setWebViewMessageHandler { message, controller ->
 *         when (message.type) {
 *             "rc:request-variables" -> controller.postVariables(
 *                 componentId = message.componentId,
 *                 variables = mapOf(
 *                     "custom" to PaywallWebViewValue.Object(
 *                         mapOf("app_segment" to PaywallWebViewValue.String("high_intent")),
 *                     ),
 *                 ),
 *             )
 *             "rc:step-complete" -> { /* read message.responses, navigate, log analytics, etc. */ }
 *             "rc:error" -> { /* log message.error */ }
 *         }
 *     }
 *     .build()
 * ```
 */
public fun interface PaywallWebViewMessageHandler {
    public fun onMessage(
        message: PaywallWebViewMessage,
        controller: PaywallWebViewController,
    )
}
