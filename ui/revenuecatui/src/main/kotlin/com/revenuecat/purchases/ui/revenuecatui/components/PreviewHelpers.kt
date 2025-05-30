@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.decode.DataSource
import coil.request.SuccessResult
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.UiConfig.AppConfig
import com.revenuecat.purchases.UiConfig.VariableConfig
import com.revenuecat.purchases.paywalls.components.IconComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.CornerRadiuses
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.FontSpec
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.MaskShape
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Padding.Companion.zero
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toFontWeight
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toPaddingValues
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toTextAlign
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BorderStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ShadowStyles
import com.revenuecat.purchases.ui.revenuecatui.components.style.BadgeStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.IconComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyMap
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallValidationResult
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.toNonEmptyMapOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatePaywallComponentsDataOrNull
import java.net.URL
import java.util.Date

private const val MILLIS_2025_01_25 = 1737763200000

@Composable
@JvmSynthetic
internal fun previewEmptyState(): PaywallState.Loaded.Components {
    val data = PaywallComponentsData(
        templateName = "template",
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = ComponentsConfig(
            base = PaywallComponentsConfig(
                // This would normally contain at least one TextComponent, but that's not needed for previews.
                stack = StackComponent(components = listOf(TestData.Components.monthlyPackageComponent)),
                background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                stickyFooter = null,
            ),
        ),
        componentsLocalizations = nonEmptyMapOf(
            LocaleId("en_US") to nonEmptyMapOf(
                LocalizationKey("text") to LocalizationData.Text("text"),
            ),
        ),
        defaultLocaleIdentifier = LocaleId("en_US"),
    )
    val offering = Offering(
        identifier = "identifier",
        serverDescription = "serverDescription",
        metadata = emptyMap(),
        availablePackages = listOf(TestData.Packages.monthly),
        paywallComponents = Offering.PaywallComponents(
            uiConfig = UiConfig(
                localizations = nonEmptyMapOf(LocaleId("en_US") to variableLocalizationKeysForEnUs()),
            ),
            data = data,
        ),
    )
    val validated = offering.validatePaywallComponentsDataOrNullForPreviews()?.getOrThrow()!!
    return offering.toComponentsPaywallState(
        validationResult = validated,
        activelySubscribedProductIds = emptySet(),
        purchasedNonSubscriptionProductIds = emptySet(),
        storefrontCountryCode = null,
        dateProvider = { Date(MILLIS_2025_01_25) },
    )
}

internal fun previewUiConfig(
    app: AppConfig = AppConfig(),
    localizations: Map<LocaleId, Map<VariableLocalizationKey, String>> = mapOf(
        LocaleId("en_US") to variableLocalizationKeysForEnUs(),
    ),
    variableConfig: VariableConfig = VariableConfig(),
): UiConfig =
    UiConfig(
        app = app,
        localizations = localizations,
        variableConfig = variableConfig,
    )

@Suppress("LongParameterList")
internal fun previewStackComponentStyle(
    children: List<ComponentStyle>,
    dimension: Dimension = Dimension.Vertical(
        alignment = HorizontalAlignment.CENTER,
        distribution = FlexDistribution.START,
    ),
    visible: Boolean = true,
    size: Size = Size(width = Fixed(200u), height = Fit),
    spacing: Dp = 16.dp,
    background: BackgroundStyles = BackgroundStyles.Color(
        color = ColorStyles(light = ColorStyle.Solid(Color.Red)),
    ),
    padding: PaddingValues = PaddingValues(all = 0.dp),
    margin: PaddingValues = PaddingValues(all = 0.dp),
    shape: Shape = Shape.Rectangle(CornerRadiuses.Dp(all = 20.0)),
    border: BorderStyles? = BorderStyles(
        width = 2.dp,
        colors = ColorStyles(light = ColorStyle.Solid(Color.Blue)),
    ),
    shadow: ShadowStyles? = null,
    badge: BadgeStyle? = null,
    scrollOrientation: Orientation? = null,
): StackComponentStyle {
    return StackComponentStyle(
        children = children,
        dimension = dimension,
        visible = visible,
        size = size,
        spacing = spacing,
        background = background,
        padding = padding,
        margin = margin,
        shape = shape,
        border = border,
        shadow = shadow,
        badge = badge,
        scrollOrientation = scrollOrientation,
        rcPackage = null,
        tabIndex = null,
        overrides = emptyList(),
    )
}

