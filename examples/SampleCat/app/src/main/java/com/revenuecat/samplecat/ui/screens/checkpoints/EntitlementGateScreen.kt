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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint
import com.revenuecat.samplecat.R
import com.revenuecat.samplecat.config.CheckpointIdentifiers
import com.revenuecat.samplecat.ui.theme.RCGreen
import kotlinx.coroutines.launch

/**
 * An entitlement gate: the app checks CustomerInfo first.
 * It fires the checkpoint only when the customer has no access.
 *
 * The checkpoints API is invite-only and experimental, so it needs [InviteOnlyCheckpointsAPI].
 */
@OptIn(InviteOnlyCheckpointsAPI::class)
@Composable
fun EntitlementGateScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var didLoad by rememberSaveable { mutableStateOf(false) }
    var hasAccess by rememberSaveable { mutableStateOf(false) }

    @StringRes var status by rememberSaveable { mutableStateOf(R.string.checkpoints_checking_customer_info) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun refreshAccess() {
        scope.launch {
            error = null
            hasAccess = try {
                Purchases.sharedInstance.awaitCustomerInfo().hasPremiumAccess
            } catch (e: PurchasesException) {
                error = e.message
                return@launch
            }

            if (hasAccess) {
                status = R.string.checkpoints_gate_skipped
                return@launch
            }

            Purchases.sharedInstance.checkpoint(
                CheckpointIdentifiers.ENTITLEMENT_GATE,
                CheckpointParams { customVariables { "gate" to "entitlement" } },
            ) { result ->
                hasAccess = result.hasPremiumAccess
                status = when {
                    hasAccess -> R.string.checkpoints_access_unlocked
                    result.flow == null -> R.string.checkpoints_locked_no_flow
                    else -> R.string.checkpoints_locked_no_access
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!didLoad) {
            didLoad = true
            refreshAccess()
        }
    }

    CheckpointSampleScaffold(
        title = stringResource(R.string.checkpoints_entitlement_gate),
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
                    if (hasAccess) R.string.checkpoints_content_unlocked else R.string.checkpoints_content_locked,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = color,
            )
            Text(
                text = error?.let { stringResource(R.string.checkpoints_failed, it) } ?: stringResource(status),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = ::refreshAccess) {
            Text(stringResource(R.string.checkpoints_refresh_access))
        }
    }
}
