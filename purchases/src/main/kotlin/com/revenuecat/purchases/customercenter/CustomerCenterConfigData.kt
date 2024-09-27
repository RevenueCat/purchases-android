package com.revenuecat.purchases.customercenter

import com.revenuecat.purchases.paywalls.PaywallColor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias RCColor = PaywallColor

private const val HASH_CODE_MULTIPLIER = 31

@Serializable
internal class CustomerCenterRoot(
    @SerialName("customer_center") val customerCenter: CustomerCenterConfigData,
)

@Serializable
class CustomerCenterConfigData(
    @Serializable(with = ScreenMapSerializer::class) val screens: Map<Screen.ScreenType, Screen>,
    val appearance: Appearance,
    val localization: Localization,
    val support: Support,
    @SerialName("last_published_app_version") val lastPublishedAppVersion: String? = null,
) {
    @Serializable
    class Localization(
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
                }
        }

        fun commonLocalizedString(key: CommonLocalizedString): String {
            return localizedStrings[key.name.lowercase()] ?: key.defaultValue
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Localization

            if (locale != other.locale) return false
            if (localizedStrings != other.localizedStrings) return false

            return true
        }

        override fun hashCode(): Int {
            var result = locale.hashCode()
            result = HASH_CODE_MULTIPLIER * result + localizedStrings.hashCode()
            return result
        }
    }

    @Serializable
    class HelpPath(
        val id: String,
        val title: String,
        val type: PathType,
        @SerialName("promotional_offer") val promotionalOffer: PathDetail.PromotionalOffer? = null,
        @SerialName("feedback_survey") val feedbackSurvey: PathDetail.FeedbackSurvey? = null,
    ) {
        @Serializable
        sealed class PathDetail {
            @Serializable
            class PromotionalOffer(
                // WIP: Rename this field name to a new android id
                @SerialName("ios_offer_id") val androidOfferId: String,
                val eligible: Boolean,
                val title: String,
                val subtitle: String,
            ) : PathDetail() {
                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (javaClass != other?.javaClass) return false

                    other as PromotionalOffer

                    if (androidOfferId != other.androidOfferId) return false
                    if (eligible != other.eligible) return false
                    if (title != other.title) return false
                    if (subtitle != other.subtitle) return false

                    return true
                }

                override fun hashCode(): Int {
                    var result = androidOfferId.hashCode()
                    result = HASH_CODE_MULTIPLIER * result + eligible.hashCode()
                    result = HASH_CODE_MULTIPLIER * result + title.hashCode()
                    result = HASH_CODE_MULTIPLIER * result + subtitle.hashCode()
                    return result
                }
            }

            @Serializable
            class FeedbackSurvey(
                val title: String,
                val options: List<Option>,
            ) : PathDetail() {
                @Serializable
                class Option(
                    val id: String,
                    val title: String,
                    @SerialName("promotional_offer") val promotionalOffer: PromotionalOffer? = null,
                ) {
                    override fun equals(other: Any?): Boolean {
                        if (this === other) return true
                        if (javaClass != other?.javaClass) return false

                        other as Option

                        if (id != other.id) return false
                        if (title != other.title) return false
                        if (promotionalOffer != other.promotionalOffer) return false

                        return true
                    }

                    override fun hashCode(): Int {
                        var result = id.hashCode()
                        result = 31 * result + title.hashCode()
                        result = 31 * result + (promotionalOffer?.hashCode() ?: 0)
                        return result
                    }
                }

                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (javaClass != other?.javaClass) return false

                    other as FeedbackSurvey

                    if (title != other.title) return false
                    if (options != other.options) return false

                    return true
                }

                override fun hashCode(): Int {
                    var result = title.hashCode()
                    result = HASH_CODE_MULTIPLIER * result + options.hashCode()
                    return result
                }
            }
        }

        @Serializable
        enum class PathType {
            @SerialName("MISSING_PURCHASE")
            MISSING_PURCHASE,

            @SerialName("REFUND_REQUEST")
            REFUND_REQUEST,

            @SerialName("CHANGE_PLANS")
            CHANGE_PLANS,

            @SerialName("CANCEL")
            CANCEL,

            @SerialName("UNKNOWN")
            UNKNOWN,
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as HelpPath

            if (id != other.id) return false
            if (title != other.title) return false
            if (type != other.type) return false
            if (promotionalOffer != other.promotionalOffer) return false
            if (feedbackSurvey != other.feedbackSurvey) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = HASH_CODE_MULTIPLIER * result + title.hashCode()
            result = HASH_CODE_MULTIPLIER * result + type.hashCode()
            result = HASH_CODE_MULTIPLIER * result + (promotionalOffer?.hashCode() ?: 0)
            result = HASH_CODE_MULTIPLIER * result + (feedbackSurvey?.hashCode() ?: 0)
            return result
        }
    }

    @Serializable
    class Appearance(
        val light: ColorInformation? = null,
        val dark: ColorInformation? = null,
    ) {
        @Serializable
        class ColorInformation(
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
        ) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as ColorInformation

                if (accentColor != other.accentColor) return false
                if (textColor != other.textColor) return false
                if (backgroundColor != other.backgroundColor) return false
                if (buttonTextColor != other.buttonTextColor) return false
                if (buttonBackgroundColor != other.buttonBackgroundColor) return false

                return true
            }

            override fun hashCode(): Int {
                var result = accentColor?.hashCode() ?: 0
                result = HASH_CODE_MULTIPLIER * result + (textColor?.hashCode() ?: 0)
                result = HASH_CODE_MULTIPLIER * result + (backgroundColor?.hashCode() ?: 0)
                result = HASH_CODE_MULTIPLIER * result + (buttonTextColor?.hashCode() ?: 0)
                result = HASH_CODE_MULTIPLIER * result + (buttonBackgroundColor?.hashCode() ?: 0)
                return result
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Appearance

            if (light != other.light) return false
            if (dark != other.dark) return false

            return true
        }

        override fun hashCode(): Int {
            var result = light?.hashCode() ?: 0
            result = HASH_CODE_MULTIPLIER * result + (dark?.hashCode() ?: 0)
            return result
        }
    }

    @Serializable
    class Screen(
        val type: ScreenType,
        val title: String,
        val subtitle: String? = null,
        val paths: List<HelpPath>,
    ) {
        @Serializable
        enum class ScreenType {
            @SerialName("MANAGEMENT")
            MANAGEMENT,

            @SerialName("NO_ACTIVE")
            NO_ACTIVE,

            @SerialName("UNKNOWN")
            UNKNOWN,
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Screen

            if (type != other.type) return false
            if (title != other.title) return false
            if (subtitle != other.subtitle) return false
            if (paths != other.paths) return false

            return true
        }

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = HASH_CODE_MULTIPLIER * result + title.hashCode()
            result = HASH_CODE_MULTIPLIER * result + (subtitle?.hashCode() ?: 0)
            result = HASH_CODE_MULTIPLIER * result + paths.hashCode()
            return result
        }
    }

    @Serializable
    class Support(
        val email: String? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Support

            return email == other.email
        }

        override fun hashCode(): Int {
            return email?.hashCode() ?: 0
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CustomerCenterConfigData

        if (screens.keys != other.screens.keys) return false
        for (key in screens.keys) {
            if (screens[key] != other.screens[key]) return false
        }
        if (appearance != other.appearance) return false
        if (localization != other.localization) return false
        if (support != other.support) return false
        if (lastPublishedAppVersion != other.lastPublishedAppVersion) return false

        return true
    }

    override fun hashCode(): Int {
        var result = screens.hashCode()
        result = HASH_CODE_MULTIPLIER * result + appearance.hashCode()
        result = HASH_CODE_MULTIPLIER * result + localization.hashCode()
        result = HASH_CODE_MULTIPLIER * result + support.hashCode()
        result = HASH_CODE_MULTIPLIER * result + (lastPublishedAppVersion?.hashCode() ?: 0)
        return result
    }
}
