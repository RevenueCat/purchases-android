package com.revenuecat.paywallstester.ui.screens.main.appinfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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

@Composable
fun SubscriberAttributesDialog(
    attributes: Map<String, String>,
    onSet: (key: String, value: String) -> Unit,
    onClear: (key: String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    var key by rememberSaveable { mutableStateOf("") }
    var value by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Subscriber attributes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Attributes are sent to RevenueCat and can be used by checkpoint rules. " +
                        "Only attributes set from this app are listed here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text(text = "Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(text = "Value") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        onSet(key, value)
                        key = ""
                        value = ""
                    },
                    enabled = key.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Set")
                }
                AttributesList(attributes = attributes, onClear = onClear)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onClearAll, enabled = attributes.isNotEmpty()) {
                Text(text = "Clear all")
            }
        },
    )
}

@Composable
private fun AttributesList(attributes: Map<String, String>, onClear: (key: String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Set from this app",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (attributes.isEmpty()) {
            Text(
                text = "No attributes set from this app yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                items(attributes.entries.sortedBy { it.key }, key = { it.key }) { (attributeKey, attributeValue) ->
                    ListItem(
                        headlineContent = { Text(text = attributeKey) },
                        supportingContent = { Text(text = attributeValue) },
                        trailingContent = {
                            IconButton(onClick = { onClear(attributeKey) }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear $attributeKey")
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Suppress("EmptyFunctionBlock")
@Preview(showBackground = true)
@Composable
private fun SubscriberAttributesDialogPreview() {
    SubscriberAttributesDialog(
        attributes = mapOf("plan" to "trial", "cohort" to "beta"),
        onSet = { _, _ -> },
        onClear = {},
        onClearAll = {},
        onDismiss = {},
    )
}
