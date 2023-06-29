package com.revenuecat.purchases.amazon.handler

import android.os.Handler
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amazon.device.iap.internal.model.UserDataResponseBuilder
import com.amazon.device.iap.model.RequestId
import com.amazon.device.iap.model.UserData
import com.amazon.device.iap.model.UserDataResponse
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.amazon.helpers.PurchasingServiceProviderForTest
import com.revenuecat.purchases.amazon.helpers.dummyUserData
import com.revenuecat.purchases.utils.MockTimestampProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserDataHandlerTest {

    private lateinit var underTest: UserDataHandler
    private lateinit var mainHandler: Handler
    private lateinit var timestampProvider: MockTimestampProvider
    private lateinit var purchasingServiceProvider: PurchasingServiceProviderForTest

    private var receivedUserData: UserData? = null
    private var receivedError: PurchasesError? = null

    private var expectedOnSuccess: (UserData) -> Unit = {
        receivedUserData = it
    }
    private var unexpectedOnSuccess: (UserData) -> Unit = {
        Assertions.fail("should be error")
    }

    private var expectedOnError: (PurchasesError) -> Unit = {
        receivedError = it
    }

    private var unexpectedOnError: (PurchasesError) -> Unit = {
        Assertions.fail("should be success")
    }

    private val mainHandlerCallbacks: MutableList<Runnable> = ArrayList()

    @Before
    fun setup() {
        purchasingServiceProvider = PurchasingServiceProviderForTest()
        mainHandlerCallbacks.clear()
        timestampProvider = MockTimestampProvider()
        setupMainHandler()
        underTest = UserDataHandler(purchasingServiceProvider, mainHandler, timestampProvider)
    }

    @Test
    fun `getting user data successfully`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getUserDataRequestId = dummyRequestId

        underTest.getUserData(expectedOnSuccess, unexpectedOnError)

        assertThat(purchasingServiceProvider.getUserDataCalledTimes).isEqualTo(1)

        val response = getDummyUserDataResponse(dummyRequestId)
        underTest.onUserDataResponse(response)

        assertThat(receivedUserData).isEqualTo(response.userData)
    }

    @Test
    fun `Failure getting user data sends appropriate error`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getUserDataRequestId = dummyRequestId

        underTest.getUserData(unexpectedOnSuccess, expectedOnError)

        assertThat(purchasingServiceProvider.getUserDataCalledTimes).isEqualTo(1)

        val response = getDummyUserDataResponse(dummyRequestId, requestStatus = UserDataResponse.RequestStatus.FAILED)
        underTest.onUserDataResponse(response)

        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `Getting user data is not supported, and sends appropriate error`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getUserDataRequestId = dummyRequestId

        underTest.getUserData(unexpectedOnSuccess, expectedOnError)

        assertThat(purchasingServiceProvider.getUserDataCalledTimes).isEqualTo(1)

        val response =
            getDummyUserDataResponse(dummyRequestId, requestStatus = UserDataResponse.RequestStatus.NOT_SUPPORTED)
        underTest.onUserDataResponse(response)

        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `callbacks are invoked just once`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getUserDataRequestId = dummyRequestId

        var receivedCount = 0
        underTest.getUserData(onSuccess = { receivedCount++ }, unexpectedOnError)

        assertThat(purchasingServiceProvider.getUserDataCalledTimes).isEqualTo(1)

        val response = getDummyUserDataResponse(dummyRequestId)
        underTest.onUserDataResponse(response)
        underTest.onUserDataResponse(response)

        assertThat(receivedCount).isOne
    }

    @Test
    fun `Exceptions are logged so they are not swallowed by Amazon`() {
        val expectedException = RuntimeException("")
        var receivedException: Throwable? = null
        var receivedLoggedException: Throwable? = null
        Purchases.logHandler = object : LogHandler {
            override fun v(tag: String, msg: String) {
            }

            override fun d(tag: String, msg: String) {
            }

            override fun i(tag: String, msg: String) {
            }

            override fun w(tag: String, msg: String) {
            }

            override fun e(tag: String, msg: String, throwable: Throwable?) {
                receivedLoggedException = throwable
            }
        }
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getUserDataRequestId = dummyRequestId

        underTest.getUserData(onSuccess = { throw expectedException }, unexpectedOnError)

        assertThat(purchasingServiceProvider.getUserDataCalledTimes).isEqualTo(1)

        val response = getDummyUserDataResponse(dummyRequestId)
        try {
            underTest.onUserDataResponse(response)
        } catch (e: Exception) {
            receivedException = e
        }

        assertThat(receivedException).isNotNull
        assertThat(receivedLoggedException).isNotNull
        assertThat(expectedException).isEqualTo(receivedException)
        assertThat(expectedException).isEqualTo(receivedLoggedException)
    }

    @Test
    fun `timeout millis when getting user data is correct`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getUserDataRequestId = dummyRequestId

        underTest.getUserData(
            unexpectedOnSuccess,
            unexpectedOnError
        )

        verify(exactly = 1) { mainHandler.postDelayed(any(), 10_000L) }
        assertThat(mainHandlerCallbacks.size).isEqualTo(1)
    }

    @Test
    fun `request fails with timeout if did not receive response`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getUserDataRequestId = dummyRequestId

        var resultError: PurchasesError? = null
        underTest.getUserData(
            unexpectedOnSuccess
        ) { resultError = it }

        assertThat(resultError).isNull()

        assertThat(mainHandlerCallbacks.size).isEqualTo(1)
        mainHandlerCallbacks[0].run()

        assertThat(resultError).isNotNull
        assertThat(resultError?.code).isEqualTo(PurchasesErrorCode.UnknownError)
        assertThat(resultError?.underlyingErrorMessage).isEqualTo(
            "Timeout error trying to get Amazon user data."
        )
    }

    @Test
    fun `request does not succeed if received response after timeout`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getUserDataRequestId = dummyRequestId

        var resultError: PurchasesError? = null
        underTest.getUserData(
            unexpectedOnSuccess
        ) { resultError = it }

        assertThat(mainHandlerCallbacks.size).isEqualTo(1)
        mainHandlerCallbacks[0].run()

        val response = getDummyUserDataResponse(
            expectedRequestId = dummyRequestId,
        )

        underTest.onUserDataResponse(response)

        assertThat(resultError).isNotNull
    }

    @Test
    fun `request succeeds if received response before timeout`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getUserDataRequestId = dummyRequestId

        var userData: UserData? = null
        underTest.getUserData(
            { userData = it },
            unexpectedOnError
        )

        assertThat(mainHandlerCallbacks.size).isEqualTo(1)

        val response = getDummyUserDataResponse(expectedRequestId = dummyRequestId)

        underTest.onUserDataResponse(response)

        mainHandlerCallbacks[0].run()

        assertThat(userData).isNotNull
    }

    @Test
    fun `getting user data before cache expiration returns cached user data without initiating new requests`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getUserDataRequestId = dummyRequestId

        timestampProvider.overridenCurrentTimeMillis = 5_000L

        var userDataRequest1: UserData? = null
        underTest.getUserData(
            { userDataRequest1 = it },
            unexpectedOnError
        )

        val response = getDummyUserDataResponse(expectedRequestId = dummyRequestId)
        underTest.onUserDataResponse(response)

        timestampProvider.overridenCurrentTimeMillis = 304_000L

        var userDataRequest2: UserData? = null
        underTest.getUserData(
            { userDataRequest2 = it },
            unexpectedOnError
        )

        assertThat(userDataRequest1).isNotNull
        assertThat(userDataRequest2).isNotNull

        assertThat(purchasingServiceProvider.getUserDataCalledTimes).isEqualTo(1)
    }

    @Test
    fun `getting user data after cache expiration returns cached user data without initiating new requests`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getUserDataRequestId = dummyRequestId
        timestampProvider.overridenCurrentTimeMillis = 5_000L

        var userDataRequest1: UserData? = null
        underTest.getUserData(
            { userDataRequest1 = it },
            unexpectedOnError
        )

        val response = getDummyUserDataResponse(expectedRequestId = dummyRequestId)
        underTest.onUserDataResponse(response)

        assertThat(userDataRequest1).isNotNull

        timestampProvider.overridenCurrentTimeMillis = 306_000L

        var userDataRequest2: UserData? = null
        underTest.getUserData(
            { userDataRequest2 = it },
            unexpectedOnError
        )

        assertThat(userDataRequest2).isNull()

        underTest.onUserDataResponse(response)
        assertThat(userDataRequest2).isNotNull

        assertThat(purchasingServiceProvider.getUserDataCalledTimes).isEqualTo(2)
    }

    private fun getDummyUserDataResponse(
        expectedRequestId: String = "${System.currentTimeMillis()}",
        requestStatus: UserDataResponse.RequestStatus = UserDataResponse.RequestStatus.SUCCESSFUL
    ): UserDataResponse {
        val userData = dummyUserData()

        val builder = UserDataResponseBuilder()
            .setRequestId(RequestId.fromString(expectedRequestId))
            .setRequestStatus(requestStatus)
            .setUserData(userData)

        return UserDataResponse(builder)
    }

    private fun setupMainHandler() {
        mainHandler = mockk()
        every {
            mainHandler.postDelayed(any(), any())
        } answers {
            mainHandlerCallbacks.add(firstArg())
        }
    }
}
