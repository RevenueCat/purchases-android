package com.revenuecat.purchases.utils

import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class UriParseMockExtension : BeforeEachCallback, AfterEachCallback {

    override fun beforeEach(context: ExtensionContext?) {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk()
    }

    override fun afterEach(context: ExtensionContext?) {
        unmockkStatic(Uri::class)
    }
}
