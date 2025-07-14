package com.revenuecat.purchasetester.ui.screens.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchasetester.R
import com.revenuecat.purchasetester.ui.components.input.PurchaseInputField
import com.revenuecat.purchasetester.ui.theme.LIGHT_GRAY

@Composable
internal fun PurchaseLoginScreen(
    modifier: Modifier = Modifier,
    viewModel: PurchaseLoginViewModel = viewModel<PurchaseLoginViewModelImpl>(
        factory = PurchaseLoginViewModelImpl.Factory
    ),
    onNavigateToOverview: () -> Unit = {},
    onNavigateToConfigure: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {},
    onNavigateToProxy: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var userIdText by remember { mutableStateOf("") }

    // Handle navigation side effects
    LaunchedEffect(state.navigateToOverview) {
        if (state.navigateToOverview) {
            onNavigateToOverview()
            viewModel.onNavigationHandled()
        }
    }

    LaunchedEffect(state.navigateToConfigure) {
        if (state.navigateToConfigure) {
            onNavigateToConfigure()
            viewModel.onNavigationHandled()
        }
    }

    LaunchedEffect(state.navigateToLogs) {
        if (state.navigateToLogs) {
            onNavigateToLogs()
            viewModel.onNavigationHandled()
        }
    }

    LaunchedEffect(state.navigateToProxy) {
        if (state.navigateToProxy) {
            onNavigateToProxy()
            viewModel.onNavigationHandled()
        }
    }

    // Show error dialog if there's an error
    state.error?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = { viewModel.onAction(PurchaseLoginActions.OnErrorDismissed) },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onAction(PurchaseLoginActions.OnErrorDismissed) }
                ) {
                    Text("OK")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.loving_cat),
            fontSize = 52.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = stringResource(R.string.login_screen_revenuecat),
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        PurchaseInputField(
            label = "User ID",
            value = userIdText,
            onValueChange = { userIdText = it },
            placeholder = stringResource(R.string.user_id_login_placeholder),
            icon = Icons.Default.Person,
            modifier = Modifier.fillMaxWidth(),
            isOptional = true,
            enabled = !state.isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.onAction(PurchaseLoginActions.OnLogin(userIdText)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !state.isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(stringResource(R.string.user_id_login))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.onAction(PurchaseLoginActions.OnAnonymousUser) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !state.isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSecondary ,
                contentColor = MaterialTheme.colorScheme.secondary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            border = BorderStroke(1.dp, LIGHT_GRAY)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onSecondary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(stringResource(R.string.random_user_login))
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.onAction(PurchaseLoginActions.OnNavigateToProxy) },
                modifier = Modifier.weight(1f),
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.proxy))
            }

            Button(
                onClick = { viewModel.onAction(PurchaseLoginActions.OnNavigateToLogs) },
                modifier = Modifier.weight(1f),
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.logs))
            }


            Button(
                onClick = { viewModel.onAction(PurchaseLoginActions.OnResetSdk) },
                modifier = Modifier.weight(1f),
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(stringResource(R.string.reset_sdk))
            }
        }
    }
}