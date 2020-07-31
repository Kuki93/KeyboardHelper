package com.example.myapplication.imkeyboard

import android.app.Activity
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

private const val MAX_SMOOTH_POSITION = 6
private const val SMOOTH_POSITION = 4
private const val VIEW_TAG_KEY = 2131165353
private const val PANEL_SHOW_VIEW_TAG_KEY = 2136665353

/**
 * 提供一个快速平滑的滚动动画
 * [position] 位置
 */
fun RecyclerView.fixSmoothScrollToPosition(position: Int, post: Boolean) {
    val layoutManager = layoutManager as? LinearLayoutManager
    layoutManager ?: kotlin.run {
        if (post) {
            post {
                smoothScrollToPosition(position)
            }
        } else {
            smoothScrollToPosition(position)
        }
        return
    }

    val firstOffset = position.minus(layoutManager.findFirstVisibleItemPosition())
    val lastOffset = position.minus(layoutManager.findLastVisibleItemPosition())

    fun postScrollTo(offsetPosition: Int) {
        if (abs(offsetPosition) < MAX_SMOOTH_POSITION) {
            if (post) {
                post {
                    smoothScrollToPosition(position)
                }
            } else {
                smoothScrollToPosition(position)
            }
        } else {
            post {
                scrollToPosition(position.minus(if (offsetPosition > 0) SMOOTH_POSITION else -SMOOTH_POSITION))
                smoothScrollToPosition(position)
            }
        }
    }

    when {
        firstOffset < 0 -> {
            postScrollTo(firstOffset)
        }
        lastOffset > 0 -> {
            postScrollTo(lastOffset)
        }
        else -> {
            if (post) {
                post {
                    scrollToPosition(position)
                }
            } else {
                scrollToPosition(position)
            }
        }
    }
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
        if (smooth) {
            fixSmoothScrollToPosition(position, post)
        } else {
            if (post) {
                post {
                    scrollToPosition(position)
                }
            } else {
                scrollToPosition(position)
            }
        }
    }
}

@JvmOverloads
fun RecyclerView.scrollToTop(smooth: Boolean = true, post: Boolean = true) {
    (layoutManager as? LinearLayoutManager)?.let { layoutManager ->
        if (layoutManager.reverseLayout) {
            layoutManager.itemCount.minus(1)
        } else {
            0
        }
    }?.also { position ->
        if (smooth) {
            fixSmoothScrollToPosition(position, post)
        } else {
            if (post) {
                post {
                    scrollToPosition(position)
                }
            } else {
                scrollToPosition(position)
            }
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
    setTag(VIEW_TAG_KEY, true)
}

fun View.clearTouchNotHideFlag() {
    setTag(VIEW_TAG_KEY, null)
}

fun View.hasTouchNotHideFlag(): Boolean {
    val result = getTag(VIEW_TAG_KEY) == true
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
