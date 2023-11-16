package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.TestTag
import com.revenuecat.purchases.ui.revenuecatui.mocks.FakeViewModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
class PaywallUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: FakeViewModel

    @Before
    fun setUp() {
        viewModel = FakeViewModel(offering = TestData.template2Offering)
    }

    @Test
    fun purchasePackageClickCallsCorrectMethods() {
        setUpUI()

        assertThat(viewModel.purchaseSelectedPackageCallCount).isEqualTo(0)

        composeTestRule.onNodeWithTag(TestTag.PURCHASE_BUTTON_TAG).performClick()

        assertThat(viewModel.purchaseSelectedPackageCallCount).isEqualTo(1)
        assertThat(viewModel.purchaseSelectedPackageParams[0]).isNotNull
    }

    @Test
    fun purchaseSelectPackageClickCallsCorrectMethods() {
        setUpUI()

        assertThat(viewModel.selectPackageCallCount).isEqualTo(0)

        val packageId = PackageType.ANNUAL.identifier!!
        composeTestRule.onNodeWithTag(TestTag.selectButtonTestTag(packageId)).performClick()

        assertThat(viewModel.selectPackageCallCount).isEqualTo(1)
        assertThat(viewModel.selectPackageCallParams[0].rcPackage.identifier).isEqualTo(packageId)
    }

    private fun setUpUI() {
        composeTestRule.setContent {
            InternalPaywall(
                options = PaywallOptions.Builder { Assert.fail("Should not be dismissed") }.build(),
                viewModel = viewModel,
            )
        }
    }
}
