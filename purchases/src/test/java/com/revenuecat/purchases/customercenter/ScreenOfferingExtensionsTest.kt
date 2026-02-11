package com.revenuecat.purchases.customercenter

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
public class ScreenOfferingExtensionsTest {

    private val mockPurchases = mockk<Purchases>()
    private val mockOfferings = mockk<Offerings>()
    private val mockCurrentOffering = mockk<Offering>()
    private val mockSpecificOffering = mockk<Offering>()

    @Before
    public fun setUp() {
        mockkObject(Purchases)
        every { Purchases.sharedInstance } returns mockPurchases

        every { mockOfferings.current } returns mockCurrentOffering
        every { mockOfferings.all } returns mapOf(
            "premium_monthly" to mockSpecificOffering,
            "premium_yearly" to mockk()
        )

        every { mockCurrentOffering.identifier } returns "current_offering"
        every { mockSpecificOffering.identifier } returns "premium_monthly"
    }

    @After
    public fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `resolveOffering - CURRENT type returns current offering`() {
        val screen = CustomerCenterConfigData.Screen(
            type = CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE,
            title = "Test Screen",
            paths = emptyList(),
            offering = CustomerCenterConfigData.ScreenOffering(
                type = CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.CURRENT
            )
        )

        val onErrorSlot = slot<(PurchasesError) -> Unit>()
        val onSuccessSlot = slot<(Offerings) -> Unit>()
        
        every { 
            mockPurchases.getOfferings(any()) 
        } answers {
            val callback = arg<ReceiveOfferingsCallback>(0)
            callback.onReceived(mockOfferings)
        }

        var resultOffering: Offering? = null
        var errorReceived: PurchasesError? = null

        screen.resolveOffering(
            purchases = mockPurchases,
            onError = { error -> errorReceived = error },
            onSuccess = { offering -> resultOffering = offering }
        )

        assertThat(resultOffering).isEqualTo(mockCurrentOffering)
        assertThat(errorReceived).isNull()
    }

    @Test
    fun `resolveOffering - SPECIFIC type returns specific offering`() {
        val screen = CustomerCenterConfigData.Screen(
            type = CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE,
            title = "Test Screen",
            paths = emptyList(),
            offering = CustomerCenterConfigData.ScreenOffering(
                type = CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.SPECIFIC,
                offeringId = "premium_monthly"
            )
        )

        every { 
            mockPurchases.getOfferings(any()) 
        } answers {
            val callback = arg<ReceiveOfferingsCallback>(0)
            callback.onReceived(mockOfferings)
        }

        var resultOffering: Offering? = null
        var errorReceived: PurchasesError? = null

        screen.resolveOffering(
            purchases = mockPurchases,
            onError = { error -> errorReceived = error },
            onSuccess = { offering -> resultOffering = offering }
        )

        assertThat(resultOffering).isEqualTo(mockSpecificOffering)
        assertThat(errorReceived).isNull()
    }

    @Test
    fun `resolveOffering - SPECIFIC type with non-existent offering ID returns null`() {
        val screen = CustomerCenterConfigData.Screen(
            type = CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE,
            title = "Test Screen",
            paths = emptyList(),
            offering = CustomerCenterConfigData.ScreenOffering(
                type = CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.SPECIFIC,
                offeringId = "non_existent_offering"
            )
        )

        every { 
            mockPurchases.getOfferings(any()) 
        } answers {
            val callback = arg<ReceiveOfferingsCallback>(0)
            callback.onReceived(mockOfferings)
        }

        var resultOffering: Offering? = null
        var errorReceived: PurchasesError? = null

        screen.resolveOffering(
            purchases = mockPurchases,
            onError = { error -> errorReceived = error },
            onSuccess = { offering -> resultOffering = offering }
        )

        assertThat(resultOffering).isNull()
        assertThat(errorReceived).isNull()
    }

    @Test
    fun `resolveOffering - SPECIFIC type with null offering ID returns null`() {
        val screen = CustomerCenterConfigData.Screen(
            type = CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE,
            title = "Test Screen",
            paths = emptyList(),
            offering = CustomerCenterConfigData.ScreenOffering(
                type = CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.SPECIFIC,
                offeringId = null
            )
        )

        every { 
            mockPurchases.getOfferings(any()) 
        } answers {
            val callback = arg<ReceiveOfferingsCallback>(0)
            callback.onReceived(mockOfferings)
        }

        var resultOffering: Offering? = null
        var errorReceived: PurchasesError? = null

        screen.resolveOffering(
            purchases = mockPurchases,
            onError = { error -> errorReceived = error },
            onSuccess = { offering -> resultOffering = offering }
        )

        assertThat(resultOffering).isNull()
        assertThat(errorReceived).isNull()
    }

    @Test
    fun `resolveOffering - no offering specified returns null`() {
        val screen = CustomerCenterConfigData.Screen(
            type = CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT,
            title = "Test Screen",
            paths = emptyList(),
            offering = null
        )

        var resultOffering: Offering? = null
        var errorReceived: PurchasesError? = null

        screen.resolveOffering(
            purchases = mockPurchases,
            onError = { error -> errorReceived = error },
            onSuccess = { offering -> resultOffering = offering }
        )

        assertThat(resultOffering).isNull()
        assertThat(errorReceived).isNull()
    }

    @Test
    fun `resolveOffering - API error calls onError`() {
        val screen = CustomerCenterConfigData.Screen(
            type = CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE,
            title = "Test Screen",
            paths = emptyList(),
            offering = CustomerCenterConfigData.ScreenOffering(
                type = CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.CURRENT
            )
        )

        val expectedError = PurchasesError(PurchasesErrorCode.NetworkError, "Network error")

        every { 
            mockPurchases.getOfferings(any()) 
        } answers {
            val callback = arg<ReceiveOfferingsCallback>(0)
            callback.onError(expectedError)
        }

        var resultOffering: Offering? = null
        var errorReceived: PurchasesError? = null

        screen.resolveOffering(
            purchases = mockPurchases,
            onError = { error -> errorReceived = error },
            onSuccess = { offering -> resultOffering = offering }
        )

        assertThat(resultOffering).isNull()
        assertThat(errorReceived).isEqualTo(expectedError)
    }

}