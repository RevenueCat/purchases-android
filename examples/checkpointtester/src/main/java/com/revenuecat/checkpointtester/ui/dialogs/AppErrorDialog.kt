package com.revenuecat.checkpointtester.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.revenuecat.checkpointtester.checkpoints.ParkedErrorPresenter
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.ErrorPresenter.Completion.Result

/**
 * The dialog behind the app's error presenters: says which presenter received the error and where the error came
 * from, and lets the tester pick how the flow goes on. Dismissing it (outside tap or system back) does what the
 * SDK's own dialog does: retry when the flow can go on, continue otherwise.
 */
@OptIn(InternalRevenueCatAPI::class)
@Composable
fun AppErrorDialog(request: ParkedErrorPresenter.Request, presenterName: String) {
    val params = request.params
    val error = params.error
    AlertDialog(
        onDismissRequest = { request.finish(if (params.flowCanContinue) Result.Retry else Result.Continued) },
        title = { Text(text = "Handled by the $presenterName error presenter") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailLine(label = "Checkpoint", value = params.checkpointIdentifier)
                DetailLine(
                    label = "Error",
                    value = "${error.code.name}: ${error.underlyingErrorMessage ?: error.message}",
                )
                DetailLine(label = "Flow can continue", value = if (params.flowCanContinue) "yes" else "no")
            }
        },
        confirmButton = {
            Row {
                if (params.flowCanContinue) {
                    TextButton(onClick = { request.finish(Result.Retry) }) { Text(text = "Retry") }
                }
                TextButton(onClick = { request.finish(Result.Continued) }) { Text(text = "Continue") }
            }
        },
        dismissButton = {
            TextButton(onClick = { request.finish(Result.NavigatedBack) }) { Text(text = "Back") }
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
