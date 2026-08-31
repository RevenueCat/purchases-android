package com.revenuecat.purchases.rules.customoperators

import com.revenuecat.purchases.rules.RulesEngine.EvaluationException
import com.revenuecat.purchases.rules.Scope
import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.rules.operators.Operators
import com.revenuecat.purchases.rules.strictEq

/** Name used in this operator's error messages. */
private const val OPERATOR_NAME = "rc.indexOf"

/** `rc.indexOf` — where a value sits in an array, or a substring in a string. */
internal object IndexOfOperator {

    private const val ARGUMENT_COUNT = 2
    private const val NOT_FOUND = -1

    /**
     * `{"rc.indexOf": [haystack, needle]}` — the position of the first
     * occurrence, or `-1` when there is none.
     *
     * - **Array**: strict equality against each element, the same test `in`
     *   uses, so an array or object needle never matches.
     * - **String**: substring search. An empty needle sits at `0`, as in JS.
     *
     * Absence returns `-1`. A haystack that is neither array nor string, or a
     * non-string needle against a string haystack, is a lowering bug and
     * throws [EvaluationException.TypeMismatch].
     */
    @Suppress("ThrowsCount")
    fun opIndexOf(args: Value, vars: Scope): Value {
        val evaluated = Operators.evalArgs(args, vars)

        if (evaluated.size != ARGUMENT_COUNT) {
            throw EvaluationException.TypeMismatch(
                "operator '$OPERATOR_NAME' expects $ARGUMENT_COUNT arguments, got ${evaluated.size}",
            )
        }

        val needle = evaluated[1]

        return when (val haystack = evaluated[0]) {
            is Value.ArrayValue ->
                Value.IntValue(haystack.items.indexOfFirst { strictEq(it, needle) }.toLong())

            is Value.StringValue -> {
                if (needle !is Value.StringValue) {
                    throw EvaluationException.TypeMismatch(
                        "operator '$OPERATOR_NAME' expected a string to search for in a string, " +
                            "got $needle",
                    )
                }
                Value.IntValue(position(needle.value, haystack.value).toLong())
            }

            else -> throw EvaluationException.TypeMismatch(
                "operator '$OPERATOR_NAME' expected an array or string to search, got $haystack",
            )
        }
    }

    /**
     * Position of the first [needle] in [haystack], or `-1`.
     *
     * Two separate questions, deliberately answered by different units. *Where*
     * the needle occurs is a match, so it is searched over UTF-16 code units
     * like `in` and `rc.split`. *How far in* that is, is a count, so the text
     * before the match is measured with [LengthOperator.stringLength] — the
     * same function behind `rc.length`, so both operators state positions in
     * one unit.
     */
    private fun position(needle: String, haystack: String): Int {
        if (needle.isEmpty()) return 0

        val start = haystack.indexOf(needle)
        return if (start < 0) {
            NOT_FOUND
        } else {
            LengthOperator.stringLength(haystack.substring(0, start))
        }
    }
}
