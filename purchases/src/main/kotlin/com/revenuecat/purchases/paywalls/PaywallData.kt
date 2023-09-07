package com.revenuecat.purchases.paywalls

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URL
import java.util.Locale

/**
 * Represents the data required to display a paywall using the `RevenueCatUI` library.
 * This data can be created and configured in the dashboard and then accessed from the `Offering/paywall`.
 *
 * @see [Paywalls Documentation](https://rev.cat/paywalls)
 */
@Serializable
data class PaywallData(
    /**
     * The type of template used to display this paywall.
     */
    @SerialName("template_name") val templateName: String,

    /**
     * Generic configuration for any paywall.
     */
    val config: Configuration,

    /**
     * The base remote URL where assets for this paywall are stored.
     */
    @SerialName("asset_base_url") @Serializable(with = URLSerializer::class) val assetBaseURL: URL,
    // TODO-PAYWALLS: do we need this one?
    @SerialName("default_locale") val defaultLocale: String,
    @SerialName("localized_strings") internal val localization: Map<String, LocalizedConfiguration>,
) {

    /**
     * @note This allows searching by `Locale` with only language code and missing region (like `en`, `es`, etc).
     *
     * @return [LocalizedConfiguration] for the given [Locale], if found.
     */
    fun configForLocale(requiredLocale: Locale): LocalizedConfiguration? {
        return localization[requiredLocale.toString()]
            ?: localization.entries.firstOrNull { (localeKey, _) ->
                @Suppress("UsePropertyAccessSyntax")
                Locale(localeKey).getISO3Language() == requiredLocale.getISO3Language()
            }?.value
    }

    /**
     * Generic configuration for any paywall.
     */
    @Serializable
    data class Configuration(
        /**
         * The list of package identifiers this paywall will display.
         */
        val packages: List<String>,

        /**
         * The package to be selected by default.
         */
        @SerialName("default_package") val defaultPackage: String? = null,

        /**
         * The images for this template.
         */
        val images: Images,

        /**
         * Whether the background image will be blurred (in templates with one).
         */
        @SerialName("blurred_background_image") val blurredBackgroundImage: Boolean = false,

        /**
         * Whether a restore purchases button should be displayed.
         */
        @SerialName("display_restore_purchases") val displayRestorePurchases: Boolean = true,

        /**
         * If set, the paywall will display a terms of service link.
         */
        @SerialName("tos_url") @Serializable(with = URLSerializer::class) val termsOfServiceURL: URL? = null,

        /**
         * If set, the paywall will display a privacy policy link.
         */
        @SerialName("privacy_url") @Serializable(with = URLSerializer::class) val privacyURL: URL? = null,

        /**
         * The set of colors used.
         */
        val colors: ColorInformation,
    ) {
        @Serializable
        data class Images(
            /**
             * Image displayed as a header in a template.
             */
            val header: String? = null,

            /**
             * Image displayed as a background in a template.
             */
            val background: String? = null,

            /**
             * Image displayed as an app icon in a template.
             */
            val icon: String? = null,
        )

        @Serializable
        data class ColorInformation(
            /**
             * Set of colors for light mode.
             */
            val light: Colors,

            /**
             * Set of colors for dark mode.
             */
            val dark: Colors? = null,
        )

        @Serializable
        data class Colors(
            /**
             * Color for the background of the paywall.
             */
            @Serializable(with = PaywallColor.Serializer::class) val background: PaywallColor,

            /**
             * Color for the primary text element.
             */
            @SerialName("text_1")
            @Serializable(with = PaywallColor.Serializer::class) val text1: PaywallColor,

            /**
             * Color for secondary text element.
             */
            @SerialName("text_2")
            @Serializable(with = PaywallColor.Serializer::class) val text2: PaywallColor? = null,

            /**
             * Background color of the main call to action button.
             */
            @SerialName("call_to_action_background")
            @Serializable(with = PaywallColor.Serializer::class) val callToActionBackground: PaywallColor,

            /**
             * Foreground color of the main call to action button.
             */
            @SerialName("call_to_action_foreground")
            @Serializable(with = PaywallColor.Serializer::class) val callToActionForeground: PaywallColor,

            /**
             * Primary accent color.
             */
            @SerialName("accent_1")
            @Serializable(with = PaywallColor.Serializer::class) val accent1: PaywallColor? = null,

            /**
             * Secondary accent color.
             */
            @SerialName("accent_2")
            @Serializable(with = PaywallColor.Serializer::class) val accent2: PaywallColor? = null,
        )
    }

    /**
     * Defines the necessary localized information for a paywall.
     */
    @Serializable
    data class LocalizedConfiguration(
        /**
         * The title of the paywall screen.
         */
        val title: String,

        /**
         * The subtitle of the paywall screen.
         */
        val subtitle: String? = null,

        /**
         * The content of the main action button for purchasing a subscription.
         */
        @SerialName("call_to_action") val callToAction: String,

        /**
         * The content of the main action button for purchasing a subscription when an intro offer is available.
         * If `null`, no information regarding trial eligibility will be displayed.
         */
        @SerialName("call_to_action_with_intro_offer") val callToActionWithIntroOffer: String? = null,

        /**
         * Description for the offer to be purchased.
         */
        @SerialName("offer_details") val offerDetails: String?,

        /**
         * Description for the offer to be purchased when an intro offer is available.
         * If `null`, no information regarding trial eligibility will be displayed.
         */
        @SerialName("offer_details_with_intro_offer") val offerDetailsWithIntroOffer: String? = null,

        /**
         * The name representing each of the packages, most commonly a variable.
         */
        @SerialName("offer_name") val offerName: String? = null,

        /**
         * An optional list of features that describe this paywall.
         */
        val features: List<Feature> = emptyList(),
    ) {
        /**
         * An item to be showcased in a paywall.
         */
        @Serializable
        data class Feature(
            /**
             * The title of the feature.
             */
            val title: String,

            /**
             * An optional description of the feature.
             */
            val content: String? = null,

            /**
             * An optional icon for the feature.
             * This must be an icon identifier known by `RevenueCatUI`.
             */
            @SerialName("icon_id") val iconID: String? = null,
        )
    }
}
