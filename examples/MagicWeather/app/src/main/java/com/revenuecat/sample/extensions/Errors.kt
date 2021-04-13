package com.revenuecat.sample.extensions

import android.app.AlertDialog
import android.content.Context

fun buildError(context: Context?, message: String) {
    context?.let {
        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setMessage(message)
        alertDialogBuilder.show()
    }
}
