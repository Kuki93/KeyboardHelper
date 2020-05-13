package com.example.myapplication.imkeyboard

import android.R
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.view.marginBottom
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs


private const val MAX_SMOOTH_POSITION = 6
private const val SMOOTH_POSITION = 4

/**
 * 提供一个快速平滑的滚动动画
 * [position] 位置
 */
fun RecyclerView.fixSmoothScrollToPosition(position: Int) {
    val layoutManager = layoutManager as? LinearLayoutManager
    layoutManager ?: kotlin.run {
        smoothScrollToPosition(position)
        return
    }

    val firstOffset = position.minus(layoutManager.findFirstVisibleItemPosition())
    val lastOffset = position.minus(layoutManager.findLastVisibleItemPosition())

    fun postScrollTo(offsetPosition: Int) {
        if (abs(offsetPosition) < MAX_SMOOTH_POSITION) {
            smoothScrollToPosition(position)
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
            smoothScrollToPosition(position)
        }
    }
}

/**
 * 打开软键盘
 */
fun openKeyboard(mEditText: View?, mContext: Activity) {
    val imm =
        mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(mEditText, InputMethodManager.RESULT_SHOWN)
    imm.toggleSoftInput(
        InputMethodManager.SHOW_FORCED,
        InputMethodManager.HIDE_IMPLICIT_ONLY
    )
}

/**
 * 关闭软键盘
 */
fun closeKeyboard(mEditText: View, mContext: Activity) {
    val imm =
        mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(mEditText.windowToken, 0)
}


interface KeyboardStateObserver {
    fun onStateChanged(
        keyboardState: Boolean,
        switchPanelState: Boolean,
        orientation: Int
    )
}

/**
 * 微信键盘处理
 */
