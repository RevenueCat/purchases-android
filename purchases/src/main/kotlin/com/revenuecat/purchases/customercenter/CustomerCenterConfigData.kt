package com.revenuecat.purchases.customercenter

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.EmptyStringToNullSerializer
import com.revenuecat.purchases.paywalls.PaywallColor
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
public typealias RCColor = PaywallColor

@OptIn(InternalRevenueCatAPI::class)
@Serializable
internal class CustomerCenterRoot(
    @SerialName("customer_center") public val customerCenter: CustomerCenterConfigData,
)

@InternalRevenueCatAPI
@Serializable
public data class CustomerCenterConfigData(
    @Serializable(with = ScreenMapSerializer::class) public val screens: Map<Screen.ScreenType, Screen>,
    public val appearance: Appearance,
    public val localization: Localization,
    public val support: Support,
    @SerialName("last_published_app_version")
    @Serializable(with = EmptyStringToNullSerializer::class)
    public val lastPublishedAppVersion: String? = null,
) {
    @Serializable
    public data class Localization(
        public val locale: String,
        @SerialName("localized_strings") public val localizedStrings: Map<String, String>,
    ) {
        public enum class VariableName(public val identifier: String) {
            PRICE("price"),
            SUB_OFFER_DURATION("sub_offer_duration"),
            SUB_OFFER_DURATION_2("sub_offer_duration_2"),
            SUB_OFFER_PRICE("sub_offer_price"),
            SUB_OFFER_PRICE_2("sub_offer_price_2"),
            DISCOUNTED_RECURRING_PAYMENT_PRICE_PER_PERIOD("discounted_recurring_payment_price_per_period"),
            DISCOUNTED_RECURRING_PAYMENT_CYCLES("discounted_recurring_payment_cycles"),
            ;

            public companion object {
                private val valueMap by lazy {
                    values().associateBy { it.identifier }
                }

                public fun valueOfIdentifier(identifier: String): VariableName? {
                    return valueMap[identifier]
                }
            }
        }

        @Serializable
        public enum class CommonLocalizedString {
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

            @SerialName("discounted_recurring_payment_then_price")
            DISCOUNTED_RECURRING_PAYMENT_THEN_PRICE,

            @SerialName("free_trial_discounted_recurring_payment_then_price")
            FREE_TRIAL_DISCOUNTED_RECURRING_PAYMENT_THEN_PRICE,

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

            @SerialName("badge_lifetime")
            BADGE_LIFETIME,

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

            @SerialName("unknown_store")
            UNKNOWN_STORE,

            @SerialName("test_store")
            TEST_STORE,

            @SerialName("card_store_promotional")
            CARD_STORE_PROMOTIONAL,

            @SerialName("resubscribe")
            RESUBSCRIBE,

            @SerialName("type_subscription")
            TYPE_SUBSCRIPTION,

            @SerialName("type_one_time_purchase")
            TYPE_ONE_TIME_PURCHASE,

            @SerialName("buy_subscription")
            BUY_SUBSCRIPTION,

            @SerialName("last_charge_was")
            LAST_CHARGE_WAS,

            @SerialName("next_billing_date_on")
            NEXT_BILLING_DATE_ON,

            @SerialName("see_all_virtual_currencies")
            SEE_ALL_VIRTUAL_CURRENCIES,

            @SerialName("virtual_currency_balances_screen_header")
            VIRTUAL_CURRENCY_BALANCES_SCREEN_HEADER,

            @SerialName("no_virtual_currency_balances_found")
            NO_VIRTUAL_CURRENCY_BALANCES_FOUND,

            @SerialName("support_ticket_create")
            SUPPORT_TICKET_CREATE,

            @SerialName("email")
            EMAIL,

            @SerialName("enter_email")
            ENTER_EMAIL,

            @SerialName("description")
            DESCRIPTION,

            @SerialName("sent")
            SENT,

            @SerialName("support_ticket_failed")
            SUPPORT_TICKET_FAILED,

            @SerialName("submit_ticket")
            SUBMIT_TICKET,

            @SerialName("invalid_email_error")
            INVALID_EMAIL_ERROR,

            @SerialName("characters_remaining")
            CHARACTERS_REMAINING,
            ;

            public val defaultValue: String
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
                        "Try {{ sub_offer_duration }} for free, " +
                            "then {{ sub_offer_price_2 }} during {{ sub_offer_duration_2 }}, " +
                            "and {{ price }} thereafter"
                    DISCOUNTED_RECURRING_PAYMENT_THEN_PRICE ->
                        "{{ discounted_recurring_payment_price_per_period }} for " +
                            "{{ discounted_recurring_payment_cycles }} periods, " +
                            "then {{ price }}"
                    FREE_TRIAL_DISCOUNTED_RECURRING_PAYMENT_THEN_PRICE ->
                        "Try {{ sub_offer_duration }} for free, " +
                            "then {{ discounted_recurring_payment_price_per_period }} for " +
                            "{{ discounted_recurring_payment_cycles }} periods, and {{ price }} thereafter"
                    DONE -> "Done"
                    RENEWS_ON_DATE_FOR_PRICE -> "Your next charge is {{ price }} on {{ date }}."
                    RENEWS_ON_DATE -> "Renews on {{ date }}"
                    PURCHASE_INFO_EXPIRED_ON_DATE -> "Expired on {{ date }}"
                    PURCHASE_INFO_EXPIRES_ON_DATE -> "Expires on {{ date }}"
                    ACTIVE -> "Active"
                    BADGE_CANCELLED -> "Cancelled"
                    BADGE_FREE_TRIAL -> "Free Trial"
                    BADGE_FREE_TRIAL_CANCELLED -> "Cancelled Trial"
                    BADGE_LIFETIME -> "Lifetime"
                    APP_STORE -> "App Store"
                    MAC_APP_STORE -> "Mac App Store"
                    GOOGLE_PLAY_STORE -> "Google Play Store"
                    AMAZON_STORE -> "Amazon Store"
                    WEB_STORE -> "Web"
                    UNKNOWN_STORE -> "Unknown"
                    TEST_STORE -> "Test Store"
                    CARD_STORE_PROMOTIONAL -> "Via Support"
                    RESUBSCRIBE -> "Resubscribe"
                    TYPE_SUBSCRIPTION -> "Subscription"
                    TYPE_ONE_TIME_PURCHASE -> "One time purchase"
                    BUY_SUBSCRIPTION -> "Buy Subscription"
                    LAST_CHARGE_WAS -> "Last charge: {{ price }}"
                    NEXT_BILLING_DATE_ON -> "Next billing date: {{ date }}"
                    SEE_ALL_VIRTUAL_CURRENCIES -> "See all in-app currencies"
                    VIRTUAL_CURRENCY_BALANCES_SCREEN_HEADER -> "In-App Currencies"
                    NO_VIRTUAL_CURRENCY_BALANCES_FOUND -> "It doesn't look like you've purchased any in-app currencies."
                    SUPPORT_TICKET_CREATE -> "Create a support ticket"
                    EMAIL -> "Email"
                    ENTER_EMAIL -> "Enter your email"
                    DESCRIPTION -> "Description"
                    SENT -> "Message sent"
                    SUPPORT_TICKET_FAILED -> "Failed to send, please try again."
                    SUBMIT_TICKET -> "Submit ticket"
                    INVALID_EMAIL_ERROR -> "Please enter a valid email address"
                    CHARACTERS_REMAINING -> "{{ count }} characters"
                }
        }

        public fun commonLocalizedString(key: CommonLocalizedString): String {
            return localizedStrings[key.name.lowercase()] ?: key.defaultValue
        }
    }

    @Serializable
    public data class HelpPath(
        public val id: String,
        public val title: String,
        public val type: PathType,
        @SerialName("promotional_offer") public val promotionalOffer: PathDetail.PromotionalOffer? = null,
        @SerialName("feedback_survey") public val feedbackSurvey: PathDetail.FeedbackSurvey? = null,
        public val url: String? = null,
        @SerialName("open_method") public val openMethod: OpenMethod? = null,
        @SerialName("action_identifier") public val actionIdentifier: String? = null,
    ) {
        @Serializable
        public sealed class PathDetail {
            @Serializable
            public data class PromotionalOffer(
                @SerialName("android_offer_id") public val androidOfferId: String,
                public val eligible: Boolean,
                public val title: String,
                public val subtitle: String,
                @SerialName("product_mapping") public val productMapping: Map<String, String>,
                @SerialName("cross_product_promotions") public val crossProductPromotions: Map<String, CrossProductPromotion> =
                    emptyMap(),
            ) : PathDetail() {
                @Deprecated(
                    "Use constructor with crossProductPromotions parameter",
                    ReplaceWith(
                        "PromotionalOffer(androidOfferId, eligible, title, subtitle, productMapping, emptyMap())",
                    ),
                )
                public constructor(
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
                public fun copy(
                    androidOfferId: String = this.androidOfferId,
                    eligible: Boolean = this.eligible,
                    title: String = this.title,
                    subtitle: String = this.subtitle,
                    productMapping: Map<String, String> = this.productMapping,
                ): PromotionalOffer = copy(androidOfferId, eligible, title, subtitle, productMapping, emptyMap())

                @Serializable
                @Poko
                public class CrossProductPromotion(
                    @SerialName("store_offer_identifier") public val storeOfferIdentifier: String,
                    @SerialName("target_product_id") public val targetProductId: String,
                )
            }

            @Serializable
            public data class FeedbackSurvey(
                public val title: String,
                public val options: List<Option>,
            ) : PathDetail() {
                @Serializable
                public data class Option(
                    public val id: String,
                    public val title: String,
                    @SerialName("promotional_offer") public val promotionalOffer: PromotionalOffer? = null,
                )
            }
        }

        @Serializable
        public enum class PathType {
            MISSING_PURCHASE,
            REFUND_REQUEST,
            CHANGE_PLANS,
            CANCEL,
            CUSTOM_URL,
            CUSTOM_ACTION,
            UNKNOWN,
        }

        @Serializable
        public enum class OpenMethod {
            IN_APP,
            EXTERNAL,
        }
    }

    @Serializable
    public data class Appearance(
        public val light: ColorInformation? = null,
        public val dark: ColorInformation? = null,
    ) {
        @Serializable
        public data class ColorInformation(
            @SerialName("accent_color") @Serializable(with = PaywallColor.Serializer::class)
            public val accentColor: RCColor? = null,
            @SerialName("text_color") @Serializable(with = PaywallColor.Serializer::class)
            public val textColor: RCColor? = null,
            @SerialName("background_color") @Serializable(with = PaywallColor.Serializer::class)
            public val backgroundColor: RCColor? = null,
            @SerialName("button_text_color") @Serializable(with = PaywallColor.Serializer::class)
            public val buttonTextColor: RCColor? = null,
            @SerialName("button_background_color") @Serializable(with = PaywallColor.Serializer::class)
            public val buttonBackgroundColor: RCColor? = null,
        )
    }

    @Serializable
    public data class ScreenOffering(
        public val type: ScreenOfferingType,
        @SerialName("offering_id") public val offeringId: String? = null,
        @SerialName("button_text") public val buttonText: String? = null,
    ) {
        @Serializable
        public enum class ScreenOfferingType(public val value: String) {
            @SerialName("CURRENT")
            CURRENT("CURRENT"),

            @SerialName("SPECIFIC")
            SPECIFIC("SPECIFIC"),
        }
    }

    @Serializable
    public data class Screen(
        public val type: ScreenType,
        public val title: String,
        @Serializable(with = EmptyStringToNullSerializer::class) public val subtitle: String? = null,
        @Serializable(with = HelpPathsSerializer::class) public val paths: List<HelpPath>,
        public val offering: ScreenOffering? = null,
    ) {
        @Serializable
        public enum class ScreenType {
            MANAGEMENT,
            NO_ACTIVE,
            UNKNOWN,
        }
    }

    @Serializable
    public data class Support(
        @Serializable(with = EmptyStringToNullSerializer::class)
        public val email: String? = null,
        @SerialName("should_warn_customer_to_update")
        public val shouldWarnCustomerToUpdate: Boolean? = null,
        @SerialName("display_virtual_currencies")
        public val displayVirtualCurrencies: Boolean? = null,
        @SerialName("support_tickets")
        public val supportTickets: SupportTickets = SupportTickets(),
    ) {
        @Serializable
        public data class SupportTickets(
            @SerialName("allow_creation")
            public val allowCreation: Boolean = false,
            @SerialName("customer_details")
            public val customerDetails: CustomerDetails = CustomerDetails(),
            @SerialName("customer_type")
            public val customerType: CustomerType = CustomerType.NOT_ACTIVE,
        ) {
            @Serializable
            public enum class CustomerType {
                @SerialName("not_active")
                NOT_ACTIVE,

                @SerialName("none")
                NONE,

                @SerialName("all")
                ALL,

                @SerialName("active")
                ACTIVE,
            }

            @Serializable
            public data class CustomerDetails(
                @SerialName("active_entitlements")
                public val activeEntitlements: Boolean = false,
                @SerialName("app_user_id")
                public val appUserId: Boolean = false,
                @SerialName("att_consent")
                public val attConsent: Boolean = false,
                public val country: Boolean = false,
                @SerialName("device_version")
                public val deviceVersion: Boolean = false,
                public val email: Boolean = false,
                @SerialName("facebook_anon_id")
                public val facebookAnonId: Boolean = false,
                public val idfa: Boolean = false,
                public val idfv: Boolean = false,
                public val ip: Boolean = false,
                @SerialName("last_opened")
                public val lastOpened: Boolean = false,
                @SerialName("last_seen_app_version")
                public val lastSeenAppVersion: Boolean = false,
                @SerialName("total_spent")
                public val totalSpent: Boolean = false,
                @SerialName("user_since")
                public val userSince: Boolean = false,
            )

            public fun allowsActiveCustomers(): Boolean {
                return customerType == CustomerCenterConfigData.Support.SupportTickets.CustomerType.ALL ||
                    customerType == CustomerCenterConfigData.Support.SupportTickets.CustomerType.ACTIVE
            }

            public fun allowsNonActiveCustomers(): Boolean {
                return customerType == CustomerCenterConfigData.Support.SupportTickets.CustomerType.ALL ||
                    customerType == CustomerCenterConfigData.Support.SupportTickets.CustomerType.NOT_ACTIVE
            }
        }
    }

    public fun getManagementScreen(): CustomerCenterConfigData.Screen? {
        return screens[CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT]
    }

    public fun getNoActiveScreen(): CustomerCenterConfigData.Screen? {
        return screens[CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE]
    }
}
