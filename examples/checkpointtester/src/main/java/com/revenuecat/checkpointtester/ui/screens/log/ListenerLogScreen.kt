package com.revenuecat.checkpointtester.ui.screens.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.checkpointtester.checkpoints.CheckpointEventLog
import com.revenuecat.checkpointtester.ui.theme.CheckpointTesterTheme

/**
 * Renders what the app-wide [com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointListener] registered in
 * MainApplication observed, so hits and results are visible regardless of which screen triggered them.
 */
@Composable
fun ListenerLogScreen(modifier: Modifier = Modifier) {
    val events by CheckpointEventLog.events.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Every checkpoint hit and result seen by the global CheckpointListener.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = CheckpointEventLog::clear, enabled = events.isNotEmpty()) {
            Text(text = "Clear")
        }
        if (events.isEmpty()) {
            Text(
                text = "Listener events will appear here as checkpoints run.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(events) { event ->
                    Text(
                        text = event,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ListenerLogScreenPreview() {
    CheckpointTesterTheme {
        ListenerLogScreen()
    }
}
