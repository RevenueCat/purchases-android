package com.revenuecat.purchases.paywalls

import java.net.URL

/**
 * Represents the data required to display a paywall using the `RevenueCatUI` library.
 * This data can be created and configured in the dashboard and then accessed from the `Offering/paywall`.
 *
 * @see [Paywalls Documentation](https://rev.cat/paywalls)
 */
data class PaywallData(
    /**
     * The type of template used to display this paywall.
     */
    val templateName: String,

    /**
     * Generic configuration for any paywall.
     */
    val config: Configuration,

    /**
     * The base remote URL where assets for this paywall are stored.
     */
    val assetBaseURL: URL,
    internal val localization: Map<String, LocalizedConfiguration>,
) {
    /**
     * Generic configuration for any paywall.
     */
    data class Configuration(
        /**
         * The list of package identifiers this paywall will display.
         */
        val packages: List<String>,

        /**
         * The package to be selected by default.
         */
        val defaultPackage: String? = null,

        /**
         * The images for this template.
         */
        val images: Images,

        /**
         * Whether the background image will be blurred (in templates with one).
         */
        val blurredBackgroundImage: Boolean = false,

        /**
         * Whether a restore purchases button should be displayed.
         */
        val displayRestorePurchases: Boolean = true,

        /**
         * If set, the paywall will display a terms of service link.
         */
        val termsOfServiceURL: URL? = null,

        /**
         * If set, the paywall will display a privacy policy link.
         */
        val privacyURL: URL? = null,

        /**
         * The set of colors used.
         */
        val colors: ColorInformation,
    ) {
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

        data class Colors(
            /**
             * Color for the background of the paywall.
             */
            val background: PaywallColor,

            /**
             * Color for the primary text element.
             */
            val text1: PaywallColor,

            /**
             * Color for secondary text element.
             */
            val text2: PaywallColor? = null,

            /**
             * Background color of the main call to action button.
             */
            val callToActionBackground: PaywallColor,

            /**
             * Foreground color of the main call to action button.
             */
            val callToActionForeground: PaywallColor,

            /**
             * Primary accent color.
             */
            val accent1: PaywallColor? = null,

            /**
             * Secondary accent color.
             */
            val accent2: PaywallColor? = null,
        )
    }

    /**
     * Defines the necessary localized information for a paywall.
     */
    data class LocalizedConfiguration(
        /**
         * The title of the paywall screen.
         */
        val title: String,

        /**
         * The subtitle of the paywall screen.
         */
        val subtitle: String?,

        /**
         * The content of the main action button for purchasing a subscription.
         */
        val callToAction: String,

        /**
         * The content of the main action button for purchasing a subscription when an intro offer is available.
         * If `null`, no information regarding trial eligibility will be displayed.
         */
        val callToActionWithIntroOffer: String?,

        /**
         * Description for the offer to be purchased.
         */
        val offerDetails: String?,

        /**
         * Description for the offer to be purchased when an intro offer is available.
         * If `null`, no information regarding trial eligibility will be displayed.
         */
        val offerDetailsWithIntroOffer: String?,

        /**
         * The name representing each of the packages, most commonly a variable.
         */
        val offerName: String?,

        /**
         * An optional list of features that describe this paywall.
         */
        val features: List<Feature>,
    ) {
        /**
         * An item to be showcased in a paywall.
         */
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
            val iconID: String? = null,
        )
    }
}
