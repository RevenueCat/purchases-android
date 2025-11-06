package com.revenuecat.purchasetester.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.logInWith
import com.revenuecat.purchases.logOutWith

@Suppress("LongMethod")
@Composable
fun LoginScreen(
    onNavigateToOverview: () -> Unit,
    onNavigateToConfigure: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToProxy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var userId by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onNavigateToProxy) {
                    Text("Proxy")
                }
                TextButton(onClick = onNavigateToLogs) {
                    Text("Logs")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("😻", style = MaterialTheme.typography.displayLarge)

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = userId,
                onValueChange = { userId = it },
                label = { Text("User ID") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (userId.isNotBlank()) {
                        Purchases.sharedInstance.logInWith(
                            userId,
                            { error -> showError = error.message },
                            { _, _ -> onNavigateToOverview() },
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Login with User ID")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (Purchases.sharedInstance.isAnonymous) {
                        onNavigateToOverview()
                    } else {
                        Purchases.sharedInstance.logOutWith(
                            { error -> showError = error.message },
                            { onNavigateToOverview() },
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue as random user")
            }

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(
                onClick = {
                    Purchases.sharedInstance.close()
                    onNavigateToConfigure()
                },
            ) {
                Text("Reset SDK")
            }
        }
    }

    if (showError != null) {
        AlertDialog(
            onDismissRequest = { showError = null },
            title = { Text("Error") },
            text = { Text(showError!!) },
            confirmButton = {
                TextButton(onClick = { showError = null }) {
                    Text("OK")
                }
            },
        )
    }
}
