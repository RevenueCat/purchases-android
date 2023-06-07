package com.revenuecat.sample.user

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.sample.ui.theme.green

@Composable
fun UserScreen(
    modifier: Modifier = Modifier,
    viewModel: UserViewModel = viewModel(),
) {
    val uiState = viewModel.uiState.collectAsState()

    uiState.value.displayErrorMessage?.let { errorMessage ->
        Toast.makeText(
            LocalContext.current,
            errorMessage,
            Toast.LENGTH_SHORT,
        ).show()
        viewModel.resetErrorMessage()
    }

    if (uiState.value.shouldStartLoginProcess) {
        Dialog(onDismissRequest = { viewModel.resetLoginProcess() }) {
            LoginDialog(viewModel)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .weight(1f, true)
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            UserInfo(uiState)
        }

        Spacer(modifier = modifier.padding(8.dp))

        Button(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 8.dp),
            onClick = { viewModel.initiateLogIn() },
        ) {
            Text(text = "Login")
        }

        Button(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 8.dp),
            onClick = { viewModel.restorePurchases() },
        ) {
            Text(text = "Restore purchases")
        }

        Spacer(modifier = modifier.padding(16.dp))
    }
}

@Composable
private fun UserInfo(uiState: State<UserState>) {
    Column {
        Text(text = "Current user: ${uiState.value.currentUserId}")
        if (uiState.value.isSubscriber) {
            Text(
                text = "You are a subscriber!",
                color = green,
            )
        } else {
            Text(
                text = "You are not a subscriber",
                color = Color.Red,
            )
        }
    }
}

@Composable
private fun LoginDialog(viewModel: UserViewModel) {
    var userId by remember {
        mutableStateOf("")
    }
    Surface(shape = MaterialTheme.shapes.medium) {
        Column {
            Column(Modifier.padding(24.dp)) {
                Text(text = "Enter user ID")
                Spacer(Modifier.size(16.dp))
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text("User ID") },
                )
            }
            Spacer(Modifier.size(4.dp))
            Row(
                Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(
                    onClick = { viewModel.resetLoginProcess() },
                    content = { Text("CANCEL") },
                )
                TextButton(
                    onClick = {
                        viewModel.logIn(userId)
                        viewModel.resetLoginProcess()
                    },
                    content = { Text("OK") },
                )
            }
        }
    }
}
