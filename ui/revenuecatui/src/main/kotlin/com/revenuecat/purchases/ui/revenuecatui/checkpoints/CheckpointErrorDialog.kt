package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * The content of a checkpoint error dialog returned by an [ErrorDialogFactory]. When a factory provides this,
 * the SDK shows the error in its own dialog using the factory-supplied [message] and [buttonTitle] instead of its
 * own localized copy.
 */
@InternalRevenueCatAPI
public class CheckpointErrorDialog(
    /** The body text of the error dialog. */
    public val message: String,
    /** The label of the dismiss button. */
    public val buttonTitle: String,
) {

    override fun equals(other: Any?): Boolean =
        other is CheckpointErrorDialog &&
            other.message == message &&
            other.buttonTitle == buttonTitle

    override fun hashCode(): Int = 31 * message.hashCode() + buttonTitle.hashCode()

    override fun toString(): String = "CheckpointErrorDialog(message=$message, buttonTitle=$buttonTitle)"
}
