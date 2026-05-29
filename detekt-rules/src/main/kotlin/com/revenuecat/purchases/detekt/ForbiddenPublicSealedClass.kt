package com.revenuecat.purchases.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.psiUtil.parents

class ForbiddenPublicSealedClass(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "ForbiddenPublicSealedClass",
        severity = Severity.Maintainability,
        description = "Sealed classes and interfaces are bad for binary compatibility in public APIs. " +
            "Adding new subclasses breaks exhaustive when expressions in consumers. " +
            "Annotate with @InternalRevenueCatAPI if intentional.",
        debt = Debt.TWENTY_MINS,
    )

    private val ignoreAnnotated: List<String> = valueOrDefault("ignoreAnnotated", emptyList())

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        if (klass.isSealed() && isPublicApi(klass) && !isAnnotatedWith(klass, ignoreAnnotated)) {
            report(CodeSmell(issue, Entity.atName(klass), message = "${klass.name}: ${issue.description}"))
        }
    }

    private fun isPublicApi(klass: KtClass): Boolean {
        if (klass.isNonPublic()) return false
        return klass.parents.filterIsInstance<KtClassOrObject>().none { it.isNonPublic() }
    }

    private fun KtModifierListOwner.isNonPublic(): Boolean =
        hasModifier(KtTokens.PRIVATE_KEYWORD) ||
            hasModifier(KtTokens.INTERNAL_KEYWORD) ||
            hasModifier(KtTokens.PROTECTED_KEYWORD)

    private fun isAnnotatedWith(klass: KtClass, annotationNames: List<String>): Boolean {
        if (annotationNames.isEmpty()) return false
        return klass.annotationEntries.any { entry ->
            val shortName = entry.shortName?.asString() ?: return@any false
            annotationNames.any { it == shortName || it.endsWith(".$shortName") }
        }
    }
}
