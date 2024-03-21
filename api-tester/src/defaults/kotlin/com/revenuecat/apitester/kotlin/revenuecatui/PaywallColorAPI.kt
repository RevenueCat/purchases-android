package com.revenuecat.apitester.kotlin.revenuecatui

import android.graphics.Color
import com.revenuecat.purchases.paywalls.PaywallColor

@Suppress("unused", "UNUSED_VARIABLE")
private class PaywallColorAPI {
    fun check(paywallColor: PaywallColor) {
        val stringRepresentation: String = paywallColor.stringRepresentation
        val underlyingColor: Color? = paywallColor.underlyingColor
        val colorInt: Int = paywallColor.colorInt
    }

    fun checkConstructor(colorInt: Int) {
        val paywallColor = PaywallColor("#FFFFFF")
        val paywallColor2 = PaywallColor(colorInt)
    }
}
