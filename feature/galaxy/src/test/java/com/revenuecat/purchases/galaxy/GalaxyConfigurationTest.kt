package com.revenuecat.purchases.galaxy

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Store
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GalaxyConfigurationTest {

    private val apiKey = "test-api-key"

    private lateinit var context: Context
    private lateinit var applicationContext: Context

    @Before
    fun setup() {
        context = mockk()
        applicationContext = mockk()
        every { context.applicationContext } returns applicationContext
        every { context.isDeviceProtectedStorage } returns false
    }

    @Test
    fun `GalaxyConfiguration Builder sets Galaxy defaults`() {
        val configuration = GalaxyConfiguration.Builder(context, apiKey).build()

        assertThat(configuration.store).isEqualTo(Store.GALAXY)
        assertThat(configuration.galaxyBillingMode).isEqualTo(GalaxyBillingMode.PRODUCTION)
        assertThat(configuration.context).isEqualTo(applicationContext)
        assertThat(configuration.apiKey).isEqualTo(apiKey)
    }

    @Test
    fun `GalaxyConfiguration Builder sets provided billing mode`() {
        val configuration = GalaxyConfiguration.Builder(
            context,
            apiKey,
            GalaxyBillingMode.TEST,
        ).build()

        assertThat(configuration.store).isEqualTo(Store.GALAXY)
        assertThat(configuration.galaxyBillingMode).isEqualTo(GalaxyBillingMode.TEST)
    }
}
