package com.revenuecat.samplecat.ui.components.spinner

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.samplecat.ui.theme.AccentColor
import com.revenuecat.samplecat.ui.theme.SampleCatTheme

/**
 * A spinner with a circular background container for better visibility.
 */
@Composable
fun SpinnerContainer(
    modifier: Modifier = Modifier,
    tint: Color = AccentColor,
) {
    Surface(
        modifier = modifier,
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

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SpinnerContainerPreview() {
    SampleCatTheme {
        SpinnerContainer()
    }
}
