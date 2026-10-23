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
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.parents

class ForbiddenPublicConstructor(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "ForbiddenPublicConstructor",
        severity = Severity.Maintainability,
        description = "Public constructors are part of the binary API and cannot change without breaking consumers. " +
            "Make the constructor internal or private, " +
            "or annotate it with @InternalRevenueCatAPI if hybrid SDKs need it.",
        debt = Debt.TWENTY_MINS,
    )

    private val ignoreAnnotated: List<String> = valueOrDefault("ignoreAnnotated", emptyList())

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        val hasImplicitConstructor = klass.primaryConstructor == null && klass.secondaryConstructors.isEmpty()
        if (hasImplicitConstructor && isPublicApi(klass)) {
            report(CodeSmell(issue, Entity.atName(klass), message = "${klass.name}: ${issue.description}"))
        }
    }

    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
        super.visitPrimaryConstructor(constructor)
        checkConstructor(constructor)
    }

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
        super.visitSecondaryConstructor(constructor)
        checkConstructor(constructor)
    }

    private fun checkConstructor(constructor: KtConstructor<*>) {
        val klass = constructor.containingClass() ?: return
        if (!constructor.isHiddenFromPublicApi() && isPublicApi(klass)) {
            report(CodeSmell(issue, Entity.from(constructor), message = "${klass.name}: ${issue.description}"))
        }
    }

    private fun isPublicApi(klass: KtClass): Boolean =
        klass.canExposeConstructor() &&
            !klass.isHiddenFromPublicApi() &&
            klass.parents.filterIsInstance<KtClassOrObject>().none { it.isNonPublic() }

    private fun KtClass.canExposeConstructor(): Boolean {
        if (this is KtEnumEntry || isEnum()) return false
        return !isInterface() && !isAnnotation() && !isSealed()
    }

    private fun KtDeclaration.isHiddenFromPublicApi(): Boolean = isNonPublic() || isAnnotatedWith(ignoreAnnotated)

    private fun KtModifierListOwner.isNonPublic(): Boolean =
        hasModifier(KtTokens.PRIVATE_KEYWORD) ||
            hasModifier(KtTokens.INTERNAL_KEYWORD) ||
            hasModifier(KtTokens.PROTECTED_KEYWORD)

    private fun KtDeclaration.isAnnotatedWith(annotationNames: List<String>): Boolean {
        if (annotationNames.isEmpty()) return false
        return annotationEntries.any { entry ->
            val shortName = entry.shortName?.asString() ?: return@any false
            annotationNames.any { it == shortName || it.endsWith(".$shortName") }
        }
    }
}
