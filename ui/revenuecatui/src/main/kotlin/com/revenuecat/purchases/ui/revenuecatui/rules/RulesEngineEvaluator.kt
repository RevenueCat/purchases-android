@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.rules

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.common.Config
import com.revenuecat.purchases.rules.RulesEngine
import com.revenuecat.purchases.rules.RulesEngineLogger
import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import java.util.concurrent.atomic.AtomicBoolean

internal object RulesEngineEvaluator {
    private val installed = AtomicBoolean(false)

    fun evaluate(
        predicate: String,
        variables: Map<String, Value>,
    ): Result<Boolean> {
        installLoggerIfNeeded()

        return RulesEngine.evaluate(predicate, variables)
    }

    private fun installLoggerIfNeeded() {
        if (installed.compareAndSet(false, true)) {
            RulesEngine.setLogger(RulesEngineLoggerAdapter)
        }
    }
}

private object RulesEngineLoggerAdapter : RulesEngineLogger {
    override fun warn(message: String) {
        if (Config.logLevel <= LogLevel.WARN) {
            Logger.w(rulesEngineWarning(message))
        }
    }

    override fun log(message: String) {
        if (Config.logLevel <= LogLevel.DEBUG) {
            Logger.d(rulesEngineLog(message))
        }
    }
}

private fun rulesEngineWarning(message: String): String =
    "Rules engine warning: $message"

private fun rulesEngineLog(message: String): String =
    "Rules engine log: $message"
