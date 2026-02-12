@file:Suppress("TooManyFunctions")

package com.revenuecat.rcttester.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitLogIn
import com.revenuecat.purchases.awaitLogOut
import com.revenuecat.purchases.data.LogInResult
import com.revenuecat.rcttester.config.SDKConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Immutable snapshot of dialog visibility; updates trigger recomposition when held in mutableStateOf. */
private data class DialogsData(
    val showLoginDialog: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val showErrorDialog: Boolean = false,
    val errorMessage: String? = null,
)

@Composable
fun UserSummarySection(
    configuration: SDKConfiguration,
    onConfigurationUpdate: (SDKConfiguration) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var userSummaryState by remember {
        mutableStateOf(
            UserSummaryState(currentAppUserID = "Not configured"),
        )
    }
    var dialogsData by remember { mutableStateOf(DialogsData()) }

    LaunchedEffect(Purchases.isConfigured) {
        if (Purchases.isConfigured) {
            userSummaryState = userSummaryState.copy(
                currentAppUserID = Purchases.sharedInstance.appUserID,
                isAnonymous = Purchases.sharedInstance.isAnonymous,
            )
        }
    }

    LaunchedEffect(Unit) {
        loadCustomerInfo(
            onSuccess = { userSummaryState = userSummaryState.copy(customerInfo = it) },
            onError = { dialogsData = dialogsData.copy(showErrorDialog = true, errorMessage = it) },
        )
    }

    UserSummaryContent(
        modifier = modifier,
        state = userSummaryState,
        callbacks = UserSummaryCallbacks(
            onMenuExpandedChange = { userSummaryState = userSummaryState.copy(menuExpanded = it) },
            onShowLoginDialog = {
                userSummaryState = userSummaryState.copy(menuExpanded = false, newAppUserID = "")
                dialogsData = dialogsData.copy(showLoginDialog = true)
            },
            onShowLogoutDialog = {
                userSummaryState = userSummaryState.copy(menuExpanded = false)
                dialogsData = dialogsData.copy(showLogoutDialog = true)
            },
            onCopyUserID = {
                copyToClipboard(context, userSummaryState.currentAppUserID)
                userSummaryState = userSummaryState.copy(menuExpanded = false)
            },
        ),
    )

    UserSummaryDialogs(
        UserSummaryDialogsParams(
            userSummaryState = userSummaryState,
            setUserSummaryState = { userSummaryState = it },
            dialogsData = dialogsData,
            setDialogsData = { dialogsData = it },
            configuration = configuration,
            context = context,
            coroutineScope = coroutineScope,
            onConfigurationUpdate = onConfigurationUpdate,
        ),
    )
}

private data class UserSummaryDialogsParams(
    val userSummaryState: UserSummaryState,
    val setUserSummaryState: (UserSummaryState) -> Unit,
    val dialogsData: DialogsData,
    val setDialogsData: (DialogsData) -> Unit,
    val configuration: SDKConfiguration,
    val context: Context,
    val coroutineScope: kotlinx.coroutines.CoroutineScope,
    val onConfigurationUpdate: (SDKConfiguration) -> Unit,
)

@Composable
private fun UserSummaryDialogs(params: UserSummaryDialogsParams) {
    if (params.dialogsData.showLoginDialog) {
        LoginDialogForUserSummary(params)
    }
    if (params.dialogsData.showLogoutDialog) {
        LogoutDialogForUserSummary(params)
    }
    if (params.dialogsData.showErrorDialog) {
        ErrorDialog(
            errorMessage = params.dialogsData.errorMessage,
            onDismiss = { params.setDialogsData(params.dialogsData.copy(showErrorDialog = false)) },
        )
    }
}

@Composable
private fun LoginDialogForUserSummary(params: UserSummaryDialogsParams) {
    LoginDialog(
        newAppUserID = params.userSummaryState.newAppUserID,
        onNewAppUserIDChange = { params.setUserSummaryState(params.userSummaryState.copy(newAppUserID = it)) },
        isLoading = params.userSummaryState.isLoading,
        onDismiss = { params.setDialogsData(params.dialogsData.copy(showLoginDialog = false)) },
        onLogin = {
            handleLogin(
                LoginParams(
                    coroutineScope = params.coroutineScope,
                    appUserID = params.userSummaryState.newAppUserID,
                    configuration = params.configuration,
                    context = params.context,
                    onConfigurationUpdate = params.onConfigurationUpdate,
                    onSuccess = { result ->
                        params.setUserSummaryState(
                            params.userSummaryState.copy(
                                customerInfo = result.customerInfo,
                                currentAppUserID = Purchases.sharedInstance.appUserID,
                                isAnonymous = Purchases.sharedInstance.isAnonymous,
                            ),
                        )
                        params.setDialogsData(params.dialogsData.copy(showLoginDialog = false))
                    },
                    onError = {
                        params.setDialogsData(
                            params.dialogsData.copy(showErrorDialog = true, errorMessage = it),
                        )
                    },
                    onLoadingChange = { params.setUserSummaryState(params.userSummaryState.copy(isLoading = it)) },
                ),
            )
        },
    )
}

