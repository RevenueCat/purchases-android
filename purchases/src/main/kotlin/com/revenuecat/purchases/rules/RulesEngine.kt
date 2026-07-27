package com.revenuecat.purchases.rules

/** Namespace for the RevenueCat rules engine. */
internal object RulesEngine {
    @Volatile
    private var _logger: RulesEngineLogger = PrintLogger

    internal val logger: RulesEngineLogger
        get() = _logger

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
