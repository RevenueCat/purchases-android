//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.concurrent.ExecutorService

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DispatcherTest {
    private var executorService: ExecutorService = mockk()
    private var dispatcher: Dispatcher = Dispatcher(executorService)

    private var errorCalled: Boolean? = false

    private var result: HTTPClient.Result? = null

    @Test
    fun canBeCreated() {
        assertNotNull(dispatcher)
    }

    @Test
    fun submitsToExecutor() {
        val result = HTTPClient.Result()

        every {
            executorService.submit(any())
        } returns null

        dispatcher.enqueue(object : Dispatcher.AsyncCall() {
            override fun call(): HTTPClient.Result {
                return result
            }
        })

        verify {
            executorService.submit(any())
        }
    }

    @Test
    fun asyncCallHandlesFailures() {
        val call = object : Dispatcher.AsyncCall() {
            override fun call(): HTTPClient.Result {
                throw HTTPClient.HTTPErrorException(0, "")
            }

            override fun onError(code: Int, message: String) {
                this@DispatcherTest.errorCalled = true
            }
        }

        call.run()

        assertTrue(this.errorCalled!!)
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

        assertNotNull(this.result)
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
}
