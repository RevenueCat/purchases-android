package com.revenuecat.purchases.ui.revenuecatui.components.sheet

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle

@Stable
internal class SimpleSheetState {
    @get:JvmSynthetic
    var sheet by mutableStateOf<ButtonComponentStyle.Action.NavigateTo.Destination.Sheet?>(null)
        private set

    @get:JvmSynthetic
    var visible by mutableStateOf(false)
        private set

    fun show(sheet: ButtonComponentStyle.Action.NavigateTo.Destination.Sheet) {
        this.sheet = sheet
        visible = true
    }

    fun hide() {
        visible = false
    }
}
