package com.revenuecat.sample

import android.content.Context
import android.content.Intent

fun Context.startCatsActivity(clearBackStack: Boolean = false) {
    Intent(this, CatsActivity::class.java).also {
        if (clearBackStack) {
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }.let {
        startActivity(it)
    }
}

fun Context.startUpsellActivity(clearBackStack: Boolean = false) {
    Intent(this, UpsellActivity::class.java).also {
        if (clearBackStack) {
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }.let {
        startActivity(it)
    }
}
