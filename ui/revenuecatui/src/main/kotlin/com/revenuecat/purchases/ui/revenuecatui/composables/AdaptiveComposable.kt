package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints

@Composable
internal fun RowScope.AdaptiveComposable(composables: List<@Composable () -> Unit>) {
    var maxSize by remember { mutableStateOf(0) }
    val viewSizes = remember { mutableStateListOf<Int>().also { it.addAll(List(composables.size) { 0 }) } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                if (coordinates.size.width != maxSize) {
                    maxSize = coordinates.size.width
                }
            }
            .align(Alignment.CenterVertically),
    ) {
        // Offscreen measurement
        Box(
            Modifier
                .graphicsLayer(alpha = 0.0f)
                .fillMaxWidth()
                .wrapContentWidth(unbounded = true)
                .align(Alignment.Center),
        ) {
            composables.forEachIndexed { index, view ->
                Box(
                    Modifier.layout { measurable, _ ->
                        if (viewSizes[index] == 0) {
                            viewSizes[index] = measurable.measure(Constraints()).width
                        }

                        layout(0, 0) {}
                    },
                ) {
                    view()
                }
            }
        }

        val selectedIndex by remember {
            derivedStateOf {
                for (i in viewSizes.indices) {
                    if (viewSizes[i] <= maxSize) {
                        return@derivedStateOf i
                    }
                }

                return@derivedStateOf 0
            }
        }

        // Display the selected view
        Box(modifier = Modifier.align(Alignment.Center)) {
            composables[selectedIndex]()
        }
    }
}
