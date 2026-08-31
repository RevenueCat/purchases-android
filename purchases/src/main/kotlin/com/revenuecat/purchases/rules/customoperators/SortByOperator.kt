package com.revenuecat.purchases.rules.customoperators

import com.revenuecat.purchases.rules.Evaluator
import com.revenuecat.purchases.rules.RulesEngine.EvaluationException
import com.revenuecat.purchases.rules.Scope
import com.revenuecat.purchases.rules.Value
import com.revenuecat.purchases.rules.operators.Operators

/** Name used in this operator's error messages. */
private const val OPERATOR_NAME = "rc.sortBy"

/** `rc.sortBy` — orders an array in ascending order by a key computed per item. */
internal object SortByOperator {

    private const val ARGUMENT_COUNT = 2

    /**
     * `{"rc.sortBy": [array, keyExpression]}` — the original items in
     * ascending key order, ties keeping their input order.
     *
     * `keyExpression` is evaluated once per item with the scope rebound to
     * that item, the same way `map` evaluates its template.
     *
     * Keys must be all strings or all finite numbers; anything else,
     * including a mix, throws [EvaluationException.TypeMismatch]. There is no
     * direction argument.
     */
    fun opSortBy(args: Value, vars: Scope): Value {
        val raw = Operators.argsAsList(args)

        if (raw.size != ARGUMENT_COUNT) {
            throw EvaluationException.TypeMismatch(
                "operator '$OPERATOR_NAME' expects $ARGUMENT_COUNT arguments, got ${raw.size}",
            )
        }

        val source = Evaluator.evaluateValue(raw[0], vars)
        if (source !is Value.ArrayValue) {
            throw EvaluationException.TypeMismatch(
                "operator '$OPERATOR_NAME' expected an array to sort, got $source",
            )
        }

        val keys = source.items.map { item ->
            Evaluator.evaluateValue(raw[1], vars.scoped(item))
        }

        return Value.ArrayValue(sorted(source.items, keys))
    }

    /**
     * Sorts by [keys], falling back to the input position so equal keys keep
     * their order.
     */
    private fun sorted(items: List<Value>, keys: List<Value>): List<Value> {
        val order = comparator(keys)

        return keys.indices
            .sortedWith { left, right ->
                val byKey = order.compare(keys[left], keys[right])
                if (byKey != 0) byKey else left.compareTo(right)
            }
            .map { items[it] }
    }

    /** The two orderings a key set can have. */
    private enum class KeyKind { STRING, NUMBER }

    /** Reads the kind of a single key, rejecting anything unorderable. */
    private fun keyKind(key: Value): KeyKind = when (key) {
        is Value.StringValue -> KeyKind.STRING
        is Value.IntValue -> KeyKind.NUMBER
        // A NaN key compares false against everything, so every pair would
        // tie and the input would come back unsorted.
        is Value.FloatValue -> if (key.value.isFinite()) {
            KeyKind.NUMBER
        } else {
            throw EvaluationException.TypeMismatch(
                "operator '$OPERATOR_NAME' expected a finite number key, got $key",
            )
        }
        else -> throw EvaluationException.TypeMismatch(
            "operator '$OPERATOR_NAME' expected string or number keys, got $key",
        )
    }

    /** Picks the ordering the whole key set supports, or throws. */
    private fun comparator(keys: List<Value>): Comparator<Value> {
        val kinds = keys.map { keyKind(it) }
        val sawString = kinds.contains(KeyKind.STRING)
        val sawNumber = kinds.contains(KeyKind.NUMBER)

        if (sawString && sawNumber) {
            throw EvaluationException.TypeMismatch(
                "operator '$OPERATOR_NAME' expected keys of a single type, " +
                    "got both strings and numbers",
            )
        }

        if (sawString) {
            return Comparator { left, right ->
                val leftString = (left as? Value.StringValue)?.value ?: ""
                val rightString = (right as? Value.StringValue)?.value ?: ""
                leftString.compareTo(rightString)
            }
        }
        return Comparator { left, right ->
            (left.toNumberOrNull() ?: 0.0).compareTo(right.toNumberOrNull() ?: 0.0)
        }
    }
}
