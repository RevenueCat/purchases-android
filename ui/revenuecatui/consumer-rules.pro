-dontwarn com.emergetools.snapshots.annotations.IgnoreEmergeSnapshot
-dontwarn com.emergetools.snapshots.annotations.EmergeSnapshotConfig

# Existence is probed via Class.forName from the purchases module (canUsePaywallUI in
# common/utils.kt) to detect whether the paywalls UI module is on the classpath. Force-keep the
# file class so the probe resolves even if the app doesn't otherwise reference it directly.
-keep class com.revenuecat.purchases.ui.revenuecatui.PaywallKt
