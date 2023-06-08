package com.revenuecat.purchases

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PurchasesExceptionTest {

    @Test
    fun `error matches originating error`() {
        val errorCode = PurchasesErrorCode.ConfigurationError
        val underlyingErrorMessage = "Underlying error message"
        val purchasesError = PurchasesError(errorCode, underlyingErrorMessage)

        val purchasesException = PurchasesException(purchasesError)

        assertThat(purchasesError).isEqualTo(purchasesException.error)
    }

    @Test
    fun `error code matches originating error`() {
        val errorCode = PurchasesErrorCode.ConfigurationError
        val underlyingErrorMessage = "Underlying error message"
        val purchasesError = PurchasesError(errorCode, underlyingErrorMessage)

        val purchasesException = PurchasesException(purchasesError)

        assertThat(errorCode).isEqualTo(purchasesException.code)
    }

    @Test
    fun `underlying error message matches`() {
        val errorCode = PurchasesErrorCode.ConfigurationError
        val underlyingErrorMessage = "Underlying error message"
        val purchasesError = PurchasesError(errorCode, underlyingErrorMessage)

        val purchasesException = PurchasesException(purchasesError)

        assertThat(underlyingErrorMessage).isEqualTo(purchasesException.underlyingErrorMessage)
    }

    @Test
    fun `error message matches originating error`() {
        val errorCode = PurchasesErrorCode.ConfigurationError
        val underlyingErrorMessage = "Underlying error message"
        val purchasesError = PurchasesError(errorCode, underlyingErrorMessage)

        val purchasesException = PurchasesException(purchasesError)

        assertThat(purchasesError.message).isEqualTo(purchasesException.message)
    }
}
