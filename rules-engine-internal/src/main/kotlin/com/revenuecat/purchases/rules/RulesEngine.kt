@file:Suppress("ForbiddenPublicSealedClass", "ForbiddenPublicDataClass")

package com.revenuecat.purchases.rules

/** Namespace for the RevenueCat rules engine. */
public object RulesEngine {
    @Volatile
    private var _logger: RulesEngineLogger = PrintLogger

    internal val logger: RulesEngineLogger
        get() = _logger

    @Synchronized
    public fun setLogger(logger: RulesEngineLogger) {
        _logger = logger
    }

    /**
     * Errors surfaced by the rules engine.
     *
     * Note on missing variables: the evaluator does **not** raise an error
     * for them — per the JSON Logic spec, they resolve to `null` and a warning
     * is logged instead.
     */
    public sealed class EvaluationError(message: String) : RuntimeException(message) {

        /** The predicate JSON could not be parsed. */
        public data class Parse(val reason: String) : EvaluationError("failed to parse predicate JSON: $reason")

        /**
         * An operator was given arguments of the wrong shape (e.g. wrong arity)
         * or types that cannot be reconciled.
         */
        public data class TypeMismatch(val detail: String) : EvaluationError("type mismatch: $detail")

        /**
         * The predicate references a JSON Logic operator the engine does not
         * implement. Carries the operator name so callers can decide policy
         * (default-deny, log, etc.).
         */
        public data class UnsupportedOperator(val name: String) : EvaluationError("unsupported operator: $name")

        /** An unexpected error that is not one of the structured cases above. */
        public data class Unknown(val reason: String) : EvaluationError("unknown error: $reason")
    }

    /**
     * Evaluates a JSON Logic predicate against a native variable scope.
     *
     * @param predicate The rule predicate as a JSON string.
     * @param variables The resolved variable scope.
     * @return [Result.success] with `true` when the predicate is truthy,
     *  `false` otherwise, or [Result.failure] carrying an [EvaluationError]
     *  when parsing or evaluation fails.
     */
    public fun evaluate(
        predicate: String,
        variables: Map<String, Value>,
    ): Result<Boolean> = try {
        Result.success(Evaluator.evaluate(ValueJson.parse(predicate), variables))
    } catch (error: EvaluationError) {
        Result.failure(error)
    } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
        Result.failure(EvaluationError.Unknown(error.message ?: "unknown error"))
    }
}
