package com.revenuecat.purchases.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.parents

class RedundantInternalApiAnnotation(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "RedundantInternalApiAnnotation",
        severity = Severity.Style,
        description = "Member is annotated with @InternalRevenueCatAPI but an enclosing " +
            "type already has that annotation. The opt-in propagates to members, so " +
            "the inner annotation is redundant.",
        debt = Debt.FIVE_MINS,
    )

    private val targetAnnotation: String = valueOrDefault("annotationName", "InternalRevenueCatAPI")

    @Suppress("ReturnCount")
    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
        super.visitAnnotationEntry(annotationEntry)
        val shortName = annotationEntry.shortName?.asString() ?: return
        if (shortName != targetAnnotation) return

        val annotatedElement = annotationEntry.parent?.parent ?: return

        for (enclosing in annotationEntry.parents.filterIsInstance<KtClassOrObject>()) {
            if (enclosing === annotatedElement) continue
            if (hasAnnotation(enclosing, targetAnnotation)) {
                report(
                    CodeSmell(
                        issue,
                        Entity.from(annotationEntry),
                        "@$targetAnnotation is redundant: enclosing ${enclosing.name} already has it.",
                    ),
                )
                return
            }
        }
    }

    private fun hasAnnotation(classOrObject: KtClassOrObject, name: String): Boolean =
        classOrObject.annotationEntries.any { it.shortName?.asString() == name }
}
