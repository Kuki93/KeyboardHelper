package com.example.myapplication.imkeyboard

object Constants {

    const val LOG_TAG = "Panel"
    //输入法高度
    const val KB_PANEL_PREFERENCE_NAME = "ky_panel_name"
    const val KEYBOARD_HEIGHT_FOR_L = "keyboard_height_for_l"
    const val KEYBOARD_HEIGHT_FOR_P = "keyboard_height_for_p"
    const val DEFAULT_KEYBOARD_HEIGHT_FOR_L = 263f
    const val DEFAULT_KEYBOARD_HEIGHT_FOR_P = 198f
    const val STATUS_BAR_HEIGHT_RES_NAME = "status_bar_height"
    const val NAVIGATION_BAR_HEIGHT_RES_NAME = "navigation_bar_height"
    const val DIMEN = "dimen"
    const val ANDROID = "android"
    /**
     * panel id, custom panel (PanelView) id is panelView's triggerViewId
     * [PanelView.getTriggerViewId]
     */
    const val PANEL_NONE = -1
    const val PANEL_KEYBOARD = 0
    const val PROTECT_KEY_CLICK_DURATION = 500L

    @JvmField
    var DEBUG = false
}
