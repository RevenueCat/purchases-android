package com.revenuecat.checkpointssample

object Constants {
    // Set CHECKPOINTS_SAMPLE_API_KEY in local.properties to override. Defaults to the checkpoint tester's
    // key and then the paywall tester's, since the three apps share the same applicationId and store project.
    val API_KEY: String = BuildConfig.CHECKPOINTS_SAMPLE_API_KEY.ifEmpty { "API_KEY" }

    const val ONBOARDING_CHECKPOINT_ID = "onboarding_complete"
    const val PLAY_GAME_CHECKPOINT_ID = "play_game"
}
