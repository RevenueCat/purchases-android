package com.revenuecat.purchases.rules.customoperators

import com.revenuecat.purchases.rules.RulesEngine.EvaluationException
import com.revenuecat.purchases.rules.Scope
import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.rules.operators.Operators
import java.util.regex.PatternSyntaxException

/**
 * Regular expression operators.
 *
 * The pattern is handed to the platform engine — `java.util.regex` here, ICU
 * on iOS, `RegExp` in Funnels — and those three disagree on parts of the
 * syntax. Patterns are authored by the backend, which is expected to stay
 * inside the subset they agree on: `\d`, `\w`, `\s` and `\b` are ASCII here
 * but Unicode-aware in ICU, `&&` inside a character class means set
 * intersection here and something else in JS, and `$` matches before a
 * trailing newline here but not in JS.
 *
 * No operator takes flags, since JS has no inline `(?i)`. Write `[aA]` for a
 * case-insensitive letter.
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
        } catch (_: PatternSyntaxException) {
            throw EvaluationException.TypeMismatch(
                "operator '$operatorName' could not compile pattern '${pattern.value}'",
            )
        }

        return Operands(input.value, regex)
    }

    internal data class Operands(val input: String, val regex: Regex)
}
