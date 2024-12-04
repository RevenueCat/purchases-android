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

        return annotations.any { isInternalAnnotation(it) }
    }

    private fun isInternalAnnotation(annotation: Annotations.Annotation): Boolean =
        annotation.dri.packageName == "com.revenuecat.purchases" && annotation.dri.classNames == "InternalRevenueCatAPI"
}
