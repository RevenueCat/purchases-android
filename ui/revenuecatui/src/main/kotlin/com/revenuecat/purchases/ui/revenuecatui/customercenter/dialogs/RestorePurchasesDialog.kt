@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.customercenter.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.emergetools.snapshots.annotations.IgnoreEmergeSnapshot
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@JvmSynthetic
@Composable
internal fun RestorePurchasesDialog(
    state: RestorePurchasesState,
    localization: CustomerCenterConfigData.Localization,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onContactSupport: (() -> Unit)?,
) {
    when (state) {
        RestorePurchasesState.INITIAL -> InitialStateDialog(
            localization = localization,
            onDismiss = onDismiss,
            onRestore = onRestore,
        )
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
        )
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
private fun InitialStateDialog(
    localization: CustomerCenterConfigData.Localization,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = localization.commonLocalizedString(
                    key = CustomerCenterConfigData.Localization.CommonLocalizedString.RESTORE_PURCHASES,
                ),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = localization.commonLocalizedString(
                    key = CustomerCenterConfigData.Localization.CommonLocalizedString.GOING_TO_CHECK_PURCHASES,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        confirmButton = {
            Button(onClick = onRestore) {
                Text(
                    localization.commonLocalizedString(
                        key = CustomerCenterConfigData.Localization.CommonLocalizedString.CHECK_PAST_PURCHASES,
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    localization.commonLocalizedString(
                        key = CustomerCenterConfigData.Localization.CommonLocalizedString.CANCEL,
                    ),
                )
            }
        },
    )
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
private fun RestoringDialog(
    localization: CustomerCenterConfigData.Localization,
) {
    AlertDialog(
        onDismissRequest = { /* Prevent dismiss while restoring */ },
        title = {
            Text(
                text = localization.commonLocalizedString(
                    key = CustomerCenterConfigData.Localization.CommonLocalizedString.PURCHASES_RESTORING,
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

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
private fun PurchasesRecoveredDialog(
    localization: CustomerCenterConfigData.Localization,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                localization.commonLocalizedString(
                    key = CustomerCenterConfigData.Localization.CommonLocalizedString.PURCHASES_RECOVERED,
                ),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                localization.commonLocalizedString(
                    key = CustomerCenterConfigData.Localization.CommonLocalizedString.PURCHASES_RECOVERED_EXPLANATION,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(
                    localization.commonLocalizedString(
                        key = CustomerCenterConfigData.Localization.CommonLocalizedString.CANCEL,
                    ),
                )
            }
        },
    )
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Composable
private fun PurchasesNotFoundDialog(
    localization: CustomerCenterConfigData.Localization,
    onDismiss: () -> Unit,
    onContactSupport: (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = localization.commonLocalizedString(
                    key = CustomerCenterConfigData.Localization.CommonLocalizedString.PURCHASES_NOT_FOUND,
                ),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            var message = localization.commonLocalizedString(
                key = CustomerCenterConfigData.Localization.CommonLocalizedString.PURCHASES_NOT_RECOVERED,
            )
            message += "\n\n"
            message += localization.commonLocalizedString(
                key = CustomerCenterConfigData.Localization.CommonLocalizedString.UPDATE_WARNING_DESCRIPTION,
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        confirmButton = {
            if (onContactSupport != null) {
                Button(onClick = onContactSupport) {
                    Text(
                        localization.commonLocalizedString(
                            key = CustomerCenterConfigData.Localization.CommonLocalizedString.CONTACT_SUPPORT,
                        ),
                    )
                }
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(
                    localization.commonLocalizedString(
                        key = CustomerCenterConfigData.Localization.CommonLocalizedString.DISMISS,
                    ),
                )
            }
        },
    )
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Preview(showBackground = true)
@IgnoreEmergeSnapshot
@Composable
private fun RestorePurchasesDialogInitialPreview() {
    RestorePurchasesDialog(
        state = RestorePurchasesState.INITIAL,
        localization = CustomerCenterConfigTestData.customerCenterData().localization,
        onDismiss = {},
        onRestore = {},
        onContactSupport = {},
    )
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Preview(showBackground = true)
@IgnoreEmergeSnapshot
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

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Preview(showBackground = true)
@IgnoreEmergeSnapshot
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

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Preview(showBackground = true)
@IgnoreEmergeSnapshot
@Composable
private fun RestorePurchasesDialogLoadingPreview() {
    RestorePurchasesDialog(
        state = RestorePurchasesState.INITIAL,
        localization = CustomerCenterConfigTestData.customerCenterData().localization,
        onDismiss = {},
        onRestore = {},
        onContactSupport = {},
    )
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@Preview(showBackground = true)
@IgnoreEmergeSnapshot
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
