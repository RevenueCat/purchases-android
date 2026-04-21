package com.revenuecat.purchases.paywalls

import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.convertToCorrectlyFormattedLocale
import com.revenuecat.purchases.utils.getDefaultLocales
import com.revenuecat.purchases.utils.serializers.GoogleListSerializer
import com.revenuecat.purchases.utils.serializers.OptionalURLSerializer
import com.revenuecat.purchases.utils.serializers.URLSerializer
import com.revenuecat.purchases.utils.sharedLanguageCodeWith
import com.revenuecat.purchases.utils.toLocale
import dev.drewhamilton.poko.Poko
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
@InternalRevenueCatAPI
@Serializable
@Poko
public class PaywallData(
    /**
     * The unique identifier for this paywall.
     */
    public val id: String? = null,

    /**
     * The type of template used to display this paywall.
     */
    @SerialName("template_name") public val templateName: String,

    /**
     * Generic configuration for any paywall.
     */
    public val config: Configuration,

    /**
     * The base remote URL where assets for this paywall are stored.
     */
    @SerialName("asset_base_url") @Serializable(with = URLSerializer::class) public val assetBaseURL: URL,

    /**
     * The revision identifier for this paywall.
     */
    public val revision: Int = 0,

    @SerialName("localized_strings") internal val localization: Map<String, LocalizedConfiguration>,

    @SerialName("localized_strings_by_tier") internal val localizationByTier:
    Map<String, Map<String, LocalizedConfiguration>> = emptyMap(),

    @SerialName("zero_decimal_place_countries")
    @Serializable(with = GoogleListSerializer::class)
    public val zeroDecimalPlaceCountries: List<String> = emptyList(),

    /**
     * The default locale to be used on a paywall if the preferred languages aren't found.
     */
    @SerialName("default_locale") public val defaultLocale: String? = null,
) {

    /**
     * Returns the [Locale] and [LocalizedConfiguration] to be used based on the current locale list
     * and the available locales for this paywall.
     */
    public val localizedConfiguration: Pair<Locale, LocalizedConfiguration>
        get() {
            return localizedConfiguration(locales = getDefaultLocales())
        }

    @VisibleForTesting
    @Suppress("ReturnCount")
    public fun localizedConfiguration(locales: List<Locale>): Pair<Locale, LocalizedConfiguration> {
        for (locale in locales) {
            val localeToCheck = locale.convertToCorrectlyFormattedLocale()
            configForLocale(localeToCheck)?.let { localizedConfiguration ->
                return (localeToCheck to localizedConfiguration)
            }
        }

        // Try finding default local
        defaultLocale?.let { defaultLocale ->
            localization.entries.firstOrNull { localization ->
                localization.key.toLocale() == defaultLocale.toLocale()
            }?.let { localization ->
                return (localization.key.toLocale() to localization.value)
            }
        }

        // Fallback to first localization
        localization.entries.first().let { localization ->
            return (localization.key.toLocale() to localization.value)
        }
    }

    /**
     * @note This allows searching by `Locale` with only language code and missing region (like `en`, `es`, etc).
     *
     * @return [LocalizedConfiguration] for the given [Locale], if found.
     */
    public fun configForLocale(requiredLocale: Locale): LocalizedConfiguration? {
        return localization[requiredLocale.toString()]
            ?: localization.entries.firstOrNull { (localeKey, _) ->
                requiredLocale.sharedLanguageCodeWith(localeKey.toLocale())
            }?.value
    }

    public val tieredLocalizedConfiguration: Pair<Locale, Map<String, LocalizedConfiguration>>
        get() {
            return tieredConfigForLocales(locales = getDefaultLocales())
        }

    @Suppress("ReturnCount")
    private fun tieredConfigForLocales(locales: List<Locale>): Pair<Locale, Map<String, LocalizedConfiguration>> {
        for (locale in locales) {
            val localeToCheck = locale.convertToCorrectlyFormattedLocale()
            tieredConfigForLocale(localeToCheck)?.let { localizedConfiguration ->
                return (localeToCheck to localizedConfiguration)
            }
        }

        // Try finding default locale
        defaultLocale?.let { defaultLocale ->
            localizationByTier.entries.firstOrNull { localization ->
                localization.key.toLocale() == defaultLocale.toLocale()
            }?.let { localization ->
                return (localization.key.toLocale() to localization.value)
            }
        }

        // Fallback to first localization
        val first = localizationByTier.entries.first()
        return (first.key.toLocale() to first.value)
    }

    @VisibleForTesting
    public fun tieredConfigForLocale(requiredLocale: Locale): Map<String, LocalizedConfiguration>? {
        return localizationByTier[requiredLocale.toString()]
            ?: localizationByTier.entries.firstOrNull { (localeKey, _) ->
                requiredLocale.sharedLanguageCodeWith(localeKey.toLocale())
            }?.value
    }

    @InternalRevenueCatAPI
    public fun copy(
        templateName: String = this.templateName,
        config: Configuration = this.config,
        assetBaseURL: URL = this.assetBaseURL,
        revision: Int = this.revision,
        localization: Map<String, LocalizedConfiguration> = this.localization,
        localizationByTier: Map<String, Map<String, LocalizedConfiguration>> = this.localizationByTier,
        zeroDecimalPlaceCountries: List<String> = this.zeroDecimalPlaceCountries,
        defaultLocale: String? = this.defaultLocale,
    ): PaywallData = PaywallData(
        templateName = templateName,
        config = config,
        assetBaseURL = assetBaseURL,
        revision = revision,
        localization = localization,
        localizationByTier = localizationByTier,
        zeroDecimalPlaceCountries = zeroDecimalPlaceCountries,
        defaultLocale = defaultLocale,
    )

    /**
     * Generic configuration for any paywall.
     */
    @Serializable
    @Poko
    public class Configuration(
        /**
         * The list of package identifiers this paywall will display.
         *
         * This defaults to an empty list for multi-tier configurations.
         */
        @SerialName("packages") public val packageIds: List<String> = emptyList(),

        /**
         * The package to be selected by default.
         */
        @SerialName("default_package") public val defaultPackage: String? = null,

        @SerialName("images_webp")
        internal val imagesWebp: Images? = null,

        @SerialName("images")
        internal val legacyImages: Images? = null,

        @SerialName("images_by_tier")
        public val imagesByTier: Map<String, Images>? = null,

        /**
         * Whether the background image will be blurred (in templates with one).
         */
        @SerialName("blurred_background_image") public val blurredBackgroundImage: Boolean = false,

        /**
         * Whether a restore purchases button should be displayed.
         */
        @SerialName("display_restore_purchases") public val displayRestorePurchases: Boolean = true,

        /**
         * If set, the paywall will display a terms of service link.
         */
        @SerialName("tos_url")
        @Serializable(with = OptionalURLSerializer::class)
        public val termsOfServiceURL: URL? = null,

        /**
         * If set, the paywall will display a privacy policy link.
         */
        @SerialName("privacy_url")
        @Serializable(with = OptionalURLSerializer::class)
        public val privacyURL: URL? = null,

        /**
         * The set of colors used for single-tier paywalls.
         */
        public val colors: ColorInformation,

        /**
         * The set of colors used for multi-tier paywalls.
         */
        @SerialName("colors_by_tier")
        public val colorsByTier: Map<String, ColorInformation>? = null,

        /**
         * Tiers used for multi-tier paywalls.
         */
        public val tiers: List<Tier>? = null,

        /**
         * The tier to be selected by default.
         */
        @SerialName("default_tier")
        public val defaultTier: String? = null,
    ) {
        public constructor(
            packageIds: List<String>,
            defaultPackage: String? = null,
            images: Images,
            imagesByTier: Map<String, Images>? = null,
            colors: ColorInformation,
            colorsByTier: Map<String, ColorInformation>? = null,
            tiers: List<Tier>? = null,
            blurredBackgroundImage: Boolean = false,
            displayRestorePurchases: Boolean = true,
            termsOfServiceURL: URL? = null,
            privacyURL: URL? = null,
        ) : this(
            packageIds = packageIds,
            imagesWebp = images,
            imagesByTier = imagesByTier,
            colors = colors,
            colorsByTier = colorsByTier,
            tiers = tiers,
            defaultPackage = defaultPackage,
            blurredBackgroundImage = blurredBackgroundImage,
            displayRestorePurchases = displayRestorePurchases,
            termsOfServiceURL = termsOfServiceURL,
            privacyURL = privacyURL,
        )

        @InternalRevenueCatAPI
        public fun copy(
            packageIds: List<String> = this.packageIds,
            defaultPackage: String? = this.defaultPackage,
            imagesWebp: Images? = this.imagesWebp,
            legacyImages: Images? = this.legacyImages,
            imagesByTier: Map<String, Images>? = this.imagesByTier,
            blurredBackgroundImage: Boolean = this.blurredBackgroundImage,
            displayRestorePurchases: Boolean = this.displayRestorePurchases,
            termsOfServiceURL: URL? = this.termsOfServiceURL,
            privacyURL: URL? = this.privacyURL,
            colors: ColorInformation = this.colors,
            colorsByTier: Map<String, ColorInformation>? = this.colorsByTier,
            tiers: List<Tier>? = this.tiers,
            defaultTier: String? = this.defaultTier,
        ): Configuration = Configuration(
            packageIds = packageIds,
            defaultPackage = defaultPackage,
            imagesWebp = imagesWebp,
            legacyImages = legacyImages,
            imagesByTier = imagesByTier,
            blurredBackgroundImage = blurredBackgroundImage,
            displayRestorePurchases = displayRestorePurchases,
            termsOfServiceURL = termsOfServiceURL,
            privacyURL = privacyURL,
            colors = colors,
            colorsByTier = colorsByTier,
            tiers = tiers,
            defaultTier = defaultTier,
        )

        /**
         * The images for this template.
         */
        public val images: Images
            get() {
                return Images(
                    header = imagesWebp?.header ?: legacyImages?.header,
                    background = imagesWebp?.background ?: legacyImages?.background,
                    icon = imagesWebp?.icon ?: legacyImages?.icon,
                )
            }

        @Serializable
        @Poko
        public class Images(
            /**
             * Image displayed as a header in a template.
             */
            @Serializable(with = EmptyStringToNullSerializer::class)
            public val header: String? = null,

            /**
             * Image displayed as a background in a template.
             */
            @Serializable(with = EmptyStringToNullSerializer::class)
            public val background: String? = null,

            /**
             * Image displayed as an app icon in a template.
             */
            @Serializable(with = EmptyStringToNullSerializer::class)
            public val icon: String? = null,
        ) {
            internal val all: List<String>
                get() = listOfNotNull(header, background, icon)
        }

        @Serializable
        @Poko
        public class Tier(
            /**
             * RevenueCat generated id to match tiers with localizations.
             */
            public val id: String,

            /**
             * The list of package identifiers this tier will display.
             */
            @SerialName("packages")
            public val packageIds: List<String>,

            /**
             * The package to be selected by default.
             */
            @SerialName("default_package")
            public val defaultPackageId: String,
        )

        @Serializable
        @Poko
        public class ColorInformation(
            /**
             * Set of colors for light mode.
             */
            public val light: Colors,

            /**
             * Set of colors for dark mode.
             */
            public val dark: Colors? = null,
        )

        @Serializable
        @Poko
        public class Colors(
            /**
             * Color for the background of the paywall.
             */
            @Serializable(with = PaywallColor.Serializer::class) public val background: PaywallColor,

            /**
             * Color for the primary text element.
             */
            @SerialName("text_1")
            @Serializable(with = PaywallColor.Serializer::class) public val text1: PaywallColor,

            /**
             * Color for secondary text element.
             */
            @SerialName("text_2")
            @Serializable(with = PaywallColor.Serializer::class) public val text2: PaywallColor? = null,

            /**
             * Color for tertiary text element.
             */
            @SerialName("text_3")
            @Serializable(with = PaywallColor.Serializer::class) public val text3: PaywallColor? = null,

            /**
             * Background color of the main call to action button.
             */
            @SerialName("call_to_action_background")
            @Serializable(with = PaywallColor.Serializer::class) public val callToActionBackground: PaywallColor,

            /**
             * Foreground color of the main call to action button.
             */
            @SerialName("call_to_action_foreground")
            @Serializable(with = PaywallColor.Serializer::class) public val callToActionForeground: PaywallColor,

            /**
             * If present, the CTA will create a vertical gradient from [callToActionBackground] to this color.
             */
            @SerialName("call_to_action_secondary_background")
            @Serializable(with = PaywallColor.Serializer::class)
            public val callToActionSecondaryBackground: PaywallColor? = null,

            /**
             * Primary accent color.
             */
            @SerialName("accent_1")
            @Serializable(with = PaywallColor.Serializer::class) public val accent1: PaywallColor? = null,

            /**
             * Secondary accent color.
             */
            @SerialName("accent_2")
            @Serializable(with = PaywallColor.Serializer::class) public val accent2: PaywallColor? = null,

            /**
             * Tertiary accent color.
             */
            @SerialName("accent_3")
            @Serializable(with = PaywallColor.Serializer::class) public val accent3: PaywallColor? = null,

            /**
             * Close button accent color.
             */
            @SerialName("close_button")
            @Serializable(with = PaywallColor.Serializer::class) public val closeButton: PaywallColor? = null,

            /**
             * Tier control background color.
             */
            @SerialName("tier_control_background")
            @Serializable(with = PaywallColor.Serializer::class)
            public val tierControlBackground: PaywallColor? = null,

            /**
             * Tier control foreground color.
             */
            @SerialName("tier_control_foreground")
            @Serializable(with = PaywallColor.Serializer::class)
            public val tierControlForeground: PaywallColor? = null,

            /**
             * Tier control selected background color.
             */
            @SerialName("tier_control_selected_background")
            @Serializable(with = PaywallColor.Serializer::class)
            public val tierControlSelectedBackground: PaywallColor? = null,

            /**
             * Tier control selected foreground color.
             */
            @SerialName("tier_control_selected_foreground")
            @Serializable(with = PaywallColor.Serializer::class)
            public val tierControlSelectedForeground: PaywallColor? = null,
        )
    }

    /**
     * Defines the necessary localized information for a paywall.
     */
    @Serializable
    @Poko
    public class LocalizedConfiguration(
        /**
         * The title of the paywall screen.
         */
        public val title: String,

        /**
         * The subtitle of the paywall screen.
         */
        @Serializable(with = EmptyStringToNullSerializer::class)
        public val subtitle: String? = null,

        /**
         * The content of the main action button for purchasing a subscription.
         */
        @SerialName("call_to_action")
        public val callToAction: String,

        /**
         * The content of the main action button for purchasing a subscription when an intro offer is available.
         * If `null`, no information regarding trial eligibility will be displayed.
         */
        @SerialName("call_to_action_with_intro_offer")
        @Serializable(with = EmptyStringToNullSerializer::class)
        public val callToActionWithIntroOffer: String? = null,

        /**
         * The content of the main action button for purchasing a subscription when multiple intro offer are available.
         * This may happen in Google Play, if you have an offer with both a free trial and a discounted price.
         * If `null`, no information regarding trial eligibility will be displayed.
         */
        @SerialName("call_to_action_with_multiple_intro_offers")
        @Serializable(with = EmptyStringToNullSerializer::class)
        public val callToActionWithMultipleIntroOffers: String? = null,

        /**
         * Description for the offer to be purchased.
         */
        @SerialName("offer_details")
        @Serializable(with = EmptyStringToNullSerializer::class)
        public val offerDetails: String? = null,

        /**
         * Description for the offer to be purchased when an intro offer is available.
         * If `null`, no information regarding trial eligibility will be displayed.
         */
        @SerialName("offer_details_with_intro_offer")
        @Serializable(with = EmptyStringToNullSerializer::class)
        public val offerDetailsWithIntroOffer: String? = null,

        /**
         * Description for the offer to be purchased when multiple intro offers are available.
         * This may happen in Google Play, if you have an offer with both a free trial and a discounted price.
         * If `null`, no information regarding trial eligibility will be displayed.
         */
        @SerialName("offer_details_with_multiple_intro_offers")
        @Serializable(with = EmptyStringToNullSerializer::class)
        public val offerDetailsWithMultipleIntroOffers: String? = null,

        /**
         * The name representing each of the packages, most commonly a variable.
         */
        @SerialName("offer_name")
        @Serializable(with = EmptyStringToNullSerializer::class)
        public val offerName: String? = null,

        /**
         * An optional list of features that describe this paywall.
         */
        public val features: List<Feature> = emptyList(),

        @SerialName("tier_name")
        @Serializable(with = EmptyStringToNullSerializer::class)
        public val tierName: String? = null,

        @SerialName("offer_overrides")
        public val offerOverrides: Map<String, OfferOverride> = emptyMap(),
    ) {
        /**
         * An item to be showcased in a paywall.
         */
        @Serializable
        @Poko
        public class Feature(
            /**
             * The title of the feature.
             */
            public val title: String,

            /**
             * An optional description of the feature.
             */
            public val content: String? = null,

            /**
             * An optional icon for the feature.
             * This must be an icon identifier known by `RevenueCatUI`.
             */
            @SerialName("icon_id") public val iconID: String? = null,
        ) {
            @JvmSynthetic
            @InternalRevenueCatAPI
            public fun copy(
                title: String = this.title,
                content: String? = this.content,
                iconID: String? = this.iconID,
            ): Feature = Feature(
                title = title,
                content = content,
                iconID = iconID,
            )
        }

        @Serializable
        @Poko
        public class OfferOverride(
            /**
             *
             */
            @SerialName("offer_name")
            public val offerName: String,

            /**
             *
             */
            @SerialName("offer_details")
            public val offerDetails: String,

            /**
             *
             */
            @SerialName("offer_details_with_intro_offer")
            @Serializable(with = EmptyStringToNullSerializer::class)
            public val offerDetailsWithIntroOffer: String? = null,

            /**
             *
             */
            @SerialName("offer_details_with_multiple_intro_offers")
            @Serializable(with = EmptyStringToNullSerializer::class)
            public val offerDetailsWithMultipleIntroOffers: String? = null,

            /**
             *
             */
            @SerialName("offer_badge")
            @Serializable(with = EmptyStringToNullSerializer::class)
            public val offerBadge: String? = null,
        )
    }
}
