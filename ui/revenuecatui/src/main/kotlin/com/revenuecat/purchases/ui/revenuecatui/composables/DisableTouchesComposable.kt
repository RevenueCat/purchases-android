package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun DisableTouchesComposable(
    shouldDisable: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box {
        content()

        if (shouldDisable) {
            // Overlay to capture touches
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                            }
                        }
                    },
            )
        }
    }
}
