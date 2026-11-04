package com.revenuecat.samplecat.ui.screens.products

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.revenuecat.samplecat.R
import com.revenuecat.samplecat.model.PurchasableState
import com.revenuecat.samplecat.ui.components.ConceptIntroduction
import com.revenuecat.samplecat.ui.components.ContentBackground
import com.revenuecat.samplecat.ui.components.ErrorMessageBox
import com.revenuecat.samplecat.ui.components.LoadingOverlay
import com.revenuecat.samplecat.ui.components.PurchasableCard
import com.revenuecat.samplecat.ui.components.WarningMessageBox
import com.revenuecat.samplecat.ui.components.spinner.SpinnerPullToRefreshIndicator
import com.revenuecat.samplecat.ui.theme.AccentColor
import com.revenuecat.samplecat.viewmodel.OfferingsUiState
import com.revenuecat.samplecat.viewmodel.UserViewModel

/**
 * Screen displaying all available products from all offerings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    userViewModel: UserViewModel,
    modifier: Modifier = Modifier,
) {
    val offeringsState by userViewModel.offeringsState.collectAsState()
    val isPurchasing by userViewModel.isPurchasing.collectAsState()
    val purchasingProductId by userViewModel.purchasingProductId.collectAsState()
    val purchaseError by userViewModel.purchaseError.collectAsState()

    val activity = LocalActivity.current

    val pullToRefreshState = rememberPullToRefreshState()

    // Derive values from sealed state
    val isRefreshing = (offeringsState as? OfferingsUiState.Success)?.isRefreshing == true
    val isLoading = offeringsState is OfferingsUiState.Loading

    Box(modifier = modifier.fillMaxSize()) {
        ContentBackground(color = AccentColor)

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { userViewModel.fetchOfferings() },
            state = pullToRefreshState,
            indicator = {
                SpinnerPullToRefreshIndicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                item {
                    ConceptIntroduction(
                        imageRes = R.drawable.visual_products,
                        title = stringResource(R.string.products_title),
                        description = stringResource(R.string.products_description),
                    )
                }

                // Show purchase error if present
                purchaseError?.let { error ->
                    item {
                        ErrorMessageBox(
                            message = stringResource(
                                error.resId,
                                *error.formatArgs.toTypedArray(),
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }

                when (val state = offeringsState) {
                    is OfferingsUiState.Loading -> {
                        // Loading state handled by overlay below
                    }

                    is OfferingsUiState.Error -> {
                        item {
                            ErrorMessageBox(
                                message = stringResource(
                                    state.error.resId,
                                    *state.error.formatArgs.toTypedArray(),
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }

                    is OfferingsUiState.Success -> {
                        // Show refresh error if present
                        state.refreshError?.let { error ->
                            item {
                                ErrorMessageBox(
                                    message = stringResource(
                                        error.resId,
                                        *error.formatArgs.toTypedArray(),
                                    ),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                        }

                        val products = state.offerings.all.values
                            .flatMap { offering -> offering.availablePackages }
                            .map { pkg -> pkg.product }
                            .distinctBy { it.id }

                        // Show empty state if no products
                        if (products.isEmpty()) {
                            item {
                                WarningMessageBox(
                                    message = stringResource(R.string.products_empty),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                        }

                        items(products, key = { it.id }) { product ->
                            val productId = product.id
                            val isPurchased = userViewModel.isProductPurchased(productId)
                            val isThisProductPurchasing = purchasingProductId == productId

                            val purchasableState = when {
                                isPurchased -> PurchasableState.Purchased
                                isThisProductPurchasing -> PurchasableState.Purchasing
                                isPurchasing -> PurchasableState.PurchasingOtherProduct
                                else -> PurchasableState.ReadyToPurchase
                            }

                            Box(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            ) {
                                PurchasableCard(
                                    title = product.title,
                                    productId = productId,
                                    description = product.description,
                                    state = purchasableState,
                                    onPurchaseClick = {
                                        activity?.let { act ->
                                            userViewModel.purchase(act, product)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Loading overlay for initial load
            if (isLoading) {
                LoadingOverlay()
            }
        }
    }
}
