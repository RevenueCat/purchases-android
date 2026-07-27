package com.revenuecat.purchases.common.networking

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetworkCapabilities

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class DeviceConnectivityCheckerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val checker = DeviceConnectivityChecker(context)

    private fun setActiveNetworkCapabilities(vararg capabilities: Int) {
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        capabilities.forEach { shadowOf(networkCapabilities).addCapability(it) }
        shadowOf(connectivityManager).setNetworkCapabilities(
            connectivityManager.activeNetwork,
            networkCapabilities,
        )
    }

    @Test
    fun `a validated network is not offline`() {
        setActiveNetworkCapabilities(
            NetworkCapabilities.NET_CAPABILITY_INTERNET,
            NetworkCapabilities.NET_CAPABILITY_VALIDATED,
        )
        assertThat(checker.isDeviceOffline()).isFalse()
    }

    @Test
    fun `a network without the validated capability is offline`() {
        setActiveNetworkCapabilities(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        assertThat(checker.isDeviceOffline()).isTrue()
    }

    @Test
    fun `no active network is offline`() {
        shadowOf(connectivityManager).setDefaultNetworkActive(false)
        assertThat(checker.isDeviceOffline()).isTrue()
    }

    @Test
    fun `an active network with unknown capabilities is offline`() {
        assertThat(checker.isDeviceOffline()).isTrue()
    }

    @Test
    fun `a missing connectivity service is not offline`() {
        val context = mockk<Context> {
            every { applicationContext } returns this@mockk
            every { getSystemService(Context.CONNECTIVITY_SERVICE) } returns null
        }
        assertThat(DeviceConnectivityChecker(context).isDeviceOffline()).isFalse()
    }

    @Test
    fun `a connectivity lookup failure is not offline`() {
        val context = mockk<Context> {
            every { applicationContext } returns this@mockk
            every { getSystemService(Context.CONNECTIVITY_SERVICE) } throws
                SecurityException("missing ACCESS_NETWORK_STATE")
        }
        assertThat(DeviceConnectivityChecker(context).isDeviceOffline()).isFalse()
    }
}
