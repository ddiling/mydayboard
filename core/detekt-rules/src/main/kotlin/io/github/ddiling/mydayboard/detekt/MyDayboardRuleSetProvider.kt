package io.github.ddiling.mydayboard.detekt

import dev.detekt.api.RuleName
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

/** Registers the project's own detekt rules under the `mydayboard` rule set id. */
class MyDayboardRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("mydayboard")

    override fun instance(): RuleSet = RuleSet(ruleSetId, mapOf(RuleName("NoAnyType") to ::NoAnyType))
}
