package com.revenuecat.samplecat.ui.screens.checkpoints

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.revenuecat.samplecat.R
import com.revenuecat.samplecat.ui.components.ContentBackground
import com.revenuecat.samplecat.ui.components.NavigationRowCard
import com.revenuecat.samplecat.ui.theme.RCGreen

/**
 * The 4 checkpoint samples. Each one shows a different way for an app to use the checkpoint result.
 */
@Composable
fun CheckpointsScreen(
    onHardPaywallClick: () -> Unit,
    onSoftPaywallClick: () -> Unit,
    onOnboardingClick: () -> Unit,
    onEntitlementGateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        ContentBackground(color = RCGreen)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item { CheckpointsHeader() }
            item {
                NavigationRowCard(
                    title = stringResource(R.string.checkpoints_hard_paywall),
                    subtitle = stringResource(R.string.checkpoints_hard_paywall_subtitle),
                    onClick = onHardPaywallClick,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            item {
                NavigationRowCard(
                    title = stringResource(R.string.checkpoints_soft_paywall),
                    subtitle = stringResource(R.string.checkpoints_soft_paywall_subtitle),
                    onClick = onSoftPaywallClick,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            item {
                NavigationRowCard(
                    title = stringResource(R.string.checkpoints_onboarding),
                    subtitle = stringResource(R.string.checkpoints_onboarding_subtitle),
                    onClick = onOnboardingClick,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            item {
                NavigationRowCard(
                    title = stringResource(R.string.checkpoints_entitlement_gate),
                    subtitle = stringResource(R.string.checkpoints_entitlement_gate_subtitle),
                    onClick = onEntitlementGateClick,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun CheckpointsHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.checkpoints_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.checkpoints_description),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.checkpoints_beta_note),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The shared layout of a checkpoint sample: a top bar with a back button, and the sample content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CheckpointSampleScaffold(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            ContentBackground(color = RCGreen)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
        }
    }
}
