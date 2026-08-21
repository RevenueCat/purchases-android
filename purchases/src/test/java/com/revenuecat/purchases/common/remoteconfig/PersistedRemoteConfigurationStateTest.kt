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
    fun `on a topic collision a shallower domain wins over a deeper one`() {
        val state = tree(
            "app" to domain(subdomains = listOf("child", "uncle")),
            "child" to domain(subdomains = listOf("grandchild")),
            "grandchild" to domain(topics = mapOf("workflows" to topic("fromGrandchild"))),
            "uncle" to domain(topics = mapOf("workflows" to topic("fromUncle"))),
        )

        assertThat(state.domainsInPrecedenceOrder).containsExactly("app", "child", "uncle", "grandchild")
        assertThat(state.mergedTopics.getValue("workflows")).containsOnlyKeys("fromUncle")
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
    fun `cycles and re-listed domains terminate and contribute once`() {
        val state = tree(
            "app" to domain(subdomains = listOf("child", "child", "app")),
            "child" to domain(subdomains = listOf("app"), topics = mapOf("workflows" to topic("wf1"))),
        )

        assertThat(state.domainsInPrecedenceOrder).containsExactly("app", "child")
        assertThat(state.mergedTopics).containsOnlyKeys("workflows")
    }

    @Test
    fun `domains deeper than the depth cap are excluded`() {
        // Root at depth 0; d1..d3 within the cap, d4 beyond it.
        val state = tree(
            "app" to domain(subdomains = listOf("d1")),
            "d1" to domain(subdomains = listOf("d2")),
            "d2" to domain(subdomains = listOf("d3")),
            "d3" to domain(subdomains = listOf("d4"), topics = mapOf("inCap" to topic("a"))),
            "d4" to domain(topics = mapOf("beyondCap" to topic("b"))),
        )

        assertThat(state.domainsInPrecedenceOrder).containsExactly("app", "d1", "d2", "d3")
        assertThat(state.mergedTopics).containsOnlyKeys("inCap")
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
