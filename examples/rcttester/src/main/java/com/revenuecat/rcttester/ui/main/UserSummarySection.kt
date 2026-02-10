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
import kotlinx.coroutines.launch

@Composable
fun UserSummarySection(
    configuration: SDKConfiguration,
    onConfigurationUpdate: (SDKConfiguration) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val state = rememberUserSummaryState()
    val dialogsState = rememberDialogsState()

    LaunchedEffect(Purchases.isConfigured) {
        updateUserIDState(state)
    }

    LaunchedEffect(Unit) {
        loadCustomerInfo(
            onSuccess = { state.customerInfo = it },
            onError = { dialogsState.errorMessage = it },
        )
    }

    UserSummaryContent(
        modifier = modifier,
        state = state.toUserSummaryState(),
        callbacks = createUserSummaryCallbacks(
            context = context,
            state = state,
            dialogsState = dialogsState,
        ),
    )

    UserSummaryDialogs(
        UserSummaryDialogsParams(
            state = state,
            dialogsState = dialogsState,
            configuration = configuration,
            context = context,
            coroutineScope = coroutineScope,
            onConfigurationUpdate = onConfigurationUpdate,
        ),
    )
}

@Composable
private fun rememberUserSummaryState(): MutableUserSummaryState {
    return remember {
        MutableUserSummaryState(
            customerInfo = null,
            isLoading = false,
            currentAppUserID = "Not configured",
            isAnonymous = true,
            menuExpanded = false,
            newAppUserID = "",
        )
    }
}

@Composable
private fun rememberDialogsState(): MutableDialogsState {
    return remember {
        MutableDialogsState(
            showLoginDialog = false,
            showLogoutDialog = false,
            showErrorDialog = false,
            errorMessage = null,
        )
    }
}

private class MutableUserSummaryState {
    var customerInfo: CustomerInfo? = null
    var isLoading: Boolean = false
    var currentAppUserID: String = "Not configured"
    var isAnonymous: Boolean = true
    var menuExpanded: Boolean = false
    var newAppUserID: String = ""

    constructor(
        customerInfo: CustomerInfo?,
        isLoading: Boolean,
        currentAppUserID: String,
        isAnonymous: Boolean,
        menuExpanded: Boolean,
        newAppUserID: String,
    ) {
        this.customerInfo = customerInfo
        this.isLoading = isLoading
        this.currentAppUserID = currentAppUserID
        this.isAnonymous = isAnonymous
        this.menuExpanded = menuExpanded
        this.newAppUserID = newAppUserID
    }

    fun toUserSummaryState(): UserSummaryState {
        return UserSummaryState(
            isLoading = isLoading,
            currentAppUserID = currentAppUserID,
            customerInfo = customerInfo,
            menuExpanded = menuExpanded,
            isAnonymous = isAnonymous,
        )
    }
}

private class MutableDialogsState {
    var showLoginDialog: Boolean = false
    var showLogoutDialog: Boolean = false
    var showErrorDialog: Boolean = false
    var errorMessage: String? = null

    constructor(
        showLoginDialog: Boolean,
        showLogoutDialog: Boolean,
        showErrorDialog: Boolean,
        errorMessage: String?,
    ) {
        this.showLoginDialog = showLoginDialog
        this.showLogoutDialog = showLogoutDialog
        this.showErrorDialog = showErrorDialog
        this.errorMessage = errorMessage
    }
}

private fun updateUserIDState(state: MutableUserSummaryState) {
    if (Purchases.isConfigured) {
        state.currentAppUserID = Purchases.sharedInstance.appUserID
        state.isAnonymous = Purchases.sharedInstance.isAnonymous
    }
}

private fun createUserSummaryCallbacks(
    context: Context,
    state: MutableUserSummaryState,
    dialogsState: MutableDialogsState,
): UserSummaryCallbacks {
    return UserSummaryCallbacks(
        onMenuExpandedChange = { state.menuExpanded = it },
        onShowLoginDialog = {
            state.newAppUserID = ""
            state.menuExpanded = false
            dialogsState.showLoginDialog = true
        },
        onShowLogoutDialog = {
            state.menuExpanded = false
            dialogsState.showLogoutDialog = true
        },
        onCopyUserID = {
            copyToClipboard(context, state.currentAppUserID)
            state.menuExpanded = false
        },
    )
}

