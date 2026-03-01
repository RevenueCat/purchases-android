package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PurchaseInformationCardViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockLocalization = CustomerCenterConfigTestData.customerCenterData().localization

    @Test
    fun `clickable card triggers callback when clicked`() {
        val onCardClick = mockk<() -> Unit>(relaxed = true)

        composeTestRule.setContent {
            PurchaseInformationCardView(
                purchaseInformation = CustomerCenterConfigTestData.purchaseInformationLifetime,
                localization = mockLocalization,
                isDetailedView = false,
                onCardClick = onCardClick
            )
        }

        composeTestRule.onNode(hasText("Lifetime")).performClick()

        verify { onCardClick() }
    }

    @Test
    fun `non-clickable card does not crash when rendered`() {
        composeTestRule.setContent {
            PurchaseInformationCardView(
                purchaseInformation = CustomerCenterConfigTestData.purchaseInformationLifetime,
                localization = mockLocalization,
                isDetailedView = true,
                onCardClick = null
            )
        }
    }

    @Test
    fun `subscription purchase card with onCardClick is clickable`() {
        val onCardClick = mockk<() -> Unit>(relaxed = true)

        composeTestRule.setContent {
            PurchaseInformationCardView(
                purchaseInformation = CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing,
                localization = mockLocalization,
                isDetailedView = false,
                onCardClick = onCardClick
            )
        }

        composeTestRule.onNode(hasText("Basic")).performClick()
        verify { onCardClick() }
    }

    @Test
    fun `lifetime purchase card with onCardClick is clickable`() {
        val onCardClick = mockk<() -> Unit>(relaxed = true)

        composeTestRule.setContent {
            PurchaseInformationCardView(
                purchaseInformation = CustomerCenterConfigTestData.purchaseInformationLifetime,
                localization = mockLocalization,
                isDetailedView = false,
                onCardClick = onCardClick
            )
        }

        composeTestRule.onAllNodes(hasText("Lifetime"))[0].performClick()
        verify { onCardClick() }
    }

}