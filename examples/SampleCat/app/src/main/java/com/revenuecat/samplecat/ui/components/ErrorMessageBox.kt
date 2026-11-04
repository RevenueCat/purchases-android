package com.revenuecat.samplecat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.samplecat.R
import com.revenuecat.samplecat.ui.theme.SampleCatTheme

/**
 * A styled error message box with red background for displaying error states.
 */
@Composable
fun ErrorMessageBox(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFCDD2))
            .padding(16.dp),
    ) {
        Text(
            text = message,
            color = Color(0xFFB71C1C),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorMessageBoxPreview() {
    SampleCatTheme {
        ErrorMessageBox(
            message = stringResource(R.string.preview_error_message),
            modifier = Modifier.padding(16.dp),
        )
    }
}
