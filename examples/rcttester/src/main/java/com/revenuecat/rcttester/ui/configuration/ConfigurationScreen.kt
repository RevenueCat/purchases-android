package com.revenuecat.rcttester.ui.configuration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.revenuecat.rcttester.config.PurchasesCompletedByType
import com.revenuecat.rcttester.config.SDKConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(
    initialConfiguration: SDKConfiguration,
    onConfigure: (SDKConfiguration) -> Unit,
    modifier: Modifier = Modifier,
) {
    var apiKey by remember { mutableStateOf(initialConfiguration.apiKey) }
    var appUserID by remember { mutableStateOf(initialConfiguration.appUserID) }
    var purchasesAreCompletedBy by remember {
        mutableStateOf(initialConfiguration.purchasesAreCompletedBy)
    }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(modifier = modifier) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
        ) {
            // API Key Section
            Text(
                text = "RevenueCat API Key",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { newValue: String -> apiKey = newValue },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )
            Text(
                text = "Your RevenueCat API key. Can also be set via BuildConfig.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            // App User ID Section
            Text(
                text = "App User ID",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            OutlinedTextField(
                value = appUserID,
                onValueChange = { newValue: String -> appUserID = newValue },
                label = { Text("App User ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )
            Text(
                text = "Leave empty to let the SDK generate an anonymous user ID.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            // Purchases Completed By Section
            Text(
                text = "Purchases Are Completed By",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = purchasesAreCompletedBy.displayName,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Purchases Completed By") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                ) {
                    PurchasesCompletedByType.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = {
                                purchasesAreCompletedBy = option
                                dropdownExpanded = false
                            },
                        )
                    }
                }
            }
            Text(
                text = purchasesCompletedByFooter(purchasesAreCompletedBy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Configure Button
            Button(
                onClick = {
                    // Sanitize API key - trim whitespace and newlines
                    val sanitizedApiKey = apiKey.trim().replace("\n", "").replace("\r", "")
                    onConfigure(
                        SDKConfiguration(
                            apiKey = sanitizedApiKey,
                            appUserID = appUserID.trim(),
                            purchasesAreCompletedBy = purchasesAreCompletedBy,
                        ),
                    )
                },
                enabled = apiKey.trim().isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Configure SDK")
            }
        }
    }
}

private fun purchasesCompletedByFooter(option: PurchasesCompletedByType): String {
    return when (option) {
        PurchasesCompletedByType.REVENUECAT ->
            "All purchases are done through RevenueCat's purchase methods. RevenueCat will also finish transactions after successful receipt posting."
        PurchasesCompletedByType.MY_APP ->
            "The app is responsible for finishing transactions (also known as 'Observer Mode')."
    }
}
