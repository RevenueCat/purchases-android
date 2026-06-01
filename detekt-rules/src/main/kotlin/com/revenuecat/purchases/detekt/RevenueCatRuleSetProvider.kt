package com.revenuecat.purchases.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class RevenueCatRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "revenuecat"

    override fun instance(config: Config): RuleSet = RuleSet(
        ruleSetId,
        listOf(ForbiddenPublicSealedClass(config)),
    )
}
