package com.revenuecat.purchases.subscriberattributes

import com.revenuecat.purchases.common.subscriberattributes.ReservedSubscriberAttribute
import com.revenuecat.purchases.common.subscriberattributes.SubscriberAttributeKey
import com.revenuecat.purchases.common.subscriberattributes.getSubscriberAttributeKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SpecialSubscriberAttributesTests {

    @Test
    fun `given an email key SubscriberAttributeKey can be created`() {
        assertThat(ReservedSubscriberAttribute.EMAIL.value.getSubscriberAttributeKey()).isInstanceOf(
            SubscriberAttributeKey.Email::class.java
        )
    }

    @Test
    fun `given a display name key SubscriberAttributeKey can be created`() {
        assertThat(ReservedSubscriberAttribute.DISPLAY_NAME.value.getSubscriberAttributeKey()).isInstanceOf(
            SubscriberAttributeKey.DisplayName::class.java
        )
    }

    @Test
    fun `given a phone number key SubscriberAttributeKey can be created`() {
        assertThat(ReservedSubscriberAttribute.PHONE_NUMBER.value.getSubscriberAttributeKey()).isInstanceOf(
            SubscriberAttributeKey.PhoneNumber::class.java
        )
    }

    @Test
    fun `given a fcm tokens key SubscriberAttributeKey can be created`() {
        assertThat(ReservedSubscriberAttribute.FCM_TOKENS.value.getSubscriberAttributeKey()).isInstanceOf(
            SubscriberAttributeKey.FCMTokens::class.java
        )
    }

    @Test
    fun `given a custom key SubscriberAttributeKey can be created`() {
        assertThat("custom".getSubscriberAttributeKey()).isInstanceOf(
            SubscriberAttributeKey.Custom::class.java
        )
    }
}
