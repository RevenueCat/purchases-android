@file:Suppress("ForbiddenPublicDataClass", "ForbiddenPublicEnum")

package com.revenuecat.snapshots

import kotlinx.serialization.json.JsonObject

data class SentrySnapshotMetadata(
    val displayName: String,
    val group: String,
    val diffThreshold: Float? = null,
    val tags: Map<String, String> = emptyMap(),
    val canvasTheme: CanvasTheme? = null,
    val context: JsonObject = JsonObject(emptyMap()),
) {
    init {
        require(displayName.isNotBlank()) { "Snapshot display name cannot be blank" }
        require(group.isNotBlank()) { "Snapshot group cannot be blank" }
        require(diffThreshold == null || diffThreshold in 0f..1f) {
            "Snapshot diff threshold must be between 0 and 1"
        }
    }

    enum class CanvasTheme(val value: String) {
        LIGHT("light"),
        DARK("dark"),
    }
}
