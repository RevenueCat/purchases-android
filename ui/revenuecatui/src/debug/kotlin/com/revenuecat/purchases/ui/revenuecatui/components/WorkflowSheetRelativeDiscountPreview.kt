@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.common.workflows.WorkflowScreen
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.PurchaseButtonComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.properties.Badge
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.CornerRadiuses
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StickyFooterComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.WorkflowPaywallUiState
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.workflow.WorkflowScreenMapper
import java.net.URL
import java.util.Date

@Preview(name = "Workflow discount — paywall", widthDp = 411, heightDp = 891)
@Composable
internal fun WorkflowDiscountPaywallPreview() = WorkflowSheetRelativeDiscountPreview(sheetPresented = false)

@Preview(name = "Workflow discount — all plans", widthDp = 411, heightDp = 891)
@Composable
internal fun WorkflowDiscountPlansSheetPreview() = WorkflowSheetRelativeDiscountPreview(sheetPresented = true)

@Composable
private fun WorkflowSheetRelativeDiscountPreview(sheetPresented: Boolean) {
    val offering = WorkflowDiscountPreviewData.offering
    val validated = requireNotNull(offering.validatePaywallComponentsDataOrNullForPreviews()).getOrThrow()
    val tracker = remember { PaywallComponentInteractionTracker { _ -> } }
    val state = remember(sheetPresented) {
        offering.toComponentsPaywallState(
            validationResult = validated,
            storefrontCountryCode = "US",
            dateProvider = { Date(MILLIS_2025_01_25) },
            purchases = MockPurchasesType(),
        ).also { state ->
            if (sheetPresented) {
                val footer = state.stickyFooter as StickyFooterComponentStyle
                val button = footer.stackComponentStyle.children.filterIsInstance<ButtonComponentStyle>()
                    .single { it.action is ButtonComponentStyle.Action.NavigateTo }
                val action = button.action as ButtonComponentStyle.Action.NavigateTo
                state.sheet.show(
                    sheet = action.destination as ButtonComponentStyle.Action.NavigateTo.Destination.Sheet,
                    state = state,
                    componentInteractionTracker = tracker,
                    onClick = { },
                )
            }
        }
    }
    LoadedWorkflowPaywall(
        workflowState = WorkflowPaywallUiState(currentStepId = "plans", stepStates = mapOf("plans" to state)),
        onTransitionComplete = { },
        clickHandler = { },
        componentInteractionTracker = tracker,
        modifier = Modifier.fillMaxSize(),
    )
}

@Suppress("MagicNumber")
private object WorkflowDiscountPreviewData {
    private val accent = color(0xFFD4B6FF)
    private val background = color(0xFF141122)
    private val surface = color(0xFF211B34)
    private val muted = color(0xFFB8AEC9)
    private val white = color(0xFFFFFFFF)
    private val selected = listOf(ComponentOverride.Condition.Selected)
    private val localizations = mapOf(
        "brand" to "P U R R   C L U B",
        "cats" to "🐈  🐈‍⬛",
        "headline" to "More purrs.\nLess boredom.",
        "subtitle" to "Daily play, clever enrichment,\nand happier indoor cats.",
        "benefits" to "✓  Play ideas for every personality\n" +
            "✓  A fresh adventure every day\n✓  One membership, all your cats",
        "recommended" to "NINE LIVES. ENDLESS POSSIBILITIES.",
        "all_plans" to "Explore all plans",
        "sheet_title" to "Pick your purr-fect plan",
        "sheet_subtitle" to "All the play. All the cats. Whichever plan fits.",
        PackageType.ANNUAL.identifier!! to "Annual",
        PackageType.THREE_MONTH.identifier!! to "3 months",
        PackageType.MONTHLY.identifier!! to "Monthly",
        "per_month" to "{{ product.price_per_month }}/mo",
        "price" to "{{ product.price }}",
        "discount" to "{{ product.relative_discount }} OFF",
        "selection" to "○", "selected" to "●",
        "continue" to "Join the club",
        "terms" to "Auto-renews. Cancel anytime.",
        "footer" to "Restore purchases   ·   Terms   ·   Privacy",
    ).mapKeys { LocalizationKey(it.key) }.mapValues { LocalizationData.Text(it.value) }

