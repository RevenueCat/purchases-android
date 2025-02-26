package com.revenuecat.purchases.customercenter;

import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.PurchasesError;

// This file is a Java file because doing default implementations in Kotlin interfaces doesn't seem to work well in
// Java apps and gives a compilation error. This is a workaround to avoid that error.
public interface CustomerCenterListener {

    default void onRestoreStarted() {

    }

    default void onRestoreFailed(PurchasesError error) {

    }

    default void onRestoreCompleted(CustomerInfo customerInfo) {

    }

    default void onCancelSubscription() {

    }

    default void onFeedbackSurveyCompleted(String feedbackSurveyOptionId) {

    }

}

