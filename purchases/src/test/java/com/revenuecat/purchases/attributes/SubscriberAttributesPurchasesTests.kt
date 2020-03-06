package com.revenuecat.purchases.attributes

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.IdentityManager
import com.revenuecat.purchases.Purchases
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ExecutorService

@RunWith(AndroidJUnit4::class)
class SubscriberAttributesPurchasesTests {
    private lateinit var underTest: Purchases
    private val appUserId = "juan"
    private val subscriberAttributesManagerMock = mockk<SubscriberAttributesManager>()

    @Before
    fun setup() {
        underTest = Purchases(
            mockk(relaxed = true),
            appUserId,
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            executorService = mockk<ExecutorService>().apply {
                val capturedRunnable = slot<Runnable>()
                every { execute(capture(capturedRunnable)) } answers { capturedRunnable.captured.run() }
            },
            identityManager = mockk<IdentityManager>(relaxed = true).apply {
                every { currentAppUserID } returns appUserId
            },
            subscriberAttributesManager = subscriberAttributesManagerMock
        )
    }

    @Test
    fun `setting email attribute`() {
        every {
            subscriberAttributesManagerMock.setAttribute(any(), any(), appUserId)
        } just Runs

        val email = "email"
        underTest.setEmail(email)

        verify {
            subscriberAttributesManagerMock.setAttribute(
                SubscriberAttributeKey.Email,
                email,
                appUserId
            )
        }
    }

    @Test
    fun `setting phone number attribute`() {
        val phoneNumber = "3154589485"

        every {
            subscriberAttributesManagerMock.setAttribute(SubscriberAttributeKey.PhoneNumber, phoneNumber, appUserId)
        } just Runs

        underTest.setPhoneNumber(phoneNumber)

        verify {
            subscriberAttributesManagerMock.setAttribute(
                SubscriberAttributeKey.PhoneNumber,
                phoneNumber,
                appUserId
            )
        }
    }

    @Test
    fun `setting display name attribute`() {
        every {
            subscriberAttributesManagerMock.setAttribute(any(), any(), appUserId)
        } just Runs

        val displayName = "Cesar"
        underTest.setDisplayName(displayName)

        verify {
            subscriberAttributesManagerMock.setAttribute(
                SubscriberAttributeKey.DisplayName,
                displayName,
                appUserId
            )
        }
    }

    @Test
    fun `setting push token attribute`() {
        every {
            subscriberAttributesManagerMock.setAttribute(any(), any(), appUserId)
        } just Runs

        val pushToken = "ajdjfh30203"
        underTest.setPushToken(pushToken)

        verify {
            subscriberAttributesManagerMock.setAttribute(
                SubscriberAttributeKey.FCMTokens,
                pushToken,
                appUserId
            )
        }
    }

    @Test
    fun `on app foregrounded attributes are synced`() {
        every {
            subscriberAttributesManagerMock.synchronizeSubscriberAttributesIfNeeded(appUserId, any(), any())
        } just Runs
        setup()
        underTest.onAppForegrounded()
        verify (exactly = 1) {
            subscriberAttributesManagerMock.synchronizeSubscriberAttributesIfNeeded(appUserId, any(), any())
        }
    }

    @Test
    fun `on app backgrounded attributes are synced`() {
        every {
            subscriberAttributesManagerMock.synchronizeSubscriberAttributesIfNeeded(appUserId, any(), any())
        } just Runs
        setup()
        underTest.onAppBackgrounded()
        verify (exactly = 1) {
            subscriberAttributesManagerMock.synchronizeSubscriberAttributesIfNeeded(appUserId, any(), any())
        }
    }
}