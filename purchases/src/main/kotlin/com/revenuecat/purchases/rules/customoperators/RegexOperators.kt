package com.revenuecat.purchases.rules.customoperators

import com.revenuecat.purchases.rules.RulesEngine.EvaluationException
import com.revenuecat.purchases.rules.Scope
import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.rules.operators.Operators
import java.util.regex.PatternSyntaxException

/**
 * Regular expression operators.
 *
 * The pattern is handed to the platform engine. This one and the iOS engine
 * are both ICU — `java.util.regex` has wrapped ICU4C since Android 2.3, so it
 * is not the OpenJDK engine a desktop JVM would use — and `RegExp` in Funnels
 * is the one that differs. Patterns are authored by the backend, which is
 * expected to stay inside the subset all three read the same way:
 *
 * - `\d`, `\w`, `\s` and `\b` cover Unicode in ICU and only ASCII in JS, so
 *   `\d` matches an Arabic-Indic digit on a device and not in Funnels. Write
 *   `[0-9]` for the ASCII meaning.
 * - `&&` inside a character class is set intersection in ICU and two literal
 *   ampersands in JS.
 * - A literal `}` needs escaping as `\}` for ICU, which reads a bare one as a
 *   malformed quantifier. JS accepts either, so an unescaped brace compiles
 *   in Funnels and throws on both devices.
 * - `$` matches before a trailing newline in ICU but not in JS.
 *
 * The one place the devices part company is an unknown escape such as `\q`:
 * this engine rejects the pattern, iOS reads it as a literal `q`. Escape only
 * what needs escaping.
 *
 * Note that the unit tests run on a desktop JVM, whose regex engine is not
 * the ICU one that ships, so a fixture cannot prove device behavior for any
 * of the above.
 *
 * No operator takes flags, since JS has no inline `(?i)`. Write `[aA]` for a
 * case-insensitive letter.
 */
internal object RegexOperators {

    private const val BINARY = 2
    private const val TERNARY = 3

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

    /**
     * `{"rc.regexExtract": [input, pattern]}` or
     * `{"rc.regexExtract": [input, pattern, group]}` — the text of the first
     * match, or of one of its capture groups. Group `0`, the default, is the
     * whole match.
     *
     * Returns `null` when the pattern does not match, and when the group
     * exists but took no part in the match, as group 1 of `(a)|(b)` does
     * against `"b"`.
     *
     * A group number the pattern does not have is a lowering bug and throws
     * [EvaluationException.TypeMismatch], as do non-string operands, a pattern
     * that does not compile, and a fractional group.
     */
    fun opRegexExtract(args: Value, vars: Scope): Value {
        val operatorName = "rc.regexExtract"
        val evaluated = Operators.evalArgs(args, vars)
        checkArity(evaluated.size, listOf(BINARY, TERNARY), operatorName)

        val operands = operands(evaluated, operatorName)
        val group = group(evaluated.getOrNull(TERNARY - 1), operatorName)
        val captureCount = operands.regex.toPattern().matcher("").groupCount()

        if (group > captureCount) {
            throw EvaluationException.TypeMismatch(
                "operator '$operatorName' asked for group $group of a pattern with " +
                    "$captureCount capture group(s)",
            )
        }

        val matched = operands.regex.find(operands.input)?.groups?.get(group)
        return matched?.let { Value.StringValue(it.value) } ?: Value.Null
    }

    /**
     * `{"rc.regexReplace": [input, pattern, replacement]}` — every match
     * replaced, left to right.
     *
     * The replacement is literal text. `$1` is not a backreference: the
     * template is read by [java.util.regex.Matcher] rather than by ICU, so
     * this is the one part of the feature where the two devices differ. `$&`
     * substitutes the match in JS, throws here, and stays literal on iOS;
     * `$1` against a pattern with no groups throws here and yields the empty
     * string on iOS. None of it is exposed. Build a replacement out of
     * captures with `rc.regexExtract` and `cat` instead.
     *
     * All operands must be strings and the pattern must compile, otherwise
     * [EvaluationException.TypeMismatch].
     */
    fun opRegexReplace(args: Value, vars: Scope): Value {
        val operatorName = "rc.regexReplace"
        val evaluated = Operators.evalArgs(args, vars)
        checkArity(evaluated.size, listOf(TERNARY), operatorName)

        val operands = operands(evaluated, operatorName)
        val replacement = evaluated[TERNARY - 1]
        if (replacement !is Value.StringValue) {
            throw EvaluationException.TypeMismatch(
                "operator '$operatorName' expected a string replacement, got $replacement",
            )
        }

        return Value.StringValue(
            operands.regex.replace(
                operands.input,
                Regex.escapeReplacement(replacement.value),
            ),
        )
    }

    /** Reads the optional group argument, defaulting to the whole match. */
    private fun group(value: Value?, operatorName: String): Int = when {
        value == null -> 0
        value is Value.IntValue && value.value >= 0 ->
            value.value.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        value is Value.FloatValue && value.value >= 0 && value.value % 1.0 == 0.0 ->
            Operators.clampedInt(value.value)
        else -> throw EvaluationException.TypeMismatch(
            "operator '$operatorName' expected a whole, non-negative group number, got $value",
        )
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