@Composable
private fun LogoutDialogForUserSummary(params: UserSummaryDialogsParams) {
    LogoutDialog(
        currentAppUserID = params.userSummaryState.currentAppUserID,
        isLoading = params.userSummaryState.isLoading,
        onDismiss = { params.setDialogsData(params.dialogsData.copy(showLogoutDialog = false)) },
        onLogout = {
            handleLogout(
                LogoutParams(
                    coroutineScope = params.coroutineScope,
                    configuration = params.configuration,
                    context = params.context,
                    onConfigurationUpdate = params.onConfigurationUpdate,
                    onSuccess = {
                        params.setUserSummaryState(
                            params.userSummaryState.copy(
                                customerInfo = it,
                                currentAppUserID = Purchases.sharedInstance.appUserID,
                                isAnonymous = Purchases.sharedInstance.isAnonymous,
                            ),
                        )
                        params.setDialogsData(params.dialogsData.copy(showLogoutDialog = false))
                    },
                    onError = {
                        params.setDialogsData(
                            params.dialogsData.copy(showErrorDialog = true, errorMessage = it),
                        )
                    },
                    onLoadingChange = { params.setUserSummaryState(params.userSummaryState.copy(isLoading = it)) },
                ),
            )
        },
    )
}

private data class UserSummaryState(
    val isLoading: Boolean = false,
    val currentAppUserID: String = "Not configured",
    val customerInfo: CustomerInfo? = null,
    val menuExpanded: Boolean = false,
    val isAnonymous: Boolean = true,
    val newAppUserID: String = "",
)

private data class UserSummaryCallbacks(
    val onMenuExpandedChange: (Boolean) -> Unit,
    val onShowLoginDialog: () -> Unit,
    val onShowLogoutDialog: () -> Unit,
    val onCopyUserID: () -> Unit,
)

@Composable
private fun UserSummaryContent(
    state: UserSummaryState,
    callbacks: UserSummaryCallbacks,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        AppUserIDRow(
            state = state,
            callbacks = callbacks,
        )
        EntitlementsRow(customerInfo = state.customerInfo)
    }
}

@Composable
private fun AppUserIDRow(
    state: UserSummaryState,
    callbacks: UserSummaryCallbacks,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(end = 8.dp),
                strokeWidth = 2.dp,
            )
        }
        Text(
            text = state.currentAppUserID,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { callbacks.onMenuExpandedChange(true) }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
        UserMenu(
            state = state,
            callbacks = callbacks,
        )
    }
}

private data class UserMenuState(
    val expanded: Boolean,
    val isAnonymous: Boolean,
)

private data class UserMenuCallbacks(
    val onDismissRequest: () -> Unit,
    val onShowLoginDialog: () -> Unit,
    val onShowLogoutDialog: () -> Unit,
    val onCopyUserID: () -> Unit,
)

@Composable
private fun UserMenu(
    state: UserSummaryState,
    callbacks: UserSummaryCallbacks,
) {
    val menuState = UserMenuState(
        expanded = state.menuExpanded,
        isAnonymous = state.isAnonymous,
    )
    val menuCallbacks = UserMenuCallbacks(
        onDismissRequest = { callbacks.onMenuExpandedChange(false) },
        onShowLoginDialog = callbacks.onShowLoginDialog,
        onShowLogoutDialog = callbacks.onShowLogoutDialog,
        onCopyUserID = callbacks.onCopyUserID,
    )
    DropdownMenu(
        expanded = menuState.expanded,
        onDismissRequest = menuCallbacks.onDismissRequest,
    ) {
        DropdownMenuItem(
            text = { Text("Log in") },
            onClick = menuCallbacks.onShowLoginDialog,
        )
        if (!menuState.isAnonymous) {
            DropdownMenuItem(
                text = { Text("Log out") },
                onClick = menuCallbacks.onShowLogoutDialog,
            )
        }
        DropdownMenuItem(
            text = { Text("Copy") },
            onClick = menuCallbacks.onCopyUserID,
        )
    }
}

