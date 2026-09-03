package com.revenuecat.checkpointssample.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stepCount = OnboardingViewModel.Step.entries.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LinearProgressIndicator(
            progress = { (state.step.ordinal + 1).toFloat() / stepCount },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(state.step.title, style = MaterialTheme.typography.headlineSmall)
        Text(state.step.body, style = MaterialTheme.typography.bodyMedium)
        if (state.finishing) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        } else {
            Button(onClick = { viewModel.advance(onFinish) }) {
                Text(state.step.buttonLabel)
            }
        }
    }
}
