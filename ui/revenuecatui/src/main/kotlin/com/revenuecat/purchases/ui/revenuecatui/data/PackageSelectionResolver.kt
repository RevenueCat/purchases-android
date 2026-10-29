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
    private val sheet = active.filter { presentedSheet != null && it.sheetPresentationKey === presentedSheet }

    fun initialSelection(): String? = defaultSelection(remembered = null) ?: allHiddenFallback()

    fun switchTab(remembered: String?): String? =
        remembered?.takeIf { rendersWhenSelected(it) }
            ?: defaultSelection(remembered = null)
            ?: remembered?.takeIf { uid -> active.any { it.uniqueId == uid } }
            ?: allHiddenFallback()

    fun enterSheet(current: String?): String? = sheet.defaultSelection(context, active)?.uniqueId
        ?: defaultSelection(remembered = null)
        ?: allHiddenFallback()
        ?: current

    fun reconcile(current: String?, remembered: String?): String? = when {
        current != null && rendersWhenSelected(current) -> current
        else -> defaultSelection(remembered)
            ?: current
            ?: allHiddenFallback()
    }

    fun reset(current: String?, remembered: String?): String? = defaultSelection(remembered)
        ?: current.takeIf {
            val selectionContext = context.withSelection(active.find { it.uniqueId == current }?.selectionInfo())
            active.any { info -> info.uniqueId == current } &&
                active.none { info -> info.resolvesVisible(selectionContext) }
        }
        ?: allHiddenFallback()

    private fun defaultSelection(remembered: String?): String? =
        sheet.authoredDefaultSelection(context, active)?.uniqueId
            ?: tab.authoredDefaultSelection(context, active)?.uniqueId
            ?: outside.authoredDefaultSelection(context, active)?.uniqueId
            ?: remembered?.takeIf { rendersWhenSelected(it) }
            ?: sheet.firstOrNull { canSelect(it) }?.uniqueId
            ?: tab.firstOrNull { canSelect(it) }?.uniqueId
            ?: outside.firstOrNull { canSelect(it) }?.uniqueId

    // Only unselected, unauthored, entirely hidden contexts get a terminal purchase fallback.
    // A visible card that hides itself on selection must not become an automatic candidate.
    private fun allHiddenFallback(): String? {
        val candidates = sheet.ifEmpty { active }
        return candidates.firstOrNull()?.uniqueId.takeIf {
            candidates.none { it.isSelectedByDefault } && candidates.none { info ->
                info.resolvesVisible(context.withSelection(null)) ||
                    info.resolvesVisible(context.withSelection(info.selectionInfo()))
            }
        }
    }

    private fun rendersWhenSelected(uniqueId: String): Boolean = active.rendersWhenSelected(uniqueId, context)

    // Unauthored fallbacks must render independently of selection and stay visible after selection.
    private fun canSelect(info: AvailablePackages.Info): Boolean = info.canSelect(context, active)
}

internal fun List<AvailablePackages.Info>.inPresentation(key: SheetPresentationKey?): List<AvailablePackages.Info> =
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

/** The authored card must render before or after selection, and some copy must remain visible once selected. */
internal fun List<AvailablePackages.Info>.authoredDefaultSelection(
    context: ComponentVisibilityContext,
    visiblePackages: List<AvailablePackages.Info> = this,
): AvailablePackages.Info? = firstOrNull { info ->
    info.isSelectedByDefault &&
        (
            info.resolvesVisible(context.withSelection(null)) ||
                info.resolvesVisible(context.withSelection(info.selectionInfo()))
            ) &&
        visiblePackages.rendersWhenSelected(info.uniqueId, context)
}

internal fun List<AvailablePackages.Info>.defaultSelection(
    context: ComponentVisibilityContext,
    visiblePackages: List<AvailablePackages.Info> = this,
): AvailablePackages.Info? =
    authoredDefaultSelection(context, visiblePackages) ?: firstOrNull { it.canSelect(context, visiblePackages) }
