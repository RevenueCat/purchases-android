package com.revenuecat.purchases.rules

/** Namespace for the RevenueCat rules engine. */
internal object RulesEngine {
    @Volatile
    private var _logger: RulesEngineLogger = PrintLogger

    internal val logger: RulesEngineLogger
        get() = _logger

    // The rules engine deliberately owns its own logging abstraction ([RulesEngineLogger]) instead
    // of routing through the SDK-wide logger. This keeps it self-contained so it can be extracted
    // back into a standalone module as mechanically as it was folded in. If we commit to it living
    // in core long-term, consider bridging this to the SDK's logging system instead.
    @Synchronized
    fun setLogger(logger: RulesEngineLogger) {
        _logger = logger
    }

    /**
     * Errors surfaced by the rules engine.
     */
    internal sealed class EvaluationException(message: String) : Exception(message) {

        /** The predicate JSON could not be parsed. */
        internal data class Parse(val reason: String) : EvaluationException("failed to parse predicate JSON: $reason")

        /**
         * The predicate reads a variable that the evaluation scope does not
         * provide, and the rule supplies no default for it. Carries the
         * dot-path that failed to resolve.
         *
         * This is a deliberate divergence from `json-logic-js`, which resolves
         * an absent variable to `null`. That `null` then coerces to `0` in a
         * numeric comparison and `""` in a string one, so a rule can answer
         * `true` for a reason that has nothing to do with the user — a missing
         * `now` turns `{">": [expires, now]}` into `expires > 0`, which is true
         * for every subscription that ever existed. An absent variable means
         * "unknown", and unknown is not the same answer as "no", so the engine
         * refuses to guess and hands the decision to the caller.
         *
         * A rule that genuinely tolerates absence says so explicitly with the
         * spec's own escape hatch, `{"var": ["path", default]}`, which never
         * raises this error.
         */
        internal data class UnresolvedVariable(val path: String) :
            EvaluationException("unresolved variable: $path")

        /**
         * An operator was given arguments of the wrong shape (e.g. wrong arity)
         * or types that cannot be reconciled.
         */
        internal data class TypeMismatch(val detail: String) : EvaluationException("type mismatch: $detail")

        /**
         * The predicate references a JSON Logic operator the engine does not
         * implement. Carries the operator name.
         */
        internal data class UnsupportedOperator(val name: String) : EvaluationException("unsupported operator: $name")

        /** An unexpected error that is not one of the structured cases above. */
        internal data class Unknown(val reason: String) : EvaluationException("unknown error: $reason")
    }

    /**
     * Evaluates a JSON Logic predicate against a native variable scope.
     *
     * @param predicate The rule predicate as a JSON string.
     * @param variables The resolved variable scope.
     * @return [Result.success] with `true` when the predicate is truthy,
     *  `false` otherwise, or [Result.failure] carrying an [EvaluationException]
     *  when parsing or evaluation fails.
     */
    fun evaluate(
        predicate: String,
        variables: Map<String, Value>,
    ): Result<Boolean> = try {
        Result.success(Evaluator.evaluate(ValueJson.parse(predicate), variables))
    } catch (error: EvaluationException) {
        Result.failure(error)
    } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
        Result.failure(EvaluationException.Unknown(error.message ?: "unknown error"))
    }
}
