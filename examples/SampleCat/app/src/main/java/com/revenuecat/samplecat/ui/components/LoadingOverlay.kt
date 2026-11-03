package com.revenuecat.samplecat.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.samplecat.ui.components.spinner.SpinnerContainer
import com.revenuecat.samplecat.ui.theme.SampleCatTheme

/**
 * A full-screen loading overlay with a centered spinner.
 * Use this for initial loading states where the entire screen should show a loader.
 */
@Composable
fun LoadingOverlay(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        SpinnerContainer()
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingOverlayPreview() {
    SampleCatTheme {
        LoadingOverlay()
    }
}
