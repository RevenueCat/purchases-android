package com.revenuecat.checkpointtester.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.checkpointtester.ui.screens.onboarding.OnboardingViewModel.Step
import com.revenuecat.checkpointtester.ui.theme.CheckpointTesterTheme

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(text = state.step.title, style = MaterialTheme.typography.headlineSmall)
        Text(text = state.step.body, style = MaterialTheme.typography.bodyMedium)

        StepControls(
            step = state.step,
            onPrevious = viewModel::previous,
            onNext = viewModel::next,
            onRestart = viewModel::restart,
        )

        state.message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StepControls(
    step: Step,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (step != Step.Welcome) {
            OutlinedButton(onClick = onPrevious, enabled = step != Step.Done) {
                Text(text = "Back")
            }
        }
        when (step) {
            Step.Welcome -> Button(onClick = onNext) { Text(text = "Continue") }
            Step.Personalize -> Button(onClick = onNext) { Text(text = "Finish onboarding") }
            Step.Done -> Button(onClick = onRestart) { Text(text = "Restart onboarding") }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    CheckpointTesterTheme {
        OnboardingScreen()
    }
}
