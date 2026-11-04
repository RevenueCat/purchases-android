package com.revenuecat.samplecat.viewmodel

import com.revenuecat.purchases.Offerings
import com.revenuecat.samplecat.model.LocalizedMessage

/**
 * Represents the UI state for offerings fetching.
 *
 * This sealed interface ensures that we can only be in one of these mutually exclusive states:
 * - [Loading]: Initial fetch in progress, no data yet
 * - [Success]: Offerings loaded successfully (may be refreshing or have a refresh error)
 * - [Error]: Initial load failed, no data to show
 */
sealed interface OfferingsUiState {
    /**
     * Initial loading state, no offerings data available yet.
     */
    data object Loading : OfferingsUiState

    /**
     * Successfully loaded offerings.
     *
     * @param offerings The loaded offerings data
     * @param isRefreshing True if a refresh is in progress (pull-to-refresh)
     * @param refreshError Localized error message if a refresh failed (keeps showing existing data)
     */
    data class Success(
        val offerings: Offerings,
        val isRefreshing: Boolean = false,
        val refreshError: LocalizedMessage? = null,
    ) : OfferingsUiState

    /**
     * Initial load failed with no data to show.
     *
     * @param error The localized error message describing what went wrong
     */
    data class Error(val error: LocalizedMessage) : OfferingsUiState
}
