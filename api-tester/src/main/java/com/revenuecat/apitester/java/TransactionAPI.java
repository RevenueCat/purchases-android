package com.revenuecat.apitester.java;

import com.revenuecat.purchases.models.Transaction;
import com.revenuecat.purchases.models.Price;
import com.revenuecat.purchases.Store;

import java.util.Date;

@SuppressWarnings({"unused"})
final class TransactionAPI {
    static void check(final Transaction transaction) {
        final String transactionIdentifier = transaction.getTransactionIdentifier();
        final String revenuecatId = transaction.getRevenuecatId();
        final String productIdentifier = transaction.getProductIdentifier();
        final String productId = transaction.getProductId();
        final Date purchaseDate = transaction.getPurchaseDate();
        final String storeTransactionId = transaction.getStoreTransactionId();
        final Store store = transaction.getStore();
        final String displayName = transaction.getDisplayName();
        final boolean isSandbox = transaction.isSandbox();
        final Date originalPurchaseDate = transaction.getOriginalPurchaseDate();
        final Price price = transaction.getPrice();

        @SuppressWarnings("deprecation") final Transaction deprecatedTransaction = new Transaction(
                "id",
                "id",
                "product",
                "product",
                new Date(),
                "store_id",
                Store.PLAY_STORE
        );

        final Transaction newTransaction = new Transaction(
                "id",
                "id",
                "product",
                "product",
                new Date(),
                "store_id",
                Store.PLAY_STORE,
                "Display Name",
                false,
                new Date(),
                new Price("", 0, "USD")
        );
    }
}
