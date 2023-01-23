package com.revenuecat.purchases

enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARN,
    ERROR;

    /**
     * Whether debug logs are enabled
     */
    val debugLogsEnabled: Boolean
        get() = this <= DEBUG

    companion object {
        /**
         * Creates a LogLevel from a Boolean.
         */
        fun debugLogsEnabled(enabled: Boolean): LogLevel {
            return if (enabled) {
                DEBUG
            } else {
                INFO
            }
        }
    }
}
