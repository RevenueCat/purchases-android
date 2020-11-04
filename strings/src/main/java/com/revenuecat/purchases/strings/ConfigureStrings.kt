package com.revenuecat.purchases.strings

object ConfigureStrings {
    const val APP_BACKGROUNDED = Emojis.INFO + " App backgrounded"
    const val APP_FOREGROUNDED = Emojis.INFO + " App foregrounded"
    const val DEBUG_ENABLE = Emojis.INFO + " Debug logging enabled"
    const val INVALID_CREDENTIALS_ERROR = Emojis.ROBOT + Emojis.ERROR + " There was a credentials issue. Check the underlying error for more details. More info here: http://errors.rev.cat/play-service-credentials" //
    const val LISTENER_SET = Emojis.INFO + " Listener set"
    const val NO_SHARED_SINGLETON = Emojis.WARNING + " There is no singleton instance. Make sure you configure Purchases before trying to get the default instance. More info here: https://errors.rev.cat/configuring-sdk" //
    const val INITIAL_APP_USER_ID = Emojis.USER + " Initial App User ID - %s"
    const val SDK_VERSION = Emojis.INFO + " SDK Version - %s"
}
