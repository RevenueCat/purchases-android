package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.ImageComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PaywallComponent
import com.revenuecat.purchases.paywalls.components.PurchaseButtonComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.components.LocalizedTextPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedImagePartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedStackPartial
import com.revenuecat.purchases.ui.revenuecatui.components.SystemFontFamily
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.LocalizationDictionary
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.imageForAllLocales
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.stringForAllLocales
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toContentScale
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toFontWeight
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toShape
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toTextAlign
import com.revenuecat.purchases.ui.revenuecatui.components.toPresentedOverrides
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyMap
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.errorIfNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.flatMap
import com.revenuecat.purchases.ui.revenuecatui.helpers.map
import com.revenuecat.purchases.ui.revenuecatui.helpers.mapError
import com.revenuecat.purchases.ui.revenuecatui.helpers.mapOrAccumulate
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyListOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.orSuccessfullyNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.zipOrAccumulate

@Suppress("TooManyFunctions")
internal class StyleFactory(
    private val localizations: NonEmptyMap<LocaleId, LocalizationDictionary>,
    private val offering: Offering,
) {

    private companion object {
        private const val DEFAULT_SPACING = 0f
        private val DEFAULT_SHAPE = RectangleShape
    }

    fun create(component: PaywallComponent): Result<ComponentStyle, NonEmptyList<PaywallValidationError>> =
        when (component) {
            is ButtonComponent -> createButtonComponentStyle(component)
            is ImageComponent -> createImageComponentStyle(component)
            is PackageComponent -> createPackageComponentStyle(component)
            is PurchaseButtonComponent -> createPurchaseButtonComponentStyle(component)
            is StackComponent -> createStackComponentStyle(component)
            is StickyFooterComponent -> createStickyFooterComponentStyle(component)
            is TextComponent -> createTextComponentStyle(component = component)
        }

    private fun createStickyFooterComponentStyle(
        component: StickyFooterComponent,
    ): Result<StickyFooterComponentStyle, NonEmptyList<PaywallValidationError>> =
        createStackComponentStyle(component.stack).map {
            StickyFooterComponentStyle(stackComponentStyle = it)
        }

    private fun createButtonComponentStyle(
        component: ButtonComponent,
    ): Result<ButtonComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        first = createStackComponentStyle(component.stack),
        second = component.action.toButtonComponentStyleAction(),
    ) { stack, action ->
        ButtonComponentStyle(
            stackComponentStyle = stack,
            action = action,
        )
    }

    private fun createPackageComponentStyle(
        component: PackageComponent,
    ): Result<PackageComponentStyle, NonEmptyList<PaywallValidationError>> = createStackComponentStyle(
        component.stack,
    ).flatMap { style ->
        offering.getPackage(component.packageId)
            .errorIfNull(
                nonEmptyListOf(
                    PaywallValidationError.MissingPackage(
                        offeringId = offering.identifier,
                        packageId = component.packageId,
                    ),
                ),
            ).map { pkg ->
                PackageComponentStyle(
                    stackComponentStyle = style,
                    rcPackage = pkg,
                    isSelectedByDefault = component.isSelectedByDefault,
                )
            }
    }

    private fun createPurchaseButtonComponentStyle(
        component: PurchaseButtonComponent,
    ): Result<ButtonComponentStyle, NonEmptyList<PaywallValidationError>> = createStackComponentStyle(
        component.stack,
    ).map {
        ButtonComponentStyle(
            stackComponentStyle = it,
            action = ButtonComponentStyle.Action.PurchasePackage,
        )
    }

    @Suppress("MaxLineLength")
    private fun ButtonComponent.Action.toButtonComponentStyleAction(): Result<ButtonComponentStyle.Action, NonEmptyList<PaywallValidationError>> {
        return when (this) {
            ButtonComponent.Action.NavigateBack -> Result.Success(ButtonComponentStyle.Action.NavigateBack)
            ButtonComponent.Action.RestorePurchases -> Result.Success(ButtonComponentStyle.Action.RestorePurchases)
            is ButtonComponent.Action.NavigateTo -> destination.toPaywallDestination()
                .map { ButtonComponentStyle.Action.NavigateTo(it) }
        }
    }

    @Suppress("MaxLineLength")
    private fun ButtonComponent.Destination.toPaywallDestination(): Result<ButtonComponentStyle.Action.NavigateTo.Destination, NonEmptyList<PaywallValidationError>> =

        when (this) {
            is ButtonComponent.Destination.CustomerCenter -> Result.Success(
                ButtonComponentStyle.Action.NavigateTo.Destination.CustomerCenter,
            )

            is ButtonComponent.Destination.PrivacyPolicy -> buttonComponentStyleUrlDestination(urlLid, method)
            is ButtonComponent.Destination.Terms -> buttonComponentStyleUrlDestination(urlLid, method)
            is ButtonComponent.Destination.Url -> buttonComponentStyleUrlDestination(urlLid, method)
        }

    private fun buttonComponentStyleUrlDestination(
        urlLid: LocalizationKey,
        method: ButtonComponent.UrlMethod,
    ) =
        localizations.stringForAllLocales(urlLid).map { urls ->
            ButtonComponentStyle.Action.NavigateTo.Destination.Url(urls, method)
        }

    private fun createStackComponentStyle(
        component: StackComponent,
    ): Result<StackComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        // Build the PresentedOverrides.
        first = component.overrides
            ?.toPresentedOverrides { partial -> Result.Success(PresentedStackPartial(partial)) }
            .orSuccessfullyNull()
            .mapError { nonEmptyListOf(it) },
        // Build all children styles.
        second = component.components
            .map { create(it) }
            .mapOrAccumulate { it },
    ) { presentedOverrides, children ->
        StackComponentStyle(
            children = children,
            dimension = component.dimension,
            size = component.size,
            spacing = (component.spacing ?: DEFAULT_SPACING).dp,
            backgroundColor = component.backgroundColor,
            padding = component.padding.toPaddingValues(),
            margin = component.margin.toPaddingValues(),
            shape = component.shape?.toShape() ?: DEFAULT_SHAPE,
            border = component.border,
            shadow = component.shadow,
            overrides = presentedOverrides,
        )
    }

    private fun createTextComponentStyle(
        component: TextComponent,
    ): Result<TextComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        // Get our texts from the localization dictionary.
        first = localizations.stringForAllLocales(component.text),
        second = component.overrides
            // Map all overrides to PresentedOverrides.
            ?.toPresentedOverrides { LocalizedTextPartial(from = it, using = localizations) }
            .orSuccessfullyNull()
            .mapError { nonEmptyListOf(it) },
    ) { texts, presentedOverrides ->
        val weight = component.fontWeight.toFontWeight()
        TextComponentStyle(
            texts = texts,
            color = component.color,
            fontSize = component.fontSize,
            fontWeight = weight,
            fontFamily = component.fontName?.let { SystemFontFamily(it, weight) },
            textAlign = component.horizontalAlignment.toTextAlign(),
            horizontalAlignment = component.horizontalAlignment.toAlignment(),
            backgroundColor = component.backgroundColor,
            size = component.size,
            padding = component.padding.toPaddingValues(),
            margin = component.margin.toPaddingValues(),
            overrides = presentedOverrides,
        )
    }

    private fun createImageComponentStyle(
        component: ImageComponent,
    ): Result<ImageComponentStyle, NonEmptyList<PaywallValidationError>> = zipOrAccumulate(
        first = component.source.withLocalizedOverrides(component.overrideSourceLid),
        second = component.overrides
            ?.toPresentedOverrides {
                it.source
                    ?.withLocalizedOverrides(it.overrideSourceLid)
                    .orSuccessfullyNull()
                    .map { sources -> PresentedImagePartial(sources = sources, partial = it) }
            }.orSuccessfullyNull()
            .mapError { nonEmptyListOf(it) },
    ) { sources, presentedOverrides ->
        ImageComponentStyle(
            sources,
            size = component.size,
            shape = component.maskShape?.toShape(),
            overlay = component.colorOverlay,
            contentScale = component.fitMode.toContentScale(),
            overrides = presentedOverrides,
        )
    }

    private fun ThemeImageUrls.withLocalizedOverrides(
        overrideSourceLid: LocalizationKey?,
    ): Result<NonEmptyMap<LocaleId, ThemeImageUrls>, NonEmptyList<PaywallValidationError.MissingImageLocalization>> =
        overrideSourceLid
            ?.let { key -> localizations.imageForAllLocales(key) }
            .orSuccessfullyNull()
            // Ensure the default source keyed by the default locale is present in the result.
            .map { nonEmptyMapOf(localizations.entry.key to this, it.orEmpty()) }
}
