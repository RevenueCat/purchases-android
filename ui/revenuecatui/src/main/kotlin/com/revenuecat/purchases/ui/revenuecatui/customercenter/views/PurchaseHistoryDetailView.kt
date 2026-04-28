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
        DetailSection(modifier = Modifier.padding(ManagementViewHorizontalPadding)) {
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

            if (purchase.periodType != PeriodType.NORMAL) {
                val periodValue = if (purchase.periodType == PeriodType.TRIAL) {
                    localization.commonLocalizedString(CommonLocalizedString.TRIAL_PERIOD)
                } else {
                    localization.commonLocalizedString(CommonLocalizedString.INTRODUCTORY_PRICE)
                }
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

@Suppress("MagicNumber")
@Preview(showBackground = true, device = "spec:width=412dp,height=915dp")
@Composable
private fun PurchaseHistoryDetailViewExpiredPreview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    CustomerCenterPreviewTheme {
        PurchaseHistoryDetailView(
            purchase = PurchaseInformation(
                title = "Annual Plan",
                pricePaid = PriceDetails.Paid("$49.99"),
                expirationOrRenewal = ExpirationOrRenewal.Expiration("Jan 1, 2024"),
                product = null,
                store = com.revenuecat.purchases.Store.PLAY_STORE,
                isSubscription = true,
                managementURL = null,
                isExpired = true,
                isTrial = false,
                isCancelled = true,
                isLifetime = false,
                purchaseDate = Date(1_700_000_000_000L),
            ),
            localization = testData.localization,
        )
    }
}
