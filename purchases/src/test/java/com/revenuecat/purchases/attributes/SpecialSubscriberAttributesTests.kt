package com.revenuecat.purchases.attributes

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SpecialSubscriberAttributesTests {

    @Test
    fun `given an email key SubscriberAttributeKey can be created`() {
        assertThat(SPECIAL_KEY_EMAIL.getSubscriberAttributeKey()).isInstanceOf(
            SubscriberAttributeKey.Email::class.java
        )
    }

    @Test
    fun `given a display name key SubscriberAttributeKey can be created`() {
        assertThat(SPECIAL_KEY_DISPLAY_NAME.getSubscriberAttributeKey()).isInstanceOf(
            SubscriberAttributeKey.DisplayName::class.java
        )
    }

    @Test
    fun `given a phone number key SubscriberAttributeKey can be created`() {
        assertThat(SPECIAL_KEY_PHONE_NUMBER.getSubscriberAttributeKey()).isInstanceOf(
            SubscriberAttributeKey.PhoneNumber::class.java
        )
    }

    @Test
    fun `given a fcm tokens key SubscriberAttributeKey can be created`() {
        assertThat(SPECIAL_KEY_FCM_TOKENS.getSubscriberAttributeKey()).isInstanceOf(
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