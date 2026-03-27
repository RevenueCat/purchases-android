@file:Suppress("LongParameterList")

package com.revenuecat.purchases.paywalls.components

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("tab_control_button")
@Immutable
public class TabControlButtonComponent(
    @get:JvmSynthetic
    @SerialName("tab_index")
    public val tabIndex: Int,
    @SerialName("tab_id")
    public val tabId: String,
    @get:JvmSynthetic
    public val stack: StackComponent,
) : PaywallComponent

@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("tab_control_toggle")
@Immutable
public class TabControlToggleComponent(
    @get:JvmSynthetic
    @SerialName("default_value")
    public val defaultValue: Boolean,
    @get:JvmSynthetic
    @SerialName("thumb_color_on")
    public val thumbColorOn: ColorScheme,
    @get:JvmSynthetic
    @SerialName("thumb_color_off")
    public val thumbColorOff: ColorScheme,
    @get:JvmSynthetic
    @SerialName("track_color_on")
    public val trackColorOn: ColorScheme,
    @get:JvmSynthetic
    @SerialName("track_color_off")
    public val trackColorOff: ColorScheme,
) : PaywallComponent

@InternalRevenueCatAPI
@Serializable
@SerialName("tab_control")
public object TabControlComponent : PaywallComponent

@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("tabs")
@Immutable
public class TabsComponent(
    @get:JvmSynthetic
    public val visible: Boolean? = null,
    @get:JvmSynthetic
    public val size: Size = Size(width = Fill, height = Fit),
    @get:JvmSynthetic
    public val padding: Padding = Padding.zero,
    @get:JvmSynthetic
    public val margin: Padding = Padding.zero,
    @get:JvmSynthetic
    @SerialName("background_color")
    public val backgroundColor: ColorScheme? = null,
    @get:JvmSynthetic
    public val background: Background? = null,
    @get:JvmSynthetic
    public val shape: Shape? = null,
    @get:JvmSynthetic
    public val border: Border? = null,
    @get:JvmSynthetic
    public val shadow: Shadow? = null,
    @get:JvmSynthetic
    public val control: TabControl,
    @get:JvmSynthetic
    public val tabs: List<Tab>,
    @get:JvmSynthetic
    @SerialName("default_tab_id")
    public val defaultTabId: String? = null,
    @get:JvmSynthetic
    public val overrides: List<ComponentOverride<PartialTabsComponent>> = emptyList(),
) : PaywallComponent {
    @InternalRevenueCatAPI
    @Poko
    @Serializable
    public class Tab(
        @get:JvmSynthetic
        public val id: String,
        @get:JvmSynthetic
        public val stack: StackComponent,
    )

    @InternalRevenueCatAPI
    @Serializable
    public sealed interface TabControl {
        @InternalRevenueCatAPI
        @Poko
        @Serializable
        @SerialName("buttons")
        public class Buttons(@get:JvmSynthetic public val stack: StackComponent) : TabControl

        @InternalRevenueCatAPI
        @Poko
        @Serializable
        @SerialName("toggle")
        public class Toggle(@get:JvmSynthetic public val stack: StackComponent) : TabControl
    }
}

@InternalRevenueCatAPI
@Poko
@Serializable
@Immutable
public class PartialTabsComponent(
    @get:JvmSynthetic
    public val visible: Boolean? = true,
    @get:JvmSynthetic
    public val size: Size? = null,
    @get:JvmSynthetic
    public val padding: Padding? = null,
    @get:JvmSynthetic
    public val margin: Padding? = null,
    @get:JvmSynthetic
    @SerialName("background_color")
    public val backgroundColor: ColorScheme? = null,
    @get:JvmSynthetic
    public val background: Background? = null,
    @get:JvmSynthetic
    public val shape: Shape? = null,
    @get:JvmSynthetic
    public val border: Border? = null,
    @get:JvmSynthetic
    public val shadow: Shadow? = null,
) : PartialComponent
