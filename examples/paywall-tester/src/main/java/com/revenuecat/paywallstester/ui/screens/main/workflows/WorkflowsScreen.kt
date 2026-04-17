package com.revenuecat.paywallstester.ui.screens.main.workflows

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Suppress("ModifierMissing")
@Composable
fun WorkflowsScreen(
    navigateToWorkflowScreen: (String) -> Unit,
) {
    var workflowId by remember { mutableStateOf("") }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Enter a workflow/offering identifier to render via the Paywall composable. " +
                    "Build the SDK with -Prevenuecat.useWorkflowsEndpoint=true to route through /workflows.",
            )
            OutlinedTextField(
                value = workflowId,
                onValueChange = { workflowId = it },
                label = { Text("Identifier") },
                modifier = Modifier.fillMaxSize(fraction = 1f),
                singleLine = true,
            )
            Button(
                onClick = { navigateToWorkflowScreen(workflowId) },
                enabled = workflowId.isNotBlank(),
            ) {
                Text("Open paywall")
            }
        }
    }
}