private data class UserSummaryDialogsParams(
    val state: MutableUserSummaryState,
    val dialogsState: MutableDialogsState,
    val configuration: SDKConfiguration,
    val context: Context,
    val coroutineScope: kotlinx.coroutines.CoroutineScope,
    val onConfigurationUpdate: (SDKConfiguration) -> Unit,
)

@Composable
private fun UserSummaryDialogs(params: UserSummaryDialogsParams) {
    if (params.dialogsState.showLoginDialog) {
        LoginDialogForUserSummary(params)
    }
    if (params.dialogsState.showLogoutDialog) {
        LogoutDialogForUserSummary(params)
    }
    if (params.dialogsState.showErrorDialog) {
        ErrorDialog(
            errorMessage = params.dialogsState.errorMessage,
            onDismiss = { params.dialogsState.showErrorDialog = false },
        )
    }
}

@Composable
private fun LoginDialogForUserSummary(params: UserSummaryDialogsParams) {
    LoginDialog(
        newAppUserID = params.state.newAppUserID,
        onNewAppUserIDChange = { params.state.newAppUserID = it },
        isLoading = params.state.isLoading,
        onDismiss = { params.dialogsState.showLoginDialog = false },
        onLogin = {
            handleLogin(
                LoginParams(
                    coroutineScope = params.coroutineScope,
                    appUserID = params.state.newAppUserID,
                    configuration = params.configuration,
                    context = params.context,
                    onConfigurationUpdate = params.onConfigurationUpdate,
                    onSuccess = { result ->
                        params.state.customerInfo = result.customerInfo
                        params.state.currentAppUserID = Purchases.sharedInstance.appUserID
                        params.state.isAnonymous = Purchases.sharedInstance.isAnonymous
                        params.dialogsState.showLoginDialog = false
                    },
                    onError = {
                        params.dialogsState.errorMessage = it
                        params.dialogsState.showErrorDialog = true
                    },
                    onLoadingChange = { params.state.isLoading = it },
                ),
            )
        },
    )
}

@Composable
private fun LogoutDialogForUserSummary(params: UserSummaryDialogsParams) {
    LogoutDialog(
        currentAppUserID = params.state.currentAppUserID,
        isLoading = params.state.isLoading,
        onDismiss = { params.dialogsState.showLogoutDialog = false },
        onLogout = {
            handleLogout(
                LogoutParams(
                    coroutineScope = params.coroutineScope,
                    configuration = params.configuration,
                    context = params.context,
                    onConfigurationUpdate = params.onConfigurationUpdate,
                    onSuccess = {
                        params.state.customerInfo = it
                        params.state.currentAppUserID = Purchases.sharedInstance.appUserID
                        params.state.isAnonymous = Purchases.sharedInstance.isAnonymous
                        params.dialogsState.showLogoutDialog = false
                    },
                    onError = {
                        params.dialogsState.errorMessage = it
                        params.dialogsState.showErrorDialog = true
                    },
                    onLoadingChange = { params.state.isLoading = it },
                ),
            )
        },
    )
}

private data class UserSummaryState(
    val isLoading: Boolean,
    val currentAppUserID: String,
    val customerInfo: CustomerInfo?,
    val menuExpanded: Boolean,
    val isAnonymous: Boolean,
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
        params.onLoadingChange(true)
        try {
            val result = Purchases.sharedInstance.awaitLogIn(params.appUserID)
            val updatedConfig = params.configuration.copy(appUserID = params.appUserID)
            updatedConfig.save(params.context)
            params.onConfigurationUpdate(updatedConfig)
            params.onSuccess(result)
        } catch (e: PurchasesException) {
            params.onError(e.message)
        } finally {
            params.onLoadingChange(false)
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
        params.onLoadingChange(true)
        try {
            val customerInfo = Purchases.sharedInstance.awaitLogOut()
            val updatedConfig = params.configuration.copy(appUserID = "")
            updatedConfig.save(params.context)
            params.onConfigurationUpdate(updatedConfig)
            params.onSuccess(customerInfo)
        } catch (e: PurchasesException) {
            params.onError(e.message)
        } finally {
            params.onLoadingChange(false)
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("App User ID", text)
    clipboard.setPrimaryClip(clip)
}
