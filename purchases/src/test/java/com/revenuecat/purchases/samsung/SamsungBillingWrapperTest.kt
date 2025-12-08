package com.revenuecat.purchases.samsung

import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.PurchasesState
import com.revenuecat.purchases.PurchasesStateCache
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import com.samsung.android.sdk.iap.lib.helper.IapHelper
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SamsungBillingWrapperTest {
    private val applicationContext: Context = mockk(relaxed = true)
    private val mainHandler: Handler = mockk(relaxed = true)
    private val stateProvider = PurchasesStateCache(PurchasesState())

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `startConnection only initializes helper once`() {
        mockkStatic(IapHelper::class)
        val instanceHelper = mockk<IapHelper>(relaxed = true)
        every { IapHelper.getInstance(applicationContext) } returns instanceHelper

        val wrapper = SamsungBillingWrapper(
            applicationContext = applicationContext,
            billingMode = SamsungBillingMode.PRODUCTION,
            mainHandler = mainHandler,
            stateProvider = stateProvider,
        )

        wrapper.startConnection()
        wrapper.startConnection()

        verify(exactly = 1) { IapHelper.getInstance(applicationContext) }
    }

    @Test
    fun `startConnectionOnMainThread posts to handler when not on main thread`() {
        mockkStatic(IapHelper::class)
        mockkStatic(Looper::class)

        val instanceHelper = mockk<IapHelper>(relaxed = true)
        every { IapHelper.getInstance(applicationContext) } returns instanceHelper

        val fakeMainThread = Thread()
        val mockMainLooper = mockk<Looper>()
        every { Looper.getMainLooper() } returns mockMainLooper
        every { mockMainLooper.thread } returns fakeMainThread

        val runnableSlot = slot<Runnable>()
        every { mainHandler.post(capture(runnableSlot)) } answers {
            runnableSlot.captured.run()
            true
        }

        val wrapper = SamsungBillingWrapper(
            applicationContext = applicationContext,
            billingMode = SamsungBillingMode.TEST,
            mainHandler = mainHandler,
            stateProvider = stateProvider,
        )

        wrapper.startConnectionOnMainThread(0)

        verify(exactly = 1) { mainHandler.post(any()) }
        verify(exactly = 1) { IapHelper.getInstance(applicationContext) }
    }

    @Test
    fun `startConnectionOnMainThread runs immediately when already on main thread`() {
        mockkStatic(IapHelper::class)
        mockkStatic(Looper::class)

        val instanceHelper = mockk<IapHelper>(relaxed = true)
        every { IapHelper.getInstance(applicationContext) } returns instanceHelper

        val mockMainLooper = mockk<Looper>()
        every { Looper.getMainLooper() } returns mockMainLooper
        every { mockMainLooper.thread } returns Thread.currentThread()

        val wrapper = SamsungBillingWrapper(
            applicationContext = applicationContext,
            billingMode = SamsungBillingMode.TEST,
            mainHandler = mainHandler,
            stateProvider = stateProvider,
        )

        wrapper.startConnectionOnMainThread(0)

        verify(exactly = 0) { mainHandler.post(any()) }
        verify(exactly = 1) { IapHelper.getInstance(applicationContext) }
    }

    @Test
    fun `startConnection sets helper operation mode for current billing mode`() {
        mockkStatic(IapHelper::class)
        val instanceHelper = mockk<IapHelper>(relaxed = true)
        every { IapHelper.getInstance(applicationContext) } returns instanceHelper

        val wrapper = SamsungBillingWrapper(
            applicationContext = applicationContext,
            billingMode = SamsungBillingMode.TEST,
            mainHandler = mainHandler,
            stateProvider = stateProvider,
        )

        wrapper.startConnection()

        verify(exactly = 1) { instanceHelper.setOperationMode(SamsungBillingMode.TEST.toSamsungOperationMode()) }
    }
}
