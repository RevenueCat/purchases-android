package com.revenuecat.purchasetester.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases_sample.R
import com.revenuecat.purchasetester.ui.theme.PurchaseTesterTheme
import kotlinx.coroutines.flow.collectLatest

private object LoginScreenDefaults {
    val TITLE_EMOJI_SIZE: TextUnit = 100.sp
    val VERTICAL_SPACING: Dp = 32.dp
    val BUTTON_SPACING: Dp = 10.dp
    val BUTTON_SPACING_LARGE: Dp = 14.dp
}

@Composable
fun LoginScreen(
    onNavigateToOverview: () -> Unit,
    onNavigateToConfigure: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToProxy: () -> Unit,
) {
    LoginScreenContent(
        onNavigateToOverview = onNavigateToOverview,
        onNavigateToConfigure = onNavigateToConfigure,
        onNavigateToLogs = onNavigateToLogs,
        onNavigateToProxy = onNavigateToProxy,
    )
}

@Composable
private fun LoginScreenContent(
    onNavigateToOverview: () -> Unit,
    onNavigateToConfigure: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToProxy: () -> Unit,
    loginScreenViewModel: LoginScreenViewModel = viewModel<LoginScreenViewModelImpl>(
        factory = LoginScreenViewModelImpl.Factory
    ),
) {
    val viewModelState by loginScreenViewModel.state.collectAsStateWithLifecycle()

    var userId by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val showErrorDialog = errorMessage != null

    LaunchedEffect(Unit) {
        loginScreenViewModel.events.collectLatest { event ->
            when (event) {
                is LoginUiEvent.Error -> {
                    errorMessage = event.message
                }

                LoginUiEvent.NavigateToOverview -> {
                    onNavigateToOverview()
                }

                LoginUiEvent.NavigateToConfigure -> {
                    onNavigateToConfigure()
                }
            }
        }
    }

    LaunchedEffect(viewModelState) {
        if (viewModelState is LoginScreenState.LoginScreenData) {
            val data = viewModelState as LoginScreenState.LoginScreenData
            userId = data.userId
        }
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text(text = "Error") },
            text = { Text(text = errorMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(LoginScreenDefaults.VERTICAL_SPACING)
                .safeDrawingPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.loving_cat),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = LoginScreenDefaults.TITLE_EMOJI_SIZE
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(LoginScreenDefaults.VERTICAL_SPACING))

                OutlinedTextField(
                    value = userId,
                    onValueChange = {
                        userId = it
                        loginScreenViewModel.saveUserId(it)
                    },
                    label = { Text(stringResource(R.string.userId)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(LoginScreenDefaults.BUTTON_SPACING_LARGE))

                Button(
                    onClick = { loginScreenViewModel.loginUser() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.loginWithUserId))
                }

                Spacer(modifier = Modifier.height(LoginScreenDefaults.BUTTON_SPACING_LARGE))

                Button(
                    onClick = { loginScreenViewModel.loginAnonymously() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.continueAsRandomUser))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomEnd),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onNavigateToProxy) {
                    Text(text = stringResource(R.string.proxy))
                }
                Spacer(modifier = Modifier.width(LoginScreenDefaults.BUTTON_SPACING))
                Button(onClick = onNavigateToLogs) {
                    Text(text = stringResource(R.string.logs))
                }
                Spacer(modifier = Modifier.width(LoginScreenDefaults.BUTTON_SPACING))
                Button(onClick = { loginScreenViewModel.resetSDK() }) {
                    Text(stringResource(R.string.resetSdk))
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Login Screen - Light Theme")
@Composable
private fun LoginScreenPreview() {
    PurchaseTesterTheme {
        LoginScreenContent(
            onNavigateToOverview = {},
            onNavigateToConfigure = {},
            onNavigateToLogs = {},
            onNavigateToProxy = {}
        )
    }
}

@Preview(showBackground = true, name = "Login Screen - Dark Theme")
@Composable
private fun LoginScreenPreview_Dark() {
    PurchaseTesterTheme(darkTheme = true) {
        LoginScreenContent(
            onNavigateToOverview = {},
            onNavigateToConfigure = {},
            onNavigateToLogs = {},
            onNavigateToProxy = {}
        )
    }
}
