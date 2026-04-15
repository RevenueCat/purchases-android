@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.paywallstester.ui.screens.main.workflows

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.awaitGetWorkflows
import com.revenuecat.purchases.common.workflows.WorkflowSummary
import com.revenuecat.purchases.ui.revenuecatui.workflow.WorkflowView

@Suppress("ModifierMissing")
@Composable
fun WorkflowsScreen() {
    var workflows by remember { mutableStateOf<List<WorkflowSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var activeIdentifier by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val response = Purchases.sharedInstance.awaitGetWorkflows()
            workflows = response.workflows
            isLoading = false
        } catch (e: Exception) {
            errorMessage = e.message ?: "Unknown error"
            isLoading = false
        }
    }

    Scaffold { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Error: $errorMessage",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            workflows.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No workflows found")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    items(workflows) { workflow ->
                        Column {
                            ListItem(
                                headlineContent = { Text(workflow.displayName) },
                                supportingContent = { Text(workflow.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeIdentifier = workflow.id },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    activeIdentifier?.let { id ->
        Dialog(
            onDismissRequest = { activeIdentifier = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            WorkflowView(
                identifier = id,
                dismissRequest = { activeIdentifier = null },
            )
        }
    }
}
