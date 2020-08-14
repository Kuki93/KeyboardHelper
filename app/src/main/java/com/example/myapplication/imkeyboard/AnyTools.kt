package com.example.myapplication.imkeyboard

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import android.transition.ArcMotion
import android.transition.ChangeBounds
import android.transition.TransitionSet
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.temp.SubsamplingScaleImageViewSharedTransition

private const val MAX_SMOOTH_POSITION = 6
private const val SMOOTH_POSITION = 4
private const val HOT_HIDE_VIEW_TAG_KEY = 2131165353
private const val PANEL_SHOW_VIEW_TAG_KEY = 2136665353
private const val PADDING_RECT_VIEW_TAG_KEY = 2136665853

/**
 * 提供一个快速平滑的滚动动画
 * [position] 位置
 */
fun RecyclerView.fixSmoothScrollToPosition(position: Int, post: Boolean) {
//    val layoutManager = layoutManager as? LinearLayoutManager
//    layoutManager ?: kotlin.run {
//        if (post) {
//            post {
//                smoothScrollToPosition(position)
//            }
//        } else {
//            smoothScrollToPosition(position)
//        }
//        return
//    }
//
//    val firstOffset = position.minus(layoutManager.findFirstVisibleItemPosition())
//    val lastOffset = position.minus(layoutManager.findLastVisibleItemPosition())
//
//    fun postScrollTo(offsetPosition: Int) {
//        if (abs(offsetPosition) < MAX_SMOOTH_POSITION) {
//            if (post) {
//                post {
//                    smoothScrollToPosition(position)
//                }
//            } else {
//                smoothScrollToPosition(position)
//            }
//        } else {
//            post {
//                scrollToPosition(position.minus(if (offsetPosition > 0) SMOOTH_POSITION else -SMOOTH_POSITION))
//                smoothScrollToPosition(position)
//            }
//        }
//    }
//
//    when {
//        firstOffset < 0 -> {
//            postScrollTo(firstOffset)
//        }
//        lastOffset > 0 -> {
//            postScrollTo(lastOffset)
//        }
//        else -> {
//            if (post) {
//                post {
//                    scrollToPosition(position)
//                }
//            } else {
//                scrollToPosition(position)
//            }
//        }
//    }
}

@JvmOverloads
fun RecyclerView.scrollToBottom(smooth: Boolean = true, post: Boolean = false) {
    (layoutManager as? LinearLayoutManager)?.let { layoutManager ->
        if (layoutManager.reverseLayout) {
            0
        } else {
            layoutManager.itemCount.minus(1)
        }
    }?.also { position ->
        scrollToPosition(position)
//        if (smooth) {
//            fixSmoothScrollToPosition(position, post)
//        } else {
//            if (post) {
//                post {
//                    scrollToPosition(position)
//                }
//            } else {
//                scrollToPosition(position)
//            }
//        }
    }
}

@JvmOverloads
fun RecyclerView.scrollToTop(smooth: Boolean = true, post: Boolean = true) {
//    (layoutManager as? LinearLayoutManager)?.let { layoutManager ->
//        if (layoutManager.reverseLayout) {
//            layoutManager.itemCount.minus(1)
//        } else {
//            0
//        }
//    }?.also { position ->
//        if (smooth) {
//            fixSmoothScrollToPosition(position, post)
//        } else {
//            if (post) {
//                post {
//                    scrollToPosition(position)
//                }
//            } else {
//                scrollToPosition(position)
//            }
//        }
//    }
}


fun RecyclerView.forceStopRecyclerViewScroll() {
    dispatchTouchEvent(
        MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_CANCEL,
            0f,
            0f,
            0
        )
    )
}

fun View.generatePaddingRect(
    @Px start: Int = paddingStart,
    @Px top: Int = paddingTop,
    @Px end: Int = paddingEnd,
    @Px bottom: Int = paddingBottom
): Rect {
    return Rect(start, top, end, bottom)
}

fun View.updatePaddingTag(
    @Px start: Int = paddingStart,
    @Px top: Int = paddingTop,
    @Px end: Int = paddingEnd,
    @Px bottom: Int = paddingBottom
) {
    setTag(PADDING_RECT_VIEW_TAG_KEY, generatePaddingRect(start, top, end, bottom))
}

fun View.getPaddingRectByTag(clear: Boolean = true): Rect? {
    return (getTag(PADDING_RECT_VIEW_TAG_KEY) as? Rect).also {
        if (clear) {
            setTag(PADDING_RECT_VIEW_TAG_KEY, null)
        }
    }
}

/**
 * 打开软键盘
 */
fun openKeyboard(mEditText: View?, mContext: Activity) {
    val imm =
        mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    mEditText?.requestFocus()
    imm.showSoftInput(mEditText, 0)
}

/**
 * 关闭软键盘
 */
fun closeKeyboard(mEditText: View?, mContext: Activity) {
    val imm =
        mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val focusView: View = mEditText ?: mContext.currentFocus ?: View(mContext)
    imm.hideSoftInputFromWindow(focusView.windowToken, 0)
}

fun View.markTouchNotHideFlag() {
    setTag(HOT_HIDE_VIEW_TAG_KEY, true)
}

fun View.clearTouchNotHideFlag() {
    setTag(HOT_HIDE_VIEW_TAG_KEY, null)
}

fun View.hasTouchNotHideFlag(): Boolean {
    val result = getTag(HOT_HIDE_VIEW_TAG_KEY) == true
    if (!result && parent is ViewGroup) {
        return (parent as ViewGroup).hasTouchNotHideFlag()
    }
    return result
}

fun View.markPanelShowViewIdFlag(id: Int) {
    setTag(PANEL_SHOW_VIEW_TAG_KEY, id)
}

fun View.clearPanelShowViewIdFlag() {
    setTag(PANEL_SHOW_VIEW_TAG_KEY, null)
}

fun View.obtainPanelShowViewId() = getTag(PANEL_SHOW_VIEW_TAG_KEY) as? Int ?: 0

fun View.isCurPanelShowViewId(view: View?) = getTag(PANEL_SHOW_VIEW_TAG_KEY) == (view?.id ?: id)

fun ViewGroup.getTouchTargetViews(
    event: MotionEvent,
    checkIsClickable: Boolean = false,
    checkVisible: Int? = null,
    location: IntArray = IntArray(2)
): List<View> {
    fun touchEventInView(
        view: View,
        x: Float,
        y: Float
    ): Boolean {
        view.getLocationOnScreen(location)
        val left = location[0]
        val top = location[1]
        val right = left + view.measuredWidth
        val bottom = top + view.measuredHeight
        return y >= top && y <= bottom && x >= left && x <= right
    }

    val result = mutableListOf<View>()
    fun ViewGroup.findView() {
        for (index in 0 until childCount) {
            val child = getChildAt(childCount.minus(index.plus(1)))
            if (checkVisible != null && child.visibility != checkVisible) continue
            if (child is ViewStub) continue
            if (touchEventInView(child, event.rawX, event.rawY)) {
                if (!checkIsClickable || child.isClickable) {
                    result.add(child)
                }
                if (child is ViewGroup) {
                    child.findView()
                }
            }
        }
    }
    findView()
    if (result.isEmpty()) {
        result.add(this)
    }
    return result
}