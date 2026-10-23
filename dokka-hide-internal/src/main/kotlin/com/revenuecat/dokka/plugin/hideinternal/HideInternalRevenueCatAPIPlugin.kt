package com.revenuecat.dokka.plugin.hideinternal

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.transformers.documentables.SuppressedByConditionDocumentableFilterTransformer
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement

class HideInternalRevenueCatAPIPlugin : DokkaPlugin() {

    val filterExtension by extending {
        plugin<DokkaBase>().preMergeDocumentableTransformer providing ::HideInternalRevenueCatAPITransformer
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement() = PluginApiPreviewAcknowledgement
}

class HideInternalRevenueCatAPITransformer(
    context: DokkaContext,
) : SuppressedByConditionDocumentableFilterTransformer(context) {

    override fun shouldBeSuppressed(d: Documentable): Boolean {
        val annotations: List<Annotations.Annotation> =
            (d as? WithExtraProperties<*>)
                ?.extra
                ?.allOfType<Annotations>()
                ?.flatMap { it.directAnnotations.values.flatten() }
                ?: emptyList()

        return annotations.any { isHiddenAnnotation(it) }
    }

    private fun isHiddenAnnotation(annotation: Annotations.Annotation): Boolean =
        hiddenAnnotations.any { (packageName, className) ->
            annotation.dri.packageName == packageName && annotation.dri.classNames == className
        }

    private companion object {
        val hiddenAnnotations = listOf(
            "com.revenuecat.purchases" to "InternalRevenueCatAPI",
            "com.revenuecat.purchases.ui.revenuecatui" to "InviteOnlyCheckpointsAPI",
        )
    }
}
