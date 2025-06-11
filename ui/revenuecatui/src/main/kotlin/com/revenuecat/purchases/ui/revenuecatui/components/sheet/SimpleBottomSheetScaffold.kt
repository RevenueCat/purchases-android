package com.revenuecat.purchases.ui.revenuecatui.components.sheet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberBackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull

/**
 * A non-experimental bottom sheet scaffold. This should be replaced by Material3's BottomSheetScaffold or
 * ModalBottomSheet when those are no longer experimental.
 */
@Composable
internal fun SimpleBottomSheetScaffold(
    sheetState: SimpleSheetState,
    state: PaywallState.Loaded.Components,
    onClick: suspend (PaywallAction) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        content()

        if (sheetState.visible) {
            sheetState.sheet?.also { sheet ->
                BackHandler { sheetState.hide() }

                val backgroundStyle = sheet.background?.let { rememberBackgroundStyle(background = it) }

                ComponentView(
                    style = sheet.stack,
                    state = state,
                    onClick = { action ->
                        when (action) {
                            is PaywallAction.External.NavigateBack -> sheetState.hide()
                            else -> onClick(action)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .applyIfNotNull(sheet.size) { size(it) }
                        .applyIfNotNull(backgroundStyle) { background(it) },
                )
            }
        }
    }
}
