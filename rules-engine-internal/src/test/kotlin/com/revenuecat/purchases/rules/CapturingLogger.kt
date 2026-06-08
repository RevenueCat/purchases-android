package com.revenuecat.purchases.rules

/**
 * Test-only logger that captures warnings and `log`-channel messages
 * separately for assertion.
 */
internal class CapturingLogger : RulesEngineLogger {

    private val capturedWarnings = mutableListOf<String>()
    private val capturedLogs = mutableListOf<String>()

    val warnings: List<String>
        @Synchronized get() = capturedWarnings.toList()

    val logs: List<String>
        @Synchronized get() = capturedLogs.toList()

    @Synchronized
    override fun warn(message: String, tag: String) {
        capturedWarnings += message
    }

    @Synchronized
    override fun log(message: String, tag: String) {
        capturedLogs += message
    }
}
