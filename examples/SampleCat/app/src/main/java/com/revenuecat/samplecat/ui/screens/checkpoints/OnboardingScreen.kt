package com.revenuecat.samplecat.ui.screens.checkpoints

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint
import com.revenuecat.samplecat.R
import com.revenuecat.samplecat.config.CheckpointIdentifiers

private enum class OnboardingStep(@StringRes val title: Int, @StringRes val message: Int) {
    Welcome(R.string.checkpoints_onboarding_welcome, R.string.checkpoints_onboarding_welcome_message),
    Personalize(R.string.checkpoints_onboarding_personalize, R.string.checkpoints_onboarding_personalize_message),
    Done(R.string.checkpoints_onboarding_done, R.string.checkpoints_onboarding_done_message),
}

/**
 * An onboarding flow that fires a checkpoint at its last step.
 * Onboarding completes when it fires the checkpoint. It does not wait for the result.
 *
 * The checkpoints API is invite-only and experimental, so it needs [InviteOnlyCheckpointsAPI].
 */
@OptIn(InviteOnlyCheckpointsAPI::class)
@Composable
fun OnboardingScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by rememberSaveable { mutableStateOf(OnboardingStep.Welcome) }

    @StringRes var checkpointStatus by rememberSaveable { mutableStateOf<Int?>(null) }

    fun finishOnboarding() {
        Purchases.sharedInstance.checkpoint(
            CheckpointIdentifiers.ONBOARDING_COMPLETE,
            CheckpointParams { customVariables { "step" to "personalize" } },
        ) { result ->
            checkpointStatus = if (result.flow == null) {
                R.string.checkpoints_no_flow
            } else {
                R.string.checkpoints_onboarding_flow_ended
            }
        }
        step = OnboardingStep.Done
    }

    CheckpointSampleScaffold(
        title = stringResource(R.string.checkpoints_onboarding),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        LinearProgressIndicator(
            progress = { (step.ordinal + 1f) / OnboardingStep.entries.size },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(step.title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(step.message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        checkpointStatus?.let { status ->
            SampleCard {
                Text(
                    text = stringResource(R.string.checkpoints_result),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(text = stringResource(status), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth()) {
            if (step == OnboardingStep.Personalize) {
                OutlinedButton(onClick = { step = OnboardingStep.Welcome }) {
                    Text(stringResource(R.string.nav_back))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            when (step) {
                OnboardingStep.Welcome -> Button(onClick = { step = OnboardingStep.Personalize }) {
                    Text(stringResource(R.string.checkpoints_onboarding_continue))
                }
                OnboardingStep.Personalize -> Button(onClick = ::finishOnboarding) {
                    Text(stringResource(R.string.checkpoints_onboarding_finish))
                }
                OnboardingStep.Done -> Button(
                    onClick = {
                        step = OnboardingStep.Welcome
                        checkpointStatus = null
                    },
                ) {
                    Text(stringResource(R.string.checkpoints_onboarding_restart))
                }
            }
        }
    }
}
