package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import android.net.Uri
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.RCColor
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrency

@Suppress("MagicNumber")
internal object CustomerCenterConfigTestData {

    @SuppressWarnings("LongMethod")
    fun customerCenterData(
        shouldWarnCustomerToUpdate: Boolean = false,
        allowSupportTicketCreation: Boolean = false,
    ): CustomerCenterConfigData {
        return CustomerCenterConfigData(
            screens = mapOf(
                CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT to CustomerCenterConfigData.Screen(
                    type = CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT,
                    title = "Manage Subscription",
                    subtitle = null,
                    paths = listOf(
                        CustomerCenterConfigData.HelpPath(
                            id = "1",
                            title = "Check for previous purchases",
                            type = CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE,
                        ),
                        CustomerCenterConfigData.HelpPath(
                            id = "2",
                            title = "Request a refund",
                            type = CustomerCenterConfigData.HelpPath.PathType.REFUND_REQUEST,
                            promotionalOffer = CustomerCenterConfigData.HelpPath.PathDetail.PromotionalOffer(
                                androidOfferId = "offer_id",
                                eligible = true,
                                title = "Wait a minute...",
                                subtitle = "Before you cancel, please consider accepting this one time offer",
                                productMapping = mapOf("monthly" to "offer_id"),
                            ),
                        ),
                        CustomerCenterConfigData.HelpPath(
                            id = "3",
                            title = "Change plans",
                            type = CustomerCenterConfigData.HelpPath.PathType.CHANGE_PLANS,
                        ),
                        CustomerCenterConfigData.HelpPath(
                            id = "4",
                            title = "Cancel subscription",
                            type = CustomerCenterConfigData.HelpPath.PathType.CANCEL,
                            feedbackSurvey = CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey(
                                title = "Why are you cancelling?",
                                options = listOf(
                                    CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey.Option(
                                        id = "1",
                                        title = "Too expensive",
                                        promotionalOffer =
                                        CustomerCenterConfigData.HelpPath.PathDetail.PromotionalOffer(
                                            androidOfferId = "offer_id",
                                            eligible = true,
                                            title = "Wait a minute...",
                                            subtitle = "Before you cancel, please consider accepting this offer",
                                            productMapping = mapOf("monthly" to "offer_id"),
                                        ),
                                    ),
                                    CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey.Option(
                                        id = "2",
                                        title = "Don't use the app",
                                    ),
                                    CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey.Option(
                                        id = "3",
                                        title = "Bought by mistake",
                                    ),
                                ),
                            ),
                        ),
                        CustomerCenterConfigData.HelpPath(
                            id = "5",
                            title = "FAQ",
                            type = CustomerCenterConfigData.HelpPath.PathType.CUSTOM_URL,
                            url = "https://www.revenuecat.com/docs/customer-center-faq",
                        ),
                    ),
                ),
                CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE to CustomerCenterConfigData.Screen(
                    type = CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE,
                    title = "No subscriptions found",
                    subtitle = "We can try checking your account for any previous purchases",
                    paths = listOf(
                        CustomerCenterConfigData.HelpPath(
                            id = "9q9719171o",
                            title = "Check for previous purchases",
                            type = CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE,
                        ),
                    ),
                ),
            ),
            appearance = standardAppearance,
            localization = CustomerCenterConfigData.Localization(
                locale = "en_US",
                localizedStrings = mapOf(
                    "cancel" to "Cancel",
                    "back" to "Back",
                ),
            ),
            support = CustomerCenterConfigData.Support(
                email = "test-support@revenuecat.com",
                shouldWarnCustomerToUpdate = shouldWarnCustomerToUpdate,
                supportTickets = CustomerCenterConfigData.Support.SupportTickets(
                    allowCreation = allowSupportTicketCreation,
                    customerDetails = CustomerCenterConfigData.Support.SupportTickets.CustomerDetails(
                        activeEntitlements = false,
                        appUserId = false,
                        attConsent = false,
                        country = false,
                        deviceVersion = false,
                        email = false,
                        facebookAnonId = false,
                        idfa = false,
                        idfv = false,
                        ip = false,
                        lastOpened = false,
                        lastSeenAppVersion = false,
                        totalSpent = false,
                        userSince = false,
                    ),
                    customerType = CustomerCenterConfigData.Support.SupportTickets.CustomerType.NOT_ACTIVE,
                ),
            ),
        )
    }

    val standardAppearance = CustomerCenterConfigData.Appearance(
        light = CustomerCenterConfigData.Appearance.ColorInformation(
            accentColor = RCColor("#007AFF"),
            textColor = RCColor("#000000"),
            backgroundColor = RCColor("#f5f5f7"),
            buttonTextColor = RCColor("#7A0000"),
            buttonBackgroundColor = RCColor("#287aff"),
        ),
        dark = CustomerCenterConfigData.Appearance.ColorInformation(
            accentColor = RCColor("#FFFFFF"),
            textColor = RCColor("#FFFFFF"),
            backgroundColor = RCColor("#A96800"),
            buttonTextColor = RCColor("#FF2600"),
            buttonBackgroundColor = RCColor("#000000"),
        ),
    )

    val purchaseInformationMonthlyRenewing = PurchaseInformation(
        title = "Basic",
        pricePaid = PriceDetails.Paid("$4.99"),
        expirationOrRenewal = ExpirationOrRenewal.Renewal("June 1st, 2024"),
        store = Store.PLAY_STORE,
        managementURL = Uri.parse("https://play.google.com/store/account/subscriptions"),
        product = TestStoreProduct(
            "monthly_product_id",
            "Basic",
            "title",
            "description",
            Price("$4.99", 4_990_000, "US"),
            Period(1, Period.Unit.MONTH, "P1M"),
        ),
        isSubscription = true,
        isExpired = false,
        isTrial = false,
        isCancelled = false,
        isLifetime = false,
    )

