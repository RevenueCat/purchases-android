package com.revenuecat.purchases.utils

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class LogMockExtension : BeforeEachCallback, AfterEachCallback {

    override fun beforeEach(context: ExtensionContext?) {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<Throwable>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    override fun afterEach(context: ExtensionContext?) {
        unmockkStatic(Log::class)
    }
}
