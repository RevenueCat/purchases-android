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
     *
     * Note on missing variables: the evaluator does **not** raise an error
     * for them — per the JSON Logic spec, they resolve to `null` and a warning
     * is logged instead.
     */
    internal sealed class EvaluationException(message: String) : Exception(message) {

        /** The predicate JSON could not be parsed. */
        internal data class Parse(val reason: String) : EvaluationException("failed to parse predicate JSON: $reason")

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
     * Applies a JSON Logic transformation predicate to a variable scope.
     *
     * The predicate is evaluated against [variables] and must produce an
     * object, which becomes the new scope.
     *
     * @param predicate The transformation predicate as a JSON string.
     * @param variables The raw variable scope.
     * @return [Result.success] with the transformed scope, or [Result.failure]
     *  carrying an [EvaluationException] when parsing or evaluation fails, or
     *  when the predicate evaluates to anything other than an object.
     */
    fun transform(
        predicate: String,
        variables: Map<String, Value>,
    ): Result<Map<String, Value>> = evaluated(predicate, variables).mapCatching { result ->
        (result as? Value.ObjectValue)?.entries
            ?: throw EvaluationException.TypeMismatch(
                "transformation predicate expected to produce an object, got $result",
            )
    }

    /**
     * Evaluates a JSON Logic predicate against a native variable scope.
     *
     * @param predicate The evaluation predicate as a JSON string.
     * @param variables The variable scope.
     * @return [Result.success] with `true` when the predicate is truthy,
     *  `false` otherwise, or [Result.failure] carrying an [EvaluationException]
     *  when parsing or evaluation fails.
     */
    fun evaluate(
        predicate: String,
        variables: Map<String, Value>,
    ): Result<Boolean> = evaluated(predicate, variables).map { it.isTruthy }

    /**
     * Parses [predicate] and evaluates it against [variables], mapping any
     * thrown error into a [Result.failure].
     */
    private fun evaluated(
        predicate: String,
        variables: Map<String, Value>,
    ): Result<Value> = try {
        Result.success(Evaluator.evaluate(ValueJson.parse(predicate), variables))
    } catch (error: EvaluationException) {
        Result.failure(error)
    } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
        Result.failure(EvaluationException.Unknown(error.message ?: "unknown error"))
    }
}
