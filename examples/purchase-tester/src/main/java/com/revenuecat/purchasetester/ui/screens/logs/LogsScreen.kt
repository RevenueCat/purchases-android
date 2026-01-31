package com.revenuecat.purchasetester.ui.screens.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases_sample.R
import com.revenuecat.purchasetester.LogMessage
import com.revenuecat.purchasetester.colorResource
import com.revenuecat.purchasetester.ui.theme.LightGrey
import com.revenuecat.purchasetester.ui.theme.PurchaseTesterTheme

private object LogsScreenDefaults {
    val LOG_ITEM_PADDING: Dp = 32.dp
    val DIVIDER_THICKNESS: Dp = 2.dp
}

@Composable
fun LogsScreen(
    onNavigateBack: () -> Unit,
) {
    LogsScreenContent(
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogsScreenContent(
    onNavigateBack: () -> Unit,
    logsViewModel: LogsViewModel = viewModel<LogsViewModelImpl>(
        factory = LogsViewModelImpl.Factory
    ),
) {
    val viewModelState by logsViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        logsViewModel.loadLogs()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.logs)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (viewModelState) {
                is LogsScreenState.Loading -> {
                    // Show nothing
                }
                is LogsScreenState.LogsData -> {
                    val logsData = viewModelState as LogsScreenState.LogsData
                    if (logsData.logs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_logs_available),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(logsData.logs) { logMessage ->
                                LogItem(logMessage = logMessage)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogItem(logMessage: LogMessage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(logMessage.logLevel.backgroundColor())
    ) {
        Text(
            text = logMessage.message,
            modifier = Modifier
                .fillMaxWidth()
                .padding(LogsScreenDefaults.LOG_ITEM_PADDING)
        )
    }
    HorizontalDivider(
        thickness = LogsScreenDefaults.DIVIDER_THICKNESS,
        color = LightGrey
    )
}

@Composable
private fun LogLevel.backgroundColor(): Color {
    val context = LocalContext.current
    return Color(ContextCompat.getColor(context, this.colorResource))
}

@Preview(showBackground = true, name = "Logs Screen - Light Theme")
@Composable
private fun LogsScreenPreview() {
    PurchaseTesterTheme {
        Surface {
            LogsScreenContent(
                onNavigateBack = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Logs Screen - Dark Theme")
@Composable
private fun LogsScreenPreview_Dark() {
    PurchaseTesterTheme(darkTheme = true) {
        Surface {
            LogsScreenContent(
                onNavigateBack = {}
            )
        }
    }
}
