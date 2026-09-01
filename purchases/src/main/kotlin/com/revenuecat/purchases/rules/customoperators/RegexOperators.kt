package com.revenuecat.purchases.rules.customoperators

import com.revenuecat.purchases.rules.RulesEngine.EvaluationException
import com.revenuecat.purchases.rules.Scope
import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.rules.operators.Operators
import java.util.regex.PatternSyntaxException

/**
 * Regular expression operators.
 *
 * Patterns are compiled by [java.util.regex.Pattern], whose syntax is
 * documented at
 * https://developer.android.com/reference/java/util/regex/Pattern. A
 * desktop JVM does not use the same engine, so these unit tests cannot
 * prove device behavior.
 *
 * No operator takes flags. Write `[aA]` for a case-insensitive letter.
 */
internal object RegexOperators {

    private const val BINARY = 2

    /**
     * `{"rc.regexMatch": [input, pattern]}` — whether the pattern occurs
     * anywhere in the input. Anchor with `^` and `$` for a whole-string test.
     *
     * Both operands must be strings and the pattern must compile, otherwise
     * [EvaluationException.TypeMismatch].
     */
    fun opRegexMatch(args: Value, vars: Scope): Value {
        val operatorName = "rc.regexMatch"
        val evaluated = Operators.evalArgs(args, vars)
        checkArity(evaluated.size, listOf(BINARY), operatorName)

        val operands = operands(evaluated, operatorName)
        return Value.BoolValue(operands.regex.containsMatchIn(operands.input))
    }

    /** Rejects an argument count no overload accepts. */
    fun checkArity(count: Int, allowed: List<Int>, operatorName: String) {
        if (count !in allowed) {
            val expected = allowed.joinToString(" or ")
            throw EvaluationException.TypeMismatch(
                "operator '$operatorName' expects $expected arguments, got $count",
            )
        }
    }

    /**
     * Type-checks the `[input, pattern]` pair every regex operator starts
     * with, and compiles the pattern.
     */
    @Suppress("ThrowsCount")
    fun operands(evaluated: List<Value>, operatorName: String): Operands {
        val input = evaluated[0]
        if (input !is Value.StringValue) {
            throw EvaluationException.TypeMismatch(
                "operator '$operatorName' expected a string to match against, got $input",
            )
        }

        val pattern = evaluated[1]
        if (pattern !is Value.StringValue) {
            throw EvaluationException.TypeMismatch(
                "operator '$operatorName' expected a string pattern, got $pattern",
            )
        }

        val regex = try {
            Regex(pattern.value)
        } catch (e: PatternSyntaxException) {
            // The exception's own message repeats the pattern and draws a caret
            // under it across three lines, so take the parts worth one line.
            val fault = e.description ?: "invalid pattern"
            val at = if (e.index >= 0) " near index ${e.index}" else ""
            throw EvaluationException.TypeMismatch(
                "operator '$operatorName' could not compile pattern '${pattern.value}': $fault$at",
            )
        }

        return Operands(input.value, regex)
    }

    internal data class Operands(val input: String, val regex: Regex)
}
