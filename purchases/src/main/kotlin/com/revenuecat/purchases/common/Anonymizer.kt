package com.revenuecat.purchases.common

internal class Anonymizer {
    private companion object {
        const val EMAIL_REGEX = "[a-zA-Z0-9_!#\$%&'*+/=?`{|}~^.]+@[a-zA-Z0-9]+\\.[a-zA-Z]+" // Based on RFC5322
        const val UUID_REGEX = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"
        const val IP_REGEX = "((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}"

        const val REDACTED = "*****"
    }

    private val anonymizeRegex = Regex("$EMAIL_REGEX|$UUID_REGEX|$IP_REGEX")

    fun anonymizedString(textToAnonymize: String): String {
        return textToAnonymize.replace(anonymizeRegex, REDACTED)
    }

    fun anonymizedMap(mapToAnonymize: Map<String, Any>): Map<String, Any> {
        return mapToAnonymize.mapValues { (_, value) -> anonymizedAny(value) }
    }

    fun anonymizedStringMap(mapToAnonymize: Map<String, String>): Map<String, String> {
        return mapToAnonymize.mapValues { (_, value) -> anonymizedString(value) }
    }

    private fun anonymizedAny(valueToAnonymize: Any): Any {
        return when (valueToAnonymize) {
            is String -> anonymizedString(valueToAnonymize)
            is List<*> -> valueToAnonymize.map { if (it == null) null else anonymizedAny(it) }
            is Map<*, *> -> valueToAnonymize.mapValues { (_, value) ->
                if (value == null) null else anonymizedAny(value)
            }
            else -> valueToAnonymize
        }
    }
}
