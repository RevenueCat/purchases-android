package com.revenuecat.purchases

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesAreCompletedBy.MY_APP
import com.revenuecat.purchases.PurchasesAreCompletedBy.REVENUECAT
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ExecutorService

@RunWith(AndroidJUnit4::class)
class PurchasesConfigurationTest {

    private val apiKey = "test-api-key"

    private lateinit var context: Context
    private lateinit var applicationContext: Context

    private lateinit var builder: PurchasesConfiguration.Builder

    @Before
    fun setup() {
        context = mockk()
        applicationContext = mockk()
        every { context.applicationContext } returns applicationContext

        builder = PurchasesConfiguration.Builder(context, apiKey)
    }

    @Test
    fun `PurchasesConfiguration has expected default parameters`() {
        val purchasesConfiguration = builder.build()
        assertThat(purchasesConfiguration.apiKey).isEqualTo(apiKey)
        assertThat(purchasesConfiguration.context).isEqualTo(applicationContext)
        assertThat(purchasesConfiguration.appUserID).isNull()
        assertThat(purchasesConfiguration.observerMode).isFalse
        assertThat(purchasesConfiguration.purchasesAreCompletedBy).isEqualTo(REVENUECAT)
        assertThat(purchasesConfiguration.service).isNull()
        assertThat(purchasesConfiguration.store).isEqualTo(Store.PLAY_STORE)
        assertThat(purchasesConfiguration.diagnosticsEnabled).isFalse
        assertThat(purchasesConfiguration.verificationMode).isEqualTo(EntitlementVerificationMode.DISABLED)
        assertThat(purchasesConfiguration.dangerousSettings).isEqualTo(DangerousSettings(autoSyncPurchases = true))
        assertThat(purchasesConfiguration.showInAppMessagesAutomatically).isTrue
        assertThat(purchasesConfiguration.pendingTransactionsForPrepaidPlansEnabled).isFalse
    }

    @Test
    fun `PurchasesConfiguration sets appUserId correctly`() {
        val testUserId = "test-user-id"
        val purchasesConfiguration = builder.appUserID(testUserId).build()
        assertThat(purchasesConfiguration.appUserID).isEqualTo(testUserId)
    }

    @Test
    fun `PurchasesConfiguration sets observerMode correctly`() {
        val purchasesConfiguration = builder.observerMode(true).build()
        assertThat(purchasesConfiguration.observerMode).isTrue
        assertThat(purchasesConfiguration.purchasesAreCompletedBy).isEqualTo(MY_APP)
    }

    @Test
    fun `PurchasesConfiguration sets purchasesAreCompletedBy correctly`() {
        val purchasesConfiguration = builder.purchasesAreCompletedBy(MY_APP).build()
        assertThat(purchasesConfiguration.purchasesAreCompletedBy).isEqualTo(MY_APP)
        assertThat(purchasesConfiguration.observerMode).isTrue
    }

    @Test
    fun `PurchasesConfiguration sets showInAppMessagesAutomatically correctly`() {
        val purchasesConfiguration = builder.showInAppMessagesAutomatically(false).build()
        assertThat(purchasesConfiguration.showInAppMessagesAutomatically).isFalse
    }

    @Test
    fun `PurchasesConfiguration sets service correctly`() {
        val serviceMock: ExecutorService = mockk()
        val purchasesConfiguration = builder.service(serviceMock).build()
        assertThat(purchasesConfiguration.service).isEqualTo(serviceMock)
    }

    @Test
    fun `PurchasesConfiguration sets store correctly`() {
        val purchasesConfiguration = builder.store(Store.AMAZON).build()
        assertThat(purchasesConfiguration.store).isEqualTo(Store.AMAZON)
    }

    @Test
    fun `PurchasesConfiguration sets diagnosticsEnabled correctly`() {
        val purchasesConfiguration = builder.diagnosticsEnabled(true).build()
        assertThat(purchasesConfiguration.diagnosticsEnabled).isTrue
    }

    @Test
    fun `PurchasesConfiguration sets informational mode correctly`() {
        val purchasesConfiguration = builder.entitlementVerificationMode(
            EntitlementVerificationMode.INFORMATIONAL
        ).build()
        assertThat(purchasesConfiguration.verificationMode).isEqualTo(EntitlementVerificationMode.INFORMATIONAL)
    }

    @Test
    fun `PurchasesConfiguration sets pending prepaid support enabled correctly`() {
        val purchasesConfiguration = builder.pendingTransactionsForPrepaidPlansEnabled(true).build()
        assertThat(purchasesConfiguration.pendingTransactionsForPrepaidPlansEnabled).isTrue
    }

    @Test
    fun `PurchasesConfiguration sets dangerous settings correctly`() {
        val dangerousSettings = DangerousSettings(autoSyncPurchases = false)
        val purchasesConfiguration = builder.dangerousSettings(dangerousSettings).build()
        assertThat(purchasesConfiguration.dangerousSettings).isEqualTo(dangerousSettings)
    }

    @Test
    fun `PurchasesConfiguration trims api key`() {
        val purchasesConfiguration = PurchasesConfiguration.Builder(context, "  test-api-key  ").build()
        assertThat(purchasesConfiguration.apiKey).isEqualTo("test-api-key")
    }
}
