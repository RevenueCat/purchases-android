package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.RCColor

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal object CustomerCenterConfigTestData {

    fun customerCenterData(
        lastPublishedAppVersion: String? = "1.0.0",
        shouldWarnCustomerToUpdate: Boolean = false
    ): CustomerCenterConfigData {
        return CustomerCenterConfigData(
            screens = mapOf(
                CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT to CustomerCenterConfigData.Screen(
                    type = CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT,
                    title = "Manage Subscription",
                    subtitle = "Manage your subscription details here",
                    paths = listOf(
                        CustomerCenterConfigData.HelpPath(
                            id = "1",
                            title = "Didn't receive purchase",
                            type = CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE
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
                                productMapping = mapOf("monthly" to "offer_id")
                            )
                        ),
                        CustomerCenterConfigData.HelpPath(
                            id = "3",
                            title = "Change plans",
                            type = CustomerCenterConfigData.HelpPath.PathType.CHANGE_PLANS
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
                                        title = "Too expensive"
                                    ),
                                    CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey.Option(
                                        id = "2",
                                        title = "Don't use the app"
                                    ),
                                    CustomerCenterConfigData.HelpPath.PathDetail.FeedbackSurvey.Option(
                                        id = "3",
                                        title = "Bought by mistake"
                                    )
                                )
                            )
                        )
                    )
                ),
                CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE to CustomerCenterConfigData.Screen(
                    type = CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE,
                    title = "No Active Subscription",
                    subtitle = "You currently have no active subscriptions",
                    paths = listOf(
                        CustomerCenterConfigData.HelpPath(
                            id = "9q9719171o",
                            title = "Check purchases",
                            type = CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE
                        )
                    )
                )
            ),
            appearance = standardAppearance,
            localization = CustomerCenterConfigData.Localization(
                locale = "en_US",
                localizedStrings = mapOf(
                    "cancel" to "Cancel",
                    "back" to "Back"
                )
            ),
            support = CustomerCenterConfigData.Support(
                email = "test-support@revenuecat.com",
                shouldWarnCustomerToUpdate = shouldWarnCustomerToUpdate
            )
        )
    }

    val standardAppearance = CustomerCenterConfigData.Appearance(
        light = CustomerCenterConfigData.Appearance.ColorInformation(
            accentColor = RCColor("#007AFF"),
            textColor = RCColor("#000000"),
            backgroundColor = RCColor("#f5f5f7"),
            buttonTextColor = RCColor("#ffffff"),
            buttonBackgroundColor = RCColor("#287aff")
        ),
        dark = CustomerCenterConfigData.Appearance.ColorInformation(
            accentColor = RCColor("#007AFF"),
            textColor = RCColor("#ffffff"),
            backgroundColor = RCColor("#000000"),
            buttonTextColor = RCColor("#000000"),
            buttonBackgroundColor = RCColor("#287aff")
        )
    )

    // We'll need to create a PurchaseInformation class for Android
    data class PurchaseInformation(
        val title: String,
        val durationTitle: String,
        val explanation: Explanation,
        val price: Price,
        val expirationOrRenewal: ExpirationOrRenewal,
        val productIdentifier: String,
        val store: Store
    ) {
        enum class Explanation {
            EARLIEST_RENEWAL
        }

        sealed class Price {
            data class Paid(val amount: String) : Price()
        }

        data class ExpirationOrRenewal(
            val label: Label,
            val date: Date
        ) {
            enum class Label {
                NEXT_BILLING_DATE,
                EXPIRES
            }

            sealed class Date {
                data class FormattedDate(val value: String) : Date()
            }
        }

        enum class Store {
            PLAY_STORE
        }
    }

    val subscriptionInformationMonthlyRenewing = PurchaseInformation(
        title = "Basic",
        durationTitle = "Monthly",
        explanation = PurchaseInformation.Explanation.EARLIEST_RENEWAL,
        price = PurchaseInformation.Price.Paid("$4.99"),
        expirationOrRenewal = PurchaseInformation.ExpirationOrRenewal(
            label = PurchaseInformation.ExpirationOrRenewal.Label.NEXT_BILLING_DATE,
            date = PurchaseInformation.ExpirationOrRenewal.Date.FormattedDate("June 1st, 2024")
        ),
        productIdentifier = "product_id",
        store = PurchaseInformation.Store.PLAY_STORE
    )

    val subscriptionInformationYearlyExpiring = PurchaseInformation(
        title = "Basic",
        durationTitle = "Yearly",
        explanation = PurchaseInformation.Explanation.EARLIEST_RENEWAL,
        price = PurchaseInformation.Price.Paid("$49.99"),
        expirationOrRenewal = PurchaseInformation.ExpirationOrRenewal(
            label = PurchaseInformation.ExpirationOrRenewal.Label.EXPIRES,
            date = PurchaseInformation.ExpirationOrRenewal.Date.FormattedDate("June 1st, 2024")
        ),
        productIdentifier = "product_id",
        store = PurchaseInformation.Store.PLAY_STORE
    )
}