class KeyboardHelper @JvmOverloads constructor(
    private val host: AppCompatActivity,
    var keyboardObserver: KeyboardStateObserver? = null
) {

    @JvmOverloads
    constructor(host: AppCompatActivity, observer: ((Boolean, Boolean, Int) -> Unit)?)
            : this(host) {
        setOnKeyboardObserver(observer)
    }

    private lateinit var provider: KeyboardHeightProvider

    private var smartEscrow = false

    private lateinit var inputView: View

    private lateinit var switchView: View

    private var switchPanelHeight: Int = 0

    private var switchPanelIsOpen = false

    private var defaultMarginBottom = 0

    private var lastNotifyState: Triple<Boolean, Boolean, Int>? = null

    private var marginMode = 0

    private var marginFixStrategy: ((Int) -> Int) = { height ->
        if (marginMode == 0) {
            height.plus(getInputViewMarginBottom())
        } else {
            height.coerceAtLeast(getInputViewMarginBottom())
        }
    }

    private var viewMarginBottomFunc: (() -> Int)? = null

    companion object {
        private const val TAG = "KeyboardHeightProvider"
        private const val ADJUST_ANIMATOR_DURATION = 80L
    }

    init {
        val observer = { height: Int, orientation: Int ->
            onKeyboardHeightChanged(height, orientation)
        }
        host.lifecycle.addObserver(object : LifecycleObserver {

            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun onCreate(owner: LifecycleOwner) {
                host.window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                            or WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                )
                val parentView = host.findViewById<View>(R.id.content)
                provider = KeyboardHeightProvider(host, parentView)
                parentView?.post {
                    provider.start()
                }
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            fun onStart(owner: LifecycleOwner) {
                provider.setKeyboardHeightObserver(observer)
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            fun onStop(owner: LifecycleOwner) {
                provider.setKeyboardHeightObserver(null)
                hideAllView()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy(owner: LifecycleOwner) {
                smartEscrow = false
                provider.setKeyboardHeightObserver(null)
                provider.close()
            }
        })
    }

    /**
     * 一般情况下[inputView]的[bottomMargin]默认不会因其他业务需求发生变化
     * 该方法为了满足此种情况的特殊需求
     *
     * [marginBottomFunc] 默认全部收起时[inputView]的[bottomMargin]，详见[getInputViewMarginBottom]
     * [marginMode] 提供了两种兼容模式，详见默认的[KeyboardHelper.marginFixStrategy]
     * [marginFixStrategy] 如果[marginMode]满足不了你的需求，可以自定义策略
     */
    @JvmOverloads
    fun setMarginFixStrategy(
        marginMode: Int = 0,
        marginBottomFunc: (() -> Int)? = null,
        marginFixStrategy: ((Int) -> Int)? = null
    ) {
        if (smartEscrow) {
            return
        }
        this.marginMode = marginMode
        this.viewMarginBottomFunc = marginBottomFunc
        marginFixStrategy?.also {
            this.marginFixStrategy = it
        }
    }

    /**
     * 开启智能托管模式
     * 该模式对布局有要求，只支持特定的布局，windowInputMode会被自动设置为adjustNothing
     * 布局可参考[activity_keyboard]
     *
     * 原理是通过监听软键盘的显示和隐藏，通过动画改变[inputView]的[bottomMargin]去刷新位置
     * [switchView]依赖[inputView]的位置
     * [autoScrollToBottom]的高度依赖[inputView]的位置
     *
     * 故通过更新[inputView]的位置，完成整个视图更新
     *
     * [inputView] 可以被软键盘和面板顶起的view, 一般是输入框所在的布局
     * [switchView] 切换面板view
     * [toggleButton] 切换按钮view
     * [autoScrollToBottom] 弹出面板或键盘自动滚动到底部，适合im
     *
     */
    @JvmOverloads
    fun startSmartEscrow(
        inputView: View,
        switchView: View,
        toggleButton: View,
        autoScrollToBottom: RecyclerView? = null
    ) {
        if (smartEscrow) {
            return
        }
        smartEscrow = true
        this.inputView = inputView
        this.switchView = switchView
        defaultMarginBottom = inputView.marginBottom
        switchPanelHeight = switchView.height
        switchView.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
            switchPanelHeight = bottom.minus(top)
        }
        inputView.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                when (event.action) {
                    KeyEvent.ACTION_UP -> {
                        return@setOnKeyListener hideSwitchPanel()
                    }
                }
            }
            return@setOnKeyListener false
        }

        switchView.visibility = View.INVISIBLE
        toggleButton.setOnClickListener {
            checkWarning()
            if (!switchPanelIsOpen) {
                showSwitchPanel()
            } else {
                val result = showKeyboard()
                if (!result) {
                    Log.w(TAG, "这绝不可能")
                }
            }
        }

        autoScrollToBottom?.also {
            it.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
                if (oldBottom > bottom) {
                    (it.layoutManager as? LinearLayoutManager)?.let { layoutManager ->
                        if (layoutManager.reverseLayout) {
                            0
                        } else {
                            layoutManager.itemCount.minus(1)
                        }
                    }?.also { position ->
                        it.scrollToPosition(position)
//                        it.fixSmoothScrollToPosition(position)
                    }
                }
            }

            it.setOnTouchListener { v, event ->
                if (
                    event.action == MotionEvent.ACTION_UP ||
                    event.action == MotionEvent.ACTION_CANCEL
                ) {
                    hideAllView()
                }
                return@setOnTouchListener false
            }
        }
    }

    fun setOnKeyboardObserver(observer: ((Boolean, Boolean, Int) -> Unit)?) {
        if (observer == null) {
            this.keyboardObserver = null
            return
        }
        this.keyboardObserver = object : KeyboardStateObserver {
            override fun onStateChanged(
                keyboardState: Boolean,
                switchPanelState: Boolean,
                orientation: Int
            ) {
                observer(keyboardState, switchPanelState, orientation)
            }
        }
    }

    /**
     * 面板是否展开
     */
    fun switchIsOpen(): Boolean {
        return switchPanelIsOpen
    }

    /**
     * 软键盘是否展开
     */
    fun keyboardIsOpen(): Boolean {
        return provider.isShowKeyboard()
    }

    /**
     * 软键盘或面板是否展开
     */
    fun hasOpenView(): Boolean {
        return switchPanelIsOpen || provider.isShowKeyboard()
    }

    /**
     * 展开面板
     *
     * @return 表示true成功，否则表示不需要展开
     */
    fun showSwitchPanel(): Boolean {
        if (switchPanelIsOpen) {
            return false
        }
        switchPanelIsOpen = true
        switchView.visibility = View.VISIBLE
        if (provider.isShowKeyboard()) {
            // 隐藏键盘
            closeKeyboard(inputView, host)
        } else {
            adjustInputViewPosition(switchPanelHeight)
        }
        return true
    }

    /**
     * 展开软键盘
     *
     * @return 表示true成功，否则表示不需要展开
     */
    fun showKeyboard(): Boolean {
        if (provider.isShowKeyboard()) {
            return false
        }
        switchPanelIsOpen = false
        // 开启键盘
        openKeyboard(inputView, host)
        return true
    }

    /**
     * 隐藏面板
     *
     * @return 表示true成功，否则表示不需要隐藏
     */
    fun hideSwitchPanel(): Boolean {
        if (!switchPanelIsOpen) {
            return false
        }
        switchPanelIsOpen = false
        adjustInputViewPosition(0)
        return true
    }

    /**
     * 隐藏软键盘
     *
     * @return 表示true成功，否则表示不需要隐藏
     */
    fun hideKeyboard(): Boolean {
        if (!provider.isShowKeyboard()) {
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
    fun hideAllView(): Boolean {
        if (!hideKeyboard()) {
            return hideSwitchPanel()
        }
        return true
    }

    private fun onKeyboardHeightChanged(height: Int, orientation: Int) {
        if (!smartEscrow) { // 是否开启自动托管模式
            notifyKeyboardStateChanged(height > 0, switchPanelIsOpen, orientation)
            return
        }
        val open = height > 0
        if (!open && switchPanelIsOpen) { // 键盘切换至表情面板
            adjustInputViewPosition(switchPanelHeight)
            return
        }
        switchPanelIsOpen = false
        adjustInputViewPosition(height)
    }

    private fun notifyKeyboardStateChanged(
        keyboardState: Boolean, switchPanelState: Boolean, orientation: Int
    ) {
        lastNotifyState?.also {
            if (it.first == keyboardState && it.second == switchPanelState && it.third == orientation) {
                return
            }
        }
        lastNotifyState = Triple(keyboardState, switchPanelState, orientation)
        keyboardObserver?.onStateChanged(keyboardState, switchPanelState, orientation)
    }

    private fun checkWarning() {
        var error = false
        if (switchPanelIsOpen && provider.isShowKeyboard()) {
            error = true
            Log.w(TAG, "状态异常，键盘和面板同时显示，尝试修复")
        }
        switchPanelIsOpen =
            inputView.marginBottom > getInputViewMarginBottom() && !provider.isShowKeyboard()
        if (error && !(switchPanelIsOpen && provider.isShowKeyboard())) {
            Log.i(TAG, "修复成功")
        }
    }

    private fun adjustInputViewPosition(height: Int) {

        val fixMargin = marginFixStrategy.invoke(height)

        val marginLp = inputView.layoutParams as ViewGroup.MarginLayoutParams
        if (marginLp.bottomMargin == fixMargin) {
            notifyKeyboardStateChanged(
                provider.isShowKeyboard(),
                switchPanelIsOpen,
                host.resources.configuration.orientation
            )
            return
        }

        fun completeAction() {
            marginLp.bottomMargin = fixMargin
            inputView.requestLayout()
            if (!switchPanelIsOpen) {
                switchView.visibility = View.INVISIBLE
            }
            notifyKeyboardStateChanged(
                provider.isShowKeyboard(),
                switchPanelIsOpen,
                host.resources.configuration.orientation
            )
        }

        ValueAnimator.ofInt(marginLp.bottomMargin, fixMargin).apply {
            duration = ADJUST_ANIMATOR_DURATION
            addUpdateListener {
                marginLp.bottomMargin = it.animatedValue as Int
                inputView.requestLayout()
            }
            doOnEnd {
                completeAction()
            }
            start()
        }
    }

    private fun getInputViewMarginBottom(): Int {
        return viewMarginBottomFunc?.invoke() ?: defaultMarginBottom
    }

    private class KeyboardHeightProvider(
        private val activity: AppCompatActivity,
        private val parentView: View
    ) : PopupWindow(activity), ViewTreeObserver.OnGlobalLayoutListener {

        private val popupView: View =
            LayoutInflater.from(activity).inflate(R.layout.test_list_item, null, false)
        private var observer: ((Int, Int) -> Unit)? = null
        private var keyboardLandscapeHeight = 0
        private var keyboardPortraitHeight = 0
        private var lastKeyboardHeight = -1

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

        fun isShowKeyboard() = lastKeyboardHeight > 0

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

        fun setKeyboardHeightObserver(observer: ((Int, Int) -> Unit)?) {
            this.observer = observer
        }

        private fun handleOnGlobalLayout() {
            val screenSize = Point()
            activity.windowManager.defaultDisplay.getSize(screenSize)
            val rect = Rect()
            popupView.getWindowVisibleDisplayFrame(rect)
            val orientation = activity.resources.configuration.orientation
            val keyboardHeight = screenSize.y - rect.bottom
            if (lastKeyboardHeight == keyboardHeight) {
                return
            }
            when {
                keyboardHeight == 0 -> {
                    notifyKeyboardHeightChanged(0, orientation)
                }
                orientation == Configuration.ORIENTATION_PORTRAIT -> {
                    keyboardPortraitHeight = keyboardHeight
                    notifyKeyboardHeightChanged(keyboardPortraitHeight, orientation)
                }
                else -> {
                    keyboardLandscapeHeight = keyboardHeight
                    notifyKeyboardHeightChanged(keyboardLandscapeHeight, orientation)
                }
            }
            lastKeyboardHeight = keyboardHeight
        }

        private fun notifyKeyboardHeightChanged(height: Int, orientation: Int) {
            val orientationLabel =
                if (orientation == Configuration.ORIENTATION_PORTRAIT) "portrait" else "landscape"
            Log.i(TAG, "onKeyboardHeightChanged in pixels: $height $orientationLabel")
            observer?.invoke(height, orientation)
        }
    }
}

