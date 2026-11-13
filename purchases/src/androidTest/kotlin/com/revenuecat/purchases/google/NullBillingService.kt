package com.revenuecat.purchases.google

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

/** A test service whose rejected binding makes Android call ServiceConnection.onNullBinding. */
class NullBillingService : Service() {
    override fun onBind(intent: Intent): IBinder? = null
}

/** Redirects only the first billing bind; all later attempts reach the actual Play Store. */
internal class NullBillingContext(base: Context) : ContextWrapper(base) {
    val nullBinding = CountDownLatch(1)
    val bindCalls = AtomicInteger()
    val nullBindingUnbinds = AtomicInteger()
    private val forwardedConnections = ConcurrentHashMap<ServiceConnection, ServiceConnection>()

    override fun getApplicationContext(): Context = this

    override fun bindService(service: Intent, connection: ServiceConnection, flags: Int): Boolean {
        if (service.action != "com.android.vending.billing.InAppBillingService.BIND" ||
            bindCalls.incrementAndGet() != 1
        ) {
            return super.bindService(service, connection, flags)
        }
        val forwardingConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) =
                connection.onServiceConnected(name, binder)

            override fun onServiceDisconnected(name: ComponentName) = connection.onServiceDisconnected(name)
            override fun onBindingDied(name: ComponentName) = connection.onBindingDied(name)
            override fun onNullBinding(name: ComponentName) {
                connection.onNullBinding(name)
                Log.i("RC_RECREATE", "Android delivered onNullBinding to the real BillingClient")
                nullBinding.countDown()
            }
        }
        forwardedConnections[connection] = forwardingConnection
        return super.bindService(Intent(this, NullBillingService::class.java), forwardingConnection, flags)
    }

    override fun unbindService(connection: ServiceConnection) {
        val forwarded = forwardedConnections.remove(connection)
        super.unbindService(forwarded ?: connection)
        if (forwarded != null) nullBindingUnbinds.incrementAndGet()
    }
}
