package com.emergetools.snapshots.annotations

/**
 * Specifies the annotated function is a Preview that should be ignored by Emerge snapshotting.
 */
@Target(AnnotationTarget.FUNCTION)
internal annotation class IgnoreEmergeSnapshot
