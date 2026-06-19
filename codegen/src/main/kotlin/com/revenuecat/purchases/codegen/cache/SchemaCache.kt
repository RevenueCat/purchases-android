package com.revenuecat.purchases.codegen.cache

import com.revenuecat.purchases.codegen.api.EntitlementSchema
import com.revenuecat.purchases.codegen.api.OfferingSchema
import com.revenuecat.purchases.codegen.api.PackageSchema
import com.revenuecat.purchases.codegen.api.ProjectSchema
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

internal class SchemaCache(private val cacheDir: File) {

    private companion object {
        private const val MS_PER_MINUTE = 60_000L
    }

    private val cacheFile: File
        get() = File(cacheDir, "revenuecat-schema.json")

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    internal fun isValid(ttlMinutes: Long): Boolean {
        if (!cacheFile.exists()) return false
        return try {
            val json = JSONObject(cacheFile.readText())
            val timestamp = json.getLong("timestamp")
            val ageMinutes = (System.currentTimeMillis() - timestamp) / MS_PER_MINUTE
            ageMinutes < ttlMinutes
        } catch (e: Exception) {
            // Corrupted or unreadable cache — treat as invalid so a fresh fetch is triggered
            false
        }
    }

    /** Returns the age of the cached data in minutes, or null if the cache doesn't exist or is unreadable. */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    internal fun cacheAgeMinutes(): Long? {
        if (!cacheFile.exists()) return null
        return try {
            val timestamp = JSONObject(cacheFile.readText()).getLong("timestamp")
            (System.currentTimeMillis() - timestamp) / MS_PER_MINUTE
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    internal fun read(): ProjectSchema? {
        if (!cacheFile.exists()) return null
        return try {
            val json = JSONObject(cacheFile.readText())
            val data = json.getJSONObject("data")
            ProjectSchema(
                entitlements = parseEntitlements(data.getJSONArray("entitlements")),
                offerings = parseOfferings(data.getJSONArray("offerings")),
            )
        } catch (e: Exception) {
            null
        }
    }

    internal fun write(schema: ProjectSchema) {
        cacheDir.mkdirs()
        val data = JSONObject().apply {
            put("entitlements", serializeEntitlements(schema.entitlements))
            put("offerings", serializeOfferings(schema.offerings))
        }
        val wrapper = JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("data", data)
        }
        cacheFile.writeText(wrapper.toString(2))
    }

    private fun parseEntitlements(arr: JSONArray): List<EntitlementSchema> {
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            EntitlementSchema(
                id = obj.getString("id"),
                lookupKey = obj.getString("lookupKey"),
                displayName = obj.getString("displayName"),
            )
        }
    }

    private fun parseOfferings(arr: JSONArray): List<OfferingSchema> {
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            OfferingSchema(
                id = obj.getString("id"),
                lookupKey = obj.getString("lookupKey"),
                displayName = obj.getString("displayName"),
                isCurrent = obj.getBoolean("isCurrent"),
                packages = parsePackages(obj.getJSONArray("packages")),
            )
        }
    }

    private fun parsePackages(arr: JSONArray): List<PackageSchema> {
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            PackageSchema(
                id = obj.getString("id"),
                lookupKey = obj.getString("lookupKey"),
                displayName = obj.getString("displayName"),
            )
        }
    }

    private fun serializeEntitlements(list: List<EntitlementSchema>): JSONArray {
        return JSONArray().apply {
            list.forEach { e ->
                put(
                    JSONObject().apply {
                        put("id", e.id)
                        put("lookupKey", e.lookupKey)
                        put("displayName", e.displayName)
                    },
                )
            }
        }
    }

    private fun serializeOfferings(list: List<OfferingSchema>): JSONArray {
        return JSONArray().apply {
            list.forEach { o ->
                put(
                    JSONObject().apply {
                        put("id", o.id)
                        put("lookupKey", o.lookupKey)
                        put("displayName", o.displayName)
                        put("isCurrent", o.isCurrent)
                        put("packages", serializePackages(o.packages))
                    },
                )
            }
        }
    }

    private fun serializePackages(list: List<PackageSchema>): JSONArray {
        return JSONArray().apply {
            list.forEach { p ->
                put(
                    JSONObject().apply {
                        put("id", p.id)
                        put("lookupKey", p.lookupKey)
                        put("displayName", p.displayName)
                    },
                )
            }
        }
    }
}