    private val purchaseButton get() = PurchaseButtonComponent(
        stack = StackComponent(
            components = listOf(text("continue", 19, background, FontWeight.BOLD)),
            backgroundColor = accent,
            padding = padding(18.0),
            shape = Shape.Pill,
        ),
    )

    private val plansSheet get() = ButtonComponent.Destination.Sheet(
        id = "all_plans",
        name = "All plans",
        backgroundBlur = true,
        stack = StackComponent(
            components = listOf(
                StackComponent(
                    components = emptyList(),
                    size = Size(SizeConstraint.Fixed(36u), SizeConstraint.Fixed(4u)),
                    backgroundColor = color(0xFF5A4D6B),
                    shape = Shape.Pill,
                ),
                text("sheet_title", 23, weight = FontWeight.BOLD),
                text("sheet_subtitle", 14, muted),
                packageCard(PackageType.ANNUAL),
                packageCard(PackageType.THREE_MONTH),
                packageCard(PackageType.MONTHLY),
                purchaseButton,
                text("terms", 12, muted),
            ),
            spacing = 20f,
            backgroundColor = surface,
            padding = Padding(top = 12.0, bottom = 32.0, leading = 24.0, trailing = 24.0),
            shape = Shape.Rectangle(CornerRadiuses.Dp(28.0, 28.0, 0.0, 0.0)),
        ),
    )

