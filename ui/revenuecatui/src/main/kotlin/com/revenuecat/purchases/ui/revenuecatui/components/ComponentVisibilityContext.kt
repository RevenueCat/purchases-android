package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.ui.unit.DpSize
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.components.state.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.CarouselComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.PackageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabsComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabsScope
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState.Loaded.Components.SelectedPackageInfo
import kotlinx.serialization.json.JsonPrimitive
import java.util.Date

/** Shared by rendering and package selection, including visibility of a package's containers. */
@Suppress("LongParameterList")
internal class ComponentVisibilityContext(
    private val screenCondition: ScreenCondition,
    private val windowDpSize: DpSize?,
    private val selectedPackage: () -> SelectedPackageInfo?,
    private val selectedTabIndex: () -> Int,
    private val customVariables: Map<String, CustomVariableValue>,
    private val selectedOfferEligibility: () -> OfferEligibility,
    private val stateReader: (String) -> JsonPrimitive? = { null },
    val currentDate: Date = Date(),
    private val scopedTabIndex: ((TabsScope) -> Int)? = null,
) {
    fun isSelectedTab(index: Int, scope: TabsScope? = null): Boolean =
        (scope?.let { scopedTabIndex?.invoke(it) } ?: selectedTabIndex()) == index

    fun withSelection(selection: SelectedPackageInfo?): ComponentVisibilityContext = ComponentVisibilityContext(
        screenCondition = screenCondition,
        windowDpSize = windowDpSize,
        selectedPackage = { selection },
        selectedTabIndex = selectedTabIndex,
        customVariables = customVariables,
        selectedOfferEligibility = { selection?.offerEligibility ?: OfferEligibility.Ineligible },
        stateReader = stateReader,
        currentDate = currentDate,
        scopedTabIndex = scopedTabIndex,
    )

    fun <T : PresentedPartial<T>> resolve(
        overrides: List<PresentedOverride<T>>,
        packageContext: PackageContext? = null,
        readsState: Boolean = false,
    ): T? = overrides.buildPresentedPartial(
        windowSize = screenCondition,
        offerEligibility = { packageContext?.offerEligibility ?: selectedOfferEligibility() },
        state = {
            if (packageContext?.computeIsSelected(
                    selectedPackage(),
                    packageContext.tabsScope?.let { scopedTabIndex?.invoke(it) } ?: selectedTabIndex(),
                ) == true
            ) {
                ComponentViewState.SELECTED
            } else {
                ComponentViewState.DEFAULT
            }
        },
        conditionContext = ConditionContext(
            selectedPackageId = { selectedPackage()?.rcPackage?.identifier },
            customVariables = customVariables,
            stateReader = if (readsState) stateReader else { _ -> null },
            windowDpSize = windowDpSize,
        ),
    )

    fun isVisible(style: ComponentStyle): Boolean = when (style) {
        is StackComponentStyle -> style.resolveVisibility(resolve(style.overrides, style, readsState = true))
        is PackageComponentStyle -> resolve(style.overrides, style)?.partial?.visible ?: style.visible
        is CarouselComponentStyle -> style.resolveVisibility(resolve(style.overrides, style))
        is TabsComponentStyle -> style.resolveVisibility(resolve(style.overrides))
        is ButtonComponentStyle -> resolve(style.overrides, style.stackComponentStyle, readsState = true)
            ?.partial?.visible ?: style.buttonVisible ?: true
        // Wrapper components render their inner stacks directly; paths contain those stacks.
        else -> true
    }
}

internal fun StackComponentStyle.resolveVisibility(partial: PresentedStackPartial?): Boolean =
    partial?.partial?.visible ?: visible

internal fun CarouselComponentStyle.resolveVisibility(partial: PresentedCarouselPartial?): Boolean =
    partial?.partial?.visible ?: visible

internal fun TabsComponentStyle.resolveVisibility(partial: PresentedTabsPartial?): Boolean =
    partial?.partial?.visible ?: visible
