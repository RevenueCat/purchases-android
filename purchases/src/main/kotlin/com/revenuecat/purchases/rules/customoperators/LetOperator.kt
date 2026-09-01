package com.revenuecat.purchases.rules.customoperators

import com.revenuecat.purchases.rules.Evaluator
import com.revenuecat.purchases.rules.RulesEngine.EvaluationException
import com.revenuecat.purchases.rules.Scope
import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.rules.operators.Operators

/** Name used in this operator's error messages. */
private const val OPERATOR_NAME = "rc.let"

/** `rc.let` — binds names that stay readable inside iteration. */
internal object LetOperator {

    private const val ARGUMENT_COUNT = 2

    /**
     * `{"rc.let": [{"name": expression, ...}, body]}` — evaluates `body`
     * with each name available to `var`.
     *
     * Iteration replaces the whole scope with the current item, so a
     * predicate nested inside `some` or `reduce` cannot otherwise reach a
     * value from the enclosing level. Binding it first is what makes
     * comparing an item against something outside the loop expressible.
     *
     * `var` reads the active data first and falls back to these names, so
     * a binding never masks a field the data actually has.
     *
     * The declarations are a literal object, not an evaluated one: a
     * single-key object anywhere else in a predicate is an operator call,
     * which is why the argument is never evaluated as a whole. Every
     * declared expression is evaluated in the scope enclosing the `rc.let`,
     * so bindings cannot see each other and their order carries no meaning;
     * nest a second `rc.let` to build one from another.
     *
     * Names must be non-empty and free of `.`, since a dotted name would be
     * unreachable behind path traversal. Anything else, including a
     * non-object declaration list or the wrong argument count, throws
     * [EvaluationException.TypeMismatch].
     */
    fun opLet(args: Value, vars: Scope): Value {
        val raw = Operators.argsAsList(args)

        if (raw.size != ARGUMENT_COUNT) {
            throw EvaluationException.TypeMismatch(
                "operator '$OPERATOR_NAME' expects $ARGUMENT_COUNT arguments, got ${raw.size}",
            )
        }

        val declarations = raw[0]
        if (declarations !is Value.ObjectValue) {
            throw EvaluationException.TypeMismatch(
                "operator '$OPERATOR_NAME' expected an object of bindings, got $declarations",
            )
        }

        return Evaluator.evaluateValue(raw[1], vars.binding(bindings(declarations, vars)))
    }

    /** Evaluates every declaration in the enclosing scope, rejecting unusable names. */
    private fun bindings(declarations: Value.ObjectValue, vars: Scope): Map<String, Value> =
        // Sorted so that a predicate with two invalid names fails the same
        // way on both engines.
        declarations.entries.keys.sorted().associateWith { name ->
            if (name.isEmpty() || name.contains(".")) {
                throw EvaluationException.TypeMismatch(
                    "operator '$OPERATOR_NAME' expected a binding name that is " +
                        "non-empty and free of '.', got '$name'",
                )
            }
            Evaluator.evaluateValue(declarations.entries[name] ?: Value.Null, vars)
        }
}
