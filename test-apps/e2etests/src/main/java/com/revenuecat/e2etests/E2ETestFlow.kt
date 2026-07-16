package com.revenuecat.e2etests

/**
 * The E2E test flow to run, selected via a Maestro launch argument. On Android, Maestro
 * `launchApp.arguments` are delivered as Activity intent extras, read in [MainActivity].
 */
internal enum class E2ETestFlow(val rawValue: String) {
    OPEN_WORKFLOW("open_workflow"),
    ;

    companion object {
        const val INTENT_EXTRA_KEY = "e2e_test_flow"

        fun fromRawValue(value: String?): E2ETestFlow? =
            entries.firstOrNull { it.rawValue == value }
    }
}
