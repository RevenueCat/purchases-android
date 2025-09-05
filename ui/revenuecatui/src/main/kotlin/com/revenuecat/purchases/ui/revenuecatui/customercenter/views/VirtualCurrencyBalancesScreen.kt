package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.getColorForTheme
import com.revenuecat.purchases.ui.revenuecatui.customercenter.theme.CustomerCenterPreviewTheme
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.VirtualCurrencyBalancesScreenViewModel
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.VirtualCurrencyBalancesScreenViewModelFactory
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesImpl
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrency

@JvmSynthetic
@Composable
internal fun VirtualCurrencyBalancesScreen(
    appearance: CustomerCenterConfigData.Appearance,
    localization: CustomerCenterConfigData.Localization,
    modifier: Modifier = Modifier,
    viewModel: VirtualCurrencyBalancesScreenViewModel = getVirtualCurrencyBalancesScreenViewModel(),
) {
    val viewState by viewModel.viewState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.onViewAppeared()
    }

    InternalVirtualCurrencyBalancesScreen(
        appearance = appearance,
        localization = localization,
        viewState = viewState,
        modifier = modifier,
    )
}

@JvmSynthetic
@Composable
@Suppress("LongMethod")
private fun InternalVirtualCurrencyBalancesScreen(
    appearance: CustomerCenterConfigData.Appearance,
    localization: CustomerCenterConfigData.Localization,
    viewState: VirtualCurrencyBalancesScreenViewState,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val textColor = appearance.getColorForTheme(isDark) { it.textColor }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        when (viewState) {
            is VirtualCurrencyBalancesScreenViewState.Loading -> {
                item {
                    CustomerCenterLoadingView()
                }
            }
            is VirtualCurrencyBalancesScreenViewState.Loaded -> {
                val virtualCurrencyBalanceData = viewState.virtualCurrencyBalanceData
                if (virtualCurrencyBalanceData.isNotEmpty()) {
                    item {
                        Text(
                            text = localization.commonLocalizedString(
                                CustomerCenterConfigData.Localization.CommonLocalizedString
                                    .VIRTUAL_CURRENCY_BALANCES_SCREEN_HEADER,
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                            color = textColor ?: MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }
                    itemsIndexed(
                        items = virtualCurrencyBalanceData,
                        key = { index, virtualCurrency ->
                            // Here, we use the code, name, and index as the key in case
                            // the backend returns multiple VCs with the same code for some reason
                            "${virtualCurrency.code}_${virtualCurrency.name}_$index"
                        },
                    ) { index, virtualCurrency ->
                        if (index > 0) {
                            Spacer(modifier = Modifier.size(CustomerCenterConstants.Layout.ITEMS_SPACING))
                        }

                        val position = when {
                            virtualCurrencyBalanceData.size == 1 -> ButtonPosition.SINGLE
                            index == 0 -> ButtonPosition.FIRST
                            index == virtualCurrencyBalanceData.size - 1 -> ButtonPosition.LAST
                            else -> ButtonPosition.MIDDLE
                        }

                        VirtualCurrencyRow(
                            appearance = appearance,
                            virtualCurrency = virtualCurrency,
                            position = position,
                        )
                    }
                } else {
                    item {
                        EmptyStateView(
                            localization = localization,
                        )
                    }
                }
            }
            is VirtualCurrencyBalancesScreenViewState.Error -> {
                item {
                    CustomerCenterErrorView(CustomerCenterState.Error(viewState.error))
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView(
    localization: CustomerCenterConfigData.Localization,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = localization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.NO_VIRTUAL_CURRENCY_BALANCES_FOUND,
            ),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

internal sealed interface VirtualCurrencyBalancesScreenViewState {
    object Loading : VirtualCurrencyBalancesScreenViewState
    data class Loaded(val virtualCurrencyBalanceData: List<VirtualCurrency>) : VirtualCurrencyBalancesScreenViewState
    data class Error(val error: PurchasesError) : VirtualCurrencyBalancesScreenViewState
}

@Composable
private fun getVirtualCurrencyBalancesScreenViewModel(
    purchases: PurchasesType = PurchasesImpl(),
    viewModel: VirtualCurrencyBalancesScreenViewModel = viewModel<VirtualCurrencyBalancesScreenViewModel>(
        factory = VirtualCurrencyBalancesScreenViewModelFactory(
            purchases = purchases,
        ),
    ),
): VirtualCurrencyBalancesScreenViewModel {
    return viewModel
}

@Preview(
    name = "Loaded with 0 VC Balances",
    showBackground = true,
)
@Composable
internal fun VirtualCurrencyBalancesScreenLoaded0VCsPreview() {
    CustomerCenterPreviewTheme {
        InternalVirtualCurrencyBalancesScreen(
            appearance = CustomerCenterConfigTestData.standardAppearance,
            localization = CustomerCenterConfigTestData.customerCenterData().localization,
            viewState = VirtualCurrencyBalancesScreenViewState.Loaded(emptyList()),
        )
    }
}

@Preview(
    name = "Loaded with 4 VC Balances",
    showBackground = true,
)
@Composable
internal fun VirtualCurrencyBalancesScreenLoaded4VCsPreview() {
    CustomerCenterPreviewTheme {
        InternalVirtualCurrencyBalancesScreen(
            appearance = CustomerCenterConfigTestData.standardAppearance,
            localization = CustomerCenterConfigTestData.customerCenterData().localization,
            viewState = VirtualCurrencyBalancesScreenViewState.Loaded(
                CustomerCenterConfigTestData.fourVirtualCurrencies.all.values.sortedByDescending { it.balance },
            ),
        )
    }
}

@Preview(
    name = "Loaded with 5 VC Balances",
    showBackground = true,
)
@Composable
internal fun VirtualCurrencyBalancesScreenLoadedVCsPreview() {
    CustomerCenterPreviewTheme {
        InternalVirtualCurrencyBalancesScreen(
            appearance = CustomerCenterConfigTestData.standardAppearance,
            localization = CustomerCenterConfigTestData.customerCenterData().localization,
            viewState = VirtualCurrencyBalancesScreenViewState.Loaded(
                CustomerCenterConfigTestData.fiveVirtualCurrencies.all.values.sortedByDescending { it.balance },
            ),
        )
    }
}

@Preview(
    name = "Loading State",
    showBackground = true,
)
@Composable
internal fun VirtualCurrencyBalancesScreenLoadingPreview() {
    CustomerCenterPreviewTheme {
        InternalVirtualCurrencyBalancesScreen(
            appearance = CustomerCenterConfigTestData.standardAppearance,
            localization = CustomerCenterConfigTestData.customerCenterData().localization,
            viewState = VirtualCurrencyBalancesScreenViewState.Loading,
        )
    }
}

@Preview(
    name = "Error State",
    showBackground = true,
)
@Composable
internal fun VirtualCurrencyBalancesScreenErrorPreview() {
    CustomerCenterPreviewTheme {
        InternalVirtualCurrencyBalancesScreen(
            appearance = CustomerCenterConfigTestData.standardAppearance,
            localization = CustomerCenterConfigTestData.customerCenterData().localization,
            viewState = VirtualCurrencyBalancesScreenViewState.Error(
                error = PurchasesError(
                    code = PurchasesErrorCode.UnknownError,
                    underlyingErrorMessage = "Mock error",
                ),
            ),
        )
    }
}
