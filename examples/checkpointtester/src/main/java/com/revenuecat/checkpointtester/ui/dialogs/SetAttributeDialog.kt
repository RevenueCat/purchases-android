package com.revenuecat.checkpointtester.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.checkpointtester.ui.theme.CheckpointTesterTheme
import com.revenuecat.purchases.Purchases

@Composable
fun SetAttributeDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var key by rememberSaveable { mutableStateOf("") }
    var value by rememberSaveable { mutableStateOf("") }
    val hasKey = key.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(text = "Subscriber attribute") },
        text = {
            AttributeFields(
                key = key,
                value = value,
                onKeyChange = { key = it },
                onValueChange = { value = it },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    Purchases.sharedInstance.setAttributes(mapOf(key.trim() to value))
                    onDismiss()
                },
                enabled = hasKey,
            ) {
                Text(text = "Set")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onDismiss) {
                    Text(text = "Cancel")
                }
                TextButton(
                    onClick = {
                        Purchases.sharedInstance.setAttributes(mapOf(key.trim() to null))
                        onDismiss()
                    },
                    enabled = hasKey,
                ) {
                    Text(text = "Unset")
                }
            }
        },
    )
}

@Composable
private fun AttributeFields(
    key: String,
    value: String,
    onKeyChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Subscriber attributes are part of the checkpoint rule evaluation scope, so changing one " +
                "here changes which rule can match on the next checkpoint. Unset deletes the attribute and " +
                "ignores the value field.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = key,
            onValueChange = onKeyChange,
            label = { Text(text = "Attribute key") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(text = "Attribute value") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SetAttributeDialogPreview() {
    CheckpointTesterTheme {
        SetAttributeDialog(onDismiss = {})
    }
}
