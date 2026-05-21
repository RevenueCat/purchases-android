package com.revenuecat.purchases.rules.operators

import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.rules.looseEq
import com.revenuecat.purchases.rules.strictEq

/**
 * Equality operators: `==`, `!=`, `===`, `!==`.
 */
internal object EqualityOperators {

    /**
     * `{"==": [a, b]}` — JSON Logic loose equality. Coerces across primitive
     * types (e.g. `1 == "1"` is true). Full coercion table in [looseEq].
     */
    fun opLooseEq(args: Value, vars: Value): Value {
        val (lhs, rhs) = Operators.evalTwo(args, vars, "==")
        return Value.BoolValue(looseEq(lhs, rhs))
    }

    /** `{"!=": [a, b]}` — JSON Logic loose inequality. Negation of `==`. */
    fun opLooseNe(args: Value, vars: Value): Value {
        val (lhs, rhs) = Operators.evalTwo(args, vars, "!=")
        return Value.BoolValue(!looseEq(lhs, rhs))
    }

    /**
     * `{"===": [a, b]}` — JSON Logic strict equality. Same type, same value
     * (`1 === "1"` is false). See [strictEq] for the numeric subtlety
     * around `IntValue` vs `FloatValue`.
     */
    fun opStrictEq(args: Value, vars: Value): Value {
        val (lhs, rhs) = Operators.evalTwo(args, vars, "===")
        return Value.BoolValue(strictEq(lhs, rhs))
    }

    /** `{"!==": [a, b]}` — JSON Logic strict inequality. Negation of `===`. */
    fun opStrictNe(args: Value, vars: Value): Value {
        val (lhs, rhs) = Operators.evalTwo(args, vars, "!==")
        return Value.BoolValue(!strictEq(lhs, rhs))
    }
}
