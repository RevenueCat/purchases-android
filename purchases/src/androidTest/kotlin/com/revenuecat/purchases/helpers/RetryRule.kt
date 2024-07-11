package com.revenuecat.purchases.helpers

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class RetryRule(private val retryCount: Int) : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return RetryStatement(base, description, retryCount)
    }

    private class RetryStatement(
        private val base: Statement,
        private val description: Description,
        private val retryCount: Int,
    ) : Statement() {

        override fun evaluate() {
            var caughtThrowable: Throwable? = null

            for (i in 0 until retryCount) {
                try {
                    base.evaluate()
                    return
                } catch (t: Throwable) {
                    caughtThrowable = t
                    println("${description.displayName}: run ${i + 1} failed")
                }
            }
            println("${description.displayName}: giving up after $retryCount failures")
            throw caughtThrowable!!
        }
    }
}
