package com.revenuecat.purchases.customercenter

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.EmptyStringToNullSerializer
import com.revenuecat.purchases.paywalls.PaywallColor
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
typealias RCColor = PaywallColor

@OptIn(InternalRevenueCatAPI::class)
@Serializable
internal class CustomerCenterRoot(
    @SerialName("customer_center") val customerCenter: CustomerCenterConfigData,
)

@InternalRevenueCatAPI
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
        enum class VariableName(val identifier: String) {
            PRICE("price"),
            SUB_OFFER_DURATION("sub_offer_duration"),
            SUB_OFFER_DURATION_2("sub_offer_duration_2"),
            SUB_OFFER_PRICE("sub_offer_price"),
            SUB_OFFER_PRICE_2("sub_offer_price_2"),
            ;

            companion object {
                private val valueMap by lazy {
                    values().associateBy { it.identifier }
                }

                fun valueOfIdentifier(identifier: String): VariableName? {
                    return valueMap[identifier]
                }
            }
        }

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

            @SerialName("purchases_not_found")
            PURCHASES_NOT_FOUND,

            @SerialName("purchases_restoring")
            PURCHASES_RESTORING,

            @SerialName("manage_subscription")
            MANAGE_SUBSCRIPTION,

            @SerialName("you_have_promo")
            YOU_HAVE_PROMO,

            @SerialName("you_have_lifetime")
            YOU_HAVE_LIFETIME,

            @SerialName("web_subscription_manage")
            WEB_SUBSCRIPTION_MANAGE,

            @SerialName("free")
            FREE,

            @SerialName("never")
            NEVER,

            @SerialName("free_trial_then_price")
            FREE_TRIAL_THEN_PRICE,

            @SerialName("single_payment_then_price")
            SINGLE_PAYMENT_THEN_PRICE,

            @SerialName("discounted_recurring_then_price")
            DISCOUNTED_RECURRING_THEN_PRICE,

            @SerialName("free_trial_single_payment_then_price")
            FREE_TRIAL_SINGLE_PAYMENT_THEN_PRICE,

            @SerialName("free_trial_discounted_then_price")
            FREE_TRIAL_DISCOUNTED_THEN_PRICE,

            @SerialName("done")
            DONE,

            @SerialName("renews_on_date_for_price")
            RENEWS_ON_DATE_FOR_PRICE,

            @SerialName("renews_on_date")
            RENEWS_ON_DATE,

            @SerialName("purchase_info_expired_on_date")
            PURCHASE_INFO_EXPIRED_ON_DATE,

            @SerialName("purchase_info_expires_on_date")
            PURCHASE_INFO_EXPIRES_ON_DATE,

            @SerialName("active")
            ACTIVE,

            @SerialName("badge_cancelled")
            BADGE_CANCELLED,

            @SerialName("badge_free_trial")
            BADGE_FREE_TRIAL,

            @SerialName("badge_free_trial_cancelled")
            BADGE_FREE_TRIAL_CANCELLED,

            @SerialName("app_store")
            APP_STORE,

            @SerialName("mac_app_store")
            MAC_APP_STORE,

            @SerialName("google_play_store")
            GOOGLE_PLAY_STORE,

            @SerialName("amazon_store")
            AMAZON_STORE,

            @SerialName("web_store")
            WEB_STORE,

            @SerialName("test_store")
            TEST_STORE,

            @SerialName("unknown_store")
            UNKNOWN_STORE,

            @SerialName("card_store_promotional")
            CARD_STORE_PROMOTIONAL,

            @SerialName("resubscribe")
            RESUBSCRIBE,
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
                        "You have an active subscription from the Google Play Store"
                    AMAZON_SUBSCRIPTION_MANAGE ->
                        "You have an active subscription from the Amazon Appstore. " +
                            "You can manage your subscription in the Amazon Appstore app."
                    GOING_TO_CHECK_PURCHASES ->
                        "Let's take a look! We're going to check your account for missing purchases."
                    CHECK_PAST_PURCHASES -> "Check past purchases"
                    PURCHASES_RECOVERED -> "Purchases restored"
                    PURCHASES_RECOVERED_EXPLANATION ->
                        "We restored your past purchases and applied them to your account."
                    PURCHASES_NOT_RECOVERED ->
                        "We could not find any purchases with your account. " +
                            "If you think this is an error, please contact support."
                    PURCHASES_NOT_FOUND -> "No past purchases"
                    PURCHASES_RESTORING -> "Restoring..."
                    MANAGE_SUBSCRIPTION -> "Manage your subscription"
                    YOU_HAVE_PROMO -> "You've been granted a subscription that doesn't renew"
                    YOU_HAVE_LIFETIME -> "Your active lifetime subscription"
                    WEB_SUBSCRIPTION_MANAGE ->
                        "You have an active subscription that was purchased on the web. " +
                            "You can manage your subscription using the button below."
                    FREE -> "Free"
                    NEVER -> "Never"
                    FREE_TRIAL_THEN_PRICE -> "First {{ sub_offer_duration }} free, then {{ price }}"
                    SINGLE_PAYMENT_THEN_PRICE -> "{{ sub_offer_duration }} for {{ sub_offer_price }}, then {{ price }}"
                    DISCOUNTED_RECURRING_THEN_PRICE ->
                        "{{ sub_offer_price }} during {{ sub_offer_duration }}, then {{ price }}"
                    FREE_TRIAL_SINGLE_PAYMENT_THEN_PRICE ->
                        "Try {{ sub_offer_duration }} for free, then {{ sub_offer_duration_2 }} for" +
                            " {{ sub_offer_price_2 }}, and {{ price }} thereafter"
                    FREE_TRIAL_DISCOUNTED_THEN_PRICE ->
                        "Try {{ sub_offer_duration }} for free, then {{ sub_offer_price_2 }} " +
                            "during {{ sub_offer_duration_2 }}, and {{ price }} thereafter"
                    DONE -> "Done"
                    RENEWS_ON_DATE_FOR_PRICE -> "Your next charge is {{ price }} on {{ date }}."
                    RENEWS_ON_DATE -> "Renews on {{ date }}"
                    PURCHASE_INFO_EXPIRED_ON_DATE -> "Expired on {{ date }}"
                    PURCHASE_INFO_EXPIRES_ON_DATE -> "Expires on {{ date }}"
                    ACTIVE -> "Active"
                    BADGE_CANCELLED -> "Cancelled"
                    BADGE_FREE_TRIAL -> "Free Trial"
                    BADGE_FREE_TRIAL_CANCELLED -> "Cancelled Trial"
                    APP_STORE -> "App Store"
                    MAC_APP_STORE -> "Mac App Store"
                    GOOGLE_PLAY_STORE -> "Google Play Store"
                    AMAZON_STORE -> "Amazon Store"
                    WEB_STORE -> "Web"
                    TEST_STORE -> "Test Store"
                    UNKNOWN_STORE -> "Unknown"
                    CARD_STORE_PROMOTIONAL -> "Via Support"
                    RESUBSCRIBE -> "Resubscribe"
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
        val url: String? = null,
        @SerialName("open_method") val openMethod: OpenMethod? = null,
    ) {
        @Serializable
        sealed class PathDetail {
            @Serializable
            data class PromotionalOffer(
                @SerialName("android_offer_id") val androidOfferId: String,
                val eligible: Boolean,
                val title: String,
                val subtitle: String,
                @SerialName("product_mapping") val productMapping: Map<String, String>,
                @SerialName("cross_product_promotions") val crossProductPromotions: Map<String, CrossProductPromotion> =
                    emptyMap(),
            ) : PathDetail() {
                @Deprecated(
                    "Use constructor with crossProductPromotions parameter",
                    ReplaceWith(
                        "PromotionalOffer(androidOfferId, eligible, title, subtitle, productMapping, emptyMap())",
                    ),
                )
                constructor(
                    androidOfferId: String,
                    eligible: Boolean,
                    title: String,
                    subtitle: String,
                    productMapping: Map<String, String>,
                ) : this(androidOfferId, eligible, title, subtitle, productMapping, emptyMap())

                @Deprecated(
                    "Use copy with crossProductPromotions parameter",
                    ReplaceWith(
                        "copy(androidOfferId, eligible, title, subtitle, productMapping, emptyMap())",
                    ),
                )
                fun copy(
                    androidOfferId: String = this.androidOfferId,
                    eligible: Boolean = this.eligible,
                    title: String = this.title,
                    subtitle: String = this.subtitle,
                    productMapping: Map<String, String> = this.productMapping,
                ) = copy(androidOfferId, eligible, title, subtitle, productMapping, emptyMap())

                @Serializable
                @Poko
                class CrossProductPromotion(
                    @SerialName("store_offer_identifier") val storeOfferIdentifier: String,
                    @SerialName("target_product_id") val targetProductId: String,
                )
            }

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
            CUSTOM_URL,
            UNKNOWN,
        }

        @Serializable
        enum class OpenMethod {
            IN_APP,
            EXTERNAL,
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
        @Serializable(with = HelpPathsSerializer::class) val paths: List<HelpPath>,
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
        @Serializable(with = EmptyStringToNullSerializer::class)
        val email: String? = null,
        @SerialName("should_warn_customer_to_update")
        val shouldWarnCustomerToUpdate: Boolean? = null,
    )

    fun getManagementScreen(): CustomerCenterConfigData.Screen? {
        return screens[CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT]
    }

    fun getNoActiveScreen(): CustomerCenterConfigData.Screen? {
        return screens[CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE]
    }
}
