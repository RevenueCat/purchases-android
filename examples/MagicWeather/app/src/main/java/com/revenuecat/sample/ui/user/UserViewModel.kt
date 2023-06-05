package com.revenuecat.sample.ui.user

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.revenuecat.purchases.CustomerInfo

class UserViewModel : ViewModel() {
    companion object {
        val shared = UserViewModel()
    }

    /*
    The latest CustomerInfo from RevenueCat.

    Updated by PurchasesDelegate whenever the Purchases SDK updates the cache
     */
    val customerInfo: MutableLiveData<CustomerInfo> by lazy {
        MutableLiveData<CustomerInfo>().apply {
            value = null
        }
    }
}
