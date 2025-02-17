package com.revenuecat.purchases.ui.revenuecatui.customercenter

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import java.lang.ref.WeakReference

interface CustomerCenterEventHandler {
    fun onDismissed()
}

class CustomerCenterLauncher(
    context: Context,
    eventHandler: CustomerCenterEventHandler
) {
    private val weakContext = WeakReference(context)

    init {
        CustomerCenterEventBus.setEventHandler(eventHandler)
    }

    fun launch(): Boolean {
        val context = weakContext.get() ?: run {
            Logger.e("Not displaying CustomerCenter because context is null")
            return false
        }

        if (isActivityFinishing(context)) {
            Logger.e("Not displaying CustomerCenter because activity/fragment is finishing or has finished.")
            return false
        }

        showCustomerCenter(context)
        return true
    }

    private fun showCustomerCenter(context: Context) {
        val intent = CustomerCenterActivity.createIntent(context)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun isActivityFinishing(context: Context): Boolean {
        val activity = when (context) {
            is Activity -> context
            is ContextWrapper -> context.baseContext as? Activity
            else -> null
        }
        return activity?.isFinishing == true
    }
} 