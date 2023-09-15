package com.revenuecat.paywallstester.ui.screens.main.appinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitLogIn
import com.revenuecat.purchases.awaitLogOut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface AppInfoScreenViewModel {
    val state: StateFlow<String?>

    fun logIn(newAppUserId: String)
    fun logOut()
}

class AppInfoScreenViewModelImpl : ViewModel(), AppInfoScreenViewModel {

    override val state: StateFlow<String?>
        get() = _state.asStateFlow()

    private val _state = MutableStateFlow<String?>(null)

    init {
        updateAppUserID()
    }

    override fun logIn(newAppUserId: String) {
        viewModelScope.launch {
            try {
                Purchases.sharedInstance.awaitLogIn(newAppUserId)
                updateAppUserID()
            } catch (e: PurchasesException) {
                _state.value = "Error logging in: ${e.message}"
            }
        }
    }

    override fun logOut() {
        viewModelScope.launch {
            try {
                Purchases.sharedInstance.awaitLogOut()
                updateAppUserID()
            } catch (e: PurchasesException) {
                _state.value = "Error logging out: ${e.message}"
            }
        }
    }

    private fun updateAppUserID() {
        _state.value = Purchases.sharedInstance.appUserID
    }
}
