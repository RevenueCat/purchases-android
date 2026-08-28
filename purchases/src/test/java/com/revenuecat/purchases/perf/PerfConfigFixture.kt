package com.revenuecat.purchases.perf

import com.revenuecat.purchases.common.networking.RCContainerTestData

/**
 * Builds a VALID RC Container Format v1 `/v1/config` response for the perf harness.
 *
 * Unlike a bare `{}` body (which is not a valid RC Container and only exercises the SDK's
 * retry/error path), this response carries both the `workflows` and `ui_config` topics with every
 * item's blob **inlined** as a container element, using distinct bytes per blob. That lets
 * [com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager] extract and cache all of them
 * synchronously at persist time (`extractInlineBlobs`), so both `WorkflowsConfigProvider.awaitReady()`
 * and `UiConfigProvider.getUiConfig()` — both awaited by `WorkflowManager.onPaywallConfigReady`, which
 * gates `getOfferings()`'s success callback on every default-flavor configuration (remote config is on
 * by default; it is only disabled for the `customEntitlementComputation` flavor) — resolve without any
 * further network round-trip beyond the single `/v1/config` request.
 *
 * Built via [RCContainerTestData.buildContainer], the same internal test helper backing
 * `RCContainerTest`/`RCContainerBackwardsCompatTest`, so the wire format never drifts from what the
 * real parser is tested against.
 */
internal object PerfConfigFixture {

    // language=json
    private const val WORKFLOW_BLOB_TEXT = """{"id":"wf_perf","steps":[{"type":"paywall","offering":"default"}]}"""
    private val WORKFLOW_BLOB = WORKFLOW_BLOB_TEXT.toByteArray()

    // language=json
    private const val APP_BLOB_TEXT = """{"colors":{},"fonts":{}}"""
    private val APP_BLOB = APP_BLOB_TEXT.toByteArray()

    // language=json
    private const val LOCALIZATIONS_BLOB_TEXT = """{}"""
    private val LOCALIZATIONS_BLOB = LOCALIZATIONS_BLOB_TEXT.toByteArray()

    // language=json
    private const val VARIABLE_CONFIG_BLOB_TEXT =
        """{"variable_compatibility_map":{},"function_compatibility_map":{}}"""
    private val VARIABLE_CONFIG_BLOB = VARIABLE_CONFIG_BLOB_TEXT.toByteArray()

    // language=json
    private const val CUSTOM_VARIABLES_BLOB_TEXT = """{}"""
    private val CUSTOM_VARIABLES_BLOB = CUSTOM_VARIABLES_BLOB_TEXT.toByteArray()

    private fun ref(bytes: ByteArray): String = RCContainerTestData.refOf(bytes)

    private fun configJson(): String = """
        {
          "domain": "app",
          "manifest": "v1.perfharness:etag1",
          "active_topics": ["workflows", "ui_config"],
          "prefetch_blobs": [],
          "topics": {
            "workflows": {
              "wf_perf": { "offering_identifier": "default", "blob_ref": "${ref(WORKFLOW_BLOB)}" }
            },
            "ui_config": {
              "app": { "blob_ref": "${ref(APP_BLOB)}" },
              "localizations": { "blob_ref": "${ref(LOCALIZATIONS_BLOB)}" },
              "variable_config": { "blob_ref": "${ref(VARIABLE_CONFIG_BLOB)}" },
              "custom_variables": { "blob_ref": "${ref(CUSTOM_VARIABLES_BLOB)}" }
            }
          }
        }
    """.trimIndent()

    /** Valid RC Container Format v1 bytes for `/v1/config`: the config element plus all 5 blobs inlined. */
    fun validRemoteConfigBytes(): ByteArray = RCContainerTestData.buildContainer(
        config = configJson().toByteArray(),
        elements = listOf(WORKFLOW_BLOB, APP_BLOB, LOCALIZATIONS_BLOB, VARIABLE_CONFIG_BLOB, CUSTOM_VARIABLES_BLOB),
    )
}
