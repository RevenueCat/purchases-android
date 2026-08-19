package com.revenuecat.purchases.ui.revenuecatui.checkpoints

internal object CheckpointIdentifierValidator {

    const val MAX_LENGTH = 255

    fun isValid(identifier: String): Boolean {
        if (identifier.length !in 1..MAX_LENGTH || !identifier.first().isAsciiLetter()) {
            return false
        }

        return identifier.drop(1).all { it.isAllowedCharacter() }
    }

    fun invalidIdentifierLogMessage(identifier: String): String =
        "Dropping invalid checkpoint identifier '$identifier'. Identifiers must start with a letter, " +
            "contain only ASCII letters, numbers, underscores, and hyphens, and be no more than " +
            "$MAX_LENGTH characters."

    private fun Char.isAllowedCharacter(): Boolean =
        isAsciiLetter() || this in '0'..'9' || this == '_' || this == '-'

    private fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'
}
