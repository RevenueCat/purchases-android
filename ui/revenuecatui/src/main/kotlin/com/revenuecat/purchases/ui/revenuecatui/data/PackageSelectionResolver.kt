package com.revenuecat.purchases.ui.revenuecatui.data

import com.revenuecat.purchases.ui.revenuecatui.components.ComponentVisibilityContext
import com.revenuecat.purchases.ui.revenuecatui.components.style.SheetPresentationKey
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState.Loaded.Components.AvailablePackages
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState.Loaded.Components.SelectedPackageInfo
import com.revenuecat.purchases.ui.revenuecatui.extensions.calculateOfferEligibility

/** Selection policy without mutations. Callers apply the result and own tab-selection memory. */
internal class PackageSelectionResolver(
    packages: AvailablePackages,
    selectedTabIndex: Int,
    presentedSheet: SheetPresentationKey?,
    private val context: ComponentVisibilityContext,
) {
    // A default authored in a sheet still establishes a root fallback after that sheet closes.
    private val hasAuthoredDefaultOutsideTabs = packages.packagesOutsideTabs.any { it.isSelectedByDefault }
    private val outside = packages.packagesOutsideTabs.inPresentation(presentedSheet)
    private val tab = packages.packagesByTab[selectedTabIndex].orEmpty().inPresentation(presentedSheet)
    private val active = outside + tab

    fun switchTab(current: String?, remembered: String?): String? = reconcile(
        current = current?.takeIf { uid -> outside.any { it.uniqueId == uid } } ?: remembered,
        remembered = remembered,
    )

    fun reconcile(current: String?, remembered: String?): String? = when {
        current != null && rendersWhenSelected(current) -> current
        // No default outside tabs means the paywall deliberately starts unselected.
        current == null && tab.isEmpty() && outside.none { it.isSelectedByDefault } -> null
        else -> defaultSelection(remembered)
            ?: outside.firstOrNull { canSelect(it) }?.uniqueId.takeIf { current != null }
            // Keep the existing all-hidden behavior, including workflow fallbacks owned elsewhere.
            ?: current
    }

    fun reset(current: String?, remembered: String?): String? = defaultSelection(remembered)
        ?: current.takeIf {
            val selectionContext = context.withSelection(active.find { it.uniqueId == current }?.selectionInfo())
            active.none { info -> info.resolvesVisible(selectionContext) }
        }

    private fun defaultSelection(remembered: String?): String? =
        tab.authoredDefaultSelection(context, active)?.uniqueId
            ?: outside.authoredDefaultSelection(context, active)?.uniqueId
            ?: remembered?.takeIf { rendersWhenSelected(it) }
            ?: tab.firstOrNull { canSelect(it) }?.uniqueId
            ?: outside.takeIf { hasAuthoredDefaultOutsideTabs }
                ?.firstOrNull { canSelect(it) }?.uniqueId

    private fun rendersWhenSelected(uniqueId: String): Boolean = active.rendersWhenSelected(uniqueId, context)

    // Unauthored fallbacks must render independently of selection and stay visible after selection.
    private fun canSelect(info: AvailablePackages.Info): Boolean = info.canSelect(context, active)
}

private fun List<AvailablePackages.Info>.inPresentation(key: SheetPresentationKey?): List<AvailablePackages.Info> =
    filter { it.sheetPresentationKey == null || it.sheetPresentationKey === key }

internal fun AvailablePackages.Info.selectionInfo(): SelectedPackageInfo = SelectedPackageInfo(
    rcPackage = pkg,
    resolvedOffer = resolvedOffer,
    uniqueId = uniqueId,
    offerEligibility = calculateOfferEligibility(resolvedOffer, pkg),
)

internal fun AvailablePackages.Info.resolvesVisible(context: ComponentVisibilityContext): Boolean =
    visibilityPaths?.any { it.isVisible(context) }
        ?: (context.resolve(visibilityOverrides, this)?.partial?.visible ?: visible)

private fun List<AvailablePackages.Info>.rendersWhenSelected(
    uniqueId: String,
    context: ComponentVisibilityContext,
): Boolean = any { it.uniqueId == uniqueId && it.resolvesVisible(context.withSelection(it.selectionInfo())) }

internal fun AvailablePackages.Info.canSelect(
    context: ComponentVisibilityContext,
    visiblePackages: List<AvailablePackages.Info>,
): Boolean = resolvesVisible(context.withSelection(null)) && visiblePackages.rendersWhenSelected(uniqueId, context)

/** Authored defaults may reveal their content when selected, but must stay visible in that state. */
internal fun List<AvailablePackages.Info>.authoredDefaultSelection(
    context: ComponentVisibilityContext,
    visiblePackages: List<AvailablePackages.Info> = this,
): AvailablePackages.Info? = firstOrNull {
    it.isSelectedByDefault && visiblePackages.rendersWhenSelected(it.uniqueId, context)
}

internal fun List<AvailablePackages.Info>.defaultSelection(
    context: ComponentVisibilityContext,
    visiblePackages: List<AvailablePackages.Info> = this,
): AvailablePackages.Info? =
    authoredDefaultSelection(context, visiblePackages) ?: firstOrNull { it.canSelect(context, visiblePackages) }
