@file:Suppress("LongParameterList")

package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.common.ComponentOverrides
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
class TabControlButtonComponent(
    @get:JvmSynthetic
    @SerialName("tab_index")
    val tabIndex: Int,
    @get:JvmSynthetic
    val stack: StackComponent,
) : PaywallComponent

@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("tab_control_toggle")
class TabControlToggleComponent(
    @get:JvmSynthetic
    @SerialName("default_value")
    val defaultValue: Boolean,
    @get:JvmSynthetic
    @SerialName("thumb_color_on")
    val thumbColorOn: ColorScheme,
    @get:JvmSynthetic
    @SerialName("thumb_color_off")
    val thumbColorOff: ColorScheme,
    @get:JvmSynthetic
    @SerialName("track_color_on")
    val trackColorOn: ColorScheme,
    @get:JvmSynthetic
    @SerialName("track_color_off")
    val trackColorOff: ColorScheme,
) : PaywallComponent

@InternalRevenueCatAPI
@Serializable
@SerialName("tab_control")
object TabControlComponent : PaywallComponent

@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("tabs")
class TabsComponent(
    @get:JvmSynthetic
    val size: Size = Size(width = Fill, height = Fit),
    @get:JvmSynthetic
    val padding: Padding = Padding.zero,
    @get:JvmSynthetic
    val margin: Padding = Padding.zero,
    @get:JvmSynthetic
    @SerialName("background_color")
    val backgroundColor: ColorScheme? = null,
    @get:JvmSynthetic
    val shape: Shape? = null,
    @get:JvmSynthetic
    val border: Border? = null,
    @get:JvmSynthetic
    val shadow: Shadow? = null,
    @get:JvmSynthetic
    val control: TabControl,
    @get:JvmSynthetic
    val tabs: List<Tab>,
    @get:JvmSynthetic
    val overrides: ComponentOverrides<PartialTabsComponent>? = null,
) : PaywallComponent {
    @InternalRevenueCatAPI
    @Poko
    @Serializable
    class Tab(
        @get:JvmSynthetic
        val stack: StackComponent,
    )

    @InternalRevenueCatAPI
    @Serializable
    sealed interface TabControl {
        @InternalRevenueCatAPI
        @Poko
        @Serializable
        @SerialName("buttons")
        class Buttons(@get:JvmSynthetic val stack: StackComponent) : TabControl

        @InternalRevenueCatAPI
        @Poko
        @Serializable
        @SerialName("toggle")
        class Toggle(@get:JvmSynthetic val stack: StackComponent) : TabControl
    }
}

@InternalRevenueCatAPI
@Poko
@Serializable
class PartialTabsComponent(
    @get:JvmSynthetic
    val visible: Boolean? = true,
    @get:JvmSynthetic
    val size: Size? = null,
    @get:JvmSynthetic
    val padding: Padding? = null,
    @get:JvmSynthetic
    val margin: Padding? = null,
    @get:JvmSynthetic
    @SerialName("background_color")
    val backgroundColor: ColorScheme? = null,
    @get:JvmSynthetic
    val shape: Shape? = null,
    @get:JvmSynthetic
    val border: Border? = null,
    @get:JvmSynthetic
    val shadow: Shadow? = null,
) : PartialComponent
