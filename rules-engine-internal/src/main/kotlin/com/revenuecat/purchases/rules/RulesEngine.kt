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
     * Evaluates a JSON Logic predicate against a native variable scope.
     *
     * @param predicate The rule predicate as a JSON string, extracted from
     *  the SDK artifact.
     * @param variables The resolved variable scope, built natively by the SDK.
     * @return [Result.success] with `true` when the predicate is truthy,
     *  `false` otherwise, or [Result.failure] carrying a [RuleError] when
     *  parsing or evaluation fails.
     */
    public fun evaluate(
        predicate: String,
        variables: Map<String, Value>,
    ): Result<Boolean> = runCatching {
        Evaluator.evaluate(ValueJson.parse(predicate), variables)
    }
}
