package com.revenuecat.purchases.ui.revenuecatui.fonts

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Represents a font family. You can add one ore more [PaywallFont] with different weights and font styles.
 */
@Parcelize
@Poko
@Immutable
public class PaywallFontFamily(public val fonts: List<PaywallFont>) : Parcelable
