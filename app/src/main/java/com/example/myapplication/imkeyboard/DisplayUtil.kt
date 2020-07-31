package com.example.myapplication.imkeyboard

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Point
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import com.example.myapplication.imkeyboard.Constants.ANDROID
import com.example.myapplication.imkeyboard.Constants.DIMEN
import com.example.myapplication.imkeyboard.Constants.NAVIGATION_BAR_HEIGHT_RES_NAME

object DisplayUtil {
    /**
     * 获取toolar的高度，但是这个方法仅仅在非沉浸下才有用。
     *
     * @param window
     * @return
     */
    @JvmStatic
    fun getToolbarHeight(window: Window): Int {
        return window.decorView.findViewById<View>(Window.ID_ANDROID_CONTENT).top
    }

    @JvmStatic
    fun getLocationOnScreen(view: View): IntArray {
        val contentViewLocationInScreen = IntArray(2)
        view.getLocationOnScreen(contentViewLocationInScreen)
        return contentViewLocationInScreen
    }

    @JvmStatic
    fun contentViewCanDrawStatusBarArea(window: Window): Boolean {
        return getLocationOnScreen(window.decorView.findViewById(Window.ID_ANDROID_CONTENT))[1] == 0
    }

    /**
     * 对应 id 为 @Android：id/content 的 FrameLayout 所加载的布局。
     * 也就是我们 setContentView 的布局高度
     *
     * @param window
     * @return
     */
    @JvmStatic
    fun getContentViewHeight(window: Window): Int {
        return window.decorView.findViewById<View>(Window.ID_ANDROID_CONTENT).height
    }

    /**
     * 实际上获取的是DecorView的布局高度，是一个 FrameLayout，其内置布局 id 为 com.android.internal.R.layout.screen_simple 的 LinearLayout
     * 包含 id为 @+id/action_mode_bar_stub_ViewStub 的 ViewStub 还有 id 为 @Android：id/content 的 FrameLayout。
     *
     * @param window
     * @return
     */
    @JvmStatic
    fun getScreenHeightWithSystemUI(window: Window): Int {
        return window.decorView.height
    }

    @JvmStatic
    fun getScreenHeightWithoutNavigationBar(context: Context): Int {
        return context.resources.displayMetrics.heightPixels
    }

    @JvmStatic
    fun getScreenHeightWithoutSystemUI(window: Window): Int {
        val r = Rect()
        window.decorView.getWindowVisibleDisplayFrame(r)
        return r.bottom - r.top
    }

    @JvmStatic
    fun getWindowVisibleDisplayFrame(contentView: View): Int {
        val r = Rect()
        contentView.getWindowVisibleDisplayFrame(r)
        return r.bottom - r.top
    }

    @JvmStatic
    fun isFullScreen(activity: Activity): Boolean {
        return (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN
                == WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    @JvmStatic
    fun isFullScreen(window: Window): Boolean {
        return (window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN
                == WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    @JvmStatic
    fun getStatusBarHeight(window: Window): Int {
        val frame = Rect()
        window.decorView.getWindowVisibleDisplayFrame(frame)
        return frame.top
    }


    @JvmStatic
    fun getNavigationBarHeight(context: Context): Int {
        return getInternalDimensionSize(context.resources, NAVIGATION_BAR_HEIGHT_RES_NAME)
    }

    private fun getInternalDimensionSize(res: Resources, key: String): Int {
        var result = 0
        val resourceId = res.getIdentifier(key, DIMEN, ANDROID)
        if (resourceId > 0) {
            result = res.getDimensionPixelSize(resourceId)
        }
        return result
    }

    @JvmStatic
    fun isPortrait(context: Context): Boolean {
        return when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                true
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                false
            }
            else -> {
                val display =
                    (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
                val point = Point()
                display.getSize(point)
                point.x <= point.y
            }
        }
    }

    /**
     *
     */
    @JvmStatic
    @TargetApi(14)
    fun isNavigationBarShow(context: Context, window: Window): Boolean {
        return isNavBarVisible(context, window)
    }

    /**
     * Decorview 源码
     * public static final ColorViewAttributes NAVIGATION_BAR_COLOR_VIEW_ATTRIBUTES =
     * new ColorViewAttributes(
     * SYSTEM_UI_FLAG_HIDE_NAVIGATION, FLAG_TRANSLUCENT_NAVIGATION,
     * Gravity.BOTTOM, Gravity.RIGHT, Gravity.LEFT,
     * Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME,
     * com.android.internal.R.id.navigationBarBackground,
     *
     * @param context
     * @param window
     * @return
     */
    @JvmStatic
    fun isNavBarVisible(context: Context, window: Window): Boolean {
        val viewGroup: ViewGroup? = window.decorView as ViewGroup?
        if (viewGroup != null) {
            for (i in 0 until viewGroup.childCount) {
                val id: Int = viewGroup.getChildAt(i).id
                if (id != View.NO_ID) {
                    var resourceEntryName: String? = null
                    try {
                        resourceEntryName = context.resources.getResourceEntryName(id)
                    } catch (e: Resources.NotFoundException) {
                    }
                    if ((("navigationBarBackground" == resourceEntryName) && viewGroup.getChildAt(i).visibility == View.VISIBLE)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    @JvmStatic
    fun dip2px(context: Context, dipValue: Float): Int {
        val scale: Float = context.resources.displayMetrics.density
        return (dipValue * scale + 0.5f).toInt()
    }
}
