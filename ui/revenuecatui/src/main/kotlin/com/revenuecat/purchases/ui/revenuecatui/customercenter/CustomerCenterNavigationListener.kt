package com.revenuecat.purchases.ui.revenuecatui.customercenter

/**
 * Listener interface for receiving callbacks when the Customer Center navigates between its screens.
 */
public fun interface CustomerCenterNavigationListener {

    /**
     * Called when the Customer Center navigates to a different screen, and once when it is first displayed.
     *
     * @param canNavigateBack Whether the Customer Center can navigate back to a previous screen. When `true`, it is
     * showing a screen that was pushed on top of its main screen, so a back button should be displayed. When
     * `false`, it is showing its main screen, so a close button should be displayed instead.
     * @param title The title of the screen the Customer Center is showing, or `null` if the screen has no title.
     */
    public fun onScreenChange(canNavigateBack: Boolean, title: String?)
}
