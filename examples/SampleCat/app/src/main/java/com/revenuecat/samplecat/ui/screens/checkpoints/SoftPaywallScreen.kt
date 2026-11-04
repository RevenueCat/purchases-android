package com.revenuecat.samplecat.ui.screens.checkpoints

import androidx.annotation.StringRes
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint
import com.revenuecat.samplecat.R
import com.revenuecat.samplecat.config.CheckpointIdentifiers

/**
 * A soft paywall: the content is always available, and the checkpoint result only updates the status.
 *
 * The checkpoints API is invite-only and experimental, so it needs [InviteOnlyCheckpointsAPI].
 */
@OptIn(InviteOnlyCheckpointsAPI::class)
@Composable
fun SoftPaywallScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var didLoad by rememberSaveable { mutableStateOf(false) }
    var hasAccess by rememberSaveable { mutableStateOf(false) }

    @StringRes var status by rememberSaveable { mutableStateOf(R.string.checkpoints_preparing) }

    fun runCheckpoint() {
        Purchases.sharedInstance.checkpoint(CheckpointIdentifiers.SOFT_PAYWALL) { result ->
            hasAccess = result.hasPremiumAccess
            status = if (result.flow == null) {
                R.string.checkpoints_soft_paywall_no_flow
            } else {
                R.string.checkpoints_soft_paywall_flow_ended
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!didLoad) {
            didLoad = true
            runCheckpoint()
        }
    }

    CheckpointSampleScaffold(
        title = stringResource(R.string.checkpoints_soft_paywall),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        SampleCard {
            Text(
                text = stringResource(R.string.checkpoints_feature_content),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.checkpoints_feature_content_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SampleCard {
            Text(
                text = stringResource(R.string.checkpoints_subscription_status),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(
                    if (hasAccess) R.string.checkpoints_subscriber else R.string.checkpoints_free_tier,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(status),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = ::runCheckpoint) {
            Text(stringResource(R.string.checkpoints_run_again))
        }
    }
}
