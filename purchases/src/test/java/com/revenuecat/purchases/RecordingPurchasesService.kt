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
    }

    override fun close(purchases: Purchases) {
        closed += purchases
    }

    companion object {
        val initialized = mutableListOf<Purchases>()
        val closed = mutableListOf<Purchases>()

        fun reset() {
            initialized.clear()
            closed.clear()
        }
    }
}
