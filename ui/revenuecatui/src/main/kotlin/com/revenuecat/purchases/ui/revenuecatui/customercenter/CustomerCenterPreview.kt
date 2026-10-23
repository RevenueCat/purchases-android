// Created by Monika on 2026-10-02

@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModelFactory
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModelImpl
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

@InternalRevenueCatAPI
@Composable
public fun CustomerCenterPreview(
    provider: CustomerCenterPreviewProvider,
    modifier: Modifier = Modifier,
    options: CustomerCenterPreviewOptions = CustomerCenterPreviewOptions(),
    onDismiss: () -> Unit,
) {
    val isDarkMode = options.isDarkMode ?: isSystemInDarkTheme()
    val colorScheme = MaterialTheme.colorScheme
    val store = remember(provider) { ViewModelStore() }
    DisposableEffect(store) {
        onDispose { store.clear() }
    }
    val viewModel = remember(store) {
        ViewModelProvider(
            store,
            CustomerCenterViewModelFactory(
                purchases = CustomerCenterPreviewPurchases(provider),
                colorScheme = colorScheme,
                isDarkMode = isDarkMode,
                previewProvider = provider,
            ),
        )[CustomerCenterViewModelImpl::class.java]
    }
    LaunchedEffect(provider, viewModel) {
        provider.customerInfoUpdates?.drop(1)?.collectLatest { viewModel.refreshCustomerCenter() }
    }
    InternalCustomerCenter(
        modifier = modifier,
        viewModel = viewModel,
        previewOptions = options,
        onDismiss = onDismiss,
    )
}

@InternalRevenueCatAPI
@Immutable
public data class CustomerCenterPreviewOptions(
    public val isDarkMode: Boolean? = null,
    public val usesExistingNavigation: Boolean = false,
    public val showCloseButton: Boolean = true,
)
