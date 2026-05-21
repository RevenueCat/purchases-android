@file:OptIn(InternalRulesEngineAPI::class)

package com.revenuecat.purchases.rules

/**
 * Test-only logger that captures messages for assertion. Lives in the
 * test source set — no production caller needs it now that the engine
 * routes warnings through [Rules.logger].
 */
internal class CapturingLogger : RulesEngineLogger {

    private val captured = mutableListOf<String>()

    val warnings: List<String>
        @Synchronized get() = captured.toList()

    @Synchronized
    override fun warn(message: String) {
        captured += message
    }
}
