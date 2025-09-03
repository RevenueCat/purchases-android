package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import android.content.res.Configuration
import android.content.res.Resources
import android.icu.number.NumberFormatter
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ManagementViewHorizontalPadding
import com.revenuecat.purchases.ui.revenuecatui.customercenter.actions.CustomerCenterAction
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.theme.CustomerCenterPreviewTheme
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies

private const val MAX_NUMBER_OF_ROWS = 4

@Composable
internal fun VirtualCurrenciesListView(
    virtualCurrencies: VirtualCurrencies,
    localization: CustomerCenterConfigData.Localization,
    onAction: (CustomerCenterAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (virtualCurrencies.all.isEmpty()) return

    val sortedCurrencies = virtualCurrencies.all.values
        .sortedByDescending { it.balance }

    // We want to limit the number of rows in the list to 4 max. We accomplish this by:
    // - Showing all currencies if there are 4 or fewer currencies
    // - Show first 3 currencies + "See All" button to limit to 4 rows if there are 5 or more currencies
    val displayedCurrencies = if (sortedCurrencies.size <= MAX_NUMBER_OF_ROWS) {
        sortedCurrencies
    } else {
        sortedCurrencies.take(3)
    }

    val displayShowAllButton = sortedCurrencies.size > MAX_NUMBER_OF_ROWS

    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = localization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.VIRTUAL_CURRENCY_BALANCES_SCREEN_HEADER
            ),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(
                bottom = CustomerCenterConstants.Layout.SECTION_TITLE_BOTTOM_PADDING,
            ),
        )

        Surface(
            shape = RoundedCornerShape(CustomerCenterConstants.Card.ROUNDED_CORNER_SIZE),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column {
                displayedCurrencies.forEachIndexed { index, currency ->
                    VirtualCurrencyRow(
                        virtualCurrencyName = currency.name,
                        virtualCurrencyCode = currency.code,
                        balance = currency.balance,
                        modifier = Modifier.padding(
                            horizontal = CustomerCenterConstants.Card.CARD_PADDING,
                            vertical = 12.dp
                        )
                    )

                    if (index < displayedCurrencies.size - 1 || displayShowAllButton) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            modifier = Modifier.padding(horizontal = CustomerCenterConstants.Card.CARD_PADDING)
                        )
                    }
                }

                if (displayShowAllButton) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAction(CustomerCenterAction.ShowAllVirtualCurrencies) }
                            .padding(
                                horizontal = CustomerCenterConstants.Card.CARD_PADDING,
                                vertical = 12.dp
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = localization.commonLocalizedString(
                                CustomerCenterConfigData.Localization.CommonLocalizedString.SEE_ALL_VIRTUAL_CURRENCIES
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VirtualCurrencyRow(
    virtualCurrencyName: String,
    virtualCurrencyCode: String,
    balance: Int,
    modifier: Modifier = Modifier,
) {
    fun formatBalance(number: Int) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        NumberFormatter.with()
            .locale(Resources.getSystem().configuration.locales.get(0))
            .format(number)
            .toString()
    } else {
        number.toString()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$virtualCurrencyName ($virtualCurrencyCode)",
            textAlign = TextAlign.Start,
            maxLines = 2
        )

        Text(text = formatBalance(balance))
    }
}

@Preview(
    name = "4 VCs, Light Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL
)
@Preview(
    name = "4 VCs, Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun VirtualCurrenciesListView_4_VCs_Preview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()

    CustomerCenterPreviewTheme {
        VirtualCurrenciesListView(
            virtualCurrencies = CustomerCenterConfigTestData.fourVirtualCurrencies,
            localization = testData.localization,
            onAction = {}
        )
    }
}

@Preview(
    name = "5 VCs, Light Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL
)
@Preview(
    name = "5 VCs, Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun VirtualCurrenciesListView_5_VCs_Preview() {
    val testData = CustomerCenterConfigTestData.customerCenterData()
    
    CustomerCenterPreviewTheme {
        VirtualCurrenciesListView(
            virtualCurrencies = CustomerCenterConfigTestData.fiveVirtualCurrencies,
            localization = testData.localization,
            onAction = {}
        )
    }
}
