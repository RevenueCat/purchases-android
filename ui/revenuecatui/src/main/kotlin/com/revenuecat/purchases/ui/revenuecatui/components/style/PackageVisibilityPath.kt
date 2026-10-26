package com.revenuecat.purchases.ui.revenuecatui.components.style

import com.revenuecat.purchases.ui.revenuecatui.components.ComponentVisibilityContext
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState.Loaded.Components.AvailablePackages

/** A path through rendered containers, rather than just the package component's own rules. */
internal class PackageVisibilityPath(
    private val components: List<ComponentStyle>,
    private val branches: List<(ComponentVisibilityContext) -> Boolean>,
) {
    fun isVisible(context: ComponentVisibilityContext): Boolean =
        branches.all { it(context) } && components.all { context.isVisible(it) }
}

/** Attach paths after the styles have been built, when all of a package's containers are known. */
internal fun AvailablePackages.withVisibilityPaths(root: ComponentStyle): AvailablePackages {
    val paths = PackageVisibilityPathCollector()
    paths.visit(root)
    fun AvailablePackages.Info.withPaths(tabIndex: Int?) =
        copy(visibilityPaths = paths.byPackage[this to tabIndex].orEmpty())
    return AvailablePackages(
        packagesOutsideTabs = packagesOutsideTabs.map { it.withPaths(null) },
        packagesByTab = packagesByTab.mapValues { (tabIndex, infos) -> infos.map { it.withPaths(tabIndex) } },
    )
}

private class PackageVisibilityPathCollector {
    val byPackage = mutableMapOf<Pair<AvailablePackages.Info, Int?>, MutableList<PackageVisibilityPath>>()

    @Suppress("LongParameterList", "CyclomaticComplexMethod", "LongMethod")
    fun visit(
        style: ComponentStyle,
        ancestors: List<ComponentStyle> = emptyList(),
        tabIndex: Int? = null,
        sheet: SheetPresentationKey? = null,
        branches: List<(ComponentVisibilityContext) -> Boolean> = emptyList(),
    ) {
        val path = ancestors + style
        fun child(child: ComponentStyle) = visit(child, path, tabIndex, sheet, branches)
        when (style) {
            is PackageComponentStyle -> {
                val info = AvailablePackages.Info(
                    pkg = style.rcPackage,
                    isSelectedByDefault = style.isSelectedByDefault,
                    resolvedOffer = style.resolvedOffer,
                    visible = style.visible,
                    visibilityOverrides = style.overrides,
                    offerEligibility = style.offerEligibility,
                    sheetPresentationKey = sheet,
                )
                byPackage.getOrPut(info to tabIndex) { mutableListOf() }
                    .add(PackageVisibilityPath(path + style.stackComponentStyle, branches))
                child(style.stackComponentStyle)
            }
            is StackComponentStyle -> {
                style.children.forEach { child(it) }
                val badges = listOfNotNull(style.badge) + style.overrides.mapNotNull { it.properties.badgeStyle }
                badges.distinct().forEach { badge ->
                    visit(
                        badge.stackStyle,
                        path,
                        tabIndex,
                        sheet,
                        branches + { context ->
                            val resolved = context.resolve(style.overrides, style, readsState = true)
                            (resolved?.badgeStyle ?: style.badge) === badge
                        },
                    )
                }
            }
            is ButtonComponentStyle -> {
                child(style.stackComponentStyle)
                val destination = (style.action as? ButtonComponentStyle.Action.NavigateTo)?.destination
                if (destination is ButtonComponentStyle.Action.NavigateTo.Destination.Sheet) {
                    // A presented sheet is detached from the button and its containers.
                    visit(destination.stack, tabIndex = tabIndex, sheet = destination.presentationKey)
                }
            }
            is TabsComponentStyle -> {
                style.tabs.forEachIndexed { index, tab ->
                    visit(tab.stack, path, index, sheet, branches + { it.isSelectedTab(index) })
                }
            }
            is CarouselComponentStyle -> style.pages.forEach { child(it) }
            is CountdownComponentStyle -> {
                val stacks = listOfNotNull(style.countdownStackComponentStyle, style.endStackComponentStyle)
                stacks.forEach { stack ->
                    visit(
                        stack,
                        ancestors,
                        tabIndex,
                        sheet,
                        branches + { context ->
                            val active = if (context.currentDate >= style.date) {
                                style.endStackComponentStyle ?: style.countdownStackComponentStyle
                            } else {
                                style.countdownStackComponentStyle
                            }
                            active === stack
                        },
                    )
                }
            }
            is HeaderComponentStyle -> visit(style.stackComponentStyle, ancestors, tabIndex, sheet, branches)
            is StickyFooterComponentStyle -> visit(style.stackComponentStyle, ancestors, tabIndex, sheet, branches)
            is TabControlButtonComponentStyle -> visit(style.stack, ancestors, tabIndex, sheet, branches)
            // Controls are built outside the tab scope and inserted into each tab that uses them.
            is TabControlStyle.Buttons -> visit(style.stack, ancestors, null, sheet, branches)
            is TabControlStyle.Toggle -> visit(style.stack, ancestors, null, sheet, branches)
            else -> Unit
        }
    }
}
