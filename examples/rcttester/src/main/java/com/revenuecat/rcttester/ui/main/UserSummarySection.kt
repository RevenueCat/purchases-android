package com.revenuecat.rcttester.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.revenuecat.rcttester.config.SDKConfiguration
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitLogIn
import com.revenuecat.purchases.awaitLogOut
import kotlinx.coroutines.launch

@Composable
fun UserSummarySection(
    configuration: SDKConfiguration,
    onConfigurationUpdate: (SDKConfiguration) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var customerInfo by remember { mutableStateOf<CustomerInfo?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var newAppUserID by remember { mutableStateOf("") }
    var currentAppUserID by remember { mutableStateOf("Not configured") }
    var isAnonymous by remember { mutableStateOf(true) }

    // Update user ID and anonymous status when SDK is configured
    LaunchedEffect(Purchases.isConfigured) {
        if (Purchases.isConfigured) {
            currentAppUserID = Purchases.sharedInstance.appUserID
            isAnonymous = Purchases.sharedInstance.isAnonymous
        }
    }

    LaunchedEffect(Unit) {
        if (Purchases.isConfigured) {
            try {
                customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
            } catch (e: PurchasesException) {
                errorMessage = e.message
            }
        }
    }

    Column(modifier = modifier) {
        // App User ID Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp),
                    strokeWidth = 2.dp,
                )
            }
            Text(
                text = currentAppUserID,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Log in") },
                    onClick = {
                        newAppUserID = ""
                        menuExpanded = false
                        showLoginDialog = true
                    },
                )
                if (!isAnonymous) {
                    DropdownMenuItem(
                        text = { Text("Log out") },
                        onClick = {
                            menuExpanded = false
                            showLogoutDialog = true
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Copy") },
                    onClick = {
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("App User ID", currentAppUserID)
                        clipboard.setPrimaryClip(clip)
                        menuExpanded = false
                    },
                )
            }
        }

        // Entitlements Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            Text(
                text = "Entitlements",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = when {
                    customerInfo == null -> "—"
                    customerInfo!!.entitlements.active.isEmpty() -> "None"
                    else -> customerInfo!!.entitlements.active.keys.sorted().joinToString(", ")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (customerInfo?.entitlements?.active?.isEmpty() == true) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }

    // Login Dialog
    if (showLoginDialog) {
        AlertDialog(
            onDismissRequest = { showLoginDialog = false },
            title = { Text("Log in") },
            text = {
                OutlinedTextField(
                    value = newAppUserID,
                    onValueChange = { newAppUserID = it },
                    label = { Text("App User ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!Purchases.isConfigured) {
                            errorMessage = "SDK not configured"
                            showErrorDialog = true
                            return@TextButton
                        }
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                val result = Purchases.sharedInstance.awaitLogIn(newAppUserID)
                                customerInfo = result.customerInfo
                                // Update stored configuration
                                val updatedConfig = configuration.copy(appUserID = newAppUserID)
                                updatedConfig.save(context)
                                onConfigurationUpdate(updatedConfig)
                                // Update local state
                                currentAppUserID = Purchases.sharedInstance.appUserID
                                isAnonymous = Purchases.sharedInstance.isAnonymous
                                showLoginDialog = false
                            } catch (e: PurchasesException) {
                                errorMessage = e.message
                                showErrorDialog = true
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = newAppUserID.isNotBlank() && !isLoading && Purchases.isConfigured,
                ) {
                    Text("Log in")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoginDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log out") },
            text = { Text("Are you sure you want to log out $currentAppUserID?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!Purchases.isConfigured) {
                            errorMessage = "SDK not configured"
                            showErrorDialog = true
                            return@TextButton
                        }
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                customerInfo = Purchases.sharedInstance.awaitLogOut()
                                // Clear the stored app user ID since we're now anonymous
                                val updatedConfig = configuration.copy(appUserID = "")
                                updatedConfig.save(context)
                                onConfigurationUpdate(updatedConfig)
                                // Update local state
                                currentAppUserID = Purchases.sharedInstance.appUserID
                                isAnonymous = Purchases.sharedInstance.isAnonymous
                                showLogoutDialog = false
                            } catch (e: PurchasesException) {
                                errorMessage = e.message
                                showErrorDialog = true
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading && Purchases.isConfigured,
                ) {
                    Text("Log out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Error Dialog
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Error") },
            text = { Text(errorMessage ?: "Unknown error") },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            },
        )
    }
}
