package com.example.myapplication.imkeyboard

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.IntDef
import androidx.annotation.Size
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView

enum class KeyboardPanelState(var value: Int) {
    KEYBOARD_PANEL_STATE_NONE(0),
    KEYBOARD_PANEL_STATE_PANEL(1),
    KEYBOARD_PANEL_STATE_KEYBOARD(2)
}

const val ACTION_KEYBOARD_TO_NONE = 1
const val ACTION_KEYBOARD_TO_PANEL = 2
const val ACTION_PANEL_TO_NONE = 3
const val ACTION_PANEL_TO_KEYBOARD = 4
const val ACTION_NONE_TO_KEYBOARD = 5
const val ACTION_NONE_TO_PANEL = 6

@IntDef(
    ACTION_KEYBOARD_TO_NONE,
    ACTION_KEYBOARD_TO_PANEL,
    ACTION_PANEL_TO_NONE,
    ACTION_PANEL_TO_KEYBOARD,
    ACTION_NONE_TO_KEYBOARD,
    ACTION_NONE_TO_PANEL
)
@Retention(AnnotationRetention.SOURCE)
annotation class KeyboardChangeAction

const val KEYBOARD_TYPE_NONE = 0
const val KEYBOARD_TYPE_CONTENT = 1
const val KEYBOARD_TYPE_PANEL = 2
const val KEYBOARD_TYPE_INPUT = 3
const val KEYBOARD_TYPE_TOGGLE = 4
const val KEYBOARD_TYPE_HELPER = 5

private const val KEYBOARD_LAYOUT_CONTENT_HEIGHT_TAG = 2131165353
private const val KEYBOARD_LAYOUT_PANEL_HEIGHT_TAG = 2131165354

@IntDef(
    KEYBOARD_TYPE_NONE,
    KEYBOARD_TYPE_CONTENT,
    KEYBOARD_TYPE_PANEL,
    KEYBOARD_TYPE_INPUT,
    KEYBOARD_TYPE_TOGGLE,
    KEYBOARD_TYPE_HELPER
)
@Retention(AnnotationRetention.SOURCE)
annotation class KeyboardType

fun ViewGroup.showOnlyViewById(showId: Int) {
    forEach {
        if (showId == it.id) {
            it.visibility = View.VISIBLE
        } else {
            it.visibility = View.GONE
        }
    }
}

fun allEquals(@Size(min = 2) vararg items: Any?): Boolean {
    if (items.size <= 1) {
        throw IllegalArgumentException("array items size must > 1")
    }
    val first = items[0]
    return items.all {
        it == first
    }
}

fun anyIsNull(@Size(min = 1) vararg items: Any?): Boolean {
    if (items.isEmpty()) {
        throw IllegalArgumentException("array items size must > 0")
    }
    return items.any {
        it == null
    }
}

fun allIsNull(@Size(min = 1) vararg items: Any?): Boolean {
    if (items.isEmpty()) {
        throw IllegalArgumentException("array items size must > 0")
    }
    return items.all {
        it == null
    }
}

fun anyNonNull(@Size(min = 1) vararg items: Any?): Boolean {
    if (items.isEmpty()) {
        throw IllegalArgumentException("array items size must > 0")
    }
    return items.any {
        it != null
    }
}

fun allNonNull(@Size(min = 1) vararg items: Any?): Boolean {
    if (items.isEmpty()) {
        throw IllegalArgumentException("array items size must > 0")
    }
    return items.all {
        it != null
    }
}

var IKeyboardLayout.contentFixedHeight: Int
    get() {
        return if (this is View) {
            (getTag(KEYBOARD_LAYOUT_CONTENT_HEIGHT_TAG) as? Int) ?: 0
        } else 0
    }
    private set(value) {
        if (this is View) {
            setTag(KEYBOARD_LAYOUT_CONTENT_HEIGHT_TAG, value)
        }
    }

var IKeyboardLayout.panelFixedHeight: Int
    get() {
        return if (this is View) {
            (getTag(KEYBOARD_LAYOUT_PANEL_HEIGHT_TAG) as? Int) ?: 0
        } else 0
    }
    private set(value) {
        if (this is View) {
            setTag(KEYBOARD_LAYOUT_PANEL_HEIGHT_TAG, value)
        }
    }

