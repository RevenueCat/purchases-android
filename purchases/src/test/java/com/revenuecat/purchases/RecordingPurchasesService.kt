package com.revenuecat.purchases

/**
 * Test [PurchasesService] declared in `src/test/resources/META-INF/services` so [PurchasesServices.default]
 * can discover it through [java.util.ServiceLoader]. ServiceLoader instantiates its own copy, so calls are
 * recorded in the companion object.
 */
@OptIn(InternalRevenueCatAPI::class)
internal class RecordingPurchasesService : PurchasesService {
    override fun initialize(purchases: Purchases) {
        initialized += purchases
        if (failOnInitialize) error("RecordingPurchasesService failing on initialize")
    }

    override fun close(purchases: Purchases) {
        closed += purchases
        if (failOnClose) error("RecordingPurchasesService failing on close")
    }

    companion object {
        val initialized = mutableListOf<Purchases>()
        val closed = mutableListOf<Purchases>()

        // When set, initialize/close record the call and then throw, to exercise the dispatcher's isolation.
        var failOnInitialize = false
        var failOnClose = false

        fun reset() {
            initialized.clear()
            closed.clear()
            failOnInitialize = false
            failOnClose = false
        }
    }
}