@Suppress("LongParameterList")
@JvmSynthetic
internal fun previewTextComponentStyle(
    text: String,
    color: ColorStyles = ColorStyles(ColorStyle.Solid(Color.Black)),
    fontSize: Int = 15,
    fontWeight: FontWeight = FontWeight.REGULAR,
    fontSpec: FontSpec? = null,
    textAlign: HorizontalAlignment = HorizontalAlignment.CENTER,
    horizontalAlignment: HorizontalAlignment = HorizontalAlignment.CENTER,
    backgroundColor: ColorStyles? = null,
    visible: Boolean = true,
    size: Size = Size(width = Fill, height = Fit),
    padding: Padding = zero,
    margin: Padding = zero,
    tabIndex: Int? = null,
    overrides: List<PresentedOverride<LocalizedTextPartial>> = emptyList(),
): TextComponentStyle {
    val weight = fontWeight.toFontWeight()
    val localeId = LocaleId("en_US")
    return TextComponentStyle(
        texts = nonEmptyMapOf(localeId to text),
        color = color,
        fontSize = fontSize,
        fontWeight = weight,
        fontSpec = fontSpec,
        textAlign = textAlign.toTextAlign(),
        horizontalAlignment = horizontalAlignment.toAlignment(),
        backgroundColor = backgroundColor,
        visible = visible,
        size = size,
        padding = padding.toPaddingValues(),
        margin = margin.toPaddingValues(),
        rcPackage = null,
        tabIndex = tabIndex,
        variableLocalizations = nonEmptyMapOf(localeId to variableLocalizationKeysForEnUs()),
        overrides = overrides,
    )
}

@Suppress("LongParameterList")
@Composable
internal fun previewIconComponentStyle(
    size: Size,
    visible: Boolean = true,
    color: ColorStyles = ColorStyles(
        light = ColorStyle.Solid(Color.Cyan),
    ),
    backgroundColor: ColorStyles = ColorStyles(
        light = ColorStyle.Solid(Color.Red),
    ),
    paddingValues: PaddingValues = PaddingValues(10.dp),
    marginValues: PaddingValues = PaddingValues(10.dp),
    border: BorderStyles? = BorderStyles(
        width = 2.dp,
        colors = ColorStyles(light = ColorStyle.Solid(Color.Cyan)),
    ),
    shadow: ShadowStyles? = ShadowStyles(
        colors = ColorStyles(ColorStyle.Solid(Color.Black)),
        radius = 10.dp,
        x = 0.dp,
        y = 3.dp,
    ),
    shape: MaskShape = MaskShape.Circle,
) = IconComponentStyle(
    baseUrl = "https://example.com",
    iconName = "test-icon-name",
    formats = IconComponent.Formats(
        webp = "test-webp",
    ),
    visible = visible,
    size = size,
    color = color,
    padding = paddingValues,
    margin = marginValues,
    iconBackground = IconComponentStyle.Background(
        shape = shape,
        border = border,
        shadow = shadow,
        color = backgroundColor,
    ),
    rcPackage = null,
    tabIndex = null,
    overrides = emptyList(),
)

@Composable
@JvmSynthetic
internal fun previewImageLoader(
    @DrawableRes resource: Int = R.drawable.android,
): ImageLoader {
    val context = LocalContext.current
    return ImageLoader.Builder(context)
        .components {
            add { chain ->
                SuccessResult(
                    drawable = context.getDrawable(resource)!!,
                    request = chain.request,
                    dataSource = DataSource.MEMORY,
                )
            }
        }
        .build()
}

@Composable
@JvmSynthetic
@Suppress("MaxLineLength")
internal fun Offering.validatePaywallComponentsDataOrNullForPreviews(): Result<PaywallValidationResult.Components, NonEmptyList<PaywallValidationError>>? =
    validatePaywallComponentsDataOrNull(
        PaywallResourceProvider(
            applicationName = "RevenueCatUI Previews",
            packageName = "com.revenuecat.purchases.ui.revenuecatui",
            resources = LocalContext.current.resources,
        ),
    )

