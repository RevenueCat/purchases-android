package com.revenuecat.purchases.interfaces;

@FunctionalInterface
public interface IsSupportedListener {

    void onReceived(boolean isSupported);

}
