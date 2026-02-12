package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VerificationResultTest {

    @Test
    fun `isVerified returns expected values`() {
        assertThat(VerificationResult.VERIFIED.isVerified).isTrue
        assertThat(VerificationResult.VERIFIED_ON_DEVICE.isVerified).isTrue
        assertThat(VerificationResult.NOT_REQUESTED.isVerified).isFalse
        assertThat(VerificationResult.FAILED.isVerified).isFalse
    }
}
