package com.revenuecat.purchases.rules.customoperators

import com.revenuecat.purchases.rules.RulesEngine.EvaluationException
import com.revenuecat.purchases.rules.Scope
import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.rules.operators.Operators

/**
 * `rc.length` — element count for arrays, UTF-16 code-unit count for
 * strings (JS `String.length` parity).
 */
internal object LengthOperator {

    /**
     * `{"rc.length": value}` — returns a numeric length suitable for
     * arithmetic and comparisons.
     *
     * - **String**: length in **UTF-16 code units** ([String.length]) —
     *   matches JS `String.length` and the same choice in
     *   `AccessorOperators.opMissingSome`.
     * - **Array**: element count.
     * - **Anything else**: throws [EvaluationException.TypeMismatch].
     *
     * Non-string/non-array inputs throw rather than defaulting to `0`.
     * A silent zero would make `{"==": [{"rc.length": {"var": "missing"}},
     * 0]}` quietly true when a key is absent.
     *
     * Uses [Operators.firstArgEvaluated] spread semantics; extra arguments
     * are silently ignored. Literal array operands must be wrapped
     * (e.g. `{"rc.length": [[1, 2, 3]]}`), not passed as a multi-element
     * arg list.
     */
    fun opLength(args: Value, vars: Scope): Value =
        when (val input = Operators.firstArgEvaluated(args, vars)) {
            is Value.StringValue -> Value.IntValue(stringLength(input.value).toLong())
            is Value.ArrayValue -> Value.IntValue(input.items.size.toLong())
            else -> throw EvaluationException.TypeMismatch(
                "operator 'rc.length' expected string or array, got $input",
            )
        }

    /**
     * The one place the engine measures a string. `rc.indexOf` reports
     * positions through this too, so every length and position handed back to
     * a rule is stated in the same unit.
     */
    fun stringLength(string: String): Int = string.length
}
