package com.revenuecat.purchases.galaxy

import android.content.Context
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Store
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.LOLLIPOP])
class GalaxyConfigurationTest {

    private val apiKey = "test-api-key"

    private lateinit var context: Context
    private lateinit var applicationContext: Context

    @Before
    fun setup() {
        context = mockk()
        applicationContext = mockk()

        every { context.applicationContext } returns applicationContext
    }

    @Test
    fun `GalaxyConfiguration builder sets store to Galaxy`() {
        val configuration = GalaxyConfiguration.Builder(
            context,
            apiKey,
            GalaxyBillingMode.PRODUCTION,
        ).build()

        assertThat(configuration.store).isEqualTo(Store.GALAXY)
    }

    @Test
    fun `GalaxyConfiguration builder sets galaxy billing mode`() {
        val configuration = GalaxyConfiguration.Builder(
            context,
            apiKey,
            GalaxyBillingMode.TEST,
        ).build()

        assertThat(configuration.galaxyBillingMode).isEqualTo(GalaxyBillingMode.TEST)
    }
}
