package com.revenuecat.purchases.common.networking

import android.content.Context
import android.net.ConnectivityManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class DeviceConnectivityCheckerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val checker = DeviceConnectivityChecker(context)

    @Test
    fun `an active network is not offline`() {
        assertThat(checker.isDeviceOffline()).isFalse()
    }

    @Test
    fun `no active network is offline`() {
        shadowOf(connectivityManager).setActiveNetworkInfo(null)

        assertThat(connectivityManager.activeNetwork).isNull()
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
