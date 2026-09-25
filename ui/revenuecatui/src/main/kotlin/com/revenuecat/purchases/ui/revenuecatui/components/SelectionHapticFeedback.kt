package com.revenuecat.purchases.ui.revenuecatui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

@JvmSynthetic
internal fun View.performSelectionHapticFeedback() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
    }
}

@JvmSynthetic
internal fun View.performToggleHapticFeedback(isOn: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        performHapticFeedback(if (isOn) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF)
    }
}
