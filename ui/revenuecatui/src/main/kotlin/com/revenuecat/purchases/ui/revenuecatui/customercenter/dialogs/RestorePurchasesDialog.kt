@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.customercenter.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.emergetools.snapshots.annotations.EmergeSnapshotConfig
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.Localization
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData

@JvmSynthetic
@Composable
internal fun RestorePurchasesDialog(
    state: RestorePurchasesState,
    localization: Localization,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onContactSupport: (() -> Unit)?,
) {
    when (state) {
        RestorePurchasesState.PURCHASES_RECOVERED -> PurchasesRecoveredDialog(
            localization,
            onDismiss,
        )
        RestorePurchasesState.PURCHASES_NOT_FOUND -> PurchasesNotFoundDialog(
            localization,
            onDismiss = onDismiss,
            onContactSupport = onContactSupport,
        )
        RestorePurchasesState.RESTORING -> RestoringDialog(
            localization,
        ).also {
            onRestore()
        }
    }
}

@Composable
private fun RestoringDialog(
    localization: Localization,
) {
    AlertDialog(
        onDismissRequest = { /* Prevent dismiss while restoring */ },
        title = {
            Text(
                text = localization.commonLocalizedString(
                    key = Localization.CommonLocalizedString.PURCHASES_RESTORING,
                ),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        },
        confirmButton = { },
    )
}

@Composable
private fun PurchasesRecoveredDialog(
    localization: Localization,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                localization.commonLocalizedString(
                    key = Localization.CommonLocalizedString.PURCHASES_RECOVERED,
                ),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                localization.commonLocalizedString(
                    key = Localization.CommonLocalizedString.PURCHASES_RECOVERED_EXPLANATION,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    localization.commonLocalizedString(
                        key = Localization.CommonLocalizedString.DONE,
                    ),
                )
            }
        },
    )
}

@Composable
private fun PurchasesNotFoundDialog(
    localization: Localization,
    onDismiss: () -> Unit,
    onContactSupport: (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = localization.commonLocalizedString(
                    key = Localization.CommonLocalizedString.PURCHASES_NOT_FOUND,
                ),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = localization.commonLocalizedString(
                    key = Localization.CommonLocalizedString.PURCHASES_NOT_RECOVERED,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        confirmButton = {
            if (onContactSupport != null) {
                TextButton(onClick = onContactSupport) {
                    Text(
                        localization.commonLocalizedString(
                            key = Localization.CommonLocalizedString.CONTACT_SUPPORT,
                        ),
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    localization.commonLocalizedString(
                        key = Localization.CommonLocalizedString.DISMISS,
                    ),
                )
            }
        },
    )
}

@Preview(showBackground = true)
@EmergeSnapshotConfig(ignore = true)
@Composable
private fun RestorePurchasesDialogRestoringPreview() {
    RestorePurchasesDialog(
        state = RestorePurchasesState.RESTORING,
        localization = CustomerCenterConfigTestData.customerCenterData().localization,
        onDismiss = {},
        onRestore = {},
        onContactSupport = {},
    )
}

@Preview(showBackground = true)
@EmergeSnapshotConfig(ignore = true)
@Composable
private fun RestorePurchasesDialogRecoveredPreview() {
    RestorePurchasesDialog(
        state = RestorePurchasesState.PURCHASES_RECOVERED,
        localization = CustomerCenterConfigTestData.customerCenterData().localization,
        onDismiss = {},
        onRestore = {},
        onContactSupport = {},
    )
}

@Preview(showBackground = true)
@EmergeSnapshotConfig(ignore = true)
@Composable
private fun RestorePurchasesDialogNotFoundPreview() {
    RestorePurchasesDialog(
        state = RestorePurchasesState.PURCHASES_NOT_FOUND,
        localization = CustomerCenterConfigTestData.customerCenterData().localization,
        onDismiss = {},
        onRestore = {},
        onContactSupport = {},
    )
}
