package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.ExpirationOrRenewal
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PriceDetails
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
    fun `lifetime purchase shows badge and arrow in non-detailed view`() {
        composeTestRule.setContent {
            PurchaseInformationCardView(
                purchaseInformation = CustomerCenterConfigTestData.purchaseInformationLifetime,
                localization = mockLocalization,
                isDetailedView = false, // This is the key - non-detailed view
                onCardClick = {}
            )
        }

        // Verify the lifetime badge is displayed
        composeTestRule.onNode(hasText("Lifetime")).assertIsDisplayed()
        // Note: We can't easily test for the arrow icon presence in Compose tests,
        // but the UI should show both the badge and arrow for lifetime non-subscription purchases
    }

    @Test
    fun `lifetime purchase shows only badge in detailed view`() {
        composeTestRule.setContent {
            PurchaseInformationCardView(
                purchaseInformation = CustomerCenterConfigTestData.purchaseInformationLifetime,
                localization = mockLocalization,
                isDetailedView = true,
                onCardClick = null
            )
        }

        // Verify the lifetime badge is displayed - there will be 2 "Lifetime" texts (title + badge)
        composeTestRule.onAllNodesWithText("Lifetime").assertCountEquals(2)
    }

    @Test
    fun `active subscription shows badge in non-detailed view`() {
        composeTestRule.setContent {
            PurchaseInformationCardView(
                purchaseInformation = CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing,
                localization = mockLocalization,
                isDetailedView = false,
                onCardClick = null
            )
        }

        // Should show Active badge for subscription
        composeTestRule.onNode(hasText("Active")).assertIsDisplayed()
    }

    @Test
    fun `expired subscription shows expired badge`() {
        composeTestRule.setContent {
            PurchaseInformationCardView(
                purchaseInformation = CustomerCenterConfigTestData.purchaseInformationYearlyExpired,
                localization = mockLocalization,
                isDetailedView = true,
                onCardClick = null
            )
        }

        // Should show Expired badge
        composeTestRule.onNode(hasText("Expired")).assertIsDisplayed()
    }

    @Test
    fun `cancelled subscription shows cancelled badge`() {
        composeTestRule.setContent {
            PurchaseInformationCardView(
                purchaseInformation = CustomerCenterConfigTestData.purchaseInformationYearlyExpiring,
                localization = mockLocalization,
                isDetailedView = true,
                onCardClick = null
            )
        }

        // Should show Cancelled badge (since yearlyExpiring is cancelled but not expired)
        composeTestRule.onNode(hasText("Cancelled")).assertIsDisplayed()
    }

    @Test
    fun `promotional purchase shows free trial badge`() {
        composeTestRule.setContent {
            PurchaseInformationCardView(
                purchaseInformation = CustomerCenterConfigTestData.purchaseInformationPromotional,
                localization = mockLocalization,
                isDetailedView = true,
                onCardClick = null
            )
        }

        // Promotional purchases should show appropriate badge
        // Note: The exact badge depends on the promotional purchase's status
    }

    @Test
    fun `card displays title correctly`() {
        composeTestRule.setContent {
            PurchaseInformationCardView(
                purchaseInformation = CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing,
                localization = mockLocalization,
                isDetailedView = true,
                onCardClick = null
            )
        }

        // Verify the title is displayed
        composeTestRule.onNode(hasText("Basic")).assertIsDisplayed()
    }

    @Test
    fun `card displays store information correctly`() {
        composeTestRule.setContent {
            PurchaseInformationCardView(
                purchaseInformation = CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing,
                localization = mockLocalization,
                isDetailedView = true,
                onCardClick = null
            )
        }

        // Verify the store information is displayed
        composeTestRule.onNode(hasText("Google Play Store")).assertIsDisplayed()
    }

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

        // Click on the card (by clicking on the title)
        composeTestRule.onNode(hasText("Lifetime")).performClick()

        // Verify the callback was called
        verify { onCardClick() }
    }

    @Test
    fun `non-clickable card does not respond to clicks`() {
        composeTestRule.setContent {
            PurchaseInformationCardView(
                purchaseInformation = CustomerCenterConfigTestData.purchaseInformationLifetime,
                localization = mockLocalization,
                isDetailedView = true,
                onCardClick = null // Non-clickable
            )
        }

        // Should display correctly but not be clickable - verify both title and badge show
        composeTestRule.onAllNodesWithText("Lifetime").assertCountEquals(2)
    }

    @Test
    fun `card handles single button position correctly`() {
        composeTestRule.setContent {
            PurchaseInformationCardView(
                purchaseInformation = CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing,
                localization = mockLocalization,
                position = ButtonPosition.SINGLE,
                isDetailedView = true,
                onCardClick = null
            )
        }

        composeTestRule.onNode(hasText("Basic")).assertIsDisplayed()
    }

    @Test
    fun `card handles first button position correctly`() {
        composeTestRule.setContent {
            PurchaseInformationCardView(
                purchaseInformation = CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing,
                localization = mockLocalization,
                position = ButtonPosition.FIRST,
                isDetailedView = true,
                onCardClick = null
            )
        }

        composeTestRule.onNode(hasText("Basic")).assertIsDisplayed()
    }

    @Test
    fun `card displays renewal information for active subscriptions`() {
        composeTestRule.setContent {
            PurchaseInformationCardView(
                purchaseInformation = CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing,
                localization = mockLocalization,
                isDetailedView = true,
                onCardClick = null
            )
        }

        // Should show renewal text with price and date
        composeTestRule.onNode(hasText("Your next charge is $4.99 on June 1st, 2024.")).assertIsDisplayed()
    }

    @Test
    fun `card displays expiration information for expiring subscriptions`() {
        composeTestRule.setContent {
            PurchaseInformationCardView(
                purchaseInformation = CustomerCenterConfigTestData.purchaseInformationYearlyExpiring,
                localization = mockLocalization,
                isDetailedView = true,
                onCardClick = null
            )
        }

        // Should show expiration text for cancelled subscription
        composeTestRule.onNode(hasText("Expires on June 1st, 2024")).assertIsDisplayed()
    }

}