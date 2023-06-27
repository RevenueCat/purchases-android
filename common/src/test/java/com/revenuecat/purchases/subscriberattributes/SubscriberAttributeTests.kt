package com.revenuecat.purchases.subscriberattributes

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.subscriberattributes.SubscriberAttributeKey
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class SubscriberAttributeTests {

    @Test
    fun `timestamp is correctly set`() {
        val now = Date()
        val dateProvider = object : DateProvider {
            override val now: Date
                get() = now
        }
        val subscriberAttribute =
            SubscriberAttribute("email", "un@email.com", dateProvider = dateProvider)
        assertThat(subscriberAttribute.setTime).isEqualTo(now)
    }

    @Test
    fun `toJSONObject works`() {
        val now = Date()
        val emailKey = "email"
        val emailValue = "un@email.com"
        val toJSONObject = SubscriberAttribute(emailKey, emailValue, setTime = now).toJSONObject()
        assertThat(toJSONObject.getString(JSON_NAME_KEY)).isEqualTo(emailKey)
        assertThat(toJSONObject.getString(JSON_NAME_VALUE)).isEqualTo(emailValue)
        assertThat(toJSONObject.getLong(JSON_NAME_SET_TIME)).isEqualTo(now.time)
    }

    @Test
    fun `JSONObject constructor works`() {
        val now = Date()
        val emailKey = "email"
        val emailValue = "un@email.com"
        val jsonObject = JSONObject().apply {
            put(JSON_NAME_KEY, emailKey)
            put(JSON_NAME_VALUE, emailValue)
            put(JSON_NAME_SET_TIME, now.time)
            put(JSON_NAME_IS_SYNCED, false)
        }
        val subscriberAttribute = SubscriberAttribute(jsonObject)
        assertThat(subscriberAttribute.key.backendKey).isEqualTo(emailKey)
        assertThat(subscriberAttribute.value).isEqualTo(emailValue)
        assertThat(subscriberAttribute.setTime).isEqualTo(now)
    }

    @Test
    fun `JSONObject missing values`() {
        val emailKey = "email"
        val emailValue = "un@email.com"
        val jsonObject = JSONObject().apply {
            put(JSON_NAME_KEY, emailKey)
            put(JSON_NAME_VALUE, emailValue)
        }
        assertThatExceptionOfType(JSONException::class.java).isThrownBy { SubscriberAttribute(jsonObject) }
    }

    @Test
    fun `null value sets JSON correctly`() {
        val emailKey = "email"
        val emailValue = null
        val toJSONObject = SubscriberAttribute(emailKey, emailValue).toJSONObject()
        assertThat(toJSONObject.isNull(JSON_NAME_VALUE)).isTrue()
    }

    @Test
    fun `JSONObject constructor with null value`() {
        val now = Date()
        val emailKey = "email"
        val jsonObject = JSONObject().apply {
            put(JSON_NAME_KEY, emailKey)
            put(JSON_NAME_VALUE, JSONObject.NULL)
            put(JSON_NAME_SET_TIME, now.time)
            put(JSON_NAME_IS_SYNCED, true)
        }
        val subscriberAttribute = SubscriberAttribute(jsonObject)
        assertThat(subscriberAttribute.key.backendKey).isEqualTo(emailKey)
        assertThat(subscriberAttribute.value).isEqualTo(null)
        assertThat(subscriberAttribute.setTime).isEqualTo(now)
        assertThat(subscriberAttribute.isSynced).isTrue()
    }

    @Test
    fun `subscriber with string as key constructor`() {
        val now = Date()
        val emailKey = "email"
        val value = "un@email.com"
        val subscriberAttribute = SubscriberAttribute(emailKey, value, setTime = now)
        assertThat(subscriberAttribute.key).isEqualTo(SubscriberAttributeKey.Custom(emailKey))
        assertThat(subscriberAttribute.value).isEqualTo(value)
        assertThat(subscriberAttribute.setTime).isEqualTo(now)
    }

    @Test
    fun `backend map looks as expected`() {
        val now = Date()
        val emailKey = "email"
        val value = "un@email.com"
        val subscriberAttribute = SubscriberAttribute(emailKey, value, setTime = now)
        val backendMap = subscriberAttribute.toBackendMap()

        assertThat(backendMap[BACKEND_NAME_VALUE]).isEqualTo(value)
        assertThat(backendMap[BACKEND_NAME_TIMESTAMP]).isEqualTo(now.time)
    }

}
