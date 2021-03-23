package com.revenuecat.purchases.amazon.handler

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amazon.device.iap.internal.model.UserDataBuilder
import com.amazon.device.iap.internal.model.UserDataResponseBuilder
import com.amazon.device.iap.model.RequestId
import com.amazon.device.iap.model.UserData
import com.amazon.device.iap.model.UserDataResponse
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.amazon.helpers.PurchasingServiceProviderForTest
import com.revenuecat.purchases.amazon.helpers.dummyUserData
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserDataHandlerTest {

    private lateinit var underTest: UserDataHandler
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

    @Before
    fun setup() {
        purchasingServiceProvider = PurchasingServiceProviderForTest()
        underTest = UserDataHandler(purchasingServiceProvider)
    }

    @Test
    fun `getting user data successfully`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getUserDataRequestId = dummyRequestId

        underTest.getUserData(expectedOnSuccess, unexpectedOnError)

        assertThat(purchasingServiceProvider.getUserDataCalled).isTrue

        val response = getDummyUserDataResponse(dummyRequestId)
        underTest.onUserDataResponse(response)

        assertThat(receivedUserData).isEqualTo(response.userData)
    }

    @Test
    fun `Failure getting user data sends appropriate error`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getUserDataRequestId = dummyRequestId

        underTest.getUserData(unexpectedOnSuccess, expectedOnError)

        assertThat(purchasingServiceProvider.getUserDataCalled).isTrue

        val response = getDummyUserDataResponse(dummyRequestId, requestStatus = UserDataResponse.RequestStatus.FAILED)
        underTest.onUserDataResponse(response)

        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `Getting user data is not supperted, and sends appropriate error`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getUserDataRequestId = dummyRequestId

        underTest.getUserData(unexpectedOnSuccess, expectedOnError)

        assertThat(purchasingServiceProvider.getUserDataCalled).isTrue

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

        assertThat(purchasingServiceProvider.getUserDataCalled).isTrue

        val response = getDummyUserDataResponse(dummyRequestId)
        underTest.onUserDataResponse(response)
        underTest.onUserDataResponse(response)

        assertThat(receivedCount).isOne
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

}
