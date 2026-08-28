@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.audiences

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.localrules.RulesDimensionValue
import com.revenuecat.purchases.common.localrules.asRulesDimensionValue
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.common.remoteconfig.readConsistent
import com.revenuecat.purchases.common.warnLog
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * The audiences topic as of one read: the audience dictionary from the static `default` blob together with the
 * `backend_predicate_results` the backend committed alongside it. Rules in [audiences] read
 * [backendPredicateResults] under `backend.*`, so consumers must evaluate the two together rather than
 * re-reading either on its own.
 */
internal data class AudiencesSnapshot(
    val audiences: Map<String, Audience>,
    val backendPredicateResults: Map<String, RulesDimensionValue>,
)

internal class AudiencesConfigProvider(
    private val manager: RemoteConfigManager,
) {
    /**
     * The current audiences topic, or `null` when the topic, its `default` item, or that item's blob is
     * unavailable. Both parts are read under one config generation: [readConsistent] re-reads them once if a
     * commit races the read, and gives up with `null` if that read is superseded too.
     */
    suspend fun getSnapshot(): AudiencesSnapshot? =
        manager.readConsistent(what = { "the audiences topic" }) { _ ->
            val audiences = manager.blobData(RemoteConfigTopic.Audiences, ITEM_DEFAULT, ::parseAudiences)
                ?: return@readConsistent null
            AudiencesSnapshot(
                audiences = audiences,
                backendPredicateResults = backendPredicateResults(),
            )
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

    private suspend fun backendPredicateResults(): Map<String, RulesDimensionValue> {
        val metadata = manager.topic(RemoteConfigTopic.Audiences)
            ?.get(ITEM_BACKEND_PREDICATE_RESULTS)
            ?.metadata
            ?: return emptyMap()
        return metadata.mapNotNull { (hash, element) ->
            val result = element.asRulesDimensionValue()
            if (result == null) {
                // Rules read these with a default (`{"var": ["backend.<hash>", false]}`), so a value shape this
                // SDK version cannot represent degrades to the rule's default instead of failing the item.
                warnLog { "Ignoring backend predicate result '$hash': its value can't be read by a rule." }
                null
            } else {
                hash to result
            }
        }.toMap()
    }

    private companion object {
        const val ITEM_DEFAULT = "default"
        const val ITEM_BACKEND_PREDICATE_RESULTS = "backend_predicate_results"
    }
}
