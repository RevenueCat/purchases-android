package com.revenuecat.purchases.ui.revenuecatui.composables

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
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

            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                sheetState.content(this)
            }
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

@Preview
@Composable
private fun SimpleBottomSheetScaffold_Preview() {
    val sheetState = SimpleSheetState().apply {
        show(backgroundBlur = true) {
            Column(
                Modifier.background(
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                ).padding(all = 16.dp),
            ) {
                Text(
                    text = "Hello from my bottom sheet",
                )
                Text(
                    text = "It's beautiful, isn't it?",
                )
            }
        }
    }

    SimpleBottomSheetScaffold(
        sheetState = sheetState,
        modifier = Modifier.fillMaxSize().background(Color.Gray),
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Blue),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Hello from my main content",
                )
            }
        },
    )
}
