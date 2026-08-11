-dontwarn com.emergetools.snapshots.annotations.IgnoreEmergeSnapshot
-dontwarn com.emergetools.snapshots.annotations.EmergeSnapshotConfig

# Instantiated reflectively by java.util.ServiceLoader through its META-INF/services descriptor: nothing
# references it directly.
-keep class com.revenuecat.purchases.ui.revenuecatui.paywalls.PaywallAssetWarmerImpl {
    <init>();
}
