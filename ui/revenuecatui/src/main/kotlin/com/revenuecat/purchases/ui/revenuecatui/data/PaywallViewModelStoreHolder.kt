package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

internal class PaywallViewModelStoreHolder : ViewModel() {
    private val entries = mutableMapOf<Any, Entry>()

    fun acquire(key: Any): Lease {
        val entry = entries.getOrPut(key) { Entry() }
        entry.referenceCount++
        return Lease(
            owner = entry.owner,
            release = { clear -> release(key, entry, clear) },
        )
    }

    private fun release(key: Any, entry: Entry, clear: Boolean) {
        if (clear) entry.pendingClear = true
        entry.referenceCount--
        clearIfUnused(key, entry)
    }

    private fun clearIfUnused(key: Any, entry: Entry) {
        if (!entry.pendingClear || entry.referenceCount > 0) return
        if (!entries.remove(key, entry)) return

        entry.store.clear()
    }

    override fun onCleared() {
        entries.toMap().forEach { (key, entry) ->
            entry.pendingClear = true
            clearIfUnused(key, entry)
        }
    }

    private class Entry(
        val store: ViewModelStore = ViewModelStore(),
        var referenceCount: Int = 0,
        var pendingClear: Boolean = false,
    ) {
        val owner = object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = store
        }
    }

    class Lease internal constructor(
        val owner: ViewModelStoreOwner,
        private val release: (Boolean) -> Unit,
    ) {
        private var isReleased = false

        fun release(clear: Boolean) {
            if (isReleased) return
            isReleased = true
            release.invoke(clear)
        }
    }
}