interface IKeyboardLayout {

    var contentView: View?

    var panelView: View

    var lastState: KeyboardPanelState

    var curState: KeyboardPanelState

    var keyboardHelper: KeyboardHelper?

    fun canAutoCollapse() = false


    fun ViewGroup.assertView(block: ((type: Int, v: View) -> Unit)? = null) {
        val stateContentMask = 1
        val statePanelMask = 2
        var flag = 0
        forEach {
            val lp = it.layoutParams as IKeyboardLayoutParams
            when (lp.keyboardType) {
                KEYBOARD_TYPE_CONTENT -> {
                    if (stateContentMask == flag.and(stateContentMask)) {
                        throw IllegalStateException("不允许设置两个content")
                    }
                    contentView = it
                    flag = flag.or(stateContentMask)
                }
                KEYBOARD_TYPE_PANEL -> {
                    if (statePanelMask == flag.and(statePanelMask)) {
                        throw IllegalStateException("不允许设置两个panel")
                    }
                    panelView = it
                    flag = flag.or(statePanelMask)
                }
                else -> {
                    block?.invoke(lp.keyboardType, it)
                }
            }
        }
        if (statePanelMask != flag.and(statePanelMask)) {
            throw IllegalStateException("panel 必须不能为空")
        }
    }

    fun updateContentFixedHeight(value: Int, fixed: Boolean = true) {
        if ((fixed && contentFixedHeight == 0) || !fixed) {
            contentFixedHeight = value
        }
    }

    fun updatePanelFixedHeight(value: Int, fixed: Boolean = false) {
        if ((fixed && panelFixedHeight == 0) || !fixed) {
            panelFixedHeight = value
        }
    }

    fun providerRecyclerView(): RecyclerView? {
        val mContentView = contentView
        mContentView ?: return null
        fun findRecyclerView(view: View): RecyclerView? {
            if (view is RecyclerView) {
                return view
            }
            if (view is ViewGroup) {
                view.forEach {
                    findRecyclerView(it)?.also { result ->
                        return result
                    }
                }
            }
            return null
        }
        return findRecyclerView(mContentView)
    }

    fun providerInputView(): View? {
        return let {
            it as? ViewGroup
        }?.let { layout ->
            var result: View? = null
            layout.forEach {
                val lp = it.layoutParams as IKeyboardLayoutParams
                if (lp.keyboardType == KEYBOARD_TYPE_INPUT) {
                    result = it
                    return@forEach
                }
            }
            result
        }
    }

    fun providerToggleViews(): List<View>? {
        return let {
            it as? ViewGroup
        }?.let { layout ->
            var result: MutableList<View>? = null
            layout.forEach {
                val lp = it.layoutParams as IKeyboardLayoutParams
                if (lp.keyboardType == KEYBOARD_TYPE_TOGGLE) {
                    result?.also { views ->
                        views.add(it)
                    } ?: kotlin.run {
                        result = mutableListOf(it)
                    }
                    return@forEach
                }
            }
            result
        }
    }

    @CallSuper
    fun startLayout(provider: IKeyboardStateManger, @KeyboardChangeAction action: Int) {
        lastState = curState
        curState = provider.currentState()
    }
}

interface IKeyboardStateManger {

    fun currentState(): KeyboardPanelState

    fun getKeyBoardHeight(): Int

    fun getSwitchPanelHeight(): Int

    fun switchPanelIsOpen(): Boolean

    fun keyboardIsOpen(): Boolean

    fun lastKeyboardIsOpen(): Boolean

    fun hasAnyPanelIsOpen(): Boolean

    fun showSwitchPanel(): Boolean

    fun showKeyboard(): Boolean

    fun hideSwitchPanel(): Boolean

    fun hideKeyboard(): Boolean

    fun hideAllPanel(): Boolean

    fun collapseAnyPanel(view: View? = null, ev: MotionEvent? = null): Boolean

    fun currentPanelViewId(): Int?

    fun handlerToggleEvent(v: View, ignoreOnPreHandler: Boolean = false)
}

interface IKeyboardLayoutParams {

    @KeyboardType
    var keyboardType: Int

}
