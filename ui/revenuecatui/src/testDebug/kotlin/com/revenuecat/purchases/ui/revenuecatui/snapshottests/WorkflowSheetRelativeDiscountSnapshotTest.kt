package com.revenuecat.purchases.ui.revenuecatui.snapshottests

import com.revenuecat.purchases.ui.revenuecatui.components.WorkflowDiscountPaywallPreview
import com.revenuecat.purchases.ui.revenuecatui.components.WorkflowDiscountPlansSheetPreview
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class WorkflowSheetRelativeDiscountSnapshotTest(testConfig: TestConfig) : BasePaparazziTest(testConfig) {

    @Test
    fun annualDiscountIncludesPlansInClosedSheet() {
        screenshotTest { WorkflowDiscountPaywallPreview() }
    }

    @Test
    fun openSheetShowsDiscountsForEachPlan() {
        screenshotTest { WorkflowDiscountPlansSheetPreview() }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = testConfigs
            .filter { it.name == "pixel6" }
            .map { arrayOf<Any>(it) }
    }
}
