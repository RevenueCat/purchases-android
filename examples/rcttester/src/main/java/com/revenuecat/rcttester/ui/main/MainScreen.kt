package com.revenuecat.rcttester.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.revenuecat.rcttester.config.SDKConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    configuration: SDKConfiguration,
    onConfigurationUpdate: (SDKConfiguration) -> Unit,
    onReconfigure: () -> Unit,
    onNavigateToOfferings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RCTTester") },
                actions = {
                    IconButton(onClick = onReconfigure) {
                        Icon(Icons.Default.Settings, contentDescription = "Reconfigure")
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // SDK Configuration Section
            item {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SDK Configuration",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        ConfigurationSummarySection(configuration = configuration)
                    }
                }
            }

            // User Section
            item {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "User",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        UserSummarySection(
                            configuration = configuration,
                            onConfigurationUpdate = onConfigurationUpdate,
                        )
                    }
                }
            }

            // Offerings Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onNavigateToOfferings),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    ListItem(
                        headlineContent = { Text("View Offerings") },
                        leadingContent = {
                            Icon(
                                Icons.Default.List,
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
        }
    }
}
