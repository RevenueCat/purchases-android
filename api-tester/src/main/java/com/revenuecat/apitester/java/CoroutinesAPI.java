package com.revenuecat.apitester.java;

import com.revenuecat.purchases.CoroutinesExtensionsKt;
import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.Purchases;

import java.util.concurrent.CompletableFuture;

import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.future.FutureKt;

@SuppressWarnings({"unused"})
final class CoroutinesAPI {
    static void check(final Purchases purchases) {
        CompletableFuture<CustomerInfo> suspendResult = FutureKt.future(
                CoroutineScopeKt.CoroutineScope(EmptyCoroutineContext.INSTANCE),
                EmptyCoroutineContext.INSTANCE,
                CoroutineStart.DEFAULT,
                (scope, continuation) -> CoroutinesExtensionsKt.awaitCustomerInfo(purchases, continuation)
        );
    }
}
