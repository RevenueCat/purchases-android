package com.revenuecat.sample.main

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.sample.BuildConfig
import com.revenuecat.sample.data.CustomerInfoEvent

data class MainState(
    val customerInfoList: List<CustomerInfoEvent> = emptyList(),
    val currentCustomerInfo: CustomerInfo? = null,
    val displayErrorMessage: String? = null,
    val shouldShowSwitchingUserDialog: Boolean = false,
    val shouldShowExplanationDialog: Boolean = false,
    val offerings: Offerings? = null,
    val currentAppUserID: String = BuildConfig.DEFAULT_APP_USER_ID_ENTITLEMENT_SAMPLE,
)
