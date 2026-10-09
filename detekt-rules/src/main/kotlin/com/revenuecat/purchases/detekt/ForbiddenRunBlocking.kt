package com.revenuecat.purchases.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class ForbiddenRunBlocking(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "ForbiddenRunBlocking",
        severity = Severity.Defect,
        description = "runBlocking can cause ANRs when called on the main thread. " +
            "Use suspend functions or callback-based APIs instead.",
        debt = Debt.TWENTY_MINS,
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val callee = expression.calleeExpression
        if (callee is KtNameReferenceExpression && callee.getReferencedName() == "runBlocking") {
            report(CodeSmell(issue, Entity.from(expression), issue.description))
        }
    }
}
