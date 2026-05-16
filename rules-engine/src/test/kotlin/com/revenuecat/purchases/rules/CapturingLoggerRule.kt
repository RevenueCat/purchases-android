package com.revenuecat.purchases.rules

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit rule that swaps `RulesEngineLog.sink` for a fresh [CapturingLogger]
 * for the duration of the test, then restores the previous sink.
 *
 * Tests that need to assert on warnings install this rule and read
 * [warnings]. Tests that don't assert on warnings still install it so the
 * default [PrintlnLogger] doesn't spam stderr during the run.
 */
internal class CapturingLoggerRule : TestRule {

    val capturing: CapturingLogger = CapturingLogger()
    val warnings: List<String> get() = capturing.warnings

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val previous = RulesEngineLog.sink
                RulesEngineLog.sink = capturing
                try {
                    base.evaluate()
                } finally {
                    RulesEngineLog.sink = previous
                }
            }
        }
    }
}
