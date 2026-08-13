package com.revenuecat.checkpointtester

object Constants {
    // Set CHECKPOINT_TESTER_API_KEY in local.properties to override. Defaults to PAYWALL_TESTER_API_KEY_A,
    // since this app shares the paywall tester's applicationId and store project.
    val API_KEY: String = BuildConfig.CHECKPOINT_TESTER_API_KEY.ifEmpty { "API_KEY" }
}
