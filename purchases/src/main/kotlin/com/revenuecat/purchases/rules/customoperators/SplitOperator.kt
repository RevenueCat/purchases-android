package com.revenuecat.purchases.rules.customoperators

import com.revenuecat.purchases.rules.RulesEngine.EvaluationException
import com.revenuecat.purchases.rules.Scope
import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.rules.operators.Operators

/**
 * `rc.split` — splits a string into an array of fields.
 */
internal object SplitOperator {

    private const val ARGUMENT_COUNT = 2

    /**
     * `{"rc.split": [input, separator]}` — every field, including empty
     * ones, as strings. Numeric-looking fields are not coerced, so `"1,2"`
     * splits into `["1", "2"]`.
     *
     * A `var` path cannot index the result of an expression, so a field is
     * read by wrapping the array in a one-element list and letting an
     * iteration operator rebind the scope to it:
     *
     * ```json
     * {"some": [[{"rc.split": [{"var": "locale"}, "-"]}],
     *           {"===": [{"var": "0"}, "es"]}]}
     * ```
     *
     * The array is also usable directly by `in` and by the iteration
     * operators, which is enough for most membership and any/all tests.
     *
     * Both operands must be strings and the separator must be non-empty,
     * otherwise [EvaluationException.TypeMismatch]. An empty separator would
     * mean "split into characters", which the engine deliberately does not
     * offer. Arity is exact rather than lenient: a one-sided call has no
     * sensible implicit separator, so it is a lowering bug worth surfacing.
     */
    @Suppress("ThrowsCount")
    fun opSplit(args: Value, vars: Scope): Value {
        val evaluated = Operators.evalArgs(args, vars)

        if (evaluated.size != ARGUMENT_COUNT) {
            throw EvaluationException.TypeMismatch(
                "operator 'rc.split' expects $ARGUMENT_COUNT arguments, got ${evaluated.size}",
            )
        }

        val input = evaluated[0]
        if (input !is Value.StringValue) {
            throw EvaluationException.TypeMismatch(
                "operator 'rc.split' expected a string to split, got $input",
            )
        }

        val separator = evaluated[1]
        if (separator !is Value.StringValue || separator.value.isEmpty()) {
            throw EvaluationException.TypeMismatch(
                "operator 'rc.split' expected a non-empty string separator, got $separator",
            )
        }

        return Value.ArrayValue(input.value.split(separator.value).map { Value.StringValue(it) })
    }
}
