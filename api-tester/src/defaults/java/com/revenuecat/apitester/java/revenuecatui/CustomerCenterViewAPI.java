package com.revenuecat.apitester.java.revenuecatui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.ui.platform.AbstractComposeView;

import com.revenuecat.purchases.customercenter.CustomerCenterListener;
import com.revenuecat.purchases.ui.revenuecatui.views.CustomerCenterView;

@SuppressWarnings({"unused"})
final class CustomerCenterViewAPI {

    static void checkType(@NonNull Context context) {
        AbstractComposeView view = new CustomerCenterView(context);
    }

    static void checkConstructors(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr,
            @NonNull Runnable dismissHandler,
            @NonNull CustomerCenterListener listener
    ) {
        new CustomerCenterView(context);
        new CustomerCenterView(context, attrs);
        new CustomerCenterView(context, attrs, defStyleAttr);
        new CustomerCenterView(context, dismissHandler::run);
        new CustomerCenterView(context, listener);
        new CustomerCenterView(context, listener, dismissHandler::run);
    }

    static void checkMethods(
            @NonNull CustomerCenterView view,
            @NonNull Runnable dismissHandler,
            @NonNull CustomerCenterListener listener
    ) {
        view.setDismissHandler(null);
        view.setDismissHandler(dismissHandler::run);
        view.setCustomerCenterListener(null);
        view.setCustomerCenterListener(listener);
    }
}
