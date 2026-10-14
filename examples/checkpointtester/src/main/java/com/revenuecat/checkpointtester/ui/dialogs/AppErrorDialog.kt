package com.revenuecat.checkpointtester.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.revenuecat.checkpointtester.checkpoints.ParkedErrorPresenter
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI

/**
 * The dialog behind the app's error presenters: says which presenter received the error, where the error came from
 * and at which stage of the flow. Acknowledging it, or dismissing it with an outside tap or system back, completes
 * the presentation; the SDK decides what the flow does next from the stage.
 */
@OptIn(InviteOnlyCheckpointsAPI::class)
@Composable
fun AppErrorDialog(request: ParkedErrorPresenter.Request, presenterName: String) {
    val params = request.params
    val error = params.error
    AlertDialog(
        onDismissRequest = { request.finish() },
        title = { Text(text = "Handled by the $presenterName error presenter") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailLine(label = "Checkpoint", value = params.checkpointIdentifier)
                DetailLine(
                    label = "Error",
                    value = "${error.code.name}: ${error.underlyingErrorMessage ?: error.message}",
                )
                DetailLine(label = "Stage", value = params.stage.toString())
            }
        },
        confirmButton = {
            TextButton(onClick = { request.finish() }) { Text(text = "OK") }
        },
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
