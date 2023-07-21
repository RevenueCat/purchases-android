package com.revenuecat.sample.main

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings

data class MainState(
    val currentCustomerInfo: CustomerInfo? = null,
    val displayErrorMessage: String? = null,
    val shouldStartSwitchingUser: Boolean = false,
    val offerings: Offerings? = null,
)
