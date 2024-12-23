@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.customercenter.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@JvmSynthetic
@Composable
internal fun RestorePurchasesDialog(
    state: RestorePurchasesState,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onContactSupport: () -> Unit,
    isRestoring: Boolean = false,
) {
    var isRestoringState by remember { mutableStateOf(isRestoring) }

    when (state) {
        RestorePurchasesState.INITIAL -> InitialStateDialog(
            isRestoringState = isRestoringState,
            onDismiss = {
                isRestoringState = false
                onDismiss()
            },
            onRestore = {
                isRestoringState = true
                onRestore()
            },
        )
        RestorePurchasesState.PURCHASES_RECOVERED -> PurchasesRecoveredDialog(onDismiss)
        RestorePurchasesState.PURCHASES_NOT_FOUND -> PurchasesNotFoundDialog(
            onDismiss = onDismiss,
            onContactSupport = onContactSupport,
        )
    }
}

@Composable
private fun InitialStateDialog(
    isRestoringState: Boolean,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
) {
    if (isRestoringState) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismiss while restoring */ },
            title = {
                Text(
                    text = "Restoring Purchases...",
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            },
            confirmButton = { },
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Restore Purchases",
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text = "Going to check for previous purchases",
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = {
                Button(onClick = onRestore) {
                    Text("Check Past Purchases")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun PurchasesRecoveredDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Purchases Recovered",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = "Your purchases have been restored successfully",
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Dismiss")
            }
        },
    )
}

@Composable
private fun PurchasesNotFoundDialog(
    onDismiss: () -> Unit,
    onContactSupport: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "No Purchases Found",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = "No previous purchases were found",
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        confirmButton = {
            Button(onClick = onContactSupport) {
                Text("Contact Support")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Dismiss")
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun RestorePurchasesDialogInitialPreview() {
    RestorePurchasesDialog(
        state = RestorePurchasesState.INITIAL,
        onDismiss = {},
        onRestore = {},
        onContactSupport = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun RestorePurchasesDialogRecoveredPreview() {
    RestorePurchasesDialog(
        state = RestorePurchasesState.PURCHASES_RECOVERED,
        onDismiss = {},
        onRestore = {},
        onContactSupport = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun RestorePurchasesDialogNotFoundPreview() {
    RestorePurchasesDialog(
        state = RestorePurchasesState.PURCHASES_NOT_FOUND,
        onDismiss = {},
        onRestore = {},
        onContactSupport = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun RestorePurchasesDialogLoadingPreview() {
    RestorePurchasesDialog(
        state = RestorePurchasesState.INITIAL,
        onDismiss = {},
        onRestore = {},
        onContactSupport = {},
        isRestoring = true,
    )
}
