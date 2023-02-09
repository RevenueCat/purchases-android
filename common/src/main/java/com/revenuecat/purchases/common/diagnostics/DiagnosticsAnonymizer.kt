package com.revenuecat.purchases.common.diagnostics

import com.revenuecat.purchases.common.Anonymizer

class DiagnosticsAnonymizer(
    private val anonymizer: Anonymizer
) {
    fun anonymizeEventIfNeeded(diagnosticsEvent: DiagnosticsEvent): DiagnosticsEvent {
        return when (diagnosticsEvent) {
            is DiagnosticsEvent.Log -> anonymizeLog(diagnosticsEvent)
            is DiagnosticsEvent.Exception -> anonymizeException(diagnosticsEvent)
            is DiagnosticsEvent.Metric -> diagnosticsEvent
        }
    }

    private fun anonymizeLog(diagnosticsLog: DiagnosticsEvent.Log): DiagnosticsEvent {
        return diagnosticsLog.copy(
            properties = anonymizeMap(diagnosticsLog.properties)
        )
    }

    private fun anonymizeException(diagnosticsException: DiagnosticsEvent.Exception): DiagnosticsEvent {
        return diagnosticsException.copy(
            message = anonymizeString(diagnosticsException.message)
        )
    }

    private fun anonymizeMap(mapToAnonymize: Map<String, Any>): Map<String, Any> {
        return mapToAnonymize.mapValues { (_, value) ->
            if (value is String) anonymizeString(value)
            else value
        }
    }

    private fun anonymizeString(textToAnonymize: String): String {
        return anonymizer.anonymizeString(textToAnonymize)
    }
}
