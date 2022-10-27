package com.revenuecat.apitester.java;

import com.revenuecat.purchases.models.Transaction;

import java.util.Date;

@SuppressWarnings({"unused"})
final class TransactionAPI {
    static void check(final Transaction transaction) {
        final String transactionIdentifier = transaction.getTransactionIdentifier();
        final String revenuecatId = transaction.getRevenuecatId();
        final String productIdentifier = transaction.getProductIdentifier();
        final String productId = transaction.getProductId();
        final Date purchaseDate = transaction.getPurchaseDate();
    }
}
