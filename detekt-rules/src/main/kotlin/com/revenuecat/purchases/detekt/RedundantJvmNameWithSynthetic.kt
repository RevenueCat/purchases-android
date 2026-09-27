package com.revenuecat.purchases.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

class RedundantJvmNameWithSynthetic(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "RedundantJvmNameWithSynthetic",
        severity = Severity.Style,
        description = "@JvmName is useless on a declaration already marked @JvmSynthetic, " +
            "because @JvmSynthetic hides the member from Java callers entirely. " +
            "Remove the @JvmName annotation.",
        debt = Debt.FIVE_MINS,
    )

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        checkDeclaration(function)
    }

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        checkDeclaration(property)
    }

    private fun checkDeclaration(declaration: KtDeclaration) {
        val names = declaration.annotationEntries.mapNotNull { it.shortName?.asString() }
        if ("JvmSynthetic" in names && "JvmName" in names) {
            val jvmNameEntry = declaration.annotationEntries.first {
                it.shortName?.asString() == "JvmName"
            }
            report(
                CodeSmell(
                    issue,
                    Entity.from(jvmNameEntry),
                    message = "@JvmName is redundant alongside @JvmSynthetic. Remove it.",
                ),
            )
        }
    }
}
