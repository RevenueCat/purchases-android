@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.audiences

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.common.remoteconfig.readConsistent
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

internal class AudiencesConfigProvider(
    private val manager: RemoteConfigManager,
) {
    /**
     * The audience dictionary from the topic's static `default` blob, or `null` when the topic, that item, or its
     * blob is unavailable. Read under one config generation: [readConsistent] re-reads once if a commit races the
     * read, and gives up with `null` if that read is superseded too.
     */
    suspend fun getAudiences(): Map<String, Audience>? =
        manager.readConsistent(what = { "the audiences topic" }) { _ ->
            manager.blobData(RemoteConfigTopic.Audiences, ITEM_DEFAULT, ::parseAudiences)
        }

    /**
     * An audience that does not parse is dropped rather than failing the whole blob, so one audience on a shape
     * this SDK version does not understand cannot make every other audience unreadable. A rule that references
     * the dropped audience still fails to resolve, which is the honest answer for that rule alone.
     */
    @Suppress("ReturnCount")
    private fun parseAudiences(bytes: ByteArray): Map<String, Audience>? {
        val entries = try {
            JsonTools.json.parseToJsonElement(bytes.decodeToString()).jsonObject
        } catch (e: SerializationException) {
            errorLog(e) { "Failed to parse the audiences blob as JSON." }
            return null
        } catch (e: IllegalArgumentException) {
            errorLog(e) { "The audiences blob is not a JSON object." }
            return null
        }
        return entries.mapNotNull { (identifier, element) ->
            try {
                identifier to JsonTools.json.decodeFromJsonElement<Audience>(element)
            } catch (e: SerializationException) {
                errorLog(e) { "Ignoring audience '$identifier' in the audiences blob: it could not be parsed." }
                null
            }
        }.toMap()
    }

    private companion object {
        const val ITEM_DEFAULT = "default"
    }
}
