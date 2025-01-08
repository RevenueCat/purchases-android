package com.revenuecat.purchases

import android.os.Bundle
import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ErrorsTest {

    @Test
    fun errorIsSerializable() {
        val purchasesError = PurchasesError(
            PurchasesErrorCode.ConfigurationError,
            "Underlying error message",
        )
        val purchasesException = PurchasesException(purchasesError)
        val bundle = Bundle().apply {
            putSerializable("exception", purchasesException)
        }
        val parcel = Parcel.obtain()
        val readBundle: Bundle?
        try {
            parcel.writeBundle(bundle)
            parcel.setDataPosition(0)
            readBundle = parcel.readBundle(javaClass.classLoader)!!
        } finally {
            parcel.recycle()
        }
        assertThat(readBundle).isNotNull
        val serializedException = readBundle!!.getSerializable("exception", PurchasesException::class.java)
        assertThat(serializedException!!.code).isEqualTo(purchasesException.code)
        assertThat(serializedException.underlyingErrorMessage).isEqualTo(purchasesException.underlyingErrorMessage)
    }
}
