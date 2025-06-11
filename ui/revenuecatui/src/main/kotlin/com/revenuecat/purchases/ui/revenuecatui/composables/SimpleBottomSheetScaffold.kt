package com.revenuecat.purchases.ui.revenuecatui.composables

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional

private const val ALPHA_SCRIM = 0.6f

/**
 * A non-experimental bottom sheet scaffold. This should be replaced by Material3's BottomSheetScaffold or
 * ModalBottomSheet when those are no longer experimental.
 */
@Composable
internal fun SimpleBottomSheetScaffold(
    sheetState: SimpleSheetState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        Scrim(
            show = sheetState.backgroundBlur,
            radius = 10.dp,
            onClick = { sheetState.hide() },
        ) { content() }

        if (sheetState.visible) {
            BackHandler { sheetState.hide() }

            sheetState.content(this)
        }
    }
}

@Stable
internal class SimpleSheetState {
    @get:JvmSynthetic
    var backgroundBlur by mutableStateOf(false)
        private set

    @get:JvmSynthetic
    var content: @Composable BoxScope.() -> Unit by mutableStateOf({})
        private set

    @get:JvmSynthetic
    var visible by mutableStateOf(false)
        private set

    fun show(backgroundBlur: Boolean, content: @Composable BoxScope.() -> Unit) {
        this.backgroundBlur = backgroundBlur
        this.content = content
        visible = true
    }

    fun hide() {
        backgroundBlur = false
        visible = false
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
