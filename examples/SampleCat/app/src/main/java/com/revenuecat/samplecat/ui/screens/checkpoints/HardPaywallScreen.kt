package com.revenuecat.samplecat.ui.screens.checkpoints

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint
import com.revenuecat.samplecat.R
import com.revenuecat.samplecat.config.CheckpointIdentifiers
import com.revenuecat.samplecat.ui.theme.RCGreen

/**
 * A hard paywall: the content stays locked until the customer in the checkpoint result has access.
 *
 * The checkpoints API is invite-only and experimental, so it needs [InviteOnlyCheckpointsAPI].
 */
@OptIn(InviteOnlyCheckpointsAPI::class)
@Composable
fun HardPaywallScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var didLoad by rememberSaveable { mutableStateOf(false) }
    var hasAccess by rememberSaveable { mutableStateOf(false) }

    @StringRes var status by rememberSaveable { mutableStateOf(R.string.checkpoints_preparing) }

    fun runCheckpoint() {
        Purchases.sharedInstance.checkpoint(
            CheckpointIdentifiers.HARD_PAYWALL,
            CheckpointParams { customVariables { "gate" to "hard" } },
        ) { result ->
            // Decide on the customer in the result. It also covers a customer who already had access.
            hasAccess = result.hasPremiumAccess
            status = when {
                hasAccess -> R.string.checkpoints_access_unlocked
                result.flow == null -> R.string.checkpoints_locked_no_flow
                else -> R.string.checkpoints_locked_no_access
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
        title = stringResource(R.string.checkpoints_hard_paywall),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        SampleCard {
            val color = if (hasAccess) RCGreen else MaterialTheme.colorScheme.error
            Icon(
                imageVector = if (hasAccess) Icons.Default.LockOpen else Icons.Default.Lock,
                contentDescription = null,
                tint = color,
            )
            Text(
                text = stringResource(
                    if (hasAccess) R.string.checkpoints_premium_unlocked else R.string.checkpoints_premium_locked,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = color,
            )
        }
        SampleCard {
            Text(
                text = stringResource(R.string.checkpoints_latest_result),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(status),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!hasAccess) {
            Button(onClick = ::runCheckpoint) {
                Text(stringResource(R.string.checkpoints_try_again))
            }
        }
    }
}
