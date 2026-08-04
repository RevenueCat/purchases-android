package com.revenuecat.purchases.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.psiUtil.parents

class SuspendFunSwallowsCancellation(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "SuspendFunSwallowsCancellation",
        severity = Severity.Defect,
        description = "Catching Exception/Throwable in a suspend function without rethrowing " +
            "CancellationException breaks structured concurrency.",
        debt = Debt.TWENTY_MINS,
    )

    private val broadTypes = setOf("Exception", "Throwable")

    override fun visitTryExpression(expression: KtTryExpression) {
        super.visitTryExpression(expression)

        val enclosingSuspendFun = expression.parents
            .filterIsInstance<KtNamedFunction>()
            .firstOrNull { it.hasModifier(KtTokens.SUSPEND_KEYWORD) }
            ?: return

        val clauses = expression.catchClauses
        val hasCancellationCatch = clauses.any { clause ->
            clause.catchParameter?.typeReference?.text.orEmpty().contains("CancellationException")
        }
        if (hasCancellationCatch) return

        clauses
            .filter { it.catchParameter?.typeReference?.text in broadTypes }
            .filterNot { mentionsCancellation(it.catchBody?.text.orEmpty()) }
            .forEach { clause ->
                val caughtType = clause.catchParameter?.typeReference?.text
                report(
                    CodeSmell(
                        issue,
                        Entity.from(clause),
                        message = "catch($caughtType) in suspend fun " +
                            "'${enclosingSuspendFun.name}' may swallow CancellationException. " +
                            "Add: if (e is CancellationException) throw e",
                    ),
                )
            }
    }

    private fun mentionsCancellation(text: String): Boolean =
        text.contains("CancellationException") ||
            text.contains("kotlinx.coroutines.CancellationException") ||
            text.contains("ensureActive")
}
