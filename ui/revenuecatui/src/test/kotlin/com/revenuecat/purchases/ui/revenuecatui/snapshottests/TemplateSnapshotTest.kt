package com.revenuecat.purchases.ui.revenuecatui.snapshottests

import coil3.annotation.ExperimentalCoilApi
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.InternalPaywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.PreviewImagesAsPrimaryColor
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

data class TemplateTestConfig(val testConfig: TestConfig, val offering: Offering) {
    override fun toString(): String {
        return "${testConfig.name}_${offering.identifier}"
    }
}

@RunWith(Parameterized::class)
class TemplateSnapshotTest(private val testConfig: TemplateTestConfig): BasePaparazziTest(testConfig.testConfig) {

    companion object {
        private val offeringsToTest = listOf(
            TestData.template1Offering,
            TestData.template2Offering,
            TestData.template3Offering,
            TestData.template4Offering,
            TestData.template5Offering,
            TestData.template7Offering,
            TestData.template7CustomPackageOffering,
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            val templateTestConfigs = mutableListOf<Array<Any>>()
            testConfigs.forEach { testConfig ->
                offeringsToTest.forEach {  offering ->
                    templateTestConfigs.add(arrayOf(TemplateTestConfig(testConfig, offering)))
                }
            }
            return templateTestConfigs
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    @Test
    fun templateFullScreen() {
        screenshotTest {
            PreviewImagesAsPrimaryColor {
                InternalPaywall(
                    options = PaywallOptions.Builder(dismissRequest = {}).build(),
                    viewModel = MockViewModel(offering = testConfig.offering),
                )
            }
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    @Test
    fun templateFooter() {
        screenshotTest {
            PreviewImagesAsPrimaryColor {
                InternalPaywall(
                    options = PaywallOptions.Builder(dismissRequest = {}).build(),
                    viewModel = MockViewModel(mode = PaywallMode.FOOTER, offering = testConfig.offering),
                )
            }
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    @Test
    fun templateCondensedFooter() {
        screenshotTest {
            PreviewImagesAsPrimaryColor {
                InternalPaywall(
                    options = PaywallOptions.Builder(dismissRequest = {}).build(),
                    viewModel = MockViewModel(mode = PaywallMode.FOOTER_CONDENSED, offering = testConfig.offering),
                )
            }
        }
    }
}
