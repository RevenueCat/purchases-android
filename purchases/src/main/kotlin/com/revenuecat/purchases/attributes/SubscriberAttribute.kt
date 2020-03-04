package com.revenuecat.purchases.attributes

import com.revenuecat.purchases.getNullableString
import com.revenuecat.purchases.timeInSeconds
import com.revenuecat.purchases.utils.DateProvider
import com.revenuecat.purchases.utils.DefaultDateProvider
import org.json.JSONException
import org.json.JSONObject
import java.util.Date

internal const val JSON_NAME_KEY = "key"
internal const val JSON_NAME_VALUE = "value"
internal const val JSON_NAME_SET_TIME = "set_time"
internal const val JSON_NAME_IS_SYNCED = "is_synced"

internal const val BACKEND_NAME_VALUE = "value"
internal const val BACKEND_NAME_TIMESTAMP = "updated_at"

internal data class SubscriberAttribute(
    val key: SubscriberAttributeKey,
    val value: String?,
    val dateProvider: DateProvider = DefaultDateProvider(),
    val setTime: Date = dateProvider.now,
    val isSynced: Boolean = false
) {

    constructor(
        key: String,
        value: String?,
        dateProvider: DateProvider = DefaultDateProvider(),
        setTime: Date = dateProvider.now,
        isSynced: Boolean = false
    ) : this(key.getSubscriberAttributeKey(), value, setTime = setTime, isSynced = isSynced)

    @Throws(JSONException::class)
    constructor(jsonObject: JSONObject) : this(
        key = jsonObject.getString(JSON_NAME_KEY).getSubscriberAttributeKey(),
        value = jsonObject.getNullableString(JSON_NAME_VALUE),
        setTime = Date(jsonObject.getLong(JSON_NAME_SET_TIME)),
        isSynced = jsonObject.getBoolean(JSON_NAME_IS_SYNCED)
    )

    fun toJSONObject() = JSONObject().apply {
        put(JSON_NAME_KEY, key.serverValue)
        value?.let { put(JSON_NAME_VALUE, value) } ?: put(JSON_NAME_VALUE, JSONObject.NULL)
        put(JSON_NAME_SET_TIME, setTime.time)
        put(JSON_NAME_IS_SYNCED, isSynced)
    }

    fun toBackendMap() =
        mapOf(
            BACKEND_NAME_VALUE to value,
            BACKEND_NAME_TIMESTAMP to setTime.timeInSeconds
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubscriberAttribute

        if (key != other.key) return false
        if (value != other.value) return false
        if (setTime != other.setTime) return false
        if (isSynced != other.isSynced) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + (value?.hashCode() ?: 0)
        result = 31 * result + setTime.hashCode()
        result = 31 * result + isSynced.hashCode()
        return result
    }
}

