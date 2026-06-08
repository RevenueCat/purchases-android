package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.RulesEngine
import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.rules.jsString

/**
 * Miscellaneous operators: `log`.
 */
internal object MiscOperators {

    /**
     * `{"log": value}` — evaluates its single argument, emits it through the
     * dedicated `log` channel of [RulesEngine.logger], and returns it unchanged
     * (identity passthrough).
     * Mirrors json-logic-js `function(a){ console.log(a); return a; }`: a
     * debug aid that never affects a rule's outcome. A missing argument is
     * [Value.Null]; operands beyond the first are ignored.
     */
    fun opLog(args: Value, vars: Value): Value {
        val value = Operators.evalArgs(args, vars).firstOrNull() ?: Value.Null
        RulesEngine.logger.log(jsString(value))
        return value
    }
}
