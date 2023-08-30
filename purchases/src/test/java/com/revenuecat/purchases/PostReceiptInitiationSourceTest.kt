package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostReceiptInitiationSourceTest {

    @Test
    fun `initiation source have correct postReceiptFieldValue`() {
        assertThat(PostReceiptInitiationSource.PURCHASE.postReceiptFieldValue).isEqualTo("purchase")
        assertThat(PostReceiptInitiationSource.RESTORE.postReceiptFieldValue).isEqualTo("restore")
        assertThat(PostReceiptInitiationSource.QUEUE.postReceiptFieldValue).isEqualTo("queue")
    }
}
