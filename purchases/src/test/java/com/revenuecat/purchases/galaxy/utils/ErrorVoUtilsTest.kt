package com.revenuecat.purchases.galaxy.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.galaxy.constants.GalaxyErrorCode
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ErrorVoUtilsTest {

    @Test
    fun `isError returns false when error code indicates success`() {
        val errorVo = mockErrorVo(GalaxyErrorCode.IAP_ERROR_NONE.code)

        assertThat(errorVo.isError()).isFalse
    }

    @Test
    fun `isError returns true when error code matches a known error`() {
        val errorVo = mockErrorVo(GalaxyErrorCode.IAP_PAYMENT_IS_CANCELED.code)

        assertThat(errorVo.isError()).isTrue
    }

    @Test
    fun `isError treats unknown error codes as errors`() {
        val unexpectedCode = 999
        val errorVo = mockErrorVo(unexpectedCode)

        assertThat(errorVo.isError()).isTrue
    }

    private fun mockErrorVo(errorCode: Int): ErrorVo =
        mockk<ErrorVo>().apply {
            every { this@apply.errorCode } returns errorCode
        }
}
