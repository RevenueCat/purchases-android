//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.json.JSONException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DispatcherTest {
    private var executorService: ExecutorService = mockk()
    private var dispatcher: Dispatcher = Dispatcher(executorService)

    private var errorCalled: Boolean? = false

    private var result: HTTPClient.Result? = null

    @Test
    fun canBeCreated() {
        assertThat(dispatcher).isNotNull
    }

    @Test
    fun executesInExecutor() {
        val result = HTTPClient.Result()

        every {
            executorService.execute(any())
        } just Runs

        dispatcher.enqueue(object : Dispatcher.AsyncCall() {
            override fun call(): HTTPClient.Result {
                return result
            }
        })

        verify {
            executorService.execute(any())
        }
    }

    @Test
    fun asyncCallHandlesFailures() {
        val call = object : Dispatcher.AsyncCall() {
            override fun call(): HTTPClient.Result {
                throw JSONException("an exception")
            }

            override fun onError(error: PurchasesError) {
                this@DispatcherTest.errorCalled = true
            }
        }

        call.run()

        assertThat(this.errorCalled!!).isTrue()
    }

    @Test
    fun asyncCallHandlesSuccess() {
        val result = HTTPClient.Result()
        val call = object : Dispatcher.AsyncCall() {
            override fun call(): HTTPClient.Result {
                return result
            }

            override fun onCompletion(result: HTTPClient.Result) {
                this@DispatcherTest.result = result
            }
        }

        call.run()

        assertThat(this.result).isNotNull
    }

    @Test
    fun closeStopsThreads() {
        every {
            executorService.shutdownNow()
        } returns null

        dispatcher.close()

        verify {
            executorService.shutdownNow()
        }
    }

    @Test
    fun securityExceptionsAreCorrectlyConvertedToPurchaseErrors() {
        val errorHolder = AtomicReference<PurchasesError>()

        val call = object : Dispatcher.AsyncCall() {
            override fun call(): HTTPClient.Result {
                throw SecurityException("missing permissoins")
            }

            override fun onError(error: PurchasesError) {
                errorHolder.set(error)
            }
        }

        call.run()

        assertThat(errorHolder.get().code).isEqualTo(PurchasesErrorCode.InsufficientPermissionsError)
    }

    @Test
    fun `execute on background when service is shutdown`() {
        every {
            executorService.isShutdown
        } returns true

        dispatcher.enqueue {
            fail("should never execute")
        }

        verify(exactly = 0) {
            executorService.execute(any())
        }
    }

    @Test
    fun `execute on background when service is not shutdown`() {
        every {
            executorService.execute(any())
        } just Runs

        every {
            executorService.isShutdown
        } returns false

        dispatcher.enqueue {
        }

        verify(exactly = 1) {
            executorService.execute(any())
        }
    }
}
