package com.revenuecat.purchases.common.localrules

import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.rules.RulesEngineLogger

/**
 * Routes rules engine diagnostics through the SDK logger, which the engine does not depend on directly so that it
 * stays extractable.
 *
 * Nothing reaches warning level: a predicate reading a dimension this SDK version does not supply is the expected
 * forward-compatibility path, not something an app developer should see warnings about on every evaluation.
 */
internal object RulesEngineLoggerBridge : RulesEngineLogger {

    override fun warn(message: String) {
        debugLog { "$LOG_PREFIX$message" }
    }

    override fun log(message: String) {
        verboseLog { "$LOG_PREFIX$message" }
    }

    private const val LOG_PREFIX = "Rules engine: "
}
