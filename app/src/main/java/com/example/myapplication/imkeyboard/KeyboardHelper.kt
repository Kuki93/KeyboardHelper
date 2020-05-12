package com.example.myapplication.imkeyboard

import android.R
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
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
        scrollToPosition(position)
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
fun openKeyboard(mEditText: EditText?, mContext: Context) {
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
fun closeKeyboard(mEditText: EditText, mContext: Context) {
    val imm =
        mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(mEditText.windowToken, 0)
}


interface KeyboardHeightObserver {
    fun onKeyboardHeightChanged(height: Int, orientation: Int)
}

interface EmojiPanelStateObserver {
    fun onEmojiPanelStateChanged(open: Boolean)
}

/**
 * 微信键盘处理
 */
class KeyboardHelper(
    private val host: AppCompatActivity,
    keyboardObserver: KeyboardHeightObserver? = null,
    emojiPanelObserver: EmojiPanelStateObserver? = null
) {

    var keyboardObserver: KeyboardHeightObserver? = null

    var emojiPanelObserver: EmojiPanelStateObserver? = null

    private lateinit var provider: KeyboardHeightProvider

    private var smartEscrow = false

    private lateinit var inputView: EditText

    private lateinit var emojiView: View

    private var emojiPanelHeight: Int = 0

    private var openEmojiPanel = false

    companion object {
        private const val TAG = "KeyboardHeightProvider"
        private const val ADJUST_ANIMATOR_DURATION = 80L
    }

    init {
        this.keyboardObserver = keyboardObserver
        this.emojiPanelObserver = emojiPanelObserver
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
                if (!hideKeyboard()) {
                    hideEmojiPanel()
                }
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
     * 开启智能托管模式
     * 该模式对布局有要求，只支持特定的布局，windowInputMode会被自动设置为adjustNothing
     */
    fun startSmartEscrow(
        inputView: EditText, // 输入框
        emojiView: View,  // 表情面板
        toggleButton: View,  // 切换按钮
        autoScrollToBottom: RecyclerView? = null // 弹出面板或键盘自动滚动到底部，适合im
    ) {
        this.inputView = inputView
        this.emojiView = emojiView
        smartEscrow = true
        emojiPanelHeight = emojiView.height
        emojiView.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
            emojiPanelHeight = bottom.minus(top)
        }
        inputView.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                when (event.action) {
                    KeyEvent.ACTION_UP -> {
                        return@setOnKeyListener hideEmojiPanel()
                    }
                }
            }
            return@setOnKeyListener false
        }
        emojiView.visibility = View.INVISIBLE
        toggleButton.setOnClickListener {
            checkWarning()
            updateEmojiPanelState(!openEmojiPanel)
            if (openEmojiPanel) {
                emojiView.visibility = View.VISIBLE
                if (provider.isShowKeyboard()) {
                    // 隐藏键盘
                    closeKeyboard(inputView, host)
                } else {
                    adjustInputViewPosition(emojiPanelHeight)
                }
            } else {
                if (!provider.isShowKeyboard()) {
                    // 开启键盘
                    openKeyboard(inputView, host)
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
                    if (!hideKeyboard()) {
                        hideEmojiPanel()
                    }
                }
                return@setOnTouchListener false
            }
        }
    }

    fun onKeyboardObserver(observer: ((Int, Int) -> Unit)?) {
        if (observer == null) {
            this.keyboardObserver = null
            return
        }
        this.keyboardObserver = object : KeyboardHeightObserver {
            override fun onKeyboardHeightChanged(height: Int, orientation: Int) {
                observer(height, orientation)
            }
        }
    }

    fun onEmojiPanelObserver(observer: ((Boolean) -> Unit)?) {
        if (observer == null) {
            this.emojiPanelObserver = null
            return
        }
        this.emojiPanelObserver = object : EmojiPanelStateObserver {
            override fun onEmojiPanelStateChanged(open: Boolean) {
                observer(open)
            }
        }
    }

    fun hideEmojiPanel(): Boolean {
        if (openEmojiPanel) {
            adjustInputViewPosition(0)
            updateEmojiPanelState(false)
            return true
        }
        return false
    }

    fun hideKeyboard(): Boolean {
        if (provider.isShowKeyboard()) {
            closeKeyboard(inputView, host)
            return true
        }
        return false
    }

    private fun onKeyboardHeightChanged(height: Int, orientation: Int) {
        keyboardObserver?.onKeyboardHeightChanged(height, orientation)
        if (!smartEscrow) { // 是否开启自动托管模式
            return
        }
        val open = height > 0
        if (!open && openEmojiPanel) { // 键盘切换至表情面板
            adjustInputViewPosition(emojiPanelHeight)
            return
        }
        adjustInputViewPosition(height)
        updateEmojiPanelState(false)
    }

    private fun updateEmojiPanelState(newState: Boolean) {
        if (openEmojiPanel == newState) {
            return
        }
        openEmojiPanel = newState
        emojiPanelObserver?.onEmojiPanelStateChanged(openEmojiPanel)
    }

    private fun checkWarning() {
        var error = false
        if (openEmojiPanel && provider.isShowKeyboard()) {
            error = true
            Log.w(TAG, "状态异常，键盘和面板同时显示，尝试修复")
        }
        updateEmojiPanelState(inputView.marginBottom > 0 && !provider.isShowKeyboard())
        if (error && !(openEmojiPanel && provider.isShowKeyboard())) {
            Log.i(TAG, "修复成功")
        }
    }

    private fun adjustInputViewPosition(height: Int, useAnim: Boolean = true) {
        val marginLp = inputView.layoutParams as ViewGroup.MarginLayoutParams
        if (marginLp.bottomMargin == height) {
            return
        }

        fun completeAction() {
            marginLp.bottomMargin = height
            inputView.requestLayout()
            if (!openEmojiPanel) {
                emojiView.visibility = View.INVISIBLE
            }
        }

        if (!useAnim) {
            completeAction()
            return
        }

        ValueAnimator.ofInt(marginLp.bottomMargin, height).apply {
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