    val offering: Offering get() {
        val screen = WorkflowScreen(
            name = "Purr Club",
            templateName = "components",
            revision = 1,
            assetBaseURL = URL("https://assets.pawwalls.com"),
            offeringIdentifier = "sheet_discount",
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        components = listOf(
                            text("brand", 12, accent, FontWeight.BOLD),
                            text("cats", 84),
                            text("headline", 38, weight = FontWeight.BOLD),
                            text("subtitle", 16, muted),
                            text("benefits", 15, color(0xFFE4DCEF), alignment = HorizontalAlignment.LEADING),
                        ),
                        spacing = 18f,
                        padding = Padding(28.0, 24.0, 28.0, 28.0),
                    ),
                    stickyFooter = StickyFooterComponent(
                        stack = StackComponent(
                            components = listOf(
                                text("recommended", 11, muted, FontWeight.SEMI_BOLD),
                                packageCard(PackageType.ANNUAL),
                                purchaseButton,
                                ButtonComponent(
                                    action = ButtonComponent.Action.NavigateTo(plansSheet),
                                    stack = StackComponent(
                                        components = listOf(text("all_plans", 15, weight = FontWeight.SEMI_BOLD)),
                                        padding = padding(6.0),
                                    ),
                                ),
                                text("footer", 11, muted),
                            ),
                            spacing = 20f,
                            backgroundColor = background,
                            padding = Padding(top = 16.0, bottom = 28.0, leading = 24.0, trailing = 24.0),
                        ),
                    ),
                    background = Background.Color(background),
                ),
            ),
            componentsLocalizations = mapOf(LocaleId("en_US") to localizations),
            defaultLocaleIdentifier = LocaleId("en_US"),
        )
        return Offering(
            identifier = "sheet_discount",
            serverDescription = "Purr Club",
            metadata = emptyMap(),
            availablePackages = listOf(
                storePackage(PackageType.ANNUAL, 79_990_000, "$79.99", Period(1, Period.Unit.YEAR, "P1Y")),
                storePackage(PackageType.THREE_MONTH, 34_990_000, "$34.99", Period(3, Period.Unit.MONTH, "P3M")),
                storePackage(PackageType.MONTHLY, 14_990_000, "$14.99", Period(1, Period.Unit.MONTH, "P1M")),
            ),
            paywallComponents = WorkflowScreenMapper.toPaywallComponents(screen, "plans", previewUiConfig()),
        )
    }

    private fun packageCard(type: PackageType) = PackageComponent(
        packageId = requireNotNull(type.identifier),
        isSelectedByDefault = type == PackageType.ANNUAL,
        stack = StackComponent(
            components = listOf(
                TextComponent(
                    text = LocalizationKey("selection"),
                    color = muted,
                    fontSize = 24,
                    size = Size(SizeConstraint.Fixed(24u), SizeConstraint.Fit()),
                    overrides = listOf(
                        ComponentOverride(
                            selected,
                            PartialTextComponent(
                                text = LocalizationKey("selected"),
                                color = accent,
                            ),
                        ),
                    ),
                ),
                StackComponent(
                    components = listOf(
                        text(
                            requireNotNull(type.identifier),
                            17,
                            weight = FontWeight.SEMI_BOLD,
                            alignment = HorizontalAlignment.LEADING,
                        ),
                        text("per_month", 13, muted, alignment = HorizontalAlignment.LEADING),
                    ),
                    dimension = Dimension.Vertical(HorizontalAlignment.LEADING, FlexDistribution.START),
                    spacing = 6f,
                ),
                TextComponent(
                    text = LocalizationKey("price"),
                    color = white,
                    fontSize = 21,
                    fontWeight = FontWeight.BOLD,
                    size = Size(SizeConstraint.Fit(), SizeConstraint.Fit()),
                ),
            ),
            dimension = Dimension.Horizontal(VerticalAlignment.CENTER, FlexDistribution.START), spacing = 12f,
            backgroundColor = color(0xFF28203C), padding = padding(18.0),
            shape = Shape.Rectangle(CornerRadiuses.Dp(18.0)), border = Border(color(0xFF57486D), 1.0),
            badge = discountBadge.takeUnless { type == PackageType.MONTHLY },
            overrides = listOf(
                ComponentOverride(
                    selected,
                    PartialStackComponent(
                        backgroundColor = color(0xFF342642),
                        border = Border(accent, 2.0),
                    ),
                ),
            ),
        ),
    )

    private val discountBadge get() = Badge(
        style = Badge.Style.Overlay,
        alignment = TwoDimensionalAlignment.TOP_TRAILING,
        stack = StackComponent(
            components = listOf(
                TextComponent(
                    text = LocalizationKey("discount"),
                    color = background,
                    fontSize = 11,
                    fontWeight = FontWeight.BOLD,
                    size = Size(SizeConstraint.Fit(), SizeConstraint.Fit()),
                ),
            ),
            size = Size(SizeConstraint.Fit(), SizeConstraint.Fit()),
            backgroundColor = accent,
            padding = Padding(4.0, 4.0, 10.0, 10.0),
            shape = Shape.Pill,
            margin = Padding(top = 0.0, bottom = 0.0, leading = 0.0, trailing = 14.0),
        ),
    )

    private fun text(
        key: String,
        size: Int,
        color: ColorScheme = white,
        weight: FontWeight = FontWeight.REGULAR,
        alignment: HorizontalAlignment = HorizontalAlignment.CENTER,
    ) = TextComponent(
        text = LocalizationKey(key),
        color = color,
        fontSize = size,
        fontWeight = weight,
        horizontalAlignment = alignment,
    )

    private fun storePackage(type: PackageType, micros: Long, price: String, period: Period) = Package(
        identifier = requireNotNull(type.identifier),
        packageType = type,
        presentedOfferingContext = PresentedOfferingContext("sheet_discount"),
        product = TestStoreProduct(
            id = type.name,
            name = type.name,
            title = type.name,
            description = type.name,
            price = Price(amountMicros = micros, currencyCode = "USD", formatted = price),
            period = period,
        ),
    )

    private fun color(argb: Long) = ColorScheme(light = ColorInfo.Hex(Color(argb).toArgb()))
    private fun padding(value: Double) = Padding(value, value, value, value)
}
