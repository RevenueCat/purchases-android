# RewardVerificationService is instantiated reflectively by java.util.ServiceLoader through the
# META-INF/services/com.revenuecat.purchases.PurchasesService descriptor, so keep the class and its
# no-argument constructor even though there are no direct references to it.
-keep class com.revenuecat.purchases.admob.rewardverification.RewardVerificationService {
    <init>();
}
