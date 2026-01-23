package com.revenuecat.purchasetester.ui.screens.login

sealed class LoginScreenState {

    object Loading : LoginScreenState()
    data class LoginScreenData(
        val userId: String = "",
    ) : LoginScreenState()

}

sealed interface LoginUiEvent {
    data class Error(val message: String) : LoginUiEvent
    object NavigateToOverview : LoginUiEvent
    object NavigateToConfigure : LoginUiEvent
}
