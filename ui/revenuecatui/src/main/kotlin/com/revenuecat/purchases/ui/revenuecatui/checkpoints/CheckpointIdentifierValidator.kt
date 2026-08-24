package com.revenuecat.purchases.ui.revenuecatui.checkpoints

internal object CheckpointIdentifierValidator {

    fun isValid(identifier: String): Boolean {
        if (identifier.length > MAX_IDENTIFIER_LENGTH || identifier.firstOrNull()?.isAsciiLetter() != true) {
            return false
        }

        return identifier.drop(1).all { it.isAllowedCharacter() }
    }

    fun invalidIdentifierLogMessage(identifier: String): String =
        "Dropping invalid checkpoint identifier '$identifier'. Identifiers must start with a letter, " +
            "contain only ASCII letters, numbers, underscores, and hyphens, and be no more than " +
            "$MAX_IDENTIFIER_LENGTH characters."

    private fun Char.isAllowedCharacter(): Boolean =
        isAsciiLetter() || this in '0'..'9' || this == '_' || this == '-'

    private fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

    private const val MAX_IDENTIFIER_LENGTH = 255
}
