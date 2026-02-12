package com.revenuecat.purchases

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PurchasesTransactionExceptionTest {

    @Test
    fun `error matches originating error`() {
        val errorCode = PurchasesErrorCode.ConfigurationError
        val underlyingErrorMessage = "Underlying error message"
        val purchasesError = PurchasesError(errorCode, underlyingErrorMessage)

        val purchasesException = PurchasesTransactionException(purchasesError, userCancelled = false)

        assertThat(purchasesError).isEqualTo(purchasesException.error)
    }

    @Test
    fun `error code matches originating error`() {
        val errorCode = PurchasesErrorCode.ConfigurationError
        val underlyingErrorMessage = "Underlying error message"
        val purchasesError = PurchasesError(errorCode, underlyingErrorMessage)

        val purchasesException = PurchasesTransactionException(purchasesError, userCancelled = true)

        assertThat(errorCode).isEqualTo(purchasesException.code)
    }

    @Test
    fun `underlying error message matches`() {
        val errorCode = PurchasesErrorCode.ConfigurationError
        val underlyingErrorMessage = "Underlying error message"
        val purchasesError = PurchasesError(errorCode, underlyingErrorMessage)

        val purchasesException = PurchasesTransactionException(purchasesError, userCancelled = false)

        assertThat(underlyingErrorMessage).isEqualTo(purchasesException.underlyingErrorMessage)
    }

    @Test
    fun `error message matches originating error`() {
        val errorCode = PurchasesErrorCode.ConfigurationError
        val underlyingErrorMessage = "Underlying error message"
        val purchasesError = PurchasesError(errorCode, underlyingErrorMessage)

        val purchasesException = PurchasesTransactionException(purchasesError, userCancelled = false)

        assertThat(purchasesError.message).isEqualTo(purchasesException.message)
    }

    @Test
    fun `user cancelled matches value passed in`() {
        val errorCode = PurchasesErrorCode.ConfigurationError
        val underlyingErrorMessage = "Underlying error message"
        val purchasesError = PurchasesError(errorCode, underlyingErrorMessage)

        var purchasesException = PurchasesTransactionException(purchasesError, userCancelled = false)
        assertThat(purchasesException.userCancelled).isEqualTo(false)

        purchasesException = PurchasesTransactionException(purchasesError, userCancelled = true)
        assertThat(purchasesException.userCancelled).isEqualTo(true)
    }
}
