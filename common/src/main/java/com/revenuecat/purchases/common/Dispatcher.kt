//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.verification.SignatureVerificationException
import org.json.JSONException
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val JITTERING_DELAY_MILLISECONDS = 5000L
private const val JITTERING_LONG_DELAY_MILLISECONDS = 10000L

enum class Delay(val minDelay: Duration, val maxDelay: Duration) {
    NONE(0.milliseconds, 0.milliseconds),
    DEFAULT(0.milliseconds, JITTERING_DELAY_MILLISECONDS.milliseconds),
    LONG(JITTERING_DELAY_MILLISECONDS.milliseconds, JITTERING_LONG_DELAY_MILLISECONDS.milliseconds)
}

open class Dispatcher(
    private val executorService: ExecutorService
) {

    abstract class AsyncCall : Runnable {
        @Throws(JSONException::class, IOException::class)
        abstract fun call(): HTTPResult

        open fun onError(error: PurchasesError) {}
        open fun onCompletion(result: HTTPResult) {}

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
            } catch (e: SignatureVerificationException) {
                onError(e.toPurchasesError().also { errorLog(it) })
            }
        }
    }

    open fun enqueue(
        command: Runnable,
        delay: Delay = Delay.NONE
    ) {
        synchronized(this.executorService) {
            if (!executorService.isShutdown) {
                val future = if (delay != Delay.NONE && executorService is ScheduledExecutorService) {
                    val delayToApply = (delay.minDelay.inWholeMilliseconds..delay.maxDelay.inWholeMilliseconds).random()
                    errorLog("TEST: EXECUTING REQUEST WITH DELAY: $delayToApply")
                    executorService.schedule(command, delayToApply, TimeUnit.MILLISECONDS)
                } else {
                    executorService.submit(command)
                }

                // Exceptions are being swallowed if using execute instead of submit
                // Future.get is blocking so we create a Thread
                // More info: https://github.com/RevenueCat/purchases-android/pull/234
                Thread {
                    try {
                        future.get()
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                        e.cause?.let { throw it }
                    }
                }.start()
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
