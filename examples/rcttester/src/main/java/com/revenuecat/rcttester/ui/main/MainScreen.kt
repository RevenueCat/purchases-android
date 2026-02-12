package com.revenuecat.rcttester.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
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
        MainScreenContent(
            configuration = configuration,
            onConfigurationUpdate = onConfigurationUpdate,
            onNavigateToOfferings = onNavigateToOfferings,
            paddingValues = paddingValues,
        )
    }
}

@Composable
private fun MainScreenContent(
    configuration: SDKConfiguration,
    onConfigurationUpdate: (SDKConfiguration) -> Unit,
    onNavigateToOfferings: () -> Unit,
    paddingValues: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ConfigurationCard(configuration = configuration)
        UserCard(
            configuration = configuration,
            onConfigurationUpdate = onConfigurationUpdate,
        )
        OfferingsCard(onNavigateToOfferings = onNavigateToOfferings)
    }
}

@Composable
private fun ConfigurationCard(configuration: SDKConfiguration) {
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

@Composable
private fun UserCard(
    configuration: SDKConfiguration,
    onConfigurationUpdate: (SDKConfiguration) -> Unit,
) {
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

@Composable
private fun OfferingsCard(onNavigateToOfferings: () -> Unit) {
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
