package com.revenuecat.snapshots

import app.cash.paparazzi.HtmlReportWriter
import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.SnapshotHandler
import app.cash.paparazzi.SnapshotVerifier
import app.cash.paparazzi.detectMaxPercentDifferenceDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.util.Locale

class SentrySnapshotHandler(
    private val metadataProvider: (Snapshot) -> SentrySnapshotMetadata,
    private val snapshotTransform: (Snapshot) -> Snapshot = { it },
) : SnapshotHandler {
    private val delegate = if (System.getProperty("paparazzi.test.verify")?.toBoolean() == true) {
        SnapshotVerifier(detectMaxPercentDifferenceDefault())
    } else {
        HtmlReportWriter()
    }
    private val isRecording = System.getProperty("paparazzi.test.record")?.toBoolean() == true

    override fun newFrameHandler(
        snapshot: Snapshot,
        frameCount: Int,
        fps: Int,
    ): SnapshotHandler.FrameHandler {
        val transformedSnapshot = snapshotTransform(snapshot)
        if (isRecording) transformedSnapshot.writeSidecar(metadataProvider(transformedSnapshot))
        return delegate.newFrameHandler(transformedSnapshot, frameCount, fps)
    }

    override fun close() = delegate.close()
}

private fun Snapshot.writeSidecar(metadata: SentrySnapshotMetadata) {
    val json = buildJsonObject {
        put("display_name", metadata.displayName)
        put("group", metadata.group)
        metadata.diffThreshold?.let { put("diff_threshold", it) }
        metadata.canvasTheme?.let { put("canvas_theme", it.value) }
        if (metadata.tags.isNotEmpty()) {
            putJsonObject("tags") {
                metadata.tags.forEach { (key, value) -> put(key, value) }
            }
        }
        if (metadata.context.isNotEmpty()) put("context", metadata.context)
    }
    val sidecar = File(
        File(requireNotNull(System.getProperty("paparazzi.snapshot.dir")), "images"),
        toPaparazziFileName(extension = "json"),
    )
    sidecar.parentFile?.mkdirs()
    sidecar.writeText(SIDECAR_JSON.encodeToString(JsonObject.serializer(), json))
}

private fun Snapshot.toPaparazziFileName(extension: String): String {
    val formattedLabel = name?.let {
        "_${it.lowercase(Locale.US).replace("\\s".toRegex(), "_")}"
    }.orEmpty()
    return "${testName.packageName}_${testName.className}_${testName.methodName}$formattedLabel.$extension"
}

private val SIDECAR_JSON = Json { prettyPrint = true }
