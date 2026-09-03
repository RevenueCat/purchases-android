package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.purchases.OwnershipType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.Localization.CommonLocalizedString
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ManagementViewHorizontalPadding
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.ExpirationOrRenewal
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PriceDetails
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation
import com.revenuecat.purchases.ui.revenuecatui.customercenter.theme.CustomerCenterPreviewTheme
import com.revenuecat.purchases.ui.revenuecatui.utils.DefaultDateFormatter
import java.util.Date
import java.util.Locale

internal fun periodTypeLabel(
    periodType: PeriodType,
    localization: CustomerCenterConfigData.Localization,
): String? = when (periodType) {
    PeriodType.TRIAL -> localization.commonLocalizedString(CommonLocalizedString.TRIAL_PERIOD)
    PeriodType.INTRO -> localization.commonLocalizedString(CommonLocalizedString.INTRODUCTORY_PRICE)
    // There is no localized string for a prepaid period, so the row is omitted instead of
    // labeling it as introductory.
    PeriodType.NORMAL, PeriodType.PREPAID -> null
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun PurchaseHistoryDetailView(
    purchase: PurchaseInformation,
    localization: CustomerCenterConfigData.Localization,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = remember { DefaultDateFormatter() }
    val locale = remember { Locale.getDefault() }

    fun Date.fmt() = dateFormatter.format(this, locale)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DetailSection(modifier = Modifier.padding(horizontal = ManagementViewHorizontalPadding)) {
            purchase.title?.let { name ->
                DetailRow(label = localization.commonLocalizedString(CommonLocalizedString.PRODUCT_NAME), value = name)
            }

            val priceValue = when (val p = purchase.pricePaid) {
                is PriceDetails.Paid -> p.price
                PriceDetails.Free -> localization.commonLocalizedString(CommonLocalizedString.FREE)
                PriceDetails.Unknown -> "-"
            }
            DetailRow(label = localization.commonLocalizedString(CommonLocalizedString.PAID_PRICE), value = priceValue)

            val statusValue = if (purchase.isExpired) {
                localization.commonLocalizedString(CommonLocalizedString.INACTIVE)
            } else {
                localization.commonLocalizedString(CommonLocalizedString.ACTIVE)
            }
            DetailRow(label = localization.commonLocalizedString(CommonLocalizedString.STATUS), value = statusValue)

            when (val exp = purchase.expirationOrRenewal) {
                is ExpirationOrRenewal.Expiration -> DetailRow(
                    label = localization.commonLocalizedString(CommonLocalizedString.EXPIRES),
                    value = exp.date,
                )
                is ExpirationOrRenewal.Renewal -> DetailRow(
                    label = localization.commonLocalizedString(CommonLocalizedString.NEXT_RENEWAL),
                    value = exp.date,
                )
                null -> Unit
            }

            purchase.unsubscribeDetectedAt?.let { date ->
                DetailRow(
                    label = localization.commonLocalizedString(CommonLocalizedString.UNSUBSCRIBED_AT),
                    value = date.fmt(),
                )
            }

            purchase.billingIssuesDetectedAt?.let { date ->
                DetailRow(
                    label = localization.commonLocalizedString(CommonLocalizedString.BILLING_ISSUE_DETECTED_AT),
                    value = date.fmt(),
                )
            }

            purchase.gracePeriodExpiresDate?.let { date ->
                DetailRow(
                    label = localization.commonLocalizedString(CommonLocalizedString.GRACE_PERIOD_EXPIRES_AT),
                    value = date.fmt(),
                )
            }

            periodTypeLabel(purchase.periodType, localization)?.let { periodValue ->
                DetailRow(
                    label = localization.commonLocalizedString(CommonLocalizedString.PERIOD_TYPE),
                    value = periodValue,
                )
            }

            purchase.refundedAt?.let { date ->
                DetailRow(
                    label = localization.commonLocalizedString(CommonLocalizedString.REFUNDED_AT),
                    value = date.fmt(),
                )
            }

            purchase.purchaseDate?.let { date ->
                DetailRow(
                    label = localization.commonLocalizedString(CommonLocalizedString.HISTORY_LATEST_PURCHASE_DATE),
                    value = date.fmt(),
                )
            }

            purchase.originalPurchaseDate?.let { date ->
                DetailRow(
                    label = localization.commonLocalizedString(CommonLocalizedString.ORIGINAL_DOWNLOAD_DATE),
                    value = date.fmt(),
                )
            }

            if (purchase.productIdentifier.isNotEmpty()) {
                DetailRow(
                    label = localization.commonLocalizedString(CommonLocalizedString.PRODUCT_ID),
                    value = purchase.productIdentifier,
                )
            }

            purchase.storeTransactionId?.let { id ->
                DetailRow(
                    label = localization.commonLocalizedString(CommonLocalizedString.TRANSACTION_ID),
                    value = id,
                )
            }

            DetailRow(
                label = localization.commonLocalizedString(CommonLocalizedString.SANDBOX),
                value = if (purchase.isSandbox) {
                    localization.commonLocalizedString(CommonLocalizedString.YES)
                } else {
                    localization.commonLocalizedString(CommonLocalizedString.NO)
                },
            )
        }

        if (purchase.ownershipType == OwnershipType.FAMILY_SHARED) {
            Text(
                text = localization.commonLocalizedString(CommonLocalizedString.SHARED_THROUGH_FAMILY_MEMBER),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = ManagementViewHorizontalPadding,
                    vertical = CustomerCenterConstants.Layout.SECTION_TITLE_BOTTOM_PADDING,
                ),
            )
        }
    }
}

@Composable
private fun DetailSection(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = CustomerCenterConstants.Layout.TOP_PADDING_AFTER_TOP_BAR),
    ) {
        content()
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = CustomerCenterConstants.Layout.DETAIL_ROW_VERTICAL_PADDING),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp")
@Composable
private fun PurchaseHistoryDetailViewPreview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    CustomerCenterPreviewTheme {
        PurchaseHistoryDetailView(
            purchase = CustomerCenterConfigTestData.purchaseInformationMonthlyRenewing,
            localization = testData.localization,
        )
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp")
@Composable
private fun PurchaseHistoryDetailViewExpiredPreview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    CustomerCenterPreviewTheme {
        PurchaseHistoryDetailView(
            purchase = CustomerCenterConfigTestData.purchaseInformationYearlyExpired,
            localization = testData.localization,
        )
    }
}
