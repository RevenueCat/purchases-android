package com.revenuecat.purchasetester.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.revenuecat.purchasetester.LogMessage
import com.revenuecat.purchasetester.MainApplication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val application = context.applicationContext as MainApplication
    val logs = remember { application.logHandler.storedLogs }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Logs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            items(logs) { logMessage ->
                LogMessageItem(logMessage)
            }
        }
    }
}

@Composable
fun LogMessageItem(logMessage: LogMessage, modifier: Modifier = Modifier) {
    val backgroundColor = when (logMessage.logLevel.name) {
        "DEBUG" -> Color.Gray
        "INFO" -> Color.Blue
        "WARN" -> Color.Yellow
        "ERROR" -> Color.Red
        else -> Color.LightGray
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor.copy(alpha = 0.2f))
            .padding(16.dp),
    ) {
        Text(
            text = logMessage.message,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    HorizontalDivider(color = Color.LightGray, thickness = 2.dp)
}
