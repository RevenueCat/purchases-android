package com.revenuecat.purchases.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.IInterface
import android.os.Looper
import android.os.Parcel
import android.os.RemoteException
import android.util.Log
import java.util.concurrent.LinkedBlockingQueue

object AdvertisingIdClient {

    data class AdInfo internal constructor(val id: String, val isLimitAdTrackingEnabled: Boolean)

    @Throws(IllegalStateException::class)
    fun getAdvertisingIdInfo(context: Context, completion: (AdInfo?) -> Unit) {
        Thread(Runnable {
            if (Looper.myLooper() == Looper.getMainLooper())
                throw IllegalStateException("Cannot be called from the main thread")

            try {
                context.packageManager.getPackageInfo("com.android.vending", 0)
            } catch (e: Exception) {
                Log.e("Purchases", "Error getting AdvertisingIdInfo", e)
                completion(null)
                return@Runnable
            }

            val connection = AdvertisingConnection()
            val intent = Intent("com.google.android.gms.ads.identifier.service.START").apply {
                setPackage("com.google.android.gms")
            }
            if (context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
                try {
                    with(AdvertisingInterface(connection.binder)) {
                        id?.let { id ->
                            completion(AdInfo(id, isLimitAdTrackingEnabled()))
                            return@Runnable
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Purchases", "Error getting AdvertisingIdInfo", e)
                } finally {
                    Handler(Looper.getMainLooper()).post {
                        context.unbindService(connection)
                    }
                }
            }
            completion(null)
        }).start()
    }

    private class AdvertisingConnection : ServiceConnection {
        internal var retrieved = false
        private val queue = LinkedBlockingQueue<IBinder>(1)

        internal val binder: IBinder
            @Throws(InterruptedException::class)
            get() {
                if (this.retrieved) throw IllegalStateException()
                this.retrieved = true
                return this.queue.take() as IBinder
            }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            try {
                this.queue.put(service)
            } catch (localInterruptedException: InterruptedException) {
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {}
    }

    private class AdvertisingInterface(private val binder: IBinder) : IInterface {

        val id: String?
            @Throws(RemoteException::class)
            get() {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                val id: String?
                try {
                    data.writeInterfaceToken("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService")
                    binder.transact(1, data, reply, 0)
                    reply.readException()
                    id = reply.readString()
                } finally {
                    reply.recycle()
                    data.recycle()
                }
                return id
            }

        override fun asBinder(): IBinder {
            return binder
        }

        @Throws(RemoteException::class)
        fun isLimitAdTrackingEnabled(): Boolean {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            val limitAdTracking: Boolean
            try {
                data.writeInterfaceToken("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService")
                data.writeInt(1)
                binder.transact(2, data, reply, 0)
                reply.readException()
                limitAdTracking = 0 != reply.readInt()
            } finally {
                reply.recycle()
                data.recycle()
            }
            return limitAdTracking
        }
    }
}
