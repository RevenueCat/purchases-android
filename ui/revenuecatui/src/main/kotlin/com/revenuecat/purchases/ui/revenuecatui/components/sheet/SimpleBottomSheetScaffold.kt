package com.revenuecat.purchases.ui.revenuecatui.components.sheet

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.properties.rememberBackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.applyIfNotNull
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional

private const val ALPHA_SCRIM = 0.6f

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
        Scrim(
            show = sheetState.visible && sheetState.sheet?.backgroundBlur == true,
            radius = 10.dp,
            onClick = { sheetState.hide() },
        ) { content() }

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

@Composable
private fun Scrim(
    show: Boolean,
    radius: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .conditional(show) { blur(radius) },
    ) {
        content()

        if (show) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(onClick = onClick)
                    // Show a translucent scrim on versions older than 12, where performant blur is not supported.
                    .conditional(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        background(Color.Black.copy(alpha = ALPHA_SCRIM))
                    },
            )
        }
    }
}
