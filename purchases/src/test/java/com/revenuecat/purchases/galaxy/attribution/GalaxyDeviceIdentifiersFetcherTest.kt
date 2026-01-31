package com.revenuecat.purchases.galaxy.attribution

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class GalaxyDeviceIdentifiersFetcherTest {

    @Test
    fun `getDeviceIdentifiers returns an empty map`() {
        val fetcher = GalaxyDeviceIdentifiersFetcher()
        val mockApplication = mockk<Application>()

        var completionCalled = false
        var receivedIdentifiers: Map<String, String> = mapOf("initial" to "value")

        fetcher.getDeviceIdentifiers(mockApplication) { deviceIdentifiers ->
            completionCalled = true
            receivedIdentifiers = deviceIdentifiers
        }

        assertTrue(completionCalled)
        assertEquals(emptyMap(), receivedIdentifiers)
    }
}
