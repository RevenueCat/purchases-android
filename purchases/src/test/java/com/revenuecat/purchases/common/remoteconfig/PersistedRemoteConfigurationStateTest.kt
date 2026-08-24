package com.revenuecat.purchases.common.remoteconfig

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PersistedRemoteConfigurationStateTest {

    @Test
    fun `merged topics union every reachable domain's topics`() {
        val state = tree(
            "app" to domain(subdomains = listOf("child"), topics = mapOf("sources" to topic("api"))),
            "child" to domain(topics = mapOf("workflows" to topic("wf1"))),
        )

        assertThat(state.mergedTopics).containsOnlyKeys("sources", "workflows")
    }

    @Test
    fun `on a topic collision the parent wins over the child`() {
        val state = tree(
            "app" to domain(subdomains = listOf("child"), topics = mapOf("workflows" to topic("fromParent"))),
            "child" to domain(topics = mapOf("workflows" to topic("fromChild"))),
        )

        assertThat(state.mergedTopics.getValue("workflows")).containsOnlyKeys("fromParent")
    }

    @Test
    fun `on a topic collision between siblings the earlier-declared sibling wins`() {
        val state = tree(
            "app" to domain(subdomains = listOf("first", "second")),
            "first" to domain(topics = mapOf("workflows" to topic("fromFirst"))),
            "second" to domain(topics = mapOf("workflows" to topic("fromSecond"))),
        )

        assertThat(state.mergedTopics.getValue("workflows")).containsOnlyKeys("fromFirst")
    }

    @Test
    fun `a subdomain's own subdomains never extend the merged view`() {
        val state = tree(
            "app" to domain(subdomains = listOf("child")),
            "child" to domain(subdomains = listOf("grandchild"), topics = mapOf("workflows" to topic("fromChild"))),
            "grandchild" to domain(topics = mapOf("beyondOneLevel" to topic("x"))),
        )

        assertThat(state.domainsInPrecedenceOrder).containsExactly("app", "child")
        assertThat(state.mergedTopics).containsOnlyKeys("workflows")
    }

    @Test
    fun `a domain entry not reachable from the root is excluded from the merged view`() {
        val state = tree(
            "app" to domain(topics = mapOf("sources" to topic("api"))),
            "orphan" to domain(topics = mapOf("workflows" to topic("wf1"))),
        )

        assertThat(state.domainsInPrecedenceOrder).containsExactly("app")
        assertThat(state.mergedTopics).containsOnlyKeys("sources")
    }

    @Test
    fun `a subdomain listed without a persisted entry contributes nothing`() {
        val state = tree(
            "app" to domain(subdomains = listOf("notSyncedYet"), topics = mapOf("sources" to topic("api"))),
        )

        assertThat(state.domainsInPrecedenceOrder).containsExactly("app")
        assertThat(state.mergedTopics).containsOnlyKeys("sources")
    }

    @Test
    fun `duplicates and a self-reference in the root's list contribute once`() {
        val state = tree(
            "app" to domain(subdomains = listOf("child", "child", "app")),
            "child" to domain(topics = mapOf("workflows" to topic("wf1"))),
        )

        assertThat(state.domainsInPrecedenceOrder).containsExactly("app", "child")
        assertThat(state.mergedTopics).containsOnlyKeys("workflows")
    }

    @Test
    fun `live blob refs span every persisted domain including unreachable entries`() {
        val state = tree(
            "app" to domain(
                prefetchBlobs = listOf("rootPrefetch"),
                topics = mapOf("sources" to ConfigTopic(mapOf("api" to item(blobRef = "rootTopicBlob")))),
            ),
            "orphan" to domain(
                prefetchBlobs = listOf("orphanPrefetch"),
                topics = mapOf("workflows" to ConfigTopic(mapOf("wf1" to item(blobRef = "orphanTopicBlob")))),
            ),
        )

        assertThat(state.liveBlobRefs)
            .containsExactlyInAnyOrder("rootPrefetch", "rootTopicBlob", "orphanPrefetch", "orphanTopicBlob")
    }

    @Test
    fun `rootState is null when the root domain has no persisted entry yet`() {
        val state = PersistedRemoteConfigurationState(
            rootDomain = "app",
            domains = mapOf("child" to domain()),
        )

        assertThat(state.rootState).isNull()
        assertThat(state.domainsInPrecedenceOrder).isEmpty()
        assertThat(state.mergedTopics).isEmpty()
    }

    private fun tree(vararg domains: Pair<String, PersistedDomainState>) =
        PersistedRemoteConfigurationState(rootDomain = "app", domains = domains.toMap())

    private fun domain(
        subdomains: List<String> = emptyList(),
        prefetchBlobs: List<String> = emptyList(),
        topics: Map<String, ConfigTopic> = emptyMap(),
    ) = PersistedDomainState(
        manifest = "v1.0.",
        subdomains = subdomains,
        prefetchBlobs = prefetchBlobs,
        topics = topics,
    )

    private fun topic(itemKey: String) = ConfigTopic(mapOf(itemKey to item()))

    private fun item(blobRef: String? = null) = RemoteConfiguration.ConfigItem(blobRef = blobRef)
}
