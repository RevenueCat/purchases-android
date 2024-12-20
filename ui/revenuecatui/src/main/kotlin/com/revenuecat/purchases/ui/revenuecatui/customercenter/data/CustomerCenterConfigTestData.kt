package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.RCColor

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal object CustomerCenterConfigTestData {

    @SuppressWarnings("LongMethod")
    fun customerCenterData(
        shouldWarnCustomerToUpdate: Boolean = false,
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
                            title = "Didn't receive purchase",
                            type = CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE,
                        ),
                        CustomerCenterConfigData.HelpPath(
                            id = "2",
                            title = "Request a refund",
                            type = CustomerCenterConfigData.HelpPath.PathType.REFUND_REQUEST,
                            promotionalOffer = CustomerCenterConfigData.HelpPath.PathDetail.PromotionalOffer(
                                androidOfferId = "offer_id",
                                eligible = true,
                                title = "title",
                                subtitle = "subtitle",
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
                    ),
                ),
                CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE to CustomerCenterConfigData.Screen(
                    type = CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE,
                    title = "No Active Subscription",
                    subtitle = "You currently have no active subscriptions",
                    paths = listOf(
                        CustomerCenterConfigData.HelpPath(
                            id = "9q9719171o",
                            title = "Check purchases",
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
            ),
        )
    }

    val standardAppearance = CustomerCenterConfigData.Appearance(
        light = CustomerCenterConfigData.Appearance.ColorInformation(
            accentColor = RCColor("#007AFF"),
            textColor = RCColor("#000000"),
            backgroundColor = RCColor("#f5f5f7"),
            buttonTextColor = RCColor("#ffffff"),
            buttonBackgroundColor = RCColor("#287aff"),
        ),
        dark = CustomerCenterConfigData.Appearance.ColorInformation(
            accentColor = RCColor("#007AFF"),
            textColor = RCColor("#ffffff"),
            backgroundColor = RCColor("#000000"),
            buttonTextColor = RCColor("#000000"),
            buttonBackgroundColor = RCColor("#287aff"),
        ),
    )

    val purchaseInformationMonthlyRenewing = PurchaseInformation(
        title = "Basic",
        durationTitle = "1 Month",
        price = "$4.99",
        expirationDateString = "June 1st, 2024",
        willRenew = true,
        active = true,
    )

    val purchaseInformationYearlyExpiring = PurchaseInformation(
        title = "Basic",
        durationTitle = "1 Year",
        price = "$40.99",
        expirationDateString = "June 1st, 2025",
        willRenew = false,
        active = true,
    )
}
