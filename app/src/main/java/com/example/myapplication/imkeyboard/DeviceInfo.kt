package com.example.myapplication.imkeyboard

import android.view.Window

data class DeviceInfo(
    val window: Window,
    val isPortrait: Boolean,
    val statusBarH: Int,
    val navigationBarH: Int,
    val toolbarH: Int,
    val screenH: Int,
    val screenWithoutSystemUiH: Int,
    val screenWithoutNavigationH: Int
) {

    /**
     * 由于pad的导航栏无论是横屏还是竖屏，都是在当前界面的底部
     * 而普通的手机，横屏状态下导航栏是在界面的两侧
     * 故需要做区分
     */
    fun getCurrentNavigationBarHeightWhenVisible(isPortrait: Boolean, isPad: Boolean): Int {
        return if (isPortrait) {
            navigationBarH
        } else {
            if (isPad) {
                navigationBarH
            } else {
                0
            }
        }
    }
}
