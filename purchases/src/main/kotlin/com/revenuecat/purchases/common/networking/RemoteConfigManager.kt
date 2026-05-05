package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.errorLog

internal class RemoteConfigManager(
    private val backend: Backend,
    private val topicFetcher: TopicFetcher,
) {
    fun updateRemoteConfigIfNeeded(
        appUserID: String,
        appInBackground: Boolean,
        completion: ((PurchasesError?) -> Unit)? = null,
    ) {
        backend.getRemoteConfig(
            appUserID = appUserID,
            appInBackground = appInBackground,
            onSuccess = { response -> fetchTopicsForResponse(response, completion) },
            onError = { error ->
                errorLog { "Failed to fetch remote config: $error" }
                completion?.invoke(error)
            },
        )
    }

    private fun fetchTopicsForResponse(
        response: RemoteConfigResponse,
        completion: ((PurchasesError?) -> Unit)?,
    ) {
        val assetSource = response.assetSources.firstOrNull()
        val tasks = response.manifest.topics.mapNotNull { (topic, variants) ->
            val entry = variants[DEFAULT_VARIANT] ?: return@mapNotNull null
            TopicTask(topic, DEFAULT_VARIANT, entry)
        }
        if (assetSource == null || tasks.isEmpty()) {
            completion?.invoke(null)
            return
        }
        val tracker = CompletionTracker(tasks.size, completion)
        tasks.forEach { task ->
            topicFetcher.fetchTopicIfNeeded(
                topic = task.topic,
                variant = task.variant,
                topicEntry = task.entry,
                assetSource = assetSource,
            ) { error -> tracker.recordCompletion(error) }
        }
    }

    private data class TopicTask(val topic: Topic, val variant: String, val entry: TopicEntry)

    private class CompletionTracker(
        totalTasks: Int,
        private val completion: ((PurchasesError?) -> Unit)?,
    ) {
        private val lock = Any()
        private var remaining = totalTasks
        private var firstError: PurchasesError? = null

        fun recordCompletion(error: PurchasesError?) {
            val shouldInvoke: Boolean
            val errorToReport: PurchasesError?
            synchronized(lock) {
                if (error != null && firstError == null) {
                    firstError = error
                }
                remaining -= 1
                shouldInvoke = remaining == 0
                errorToReport = firstError
            }
            if (shouldInvoke) {
                completion?.invoke(errorToReport)
            }
        }
    }

    private companion object {
        const val DEFAULT_VARIANT = "DEFAULT"
    }
}