    val purchaseInformationYearlyExpiring = PurchaseInformation(
        title = "Basic",
        pricePaid = PriceDetails.Paid("$40.99"),
        expirationOrRenewal = ExpirationOrRenewal.Expiration("June 1st, 2024"),
        store = Store.PLAY_STORE,
        managementURL = Uri.parse("https://play.google.com/store/account/subscriptions"),
        product = TestStoreProduct(
            "yearly_product_id",
            "Basic",
            "title",
            "description",
            Price("$40.99", 40_990_000, "US"),
            Period(1, Period.Unit.YEAR, "P1Y"),
        ),
        isSubscription = true,
        isExpired = false,
        isTrial = false,
        isCancelled = true,
        isLifetime = false,
    )

    val purchaseInformationYearlyExpired = PurchaseInformation(
        title = "Basic",
        pricePaid = PriceDetails.Paid("$40.99"),
        expirationOrRenewal = ExpirationOrRenewal.Expiration("June 1st, 2024"),
        store = Store.PLAY_STORE,
        managementURL = Uri.parse("https://play.google.com/store/account/subscriptions"),
        product = TestStoreProduct(
            "yearly_product_id",
            "Basic",
            "title",
            "description",
            Price("$40.99", 40_990_000, "US"),
            Period(1, Period.Unit.YEAR, "P1Y"),
        ),
        isSubscription = true,
        isExpired = true,
        isTrial = false,
        isCancelled = true,
        isLifetime = false,
    )

    val purchaseInformationLifetime = PurchaseInformation(
        title = "Lifetime",
        pricePaid = PriceDetails.Paid("$100.99"),
        expirationOrRenewal = null,
        store = Store.APP_STORE,
        managementURL = Uri.parse("https://play.google.com/store/account/subscriptions"),
        product = null,
        isSubscription = false,
        isExpired = false,
        isTrial = false,
        isCancelled = false,
        isLifetime = true,
    )

    val purchaseInformationFreeTrial = PurchaseInformation(
        title = "Premium",
        pricePaid = PriceDetails.Free,
        expirationOrRenewal = ExpirationOrRenewal.Expiration("June 15th, 2024"),
        store = Store.PLAY_STORE,
        managementURL = Uri.parse("https://play.google.com/store/account/subscriptions"),
        product = TestStoreProduct(
            "premium_yearly_product_id",
            "Premium",
            "title",
            "description",
            Price("$59.99", 59_990_000, "US"),
            Period(1, Period.Unit.YEAR, "P1Y"),
        ),
        isSubscription = true,
        isExpired = false,
        isTrial = true,
        isCancelled = false,
        isLifetime = false,
    )

    val purchaseInformationPromotional = PurchaseInformation(
        title = "Entitlement",
        pricePaid = PriceDetails.Free,
        expirationOrRenewal = ExpirationOrRenewal.Expiration("October 25th, 2025"),
        store = Store.PROMOTIONAL,
        managementURL = null,
        product = null,
        isSubscription = false,
        isExpired = false,
        isTrial = false,
        isCancelled = true,
        isLifetime = true,
    )

    val purchaseInformationPromotionalLifetime = PurchaseInformation(
        title = "Entitlement",
        pricePaid = PriceDetails.Free,
        expirationOrRenewal = ExpirationOrRenewal.Expiration("September 6th, 2225"),
        store = Store.PROMOTIONAL,
        managementURL = null,
        product = null,
        isSubscription = false,
        isExpired = false,
        isTrial = false,
        isCancelled = true,
        isLifetime = true,
    )

    val fourVirtualCurrencies = VirtualCurrencies(
        all = mapOf(
            "GLD" to VirtualCurrency(
                balance = 100,
                name = "Gold",
                code = "GLD",
                serverDescription = "It's gold",
            ),
            "SLV" to VirtualCurrency(
                balance = 200,
                name = "Silver",
                code = "SLV",
                serverDescription = "It's silver",
            ),
            "BRNZ" to VirtualCurrency(
                balance = 300,
                name = "Bronze",
                code = "BRNZ",
                serverDescription = "It's bronze",
            ),
            "PLTNM" to VirtualCurrency(
                balance = 400,
                name = "Platinum",
                code = "PLTNM",
                serverDescription = "It's platinum",
            ),
        ),
    )

    val fiveVirtualCurrencies = VirtualCurrencies(
        all = mapOf(
            "GLD" to VirtualCurrency(
                balance = 100,
                name = "Gold",
                code = "GLD",
                serverDescription = "It's gold",
            ),
            "SLV" to VirtualCurrency(
                balance = 200,
                name = "Silver",
                code = "SLV",
                serverDescription = "It's silver",
            ),
            "BRNZ" to VirtualCurrency(
                balance = 300,
                name = "Bronze",
                code = "BRNZ",
                serverDescription = "It's bronze",
            ),
            "PLTNM" to VirtualCurrency(
                balance = 400,
                name = "Platinum",
                code = "PLTNM",
                serverDescription = "It's platinum",
            ),
            "RC_COIN" to VirtualCurrency(
                balance = 1,
                name = "RC Coin",
                code = "RC_COIN",
                serverDescription = "RevenueCat Coin",
            ),
        ),
    )
}
