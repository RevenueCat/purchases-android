package com.revenuecat.samplecat.ui.components.spinner

import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.samplecat.ui.theme.AccentColor
import com.revenuecat.samplecat.ui.theme.SampleCatTheme

/**
 * A custom pull-to-refresh indicator using the yarn ball Spinner.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpinnerPullToRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = AccentColor,
) {
    val threshold = PullToRefreshDefaults.PositionalThreshold

    // Animate alpha based on pull progress
    var alpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(state.distanceFraction, isRefreshing) {
        val targetAlpha = when {
            isRefreshing -> 1f
            state.distanceFraction > 0f -> (state.distanceFraction).coerceIn(0f, 1f)
            else -> 0f
        }
        animate(alpha, targetAlpha) { value, _ ->
            alpha = value
        }
    }

    Surface(
        modifier = modifier
            .graphicsLayer {
                translationY = (state.distanceFraction * threshold.toPx()).coerceAtLeast(0f)
            }
            .alpha(alpha),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        tonalElevation = 6.dp,
    ) {
        Spinner(
            tint = tint,
            modifier = Modifier
                .padding(12.dp)
                .size(40.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun SpinnerPullToRefreshIndicatorPreview() {
    SampleCatTheme {
        SpinnerPullToRefreshIndicator(
            state = rememberPullToRefreshState(),
            isRefreshing = true,
        )
    }
}