@Composable
private fun EntitlementsRow(customerInfo: CustomerInfo?) {
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
            text = formatEntitlements(customerInfo),
            style = MaterialTheme.typography.bodyMedium,
            color = getEntitlementsColor(customerInfo),
        )
    }
}

private fun formatEntitlements(customerInfo: CustomerInfo?): String {
    return when {
        customerInfo == null -> "—"
        customerInfo.entitlements.active.isEmpty() -> "None"
        else -> customerInfo.entitlements.active.keys.sorted().joinToString(", ")
    }
}

@Composable
private fun getEntitlementsColor(customerInfo: CustomerInfo?): androidx.compose.ui.graphics.Color {
    return if (customerInfo?.entitlements?.active?.isEmpty() == true) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
}

@Composable
private fun LoginDialog(
    newAppUserID: String,
    onNewAppUserIDChange: (String) -> Unit,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onLogin: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log in") },
        text = {
            OutlinedTextField(
                value = newAppUserID,
                onValueChange = onNewAppUserIDChange,
                label = { Text("App User ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onLogin,
                enabled = newAppUserID.isNotBlank() && !isLoading && Purchases.isConfigured,
            ) {
                Text("Log in")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun LogoutDialog(
    currentAppUserID: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onLogout: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log out") },
        text = { Text("Are you sure you want to log out $currentAppUserID?") },
        confirmButton = {
            TextButton(
                onClick = onLogout,
                enabled = !isLoading && Purchases.isConfigured,
            ) {
                Text("Log out")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ErrorDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = { Text(errorMessage ?: "Unknown error") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
    )
}

private suspend fun loadCustomerInfo(
    onSuccess: (CustomerInfo) -> Unit,
    onError: (String?) -> Unit,
) {
    if (!Purchases.isConfigured) return
    try {
        onSuccess(Purchases.sharedInstance.awaitCustomerInfo())
    } catch (e: PurchasesException) {
        onError(e.message)
    }
}

private data class LoginParams(
    val coroutineScope: kotlinx.coroutines.CoroutineScope,
    val appUserID: String,
    val configuration: SDKConfiguration,
    val context: Context,
    val onConfigurationUpdate: (SDKConfiguration) -> Unit,
    val onSuccess: (LogInResult) -> Unit,
    val onError: (String?) -> Unit,
    val onLoadingChange: (Boolean) -> Unit,
)

private fun handleLogin(params: LoginParams) {
    if (!Purchases.isConfigured) {
        params.onError("SDK not configured")
        return
    }
    params.coroutineScope.launch {
        withContext(Dispatchers.Main.immediate) { params.onLoadingChange(true) }
        try {
            val result = Purchases.sharedInstance.awaitLogIn(params.appUserID)
            val updatedConfig = params.configuration.copy(appUserID = params.appUserID)
            updatedConfig.save(params.context)
            withContext(Dispatchers.Main.immediate) {
                params.onConfigurationUpdate(updatedConfig)
                params.onSuccess(result)
            }
        } catch (e: PurchasesException) {
            withContext(Dispatchers.Main.immediate) { params.onError(e.message) }
        } finally {
            withContext(Dispatchers.Main.immediate) { params.onLoadingChange(false) }
        }
    }
}

private data class LogoutParams(
    val coroutineScope: kotlinx.coroutines.CoroutineScope,
    val configuration: SDKConfiguration,
    val context: Context,
    val onConfigurationUpdate: (SDKConfiguration) -> Unit,
    val onSuccess: (CustomerInfo) -> Unit,
    val onError: (String?) -> Unit,
    val onLoadingChange: (Boolean) -> Unit,
)

private fun handleLogout(params: LogoutParams) {
    if (!Purchases.isConfigured) {
        params.onError("SDK not configured")
        return
    }
    params.coroutineScope.launch {
        withContext(Dispatchers.Main.immediate) { params.onLoadingChange(true) }
        try {
            val customerInfo = Purchases.sharedInstance.awaitLogOut()
            val updatedConfig = params.configuration.copy(appUserID = "")
            updatedConfig.save(params.context)
            withContext(Dispatchers.Main.immediate) {
                params.onConfigurationUpdate(updatedConfig)
                params.onSuccess(customerInfo)
            }
        } catch (e: PurchasesException) {
            withContext(Dispatchers.Main.immediate) { params.onError(e.message) }
        } finally {
            withContext(Dispatchers.Main.immediate) { params.onLoadingChange(false) }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("App User ID", text)
    clipboard.setPrimaryClip(clip)
}
