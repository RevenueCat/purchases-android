package com.revenuecat.paywallstester.ui.screens.main.customvariables

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.revenuecat.paywallstester.Constants
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue

/**
 * Singleton holder for custom variables that can be accessed from Activities.
 * This is used because Activities can't easily access the ViewModel.
 */
object CustomVariablesHolder {
    var customVariables: Map<String, CustomVariableValue> = Constants.CUSTOM_VARIABLES
        private set

    fun update(variables: Map<String, CustomVariableValue>) {
        customVariables = variables
    }
}

/**
 * ViewModel to manage custom variables state across the app.
 * This allows variables to be edited in one place and used across all paywall screens.
 */
class CustomVariablesViewModel : ViewModel() {

    private val _customVariables = mutableStateOf(Constants.CUSTOM_VARIABLES)
    val customVariables: Map<String, CustomVariableValue>
        get() = _customVariables.value

    init {
        // Sync to holder on initialization
        syncToHolder()
    }

    private val _isEditorVisible = mutableStateOf(false)
    val isEditorVisible: Boolean
        get() = _isEditorVisible.value

    fun showEditor() {
        _isEditorVisible.value = true
    }

    fun hideEditor() {
        _isEditorVisible.value = false
    }

    fun addVariable(name: String, value: CustomVariableValue) {
        _customVariables.value = _customVariables.value + (name to value)
        syncToHolder()
    }

    fun removeVariable(name: String) {
        _customVariables.value = _customVariables.value - name
        syncToHolder()
    }

    fun updateVariable(name: String, value: CustomVariableValue) {
        _customVariables.value = _customVariables.value + (name to value)
        syncToHolder()
    }

    fun clearAllVariables() {
        _customVariables.value = emptyMap()
        syncToHolder()
    }

    fun resetToDefaults() {
        _customVariables.value = Constants.CUSTOM_VARIABLES
        syncToHolder()
    }

    private fun syncToHolder() {
        CustomVariablesHolder.update(_customVariables.value)
    }
}
