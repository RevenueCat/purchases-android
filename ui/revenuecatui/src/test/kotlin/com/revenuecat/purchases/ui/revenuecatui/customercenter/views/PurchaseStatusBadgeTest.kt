package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PriceDetails
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.TransactionDetails
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PurchaseStatusBadgeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockLocalization = mockk<CustomerCenterConfigData.Localization> {
        io.mockk.every { 
            commonLocalizedString(CustomerCenterConfigData.Localization.CommonLocalizedString.BADGE_LIFETIME) 
        } returns "Lifetime"
        io.mockk.every { 
            commonLocalizedString(CustomerCenterConfigData.Localization.CommonLocalizedString.ACTIVE) 
        } returns "Active"
        io.mockk.every { 
            commonLocalizedString(CustomerCenterConfigData.Localization.CommonLocalizedString.EXPIRED) 
        } returns "Expired"
        io.mockk.every { 
            commonLocalizedString(CustomerCenterConfigData.Localization.CommonLocalizedString.BADGE_FREE_TRIAL) 
        } returns "Free Trial"
        io.mockk.every { 
            commonLocalizedString(CustomerCenterConfigData.Localization.CommonLocalizedString.BADGE_CANCELLED) 
        } returns "Cancelled"
        io.mockk.every { 
            commonLocalizedString(CustomerCenterConfigData.Localization.CommonLocalizedString.BADGE_FREE_TRIAL_CANCELLED) 
        } returns "Cancelled Trial"
    }

    @Test
    fun `displays lifetime badge for lifetime purchase`() {
        composeTestRule.setContent {
            PurchaseStatusBadge(
                purchaseInformation = CustomerCenterConfigTestData.purchaseInformationLifetime,
                localization = mockLocalization
            )
        }

        composeTestRule.onNode(hasText("Lifetime")).assertIsDisplayed()
    }

    @Test
    fun `displays active badge for regular subscription`() {
        composeTestRule.setContent {
            PurchaseStatusBadge(
                purchaseInformation = CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing,
                localization = mockLocalization
            )
        }

        composeTestRule.onNode(hasText("Active")).assertIsDisplayed()
    }
}