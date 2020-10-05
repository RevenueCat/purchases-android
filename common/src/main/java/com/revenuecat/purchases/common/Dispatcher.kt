//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import com.revenuecat.purchases.PurchasesError
import org.json.JSONException
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

private const val JITTERING_DELAY_MILLISECONDS = 1000

open class Dispatcher(
    private val executorService: ExecutorService
) {

    abstract class AsyncCall : Runnable {
        @Throws(JSONException::class, IOException::class)
        abstract fun call(): HTTPClient.Result

        open fun onError(error: PurchasesError) {}
        open fun onCompletion(result: HTTPClient.Result) {}

        override fun run() {
            try {
                onCompletion(call())
            } catch (e: JSONException) {
                onError(e.toPurchasesError().also { errorLog(it) })
            } catch (e: IOException) {
                onError(e.toPurchasesError().also { errorLog(it) })
            } catch (e: SecurityException) {
                // This can happen if a user disables the INTERNET permission.
                onError(e.toPurchasesError().also { errorLog(it) })
            }
        }
    }

    open fun enqueue(
        command: Runnable,
        randomDelay: Boolean = false
    ) {
        synchronized(this.executorService) {
            if (!executorService.isShutdown) {
                if (randomDelay && executorService is ScheduledExecutorService) {
                    val randomDelay = (0..JITTERING_DELAY_MILLISECONDS).random()
                    executorService.schedule(command, randomDelay.toLong(), TimeUnit.MILLISECONDS)
                } else {
                    executorService.execute(command)
                }
            }
        }
    }

    open fun close() {
        synchronized(this.executorService) {
            this.executorService.shutdownNow()
        }
    }

    open fun isClosed() = synchronized(this.executorService) { this.executorService.isShutdown }
}
