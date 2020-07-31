package com.example.myapplication.imkeyboard

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.view.Window
import com.example.myapplication.imkeyboard.DisplayUtil.getScreenHeightWithSystemUI
import com.example.myapplication.imkeyboard.DisplayUtil.getWindowVisibleDisplayFrame

class DeviceRuntime(val context: Context, val window: Window) {

    var deviceInfoP: DeviceInfo? = null
    var deviceInfoL: DeviceInfo? = null

    var isNavigationBarShow: Boolean = false
    var isPortrait: Boolean = false
    var isPad: Boolean = false
    var isFullScreen: Boolean = false;

    init {
        isPad =
            (context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    fun getDeviceInfoByOrientation(cache: Boolean = false): DeviceInfo {
        isPortrait = DisplayUtil.isPortrait(context)
        isNavigationBarShow = DisplayUtil.isNavigationBarShow(context, window)
        isFullScreen = DisplayUtil.isFullScreen(window)

        if (cache) {
            deviceInfoP?.takeIf {
                it.isPortrait
            }?.also {
                return it
            }

            deviceInfoL?.takeIf {
                !it.isPortrait
            }?.also {
                return it
            }
        }

        val navigationBarHeight = DisplayUtil.getNavigationBarHeight(context)
        val statusBarHeight = DisplayUtil.getStatusBarHeight(window)
        //以这种方式计算出来的toolbar，如果和statusBarHeight一样，则实际上就是statusBar的高度，大于statusBar的才是toolBar的高度。
        var toolbarH = DisplayUtil.getToolbarHeight(window)
        if (toolbarH == statusBarHeight) {
            toolbarH = 0
        }
        val screenHeight = DisplayUtil.getScreenHeightWithSystemUI(window)
        val screenWithoutSystemUIHeight = DisplayUtil.getScreenHeightWithoutSystemUI(window)
        val screenWithoutNavigationHeight = DisplayUtil.getScreenHeightWithoutNavigationBar(context)

        return if (isPortrait) {
            DeviceInfo(
                window, true,
                statusBarHeight, navigationBarHeight, toolbarH,
                screenHeight, screenWithoutSystemUIHeight, screenWithoutNavigationHeight
            )
        } else {
            DeviceInfo(
                window, false,
                statusBarHeight, navigationBarHeight, toolbarH,
                screenHeight, screenWithoutSystemUIHeight, screenWithoutNavigationHeight
            )
        }.also {
            deviceInfoL = it
        }
    }

    fun getKeyboardHeight(window: Window, contentView: View): Int {
        val screenHeight = getScreenHeightWithSystemUI(window)
        val contentHeight = getWindowVisibleDisplayFrame(contentView)
        val info = getDeviceInfoByOrientation(true)
        val systemUIHeight = if (isFullScreen) {
            0
        } else {
            info.statusBarH + (if (isNavigationBarShow) info.getCurrentNavigationBarHeightWhenVisible(
                isPortrait,
                isPad
            ) else 0)
        }
        return screenHeight - contentHeight - systemUIHeight
    }
}
