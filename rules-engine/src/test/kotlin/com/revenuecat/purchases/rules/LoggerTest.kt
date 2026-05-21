package com.revenuecat.purchases.rules

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class LoggerTest {

    @Test
    fun `CapturingLogger records warnings in order`() {
        val logger = CapturingLogger()
        logger.warn("first")
        logger.warn("second")
        assertThat(logger.warnings).containsExactly("first", "second")
    }

    @Test
    fun `PrintlnLogger does not crash`() {
        // Smoke test: just make sure the default logger is callable. We
        // can't easily intercept stderr, but the goal here is to catch
        // crashes / mis-typed format strings rather than verify content.
        PrintlnLogger.warn("smoke")
    }

    @Test
    fun `Rules logger swap is visible to subsequent reads`() {
        val previous = Rules.logger
        val capturing = CapturingLogger()
        Rules.logger = capturing
        try {
            Rules.logger.warn("hello")
            Rules.logger.warn("world")
            assertThat(capturing.warnings).containsExactly("hello", "world")
        } finally {
            Rules.logger = previous
        }
        assertThat(Rules.logger).isSameAs(previous)
    }
}
