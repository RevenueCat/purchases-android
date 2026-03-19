@file:JvmSynthetic
@file:Suppress("LongMethod")

package com.revenuecat.purchases.ui.revenuecatui.components.nav

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberBackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.NavHostComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.composables.SimpleBottomSheetScaffold
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional

@Composable
internal fun NavHostComponentView(
    navHostStyle: NavHostComponentStyle,
    state: PaywallState.Loaded.Components,
    clickHandler: suspend (PaywallAction.External) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navState = remember(navHostStyle) { NavHostState(navHostStyle.startPage) }
    val currentPage = navHostStyle.pages[navState.currentPageId] ?: return
    val background = rememberBackgroundStyle(currentPage.background)

    val onClick: suspend (PaywallAction) -> Unit = { action: PaywallAction ->
        when (action) {
            is PaywallAction.Internal.NavigateToPage -> navState.navigateTo(action.pageId)
            is PaywallAction.Internal.Close -> clickHandler(PaywallAction.External.NavigateBack)
            is PaywallAction.External.NavigateBack -> {
                if (!navState.navigateBack()) {
                    clickHandler(action)
                }
            }
            is PaywallAction.External -> clickHandler(action)
            is PaywallAction.Internal -> {
                // Handle other internal actions (e.g., sheet navigation)
                handleInternalAction(action, state, clickHandler)
            }
        }
    }

    val safeDrawingInsets = WindowInsets.safeDrawing

    SimpleBottomSheetScaffold(
        sheetState = state.sheet,
        modifier = modifier.background(background),
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(safeDrawingInsets.only(WindowInsetsSides.Horizontal)),
        ) {
            // Sticky header — instant swap, no animation
            currentPage.stickyHeader?.let {
                ComponentView(
                    style = it,
                    state = state,
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(safeDrawingInsets.only(WindowInsetsSides.Top)),
                )
            }

            // Body — animated slide left/right
            AnimatedContent(
                targetState = navState.currentPageId,
                transitionSpec = {
                    if (navState.direction == NavHostState.Direction.BACKWARD) {
                        slideInHorizontally(initialOffsetX = { -it }) togetherWith
                            slideOutHorizontally(targetOffsetX = { it })
                    } else {
                        slideInHorizontally(initialOffsetX = { it }) togetherWith
                            slideOutHorizontally(targetOffsetX = { -it })
                    }
                },
                label = "NavHostPageTransition",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { pageId ->
                val page = navHostStyle.pages[pageId] ?: return@AnimatedContent
                ComponentView(
                    style = page.stack,
                    state = state,
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                )
            }

            // Sticky footer — instant swap, no animation
            currentPage.stickyFooter?.let {
                ComponentView(
                    style = it,
                    state = state,
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(safeDrawingInsets.only(WindowInsetsSides.Bottom)),
                )
            }
        }
    }
}

private suspend fun handleInternalAction(
    action: PaywallAction.Internal,
    state: PaywallState.Loaded.Components,
    externalClickHandler: suspend (PaywallAction.External) -> Unit,
) {
    when (action) {
        is PaywallAction.Internal.NavigateTo -> when (action.destination) {
            is PaywallAction.Internal.NavigateTo.Destination.Sheet ->
                state.sheet.show(action.destination.sheet, state) {
                    when (it) {
                        is PaywallAction.External -> externalClickHandler(it)
                        is PaywallAction.Internal -> handleInternalAction(it, state, externalClickHandler)
                    }
                }
        }
        is PaywallAction.Internal.NavigateToPage -> {
            // Already handled in the main onClick lambda
        }
        is PaywallAction.Internal.Close -> {
            externalClickHandler(PaywallAction.External.NavigateBack)
        }
    }
}

/**
 * Shows the provided sheet as this SimpleSheetState's sheet content.
 * Duplicated from LoadedPaywallComponents to avoid tight coupling for the POC.
 */
private fun com.revenuecat.purchases.ui.revenuecatui.composables.SimpleSheetState.show(
    sheet: ButtonComponentStyle.Action.NavigateTo.Destination.Sheet,
    state: PaywallState.Loaded.Components,
    onClick: suspend (PaywallAction) -> Unit,
) {
    show(
        backgroundBlur = sheet.backgroundBlur,
        content = {
            ComponentView(
                style = sheet.stack,
                state = state,
                onClick = { action ->
                    when (action) {
                        is PaywallAction.External.NavigateBack -> hide()
                        else -> onClick(action)
                    }
                },
                modifier = Modifier
                    .applyIfNotNull(sheet.size) { size(it) }
                    .conditional(sheet.size == null) { fillMaxWidth() },
            )
        },
        onDismiss = {
            state.resetToDefaultPackage()
        },
    )
}
