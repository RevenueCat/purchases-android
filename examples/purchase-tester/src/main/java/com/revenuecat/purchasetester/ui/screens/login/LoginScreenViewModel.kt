package com.revenuecat.purchasetester.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.logInWith
import com.revenuecat.purchases.logOutWith
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface LoginScreenViewModel {

    val state: StateFlow<LoginScreenState>
    val events: SharedFlow<LoginUiEvent>

    fun saveUserId(userId: String)
    fun loginUser()
    fun loginAnonymously()
    fun resetSDK()
}

class LoginScreenViewModelImpl : ViewModel(), LoginScreenViewModel {

    companion object {
        private const val SUBSCRIPTION_TIMEOUT_MILLIS = 5000L

        val Factory = viewModelFactory {
            initializer {
                LoginScreenViewModelImpl()
            }
        }
    }

    override val events: SharedFlow<LoginUiEvent>
        get() = _events.asSharedFlow()

    private val _events = MutableSharedFlow<LoginUiEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    private val userEdits = MutableStateFlow<LoginScreenState.LoginScreenData?>(null)

    private val _state: StateFlow<LoginScreenState> = userEdits
        .map { it ?: LoginScreenState.LoginScreenData() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
            initialValue = LoginScreenState.Loading
        )

    override val state: StateFlow<LoginScreenState>
        get() = _state

    override fun saveUserId(userId: String) {
        updateData { copy(userId = userId.trim()) }
    }

    override fun loginUser() {
        val currentState = state.value
        if (currentState !is LoginScreenState.LoginScreenData) {
            emitEvent(LoginUiEvent.Error("Login state not ready"))
            return
        }

        viewModelScope.launch {
            runCatching {
                Purchases.sharedInstance.logInWith(
                    currentState.userId,
                    { error ->
                        emitEvent(LoginUiEvent.Error(error.message))
                    },
                    { _, _ ->
                        emitEvent(LoginUiEvent.NavigateToOverview)
                    },
                )
            }.onFailure { throwable ->
                val message = throwable.message ?: "Failed to login"
                emitEvent(LoginUiEvent.Error(message))
            }
        }
    }

    override fun loginAnonymously() {
        viewModelScope.launch {
            runCatching {
                if (Purchases.sharedInstance.isAnonymous) {
                    emitEvent(LoginUiEvent.NavigateToOverview)
                } else {
                    Purchases.sharedInstance.logOutWith(
                        { error ->
                            emitEvent(LoginUiEvent.Error(error.message))
                        },
                        {
                            emitEvent(LoginUiEvent.NavigateToOverview)
                        },
                    )
                }
            }.onFailure { throwable ->
                val message = throwable.message ?: "Failed to continue as anonymous"
                emitEvent(LoginUiEvent.Error(message))
            }
        }
    }

    override fun resetSDK() {
        viewModelScope.launch {
            runCatching {
                Purchases.sharedInstance.close()
                emitEvent(LoginUiEvent.NavigateToConfigure)
            }.onFailure { throwable ->
                val message = throwable.message ?: "Failed to reset SDK"
                emitEvent(LoginUiEvent.Error(message))
            }
        }
    }

    private fun emitEvent(event: LoginUiEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }

    private inline fun updateData(
        block: LoginScreenState.LoginScreenData.() -> LoginScreenState.LoginScreenData,
    ) {
        val current = _state.value
        if (current is LoginScreenState.LoginScreenData) {
            userEdits.value = block(current)
        }
    }
}
