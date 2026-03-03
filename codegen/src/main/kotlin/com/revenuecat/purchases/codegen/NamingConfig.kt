package com.revenuecat.purchases.codegen

internal object NamingConfig {

    private val KOTLIN_RESERVED = setOf(
        "as", "break", "class", "continue", "do", "else", "false", "for",
        "fun", "if", "in", "interface", "is", "null", "object", "package",
        "return", "super", "this", "throw", "true", "try", "typealias",
        "typeof", "val", "var", "when", "while"
    )

    internal fun toIdentifier(raw: String, style: NamingStyle): String {
        val stripped = stripPrefix(raw)
        val converted = when (style) {
            NamingStyle.CAMEL_CASE -> snakeToCamel(stripped)
            NamingStyle.SNAKE_CASE -> camelToSnake(stripped)
            NamingStyle.AS_IS -> stripped
        }
        val sanitized = sanitize(converted)
        return if (sanitized in KOTLIN_RESERVED) "`$sanitized`" else sanitized
    }

    internal fun toConstant(raw: String): String {
        val stripped = stripPrefix(raw)
        val upper = stripped.replace(Regex("[^a-zA-Z0-9]"), "_").uppercase()
        val sanitized = if (upper.isEmpty() || upper.first().isDigit()) "_$upper" else upper
        return if (sanitized in KOTLIN_RESERVED) "`$sanitized`" else sanitized
    }

    private fun stripPrefix(input: String): String {
        // Strip RevenueCat's standard "$rc_" prefix from package identifiers
        // and any leading non-alphanumeric characters
        val withoutRcPrefix = if (input.startsWith("\$rc_")) {
            input.removePrefix("\$rc_")
        } else {
            input
        }
        return withoutRcPrefix.trimStart { !it.isLetterOrDigit() && it != '_' }
    }

    private fun snakeToCamel(input: String): String {
        val parts = input.split(Regex("[_\\-\\s]+"))
        if (parts.isEmpty()) return input
        return parts.first().lowercase() + parts.drop(1).joinToString("") { part ->
            part.replaceFirstChar { it.uppercase() }
        }
    }

    private fun camelToSnake(input: String): String {
        return input.replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .replace(Regex("[\\-\\s]+"), "_")
            .lowercase()
    }

    private fun sanitize(input: String): String {
        val cleaned = input.replace(Regex("[^a-zA-Z0-9_]"), "_")
        return if (cleaned.isEmpty() || cleaned.first().isDigit()) "_$cleaned" else cleaned
    }
}
