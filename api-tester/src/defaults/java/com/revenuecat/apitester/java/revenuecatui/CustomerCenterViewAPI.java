package com.revenuecat.apitester.java.revenuecatui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.ui.platform.AbstractComposeView;

import com.revenuecat.purchases.customercenter.CustomerCenterListener;
import com.revenuecat.purchases.ui.revenuecatui.views.CustomerCenterView;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

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
        Function0<Unit> dismissHandlerFunction = new Function0<Unit>() {
            @Override
            public Unit invoke() {
                dismissHandler.run();
                return Unit.INSTANCE;
            }
        };

        new CustomerCenterView(context);
        new CustomerCenterView(context, attrs);
        new CustomerCenterView(context, attrs, defStyleAttr);
        new CustomerCenterView(context, dismissHandlerFunction);
        new CustomerCenterView(context, listener);
        new CustomerCenterView(context, listener, dismissHandlerFunction);
    }

    static void checkMethods(
            @NonNull CustomerCenterView view,
            @NonNull Runnable dismissHandler,
            @NonNull CustomerCenterListener listener
    ) {
        Function0<Unit> dismissHandlerFunction = new Function0<Unit>() {
            @Override
            public Unit invoke() {
                dismissHandler.run();
                return Unit.INSTANCE;
            }
        };

        view.setDismissHandler(null);
        view.setDismissHandler(dismissHandlerFunction);
        view.setCustomerCenterListener(null);
        view.setCustomerCenterListener(listener);
    }
}
