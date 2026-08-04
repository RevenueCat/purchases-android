package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test

class CheckpointCallStoreTest {

    @After
    fun tearDown() {
        CheckpointCallStore.clear()
    }

    @Test
    fun `get retrieves the stored entry without removing it`() {
        val entry = CheckpointCallStore.Entry(delegate = mockk(), presentation = mockk())
        CheckpointCallStore.store("call-id", entry)

        assertThat(CheckpointCallStore.get("call-id")).isSameAs(entry)
        assertThat(CheckpointCallStore.get("call-id")).isSameAs(entry)
    }

    @Test
    fun `get returns null for an unknown callId`() {
        assertThat(CheckpointCallStore.get("unknown-call-id")).isNull()
    }

    @Test
    fun `remove returns the entry and deletes it`() {
        val entry = CheckpointCallStore.Entry(delegate = mockk(), presentation = mockk())
        CheckpointCallStore.store("call-id", entry)

        assertThat(CheckpointCallStore.remove("call-id")).isSameAs(entry)
        assertThat(CheckpointCallStore.get("call-id")).isNull()
    }

    @Test
    fun `remove returns null for an unknown callId`() {
        assertThat(CheckpointCallStore.remove("unknown-call-id")).isNull()
    }

    @Test
    fun `remove does not affect other entries`() {
        val entry = CheckpointCallStore.Entry(delegate = mockk(), presentation = mockk())
        val otherEntry = CheckpointCallStore.Entry(delegate = mockk(), presentation = mockk())
        CheckpointCallStore.store("call-id", entry)
        CheckpointCallStore.store("other-call-id", otherEntry)

        CheckpointCallStore.remove("call-id")

        assertThat(CheckpointCallStore.get("other-call-id")).isSameAs(otherEntry)
    }
}
