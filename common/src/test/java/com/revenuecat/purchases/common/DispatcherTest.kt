//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.json.JSONException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.concurrent.ExecutorService
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DispatcherTest {

    private val mockExecutorService: ExecutorService = mockk()
    private val dispatcherWithMockExecutor = Dispatcher(mockExecutorService)

    private lateinit var currentThreadExecutorService: CurrentThreadExecutorService

    private lateinit var dispatcher: Dispatcher

    private var errorCalled: Boolean? = false

    private var result: HTTPClient.Result? = null


    @Before
    fun `setup`() {
        currentThreadExecutorService = CurrentThreadExecutorService()
        dispatcher = Dispatcher(currentThreadExecutorService)
    }

    @Test
    fun canBeCreated() {
        assertThat(dispatcher).isNotNull
    }

    @Test
    fun executesInExecutor() {
        val result = HTTPClient.Result()

        every {
            mockExecutorService.isShutdown
        } returns false

        every {
            mockExecutorService.submit(any())
        } returns mockk()

        dispatcherWithMockExecutor.enqueue(object : Dispatcher.AsyncCall() {
            override fun call(): HTTPClient.Result {
                return result
            }
        })

        verify {
            mockExecutorService.submit(any())
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
            mockExecutorService.shutdownNow()
        } returns null

        dispatcherWithMockExecutor.close()

        verify {
            mockExecutorService.shutdownNow()
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
            mockExecutorService.isShutdown
        } returns true


        dispatcherWithMockExecutor.enqueue({
            fail("should never execute")
        })

        verify(exactly = 0) {
            mockExecutorService.execute(any())
        }
    }

    @Test
    fun `execute on background when service is not shutdown`() {
        dispatcher.enqueue({ })

        assertThat(currentThreadExecutorService.executeCalled).isTrue
    }

    class CurrentThreadExecutorService(
        private val callerRunsPolicy: CallerRunsPolicy = CallerRunsPolicy()
    ): ThreadPoolExecutor(
        0,
        1,
        0L,
        TimeUnit.SECONDS,
        SynchronousQueue(),
        callerRunsPolicy
    ) {

        var executeCalled = false

        override fun execute(command: Runnable?) {
            executeCalled = true
            callerRunsPolicy.rejectedExecution(command, this)
        }
    }
}
