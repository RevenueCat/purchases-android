package com.revenuecat.paywallstester.ui.screens.main.customvariables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue

private enum class VariableType(val displayName: String) {
    STRING("String"),
    NUMBER("Number"),
    BOOLEAN("Boolean"),
}

/**
 * Convenience overload that accepts a ViewModel directly.
 */
@Composable
fun CustomVariablesEditorDialog(
    viewModel: CustomVariablesViewModel,
    onDismiss: () -> Unit,
) {
    CustomVariablesEditorDialog(
        customVariables = viewModel.customVariables,
        onAddVariable = { name, value -> viewModel.addVariable(name, value) },
        onRemoveVariable = { name -> viewModel.removeVariable(name) },
        onDismiss = onDismiss,
    )
}

@Suppress("LongMethod")
@Composable
fun CustomVariablesEditorDialog(
    customVariables: Map<String, CustomVariableValue>,
    onAddVariable: (String, CustomVariableValue) -> Unit,
    onRemoveVariable: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Custom Variables",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add variable",
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (customVariables.isEmpty()) {
                    Text(
                        text = "No custom variables defined.\nTap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                    ) {
                        items(
                            items = customVariables.entries.sortedBy { it.key }.toList(),
                            key = { it.key },
                        ) { (name, value) ->
                            VariableRow(
                                name = name,
                                value = value,
                                onDelete = { onRemoveVariable(name) },
                            )
                            HorizontalDivider()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddVariableDialog(
            existingKeys = customVariables.keys,
            onAdd = { name, value ->
                onAddVariable(name, value)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun VariableRow(
    name: String,
    value: CustomVariableValue,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.width(8.dp))
                TypeBadge(value = value)
            }
            Text(
                text = value.stringValue,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete variable",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun TypeBadge(value: CustomVariableValue) {
    val (text, color) = when (value) {
        is CustomVariableValue.String -> "String" to MaterialTheme.colorScheme.primary
        is CustomVariableValue.Number -> "Number" to MaterialTheme.colorScheme.tertiary
        is CustomVariableValue.Boolean -> "Bool" to MaterialTheme.colorScheme.secondary
        else -> "Unknown" to MaterialTheme.colorScheme.outline
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
private fun AddVariableDialog(
    existingKeys: Set<String>,
    onAdd: (String, CustomVariableValue) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(VariableType.STRING) }
    var stringValue by remember { mutableStateOf("") }
    var numberValue by remember { mutableStateOf("") }
    var booleanValue by remember { mutableStateOf(false) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    val nameError = when {
        name.isBlank() -> "Name is required"
        name in existingKeys -> "Variable already exists"
        else -> null
    }

    val valueError = when (selectedType) {
        VariableType.STRING -> if (stringValue.isBlank()) "Value is required" else null
        VariableType.NUMBER -> {
            if (numberValue.isBlank()) {
                "Value is required"
            } else if (numberValue.toDoubleOrNull() == null) {
                "Invalid number"
            } else {
                null
            }
        }
        VariableType.BOOLEAN -> null
    }

    val canAdd = nameError == null && valueError == null && name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Variable") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    isError = name.isNotBlank() && nameError != null,
                    supportingText = if (name.isNotBlank() && nameError != null) {
                        { Text(nameError) }
                    } else {
                        null
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false },
                    ) {
                        VariableType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    selectedType = type
                                    typeDropdownExpanded = false
                                },
                            )
                        }
                    }
                }

                when (selectedType) {
                    VariableType.STRING -> {
                        OutlinedTextField(
                            value = stringValue,
                            onValueChange = { stringValue = it },
                            label = { Text("Value") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    VariableType.NUMBER -> {
                        OutlinedTextField(
                            value = numberValue,
                            onValueChange = { numberValue = it },
                            label = { Text("Value") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = numberValue.isNotBlank() && numberValue.toDoubleOrNull() == null,
                            supportingText = if (numberValue.isNotBlank() && numberValue.toDoubleOrNull() == null) {
                                { Text("Invalid number") }
                            } else {
                                null
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    VariableType.BOOLEAN -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Value")
                            Switch(
                                checked = booleanValue,
                                onCheckedChange = { booleanValue = it },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = when (selectedType) {
                        VariableType.STRING -> CustomVariableValue.String(stringValue)
                        VariableType.NUMBER -> CustomVariableValue.Number(numberValue.toDouble())
                        VariableType.BOOLEAN -> CustomVariableValue.Boolean(booleanValue)
                    }
                    onAdd(name, value)
                },
                enabled = canAdd,
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun CustomVariablesEditorDialogPreview() {
    CustomVariablesEditorDialog(
        customVariables = mapOf(
            "user_name" to CustomVariableValue.String("John"),
            "user_points" to CustomVariableValue.Number(100),
            "is_premium" to CustomVariableValue.Boolean(true),
        ),
        onAddVariable = { _, _ -> },
        onRemoveVariable = {},
        onDismiss = {},
    )
}

@Preview
@Composable
private fun CustomVariablesEditorDialogEmptyPreview() {
    CustomVariablesEditorDialog(
        customVariables = emptyMap(),
        onAddVariable = { _, _ -> },
        onRemoveVariable = {},
        onDismiss = {},
    )
}
