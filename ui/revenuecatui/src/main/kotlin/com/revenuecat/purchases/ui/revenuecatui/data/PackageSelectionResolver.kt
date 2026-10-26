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
        tab.firstOrNull { it.isSelectedByDefault && canSelect(it) }?.uniqueId
            ?: outside.firstOrNull { it.isSelectedByDefault && canSelect(it) }?.uniqueId
            ?: remembered?.takeIf { rendersWhenSelected(it) }
            ?: tab.firstOrNull { canSelect(it) }?.uniqueId
            ?: outside.takeIf { infos -> infos.any { it.isSelectedByDefault } }
                ?.firstOrNull { canSelect(it) }?.uniqueId

    private fun rendersWhenSelected(uniqueId: String): Boolean = active
        .filter { it.uniqueId == uniqueId }
        .any { it.resolvesVisible(context.withSelection(it.selectionInfo())) }

    // Candidates must render independently of selection and stay visible after selection.
    private fun canSelect(info: AvailablePackages.Info): Boolean =
        info.resolvesVisible(context.withSelection(null)) && rendersWhenSelected(info.uniqueId)
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

internal fun List<AvailablePackages.Info>.defaultSelection(
    context: ComponentVisibilityContext,
): AvailablePackages.Info? =
    firstOrNull { it.isSelectedByDefault && it.resolvesVisible(context) }
        ?: firstOrNull { it.resolvesVisible(context) }