/**
 * This is only VisibleForTesting because we could use the same convenience when writing tests. We're not actually
 * testing this function.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@JvmSynthetic
@Suppress("CyclomaticComplexMethod")
internal fun variableLocalizationKeysForEnUs(): NonEmptyMap<VariableLocalizationKey, String> =
    VariableLocalizationKey.values().associateWith { key ->
        when (key) {
            VariableLocalizationKey.ANNUAL -> "annual"
            VariableLocalizationKey.ANNUAL_SHORT -> "yr"
            VariableLocalizationKey.ANNUALLY -> "annually"
            VariableLocalizationKey.DAILY -> "daily"
            VariableLocalizationKey.DAY -> "day"
            VariableLocalizationKey.DAY_SHORT -> "day"
            VariableLocalizationKey.FREE_PRICE -> "free"
            VariableLocalizationKey.MONTH -> "month"
            VariableLocalizationKey.MONTH_SHORT -> "mo"
            VariableLocalizationKey.MONTHLY -> "monthly"
            VariableLocalizationKey.LIFETIME -> "lifetime"
            VariableLocalizationKey.NUM_DAY_FEW -> "%d days"
            VariableLocalizationKey.NUM_DAY_MANY -> "%d days"
            VariableLocalizationKey.NUM_DAY_ONE -> "%d day"
            VariableLocalizationKey.NUM_DAY_OTHER -> "%d days"
            VariableLocalizationKey.NUM_DAY_TWO -> "%d days"
            VariableLocalizationKey.NUM_DAY_ZERO -> "%d day"
            VariableLocalizationKey.NUM_MONTH_FEW -> "%d months"
            VariableLocalizationKey.NUM_MONTH_MANY -> "%d months"
            VariableLocalizationKey.NUM_MONTH_ONE -> "%d month"
            VariableLocalizationKey.NUM_MONTH_OTHER -> "%d months"
            VariableLocalizationKey.NUM_MONTH_TWO -> "%d months"
            VariableLocalizationKey.NUM_MONTH_ZERO -> "%d month"
            VariableLocalizationKey.NUM_WEEK_FEW -> "%d weeks"
            VariableLocalizationKey.NUM_WEEK_MANY -> "%d weeks"
            VariableLocalizationKey.NUM_WEEK_ONE -> "%d week"
            VariableLocalizationKey.NUM_WEEK_OTHER -> "%d weeks"
            VariableLocalizationKey.NUM_WEEK_TWO -> "%d weeks"
            VariableLocalizationKey.NUM_WEEK_ZERO -> "%d week"
            VariableLocalizationKey.NUM_YEAR_FEW -> "%d years"
            VariableLocalizationKey.NUM_YEAR_MANY -> "%d years"
            VariableLocalizationKey.NUM_YEAR_ONE -> "%d year"
            VariableLocalizationKey.NUM_YEAR_OTHER -> "%d years"
            VariableLocalizationKey.NUM_YEAR_TWO -> "%d years"
            VariableLocalizationKey.NUM_YEAR_ZERO -> "%d year"
            VariableLocalizationKey.PERCENT -> "%d%%"
            VariableLocalizationKey.WEEK -> "week"
            VariableLocalizationKey.WEEK_SHORT -> "wk"
            VariableLocalizationKey.WEEKLY -> "weekly"
            VariableLocalizationKey.YEAR -> "year"
            VariableLocalizationKey.YEAR_SHORT -> "yr"
            VariableLocalizationKey.YEARLY -> "yearly"
            VariableLocalizationKey.NUM_DAYS_SHORT -> "%dd"
            VariableLocalizationKey.NUM_WEEKS_SHORT -> "%dwk"
            VariableLocalizationKey.NUM_MONTHS_SHORT -> "%dmo"
            VariableLocalizationKey.NUM_YEARS_SHORT -> "%dyr"
        }
    }.toNonEmptyMapOrNull()!!
