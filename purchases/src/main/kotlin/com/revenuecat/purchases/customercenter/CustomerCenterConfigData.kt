package com.revenuecat.purchases.customercenter

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.paywalls.EmptyStringToNullSerializer
import com.revenuecat.purchases.paywalls.PaywallColor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias RCColor = PaywallColor

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Serializable
internal class CustomerCenterRoot(
    @SerialName("customer_center") val customerCenter: CustomerCenterConfigData,
)

@ExperimentalPreviewRevenueCatPurchasesAPI
@Serializable
data class CustomerCenterConfigData(
    @Serializable(with = ScreenMapSerializer::class) val screens: Map<Screen.ScreenType, Screen>,
    val appearance: Appearance,
    val localization: Localization,
    val support: Support,
    @SerialName("last_published_app_version")
    @Serializable(with = EmptyStringToNullSerializer::class)
    val lastPublishedAppVersion: String? = null,
) {
    @Serializable
    data class Localization(
        val locale: String,
        @SerialName("localized_strings") val localizedStrings: Map<String, String>,
    ) {
        @Serializable
        enum class CommonLocalizedString {
            @SerialName("no_thanks")
            NO_THANKS,

            @SerialName("no_subscriptions_found")
            NO_SUBSCRIPTIONS_FOUND,

            @SerialName("try_check_restore")
            TRY_CHECK_RESTORE,

            @SerialName("restore_purchases")
            RESTORE_PURCHASES,

            @SerialName("cancel")
            CANCEL,

            @SerialName("billing_cycle")
            BILLING_CYCLE,

            @SerialName("current_price")
            CURRENT_PRICE,

            @SerialName("expired")
            EXPIRED,

            @SerialName("expires")
            EXPIRES,

            @SerialName("next_billing_date")
            NEXT_BILLING_DATE,

            @SerialName("refund_canceled")
            REFUND_CANCELED,

            @SerialName("refund_error_generic")
            REFUND_ERROR_GENERIC,

            @SerialName("refund_granted")
            REFUND_GRANTED,

            @SerialName("refund_status")
            REFUND_STATUS,

            @SerialName("sub_earliest_expiration")
            SUB_EARLIEST_EXPIRATION,

            @SerialName("sub_earliest_renewal")
            SUB_EARLIEST_RENEWAL,

            @SerialName("sub_expired")
            SUB_EXPIRED,

            @SerialName("contact_support")
            CONTACT_SUPPORT,

            @SerialName("default_body")
            DEFAULT_BODY,

            @SerialName("default_subject")
            DEFAULT_SUBJECT,

            @SerialName("dismiss")
            DISMISS,

            @SerialName("update_warning_title")
            UPDATE_WARNING_TITLE,

            @SerialName("update_warning_description")
            UPDATE_WARNING_DESCRIPTION,

            @SerialName("update_warning_update")
            UPDATE_WARNING_UPDATE,

            @SerialName("update_warning_ignore")
            UPDATE_WARNING_IGNORE,

            @SerialName("please_contact_support")
            PLEASE_CONTACT_SUPPORT,

            @SerialName("apple_subscription_manage")
            APPLE_SUBSCRIPTION_MANAGE,

            @SerialName("google_subscription_manage")
            GOOGLE_SUBSCRIPTION_MANAGE,

            @SerialName("amazon_subscription_manage")
            AMAZON_SUBSCRIPTION_MANAGE,

            @SerialName("platform_mismatch")
            PLATFORM_MISMATCH,

            @SerialName("going_to_check_purchases")
            GOING_TO_CHECK_PURCHASES,

            @SerialName("check_past_purchases")
            CHECK_PAST_PURCHASES,

            @SerialName("purchases_recovered")
            PURCHASES_RECOVERED,

            @SerialName("purchases_recovered_explanation")
            PURCHASES_RECOVERED_EXPLANATION,

            @SerialName("purchases_not_recovered")
            PURCHASES_NOT_RECOVERED,
            ;

            val defaultValue: String
                get() = when (this) {
                    NO_THANKS -> "No, thanks"
                    NO_SUBSCRIPTIONS_FOUND -> "No Subscriptions found"
                    TRY_CHECK_RESTORE -> "We can try checking your Apple account for any previous purchases"
                    RESTORE_PURCHASES -> "Restore purchases"
                    CANCEL -> "Cancel"
                    BILLING_CYCLE -> "Billing cycle"
                    CURRENT_PRICE -> "Current price"
                    EXPIRED -> "Expired"
                    EXPIRES -> "Expires"
                    NEXT_BILLING_DATE -> "Next billing date"
                    REFUND_CANCELED -> "Refund canceled"
                    REFUND_ERROR_GENERIC -> "An error occurred while processing the refund request. Please try again."
                    REFUND_GRANTED -> "Refund granted successfully!"
                    REFUND_STATUS -> "Refund status"
                    SUB_EARLIEST_EXPIRATION -> "This is your subscription with the earliest expiration date."
                    SUB_EARLIEST_RENEWAL -> "This is your subscription with the earliest billing date."
                    SUB_EXPIRED -> "This subscription has expired."
                    CONTACT_SUPPORT -> "Contact support"
                    DEFAULT_BODY -> "Please describe your issue or question."
                    DEFAULT_SUBJECT -> "Support Request"
                    DISMISS -> "Dismiss"
                    UPDATE_WARNING_TITLE -> "Update available"
                    UPDATE_WARNING_DESCRIPTION ->
                        "Downloading the latest version of the app may help solve the problem."
                    UPDATE_WARNING_UPDATE -> "Update"
                    UPDATE_WARNING_IGNORE -> "Continue"
                    PLATFORM_MISMATCH -> "Platform mismatch"
                    PLEASE_CONTACT_SUPPORT -> "Please contact support to manage your subscription."
                    APPLE_SUBSCRIPTION_MANAGE ->
                        "You can manage your subscription by using the App Store app on an Apple device."
                    GOOGLE_SUBSCRIPTION_MANAGE ->
                        "You can manage your subscription by using the Play Store app on an Android device"
                    AMAZON_SUBSCRIPTION_MANAGE ->
                        "You can manage your subscription in the Amazon Appstore app on an Amazon device."
                    GOING_TO_CHECK_PURCHASES ->
                        "Let’s take a look! We’re going to check your account for missing purchases."
                    CHECK_PAST_PURCHASES -> "Check past purchases"
                    PURCHASES_RECOVERED -> "Purchases recovered!"
                    PURCHASES_RECOVERED_EXPLANATION ->
                        "We applied the previously purchased items to your account. Sorry for the inconvenience."
                    PURCHASES_NOT_RECOVERED ->
                        "We couldn't find any additional purchases under this account. " +
                            "Contact support for assistance if you think this is an error."
                }
        }

        fun commonLocalizedString(key: CommonLocalizedString): String {
            return localizedStrings[key.name.lowercase()] ?: key.defaultValue
        }
    }

    @Serializable
    data class HelpPath(
        val id: String,
        val title: String,
        val type: PathType,
        @SerialName("promotional_offer") val promotionalOffer: PathDetail.PromotionalOffer? = null,
        @SerialName("feedback_survey") val feedbackSurvey: PathDetail.FeedbackSurvey? = null,
    ) {
        @Serializable
        sealed class PathDetail {
            @Serializable
            data class PromotionalOffer(
                // WIP: Rename this field name to a new android id
                @SerialName("ios_offer_id") val androidOfferId: String,
                val eligible: Boolean,
                val title: String,
                val subtitle: String,
            ) : PathDetail()

            @Serializable
            data class FeedbackSurvey(
                val title: String,
                val options: List<Option>,
            ) : PathDetail() {
                @Serializable
                data class Option(
                    val id: String,
                    val title: String,
                    @SerialName("promotional_offer") val promotionalOffer: PromotionalOffer? = null,
                )
            }
        }

        @Serializable
        enum class PathType {
            MISSING_PURCHASE,
            REFUND_REQUEST,
            CHANGE_PLANS,
            CANCEL,
            UNKNOWN,
        }
    }

    @Serializable
    data class Appearance(
        val light: ColorInformation? = null,
        val dark: ColorInformation? = null,
    ) {
        @Serializable
        data class ColorInformation(
            @SerialName("accent_color") @Serializable(with = PaywallColor.Serializer::class)
            val accentColor: RCColor? = null,
            @SerialName("text_color") @Serializable(with = PaywallColor.Serializer::class)
            val textColor: RCColor? = null,
            @SerialName("background_color") @Serializable(with = PaywallColor.Serializer::class)
            val backgroundColor: RCColor? = null,
            @SerialName("button_text_color") @Serializable(with = PaywallColor.Serializer::class)
            val buttonTextColor: RCColor? = null,
            @SerialName("button_background_color") @Serializable(with = PaywallColor.Serializer::class)
            val buttonBackgroundColor: RCColor? = null,
        )
    }

    @Serializable
    data class Screen(
        val type: ScreenType,
        val title: String,
        @Serializable(with = EmptyStringToNullSerializer::class) val subtitle: String? = null,
        val paths: List<HelpPath>,
    ) {
        @Serializable
        enum class ScreenType {
            MANAGEMENT,
            NO_ACTIVE,
            UNKNOWN,
        }
    }

    @Serializable
    data class Support(
        @Serializable(with = EmptyStringToNullSerializer::class) val email: String? = null,
    )
}
