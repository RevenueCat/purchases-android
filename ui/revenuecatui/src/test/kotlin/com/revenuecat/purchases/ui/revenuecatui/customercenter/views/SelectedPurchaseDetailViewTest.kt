package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.ExpirationOrRenewal
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PriceDetails
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SelectedPurchaseDetailViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testData = CustomerCenterConfigTestData.customerCenterData()
    private val localization = testData.localization
    private val managementScreen =
        testData.screens[CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT]!!

    private val contactSupportText = localization.commonLocalizedString(
        CustomerCenterConfigData.Localization.CommonLocalizedString.CONTACT_SUPPORT,
    )

    // Play Store purchase: Contact Support never shown, so any button = Create Ticket
    private val playStorePurchase = CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing

    // Non-Play-Store purchase: Contact Support can show when Create Ticket doesn't
    private val appStorePurchase = PurchaseInformation(
        title = "Basic",
        pricePaid = PriceDetails.Paid("$4.99"),
        expirationOrRenewal = ExpirationOrRenewal.Renewal("June 1st, 2024"),
        store = Store.APP_STORE,
        managementURL = Uri.parse("https://apps.apple.com/account/subscriptions"),
        product = null,
        isSubscription = true,
        isExpired = false,
        isTrial = false,
        isCancelled = false,
        isLifetime = false,
    )

    private fun supportTickets(
        allowCreation: Boolean,
        customerType: CustomerCenterConfigData.Support.SupportTickets.CustomerType,
    ) = CustomerCenterConfigData.Support.SupportTickets(
        allowCreation = allowCreation,
        customerType = customerType,
    )

    private fun renderView(
        purchase: PurchaseInformation = playStorePurchase,
        tickets: CustomerCenterConfigData.Support.SupportTickets,
        hasActiveSubscriptions: Boolean = true,
    ) {
        composeTestRule.setContent {
            SelectedPurchaseDetailView(
                contactEmail = testData.support.email,
                localization = localization,
                purchaseInformation = purchase,
                supportedPaths = managementScreen.paths,
                supportTickets = tickets,
                hasActiveSubscriptions = hasActiveSubscriptions,
                onAction = {},
            )
        }
    }

    // --- Contact Support ---

    @Test
    fun `contact support not shown for Play Store purchase`() {
        renderView(
            purchase = playStorePurchase,
            tickets = supportTickets(allowCreation = false, customerType = CustomerCenterConfigData.Support.SupportTickets.CustomerType.ALL),
        )

        composeTestRule.onNodeWithText(contactSupportText).assertDoesNotExist()
    }

    @Test
    fun `contact support shown for non Play Store purchase when create ticket is disabled`() {
        renderView(
            purchase = appStorePurchase,
            tickets = supportTickets(allowCreation = false, customerType = CustomerCenterConfigData.Support.SupportTickets.CustomerType.ALL),
        )

        composeTestRule.onNodeWithText(contactSupportText).assertExists()
    }

    @Test
    fun `contact support not shown when email is null`() {
        composeTestRule.setContent {
            SelectedPurchaseDetailView(
                contactEmail = null,
                localization = localization,
                purchaseInformation = appStorePurchase,
                supportedPaths = managementScreen.paths,
                supportTickets = supportTickets(
                    allowCreation = false,
                    customerType = CustomerCenterConfigData.Support.SupportTickets.CustomerType.ALL,
                ),
                hasActiveSubscriptions = true,
                onAction = {},
            )
        }

        composeTestRule.onNodeWithText(contactSupportText).assertDoesNotExist()
    }

    // --- Create Ticket: customerType ---
    // Using Play Store purchase isolates the Create Ticket path (Contact Support is always gated
    // by store, so any "Contact support" button on a Play Store purchase = Create Ticket button).

    @Test
    fun `create ticket shown when customerType ALL regardless of subscription state`() {
        renderView(
            tickets = supportTickets(
                allowCreation = true,
                customerType = CustomerCenterConfigData.Support.SupportTickets.CustomerType.ALL,
            ),
            hasActiveSubscriptions = false,
        )

        composeTestRule.onNodeWithText(contactSupportText).assertExists()
    }

    @Test
    fun `create ticket shown when customerType ACTIVE and user has active subscriptions`() {
        renderView(
            tickets = supportTickets(
                allowCreation = true,
                customerType = CustomerCenterConfigData.Support.SupportTickets.CustomerType.ACTIVE,
            ),
            hasActiveSubscriptions = true,
        )

        composeTestRule.onNodeWithText(contactSupportText).assertExists()
    }

    @Test
    fun `create ticket not shown when customerType ACTIVE and user has no active subscriptions`() {
        renderView(
            tickets = supportTickets(
                allowCreation = true,
                customerType = CustomerCenterConfigData.Support.SupportTickets.CustomerType.ACTIVE,
            ),
            hasActiveSubscriptions = false,
        )

        composeTestRule.onNodeWithText(contactSupportText).assertDoesNotExist()
    }

    @Test
    fun `create ticket shown when customerType NOT_ACTIVE and user has no active subscriptions`() {
        renderView(
            tickets = supportTickets(
                allowCreation = true,
                customerType = CustomerCenterConfigData.Support.SupportTickets.CustomerType.NOT_ACTIVE,
            ),
            hasActiveSubscriptions = false,
        )

        composeTestRule.onNodeWithText(contactSupportText).assertExists()
    }

    @Test
    fun `create ticket not shown when customerType NOT_ACTIVE and user has active subscriptions`() {
        renderView(
            tickets = supportTickets(
                allowCreation = true,
                customerType = CustomerCenterConfigData.Support.SupportTickets.CustomerType.NOT_ACTIVE,
            ),
            hasActiveSubscriptions = true,
        )

        composeTestRule.onNodeWithText(contactSupportText).assertDoesNotExist()
    }

    @Test
    fun `create ticket not shown when customerType NONE`() {
        renderView(
            tickets = supportTickets(
                allowCreation = true,
                customerType = CustomerCenterConfigData.Support.SupportTickets.CustomerType.NONE,
            ),
        )

        composeTestRule.onNodeWithText(contactSupportText).assertDoesNotExist()
    }

    @Test
    fun `create ticket not shown when allowCreation false`() {
        renderView(
            tickets = supportTickets(
                allowCreation = false,
                customerType = CustomerCenterConfigData.Support.SupportTickets.CustomerType.ALL,
            ),
        )

        composeTestRule.onNodeWithText(contactSupportText).assertDoesNotExist()
    }

    // --- Mutual exclusivity ---

    @Test
    fun `create ticket takes priority over contact support for non Play Store purchase`() {
        // Both create ticket and contact support conditions are met, but only one button should show.
        renderView(
            purchase = appStorePurchase,
            tickets = supportTickets(
                allowCreation = true,
                customerType = CustomerCenterConfigData.Support.SupportTickets.CustomerType.ALL,
            ),
            hasActiveSubscriptions = true,
        )

        // Exactly one "Contact support" button — the Create Ticket variant
        val nodes = composeTestRule.onAllNodesWithText(contactSupportText).fetchSemanticsNodes()
        assert(nodes.size == 1) {
            "Expected exactly one 'Contact support' button, found ${nodes.size}"
        }
    }
}
