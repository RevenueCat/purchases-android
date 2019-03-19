package com.revenuecat.purchases.interfaces;

@FunctionalInterface
public interface Callback<T> {

    void onReceived(T result);

}
