package com.revenuecat.purchases.ui.revenuecatui.customercenter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import java.lang.ref.WeakReference

class CustomerCenterActivity : ComponentActivity() {
    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, CustomerCenterActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CustomerCenter(
                modifier = Modifier.fillMaxSize(),
                onDismiss = {
                    CustomerCenterEventBus.onDismissed()
                    finish()
                }
            )
        }
    }
}

internal object CustomerCenterEventBus {
    private var weakHandler = WeakReference<CustomerCenterEventHandler>(null)

    fun setEventHandler(handler: CustomerCenterEventHandler?) {
        weakHandler = WeakReference(handler)
    }

    fun onDismissed() {
        weakHandler.get()?.onDismissed()
        // Clear reference after dismissal
        weakHandler.clear()
    }
} 