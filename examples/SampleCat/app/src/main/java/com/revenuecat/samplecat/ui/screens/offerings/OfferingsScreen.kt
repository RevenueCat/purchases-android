package com.revenuecat.samplecat.ui.screens.offerings

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
import com.revenuecat.samplecat.ui.components.ConceptIntroduction
import com.revenuecat.samplecat.ui.components.ContentBackground
import com.revenuecat.samplecat.ui.components.ErrorMessageBox
import com.revenuecat.samplecat.ui.components.LoadingOverlay
import com.revenuecat.samplecat.ui.components.OfferingCard
import com.revenuecat.samplecat.ui.components.WarningMessageBox
import com.revenuecat.samplecat.ui.components.spinner.SpinnerPullToRefreshIndicator
import com.revenuecat.samplecat.ui.theme.RCGreen
import com.revenuecat.samplecat.viewmodel.OfferingsUiState
import com.revenuecat.samplecat.viewmodel.UserViewModel

/**
 * Screen displaying the list of available RevenueCat offerings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferingsScreen(
    userViewModel: UserViewModel,
    onOfferingClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val offeringsState by userViewModel.offeringsState.collectAsState()

    val pullToRefreshState = rememberPullToRefreshState()

    // Derive values from sealed state
    val isRefreshing = (offeringsState as? OfferingsUiState.Success)?.isRefreshing == true
    val isLoading = offeringsState is OfferingsUiState.Loading

    Box(modifier = modifier.fillMaxSize()) {
        ContentBackground(color = RCGreen)

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
                        imageRes = R.drawable.visual_offerings,
                        title = stringResource(R.string.offerings_title),
                        description = stringResource(R.string.offerings_description),
                    )
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

                        val offeringsList = state.offerings.all.values.toList()

                        // Show empty state if no offerings
                        if (offeringsList.isEmpty()) {
                            item {
                                WarningMessageBox(
                                    message = stringResource(R.string.offerings_empty),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                        }

                        items(offeringsList, key = { it.identifier }) { offering ->
                            Box(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            ) {
                                OfferingCard(
                                    offering = offering,
                                    onClick = { onOfferingClick(offering.identifier) },
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
