package com.revenuecat.sample.ui.user

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.revenuecat.purchases.PurchaserInfo

class UserViewModel : ViewModel() {
    companion object {
        val shared = UserViewModel()
    }

    /*
    The latest PurchaserInfo from RevenueCat.

    Updated by PurchasesDelegate whenever the Purchases SDK updates the cache
    */
    val purchaserInfo: MutableLiveData<PurchaserInfo> by lazy {
        MutableLiveData<PurchaserInfo>().apply {
            value = null
        }
    }
}
