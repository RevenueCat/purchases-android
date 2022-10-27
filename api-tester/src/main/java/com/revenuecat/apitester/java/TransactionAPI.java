package com.revenuecat.apitester.java;

import com.revenuecat.purchases.models.Transaction;

import java.util.Date;

@SuppressWarnings({"unused"})
final class TransactionAPI {
    static void check(final Transaction transaction) {
        final String transactionIdentifier = transaction.getTransactionIdentifier();
        final String productIdentifier = transaction.getProductIdentifier();
        final Date purchaseDate = transaction.getPurchaseDate();
    }
}
