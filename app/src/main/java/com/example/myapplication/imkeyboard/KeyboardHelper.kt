package com.example.myapplication.imkeyboard

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.PopupWindow
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_keyboard3.*

interface KeyboardStateObserver {
    fun onStateChanged(
        helper: KeyboardHelper,
        keyboardVisible: Boolean,
        switchPanelVisible: Boolean,
        orientation: Int
    )
}

interface TogglePreHandleListener {

    /**
     * @return true: 表示拦截处理，不再调用默认实现
     */
    fun onClickToggleButton(
        helper: KeyboardHelper,
        view: View,
        keyboardVisible: Boolean,
        switchPanelVisible: Boolean
    ): Boolean
}

open class SimpleKeyboardStateObserver : KeyboardStateObserver {
    override fun onStateChanged(
        helper: KeyboardHelper,
        keyboardVisible: Boolean,
        switchPanelVisible: Boolean,
        orientation: Int
    ) {

    }
}

open class SimpleTogglePreHandleListener : TogglePreHandleListener {
    override fun onClickToggleButton(
        helper: KeyboardHelper,
        view: View,
        keyboardVisible: Boolean,
        switchPanelVisible: Boolean
    ): Boolean = false
}

/**
 * 微信键盘处理
 */
class KeyboardHelper private constructor(private val host: Activity) :
    IKeyboardStateManger {

    companion object {
        fun from(host: Activity): KeyboardHelper {
            return KeyboardHelper(host)
        }
    }

    private lateinit var keyboardMonitor: KeyboardChangeMonitor

    private lateinit var keyboardLayout: IKeyboardLayout

    private lateinit var switchPanel: View

    private var inputView: View? = null

    private var recyclerView: RecyclerView? = null

    private var automaticMode = false

    private var switchPanelHeight: Int = 0

    private var keyboardHeight: Int = 0

    private var switchPanelIsOpen = false

    private var lastNotifyState: Pair<Boolean, Boolean>? = null

    private var moreKeyboardObservers: Set<KeyboardStateObserver>? = null

    var keyboardObserver: KeyboardStateObserver? = null

    var togglePreHandleListener: TogglePreHandleListener? = null

    init {
        val observer = { height: Int, orientation: Int ->
            onKeyboardHeightChanged(height, orientation)
        }
        if (host is ComponentActivity) {
            host.lifecycle.addObserver(object : LifecycleObserver {

                @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
                fun onCreate(owner: LifecycleOwner) {
                    host.window.setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                                or WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                    )
                    val parentView = host.findViewById<View>(Window.ID_ANDROID_CONTENT)
                    keyboardMonitor = KeyboardChangeMonitor(host, parentView)
                    keyboardMonitor.observer = observer
                    parentView?.post {
                        keyboardMonitor.start()
                    }
                }

                @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
                fun onStop(owner: LifecycleOwner) {
                    if (owner is Activity && owner.isFinishing) {
                        keyboardMonitor.observer = null
                        hideAllPanel()
                    }
                }

                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                fun onDestroy(owner: LifecycleOwner) {
                    automaticMode = false
                    keyboardMonitor.observer = null
                    keyboardObserver = null
                    keyboardMonitor.close()
                }
            })
        }
    }

    fun startAutomaticMode(layout: IKeyboardLayout) {
        if (automaticMode) {
            return
        }
        layout.keyboardHelper = this
        automaticMode = true
        keyboardLayout = layout
        inputView = keyboardLayout.providerInputView()
        recyclerView = keyboardLayout.providerRecyclerView()
        switchPanel = keyboardLayout.panelView

        switchPanelHeight = switchPanel.height

        switchPanel.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
            switchPanelHeight = bottom.minus(top)
        }

        (host as? ComponentActivity)?.also {
            it.onBackPressedDispatcher.addCallback(it, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!hideSwitchPanel()) {
                        isEnabled = false
                        it.onBackPressedDispatcher.onBackPressed()
                    }
                }
            })
        }

        switchPanel.markTouchNotHideFlag()

        keyboardLayout.providerToggleViews()?.forEach {
            it.markTouchNotHideFlag()
            it.setOnClickListener { v ->
                handlerToggleEvent(v)
            }
        }

        recyclerView?.also {
            if (!layout.canAutoCollapse()) {
                it.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {

                    override fun onInterceptTouchEvent(rv: RecyclerView, ev: MotionEvent): Boolean {
                        if (ev.action == MotionEvent.ACTION_DOWN) {
                            collapseAnyPanel(it, ev)
                        }
                        return super.onInterceptTouchEvent(rv, ev)
                    }
                })
            }
        }
    }

    fun addMoreKeyboardObservers(keyboardObserver: KeyboardStateObserver) {
        moreKeyboardObservers?.toMutableSet()?.also {
            it.add(keyboardObserver)
        } ?: kotlin.run {
            moreKeyboardObservers = hashSetOf(keyboardObserver)
        }
    }

    fun removeMoreKeyboardObservers(keyboardObserver: KeyboardStateObserver) {
        moreKeyboardObservers?.toMutableSet()?.also {
            it.remove(keyboardObserver)
        }
    }

    fun clearMoreKeyboardObservers() {
        moreKeyboardObservers = null
    }

    fun addLambdaMoreKeyboardObservers(
        observer: (KeyboardHelper, Boolean, Boolean, Int) -> Unit
    ) {
        addMoreKeyboardObservers(object : SimpleKeyboardStateObserver() {
            override fun onStateChanged(
                helper: KeyboardHelper,
                keyboardVisible: Boolean,
                switchPanelVisible: Boolean,
                orientation: Int
            ) {
                observer.invoke(helper, keyboardVisible, switchPanelVisible, orientation)
            }
        })
    }

    fun setLambdaKeyboardObserver(
        observer: ((KeyboardHelper, Boolean, Boolean, Int) -> Unit)? = null
    ) {
        if (observer == null) {
            keyboardObserver = null
            return
        }
        keyboardObserver = object : SimpleKeyboardStateObserver() {
            override fun onStateChanged(
                helper: KeyboardHelper,
                keyboardVisible: Boolean,
                switchPanelVisible: Boolean,
                orientation: Int
            ) {
                observer.invoke(helper, keyboardVisible, switchPanelVisible, orientation)
            }
        }
    }

    fun setLambdaPreHandleToggleClickListener(
        clickListener: ((KeyboardHelper, View, Boolean, Boolean) -> Boolean)? = null
    ) {
        if (clickListener == null) {
            togglePreHandleListener = null
            return
        }
        togglePreHandleListener = object : SimpleTogglePreHandleListener() {
            override fun onClickToggleButton(
                helper: KeyboardHelper,
                view: View,
                keyboardVisible: Boolean,
                switchPanelVisible: Boolean
            ): Boolean {
                return clickListener.invoke(helper, view, keyboardVisible, switchPanelVisible)
            }
        }
    }

    override fun currentState(): KeyboardPanelState {
        return if (hasAnyPanelIsOpen()) {
            if (switchPanelIsOpen) {
                KeyboardPanelState.KEYBOARD_PANEL_STATE_PANEL
            } else {
                KeyboardPanelState.KEYBOARD_PANEL_STATE_KEYBOARD
            }
        } else {
            KeyboardPanelState.KEYBOARD_PANEL_STATE_NONE
        }
    }

    override fun getKeyBoardHeight(): Int {
        return keyboardHeight
    }

    override fun getSwitchPanelHeight(): Int {
        return switchPanelHeight
    }

    override fun switchPanelIsOpen(): Boolean {
        return switchPanelIsOpen
    }

    /**
     * 软键盘是否展开
     */
    override fun keyboardIsOpen(): Boolean {
        return keyboardMonitor.isShowKeyboard()
    }

    override fun lastKeyboardIsOpen(): Boolean {
        return keyboardMonitor.lastIsShowKeyboard()
    }

    /**
     * 软键盘或面板是否展开
     */
    override fun hasAnyPanelIsOpen(): Boolean {
        return switchPanelIsOpen || keyboardMonitor.isShowKeyboard()
    }

    /**
     * 展开面板
     *
     * @return 表示true成功，否则表示不需要展开
     */
    override fun showSwitchPanel(): Boolean {
        if (switchPanelIsOpen) {
            return false
        }
        switchPanelIsOpen = true
        if (keyboardMonitor.isShowKeyboard()) {
            // 隐藏键盘
            closeKeyboard(inputView, host)
        } else {
            adjustInputViewPosition(ACTION_NONE_TO_PANEL)
        }
        return true
    }

    /**
     * 展开软键盘
     *
     * @return 表示true成功，否则表示不需要展开
     */
    override fun showKeyboard(): Boolean {
        if (keyboardMonitor.isShowKeyboard()) {
            return false
        }
        // 开启键盘
        openKeyboard(inputView, host)
        return true
    }

    /**
     * 隐藏面板
     *
     * @return 表示true成功，否则表示不需要隐藏
     */
    override fun hideSwitchPanel(): Boolean {
        if (!switchPanelIsOpen) {
            return false
        }
        switchPanelIsOpen = false
        adjustInputViewPosition(ACTION_PANEL_TO_NONE)
        return true
    }

    /**
     * 隐藏软键盘
     *
     * @return 表示true成功，否则表示不需要隐藏
     */
    override fun hideKeyboard(): Boolean {
        if (!keyboardMonitor.isShowKeyboard()) {
            return false
        }
        closeKeyboard(inputView, host)
        return true
    }

    /**
     * 隐藏软键盘或面板
     *
     *  @return 表示true成功，否则表示不需要隐藏
     */
    override fun hideAllPanel(): Boolean {
        if (!hideKeyboard()) {
            return hideSwitchPanel()
        }
        return true
    }

    override fun collapseAnyPanel(view: View?, ev: MotionEvent?): Boolean {
        if (!hasAnyPanelIsOpen()) {
            return false
        }
        fun List<View>.handler(): Boolean {
            return all {
                !it.hasTouchNotHideFlag()
            }.takeIf {
                it
            }?.let {
                hideAllPanel()
            } ?: false
        }
        return if (view != null) {
            if (view is ViewGroup && ev != null) {
                view.getTouchTargetViews(ev, true, View.VISIBLE).handler()
            } else {
                listOf(view).handler()
            }
        } else {
            hideAllPanel()
        }
    }

    override fun currentPanelViewId(): Int? {
        if (switchPanelIsOpen) {
            return switchPanel.obtainPanelShowViewId()
        }
        return null
    }

    override fun handlerToggleEvent(v: View, ignoreOnPreHandler: Boolean) {
        checkWarning()
        if (!switchPanelIsOpen) {
            if (ignoreOnPreHandler || togglePreHandleListener?.onClickToggleButton(
                    this, v, keyboardVisible = false, switchPanelVisible = true
                ) != true
            ) {
                switchPanel.markPanelShowViewIdFlag(v.id)
                showSwitchPanel()
            }
        } else {
            if (switchPanel.isCurPanelShowViewId(v)) {
                if (ignoreOnPreHandler || togglePreHandleListener?.onClickToggleButton(
                        this, v, keyboardVisible = true, switchPanelVisible = false
                    ) != true
                ) {
                    showKeyboard()
                }
            } else {
                if (ignoreOnPreHandler || togglePreHandleListener?.onClickToggleButton(
                        this, v, keyboardVisible = false, switchPanelVisible = true
                    ) != true
                ) {
                    switchPanel.markPanelShowViewIdFlag(v.id)
                    keyboardObserver?.onStateChanged(
                        this, keyboardVisible = false, switchPanelVisible = true,
                        orientation = host.resources.configuration.orientation
                    )
                    moreKeyboardObservers?.forEach {
                        it.onStateChanged(
                            this, keyboardVisible = false, switchPanelVisible = true,
                            orientation = host.resources.configuration.orientation
                        )
                    }
                }
            }
        }
    }

    private fun onKeyboardHeightChanged(height: Int, orientation: Int) {
        if (height > 0) {
            keyboardHeight = height
        }
        if (!automaticMode) { // 是否开启自动托管模式
            notifyKeyboardStateChanged(keyboardIsOpen(), switchPanelIsOpen, orientation)
            return
        }
        val open = height > 0
        if (!open && switchPanelIsOpen) { // 键盘切换至表情面板
            adjustInputViewPosition(ACTION_KEYBOARD_TO_PANEL)
            return
        }
        val action = if (height > 0) {
            if (switchPanelIsOpen) {
                ACTION_PANEL_TO_KEYBOARD
            } else {
                ACTION_NONE_TO_KEYBOARD
            }
        } else {
            ACTION_KEYBOARD_TO_NONE
        }
        switchPanelIsOpen = false
        adjustInputViewPosition(action)
    }

    private fun notifyKeyboardStateChanged(
        keyboardVisible: Boolean,
        switchPanelVisible: Boolean,
        orientation: Int
    ) {
        lastNotifyState?.also {
            if (it.first == keyboardVisible && it.second == switchPanelVisible) {
                return
            }
        }
        lastNotifyState = Pair(keyboardVisible, switchPanelVisible)
        keyboardObserver?.onStateChanged(
            this,
            keyboardVisible,
            switchPanelVisible,
            orientation
        )
        moreKeyboardObservers?.forEach {
            it.onStateChanged(
                this,
                keyboardVisible,
                switchPanelVisible,
                orientation
            )
        }
    }

    private fun checkWarning() {
        var error = false
        if (switchPanelIsOpen && keyboardMonitor.isShowKeyboard()) {
            error = true
        }
        switchPanelIsOpen = switchPanel.top < ((keyboardLayout as? View)?.height
            ?: 0) && !keyboardMonitor.isShowKeyboard()
    }

    /**
     *
     * @param mode 1：表示收起面板  2: 表示显示面板  3:  键盘切换至面板 4 键盘操作
     */
    private fun adjustInputViewPosition(@KeyboardChangeAction action: Int) {
        when (action) {
            ACTION_NONE_TO_KEYBOARD, ACTION_NONE_TO_PANEL -> {
                recyclerView?.scrollToBottom()
            }
        }
        if (!switchPanelIsOpen) {
            switchPanel.clearPanelShowViewIdFlag()
        }
        notifyKeyboardStateChanged(
            keyboardIsOpen(),
            switchPanelIsOpen,
            host.resources.configuration.orientation
        )
        Handler(Looper.getMainLooper()).postAtFrontOfQueue {
            keyboardLayout.startLayout(this, action)
        }
    }

    private class KeyboardChangeMonitor(
        private val activity: Activity,
        private val parentView: View
    ) : PopupWindow(activity), ViewTreeObserver.OnGlobalLayoutListener {

        var observer: ((Int, Int) -> Unit)? = null

        private val popupView: View = View(activity)
        private var lastKeyboardHeight = 0
        private var currentKeyboardHeight = 0

        private var originContentHeight = 0

        private val parentVisibleHeight: Int
            get() {
                return Rect().also {
                    parentView.getWindowVisibleDisplayFrame(it)
                }.height()
            }

        init {
            contentView = popupView
            softInputMode =
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            inputMethodMode = INPUT_METHOD_NEEDED
            width = 0
            height = WindowManager.LayoutParams.MATCH_PARENT
            popupView.viewTreeObserver.addOnGlobalLayoutListener(this)
        }

        override fun onGlobalLayout() {
            handleOnGlobalLayout()
        }

        fun isShowKeyboard() = currentKeyboardHeight > 120

        fun lastIsShowKeyboard() = lastKeyboardHeight > 120

        fun start() {
            if (!isShowing && parentView.windowToken != null) {
                setBackgroundDrawable(ColorDrawable(0))
                showAtLocation(parentView, Gravity.NO_GRAVITY, 0, 0)
            }
        }

        fun close() {
            popupView.viewTreeObserver.removeOnGlobalLayoutListener(this)
            observer = null
            dismiss()
        }

        private fun handleOnGlobalLayout() {
            val contentHeight = popupView.height
            if (contentHeight <= 0) {
                return // 忽略无需处理
            }
            val open = if (parentVisibleHeight.minus(contentHeight) <= activity.dip(100f)) {
                originContentHeight = contentHeight
                false
            } else {
                true
            }
            if (open && originContentHeight <= 0) {
                originContentHeight = parentVisibleHeight
            }
            val newHeight = if (open) {
                originContentHeight.minus(contentHeight)
            } else {
                0
            }
            if (currentKeyboardHeight == newHeight) {
                return
            }
            lastKeyboardHeight = currentKeyboardHeight
            currentKeyboardHeight = newHeight
            val orientation = activity.resources.configuration.orientation
            when {
                newHeight < 120 -> {
                    currentKeyboardHeight = 0
                    notifyKeyboardHeightChanged(0, orientation)
                }
                orientation == Configuration.ORIENTATION_PORTRAIT -> {
                    notifyKeyboardHeightChanged(newHeight.coerceAtLeast(0), orientation)
                }
                else -> {
                    notifyKeyboardHeightChanged(newHeight.coerceAtLeast(0), orientation)
                }
            }
        }

        private fun notifyKeyboardHeightChanged(height: Int, orientation: Int) {
            observer?.invoke(height, orientation)
        }

        fun Context.dip(value: Float): Float {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                resources.displayMetrics
            )
        }
    }

}
