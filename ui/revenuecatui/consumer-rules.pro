-dontwarn com.emergetools.snapshots.annotations.IgnoreEmergeSnapshot
-dontwarn com.emergetools.snapshots.annotations.EmergeSnapshotConfig

# CheckpointPresenterImpl is instantiated reflectively by java.util.ServiceLoader through the
# META-INF/services/com.revenuecat.purchases.checkpoints.CheckpointPresenter descriptor, so keep the class and
# its no-argument constructor even though there are no direct references to it.
-keep class com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointPresenterImpl {
    <init>();
}
