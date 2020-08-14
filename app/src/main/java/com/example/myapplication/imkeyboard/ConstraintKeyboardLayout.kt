package com.example.myapplication.imkeyboard

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.*
import com.example.myapplication.R
import kotlinx.android.synthetic.main.activity_keyboard3.*

/**
 * 我这个键盘表情切换，比@文哥的更平滑
 *
 * @see cn.neoclub.uki.widget.panel.PanelRootConstraintLayout
 * @see cn.neoclub.uki.widget.panel.PanelFrameLayout
 */
open class ConstraintKeyboardLayout @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attr, defStyleAttr), IKeyboardLayout {

    private val layoutTransition = AutoTransition().apply {
        ordering = TransitionSet.ORDERING_TOGETHER
    }

    private val expandToKeyboardSet = ConstraintSet()

    private val expandToPanelSet = ConstraintSet()

    private val collapseSet = ConstraintSet()

    private val fixedContent: Boolean

    private val clipContent: Boolean

    private val autoMode: Boolean

    private val autoCollapse: Boolean

    private var fixedContentHeight = 0

    private var contentClipRect: Rect? = null

    private var helper: View? = null

    private var autoReferenceId = View.NO_ID

    private var toggleStringIds = mutableListOf<String>()

    private var toggleIds = mutableListOf<Int>()

    private var resizePadding = 0

    override var contentView: View? = null

    override lateinit var panelView: View

    override var lastState: KeyboardPanelState = KeyboardPanelState.KEYBOARD_PANEL_STATE_NONE

    override var curState: KeyboardPanelState = KeyboardPanelState.KEYBOARD_PANEL_STATE_NONE

    override var keyboardHelper: KeyboardHelper? =
        (context as? Activity)?.let { KeyboardHelper.from(it) }

    init {
        val typedArray = context.obtainStyledAttributes(attr, R.styleable.ConstraintKeyboardLayout)

        autoCollapse = typedArray.getBoolean(
            R.styleable.ConstraintKeyboardLayout_keyboard_auto_collapse_keyboard,
            false
        )

        autoReferenceId = typedArray.getResourceId(
            R.styleable.ConstraintKeyboardLayout_keyboard_auto_collapse_above_view_top_id,
            View.NO_ID
        )

        fixedContent =
            typedArray.getBoolean(R.styleable.ConstraintKeyboardLayout_keyboard_fixed_content, true)
        clipContent =
            typedArray.getBoolean(R.styleable.ConstraintKeyboardLayout_keyboard_clip_content, true)

        typedArray.getResourceId(
            R.styleable.ConstraintKeyboardLayout_keyboard_expand_to_keyboard_constraint,
            -1
        ).takeIf {
            it > 0
        }?.also {
            expandToKeyboardSet.load(context, it)
        }

        typedArray.getResourceId(
            R.styleable.ConstraintKeyboardLayout_keyboard_expand_to_panel_constraint,
            -1
        ).takeIf {
            it > 0
        }?.also {
            expandToPanelSet.load(context, it)
        }

        typedArray.getResourceId(
            R.styleable.ConstraintKeyboardLayout_keyboard_collapse_constraint,
            -1
        ).takeIf {
            it > 0
        }?.also {
            collapseSet.load(context, it)
        }

        autoMode =
            typedArray.getBoolean(R.styleable.ConstraintKeyboardLayout_keyboard_auto_mode, true)

        typedArray.getString(R.styleable.ConstraintKeyboardLayout_keyboard_toggleIds)?.takeIf {
            it.isNotEmpty()
        }?.also {
            var begin = 0
            while (true) {
                val end: Int = it.indexOf(44.toChar(), begin)
                if (end == -1) {
                    toggleStringIds.add(it.substring(begin))
                    break
                }
                toggleStringIds.add(it.substring(begin, end))
                begin = end + 1
            }
        }

        typedArray.recycle()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        toggleIds.clear()
        toggleStringIds.forEach {
            var rscId = findId(it)
            if (rscId == 0) {
                rscId = context.resources.getIdentifier(it, "id", context.packageName)
            }
            if (rscId != 0) {
                toggleIds.add(rscId)
            }
        }
        if (autoMode) {
            keyboardHelper?.startAutomaticMode(this)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            if (autoCollapse && keyboardHelper?.hasAnyPanelIsOpen() == true) {
                if (takeIf {
                        autoReferenceId != View.NO_ID
                    }?.let {
                        getViewById(autoReferenceId)?.top
                    }?.takeIf {
                        ev.y >= it
                    } == null) {
                    if (keyboardHelper?.collapseAnyPanel(this, ev) == true) {
                        return true
                    }
                }
            }
            // 防止被clipRect裁剪的contentView的被裁剪区域响应事件
            if (!clipContent || contentClipRect == null) {
                return super.dispatchTouchEvent(ev)
            }
            val mContentView = contentView
            mContentView ?: return super.dispatchTouchEvent(ev)
            val x = ev.x.toInt()
            val y = ev.y.toInt()
            contentClipRect?.takeIf {
                Rect(
                    mContentView.left,
                    mContentView.top,
                    mContentView.right,
                    mContentView.bottom
                ).contains(x, y) && !it.contains(x, y)
            }?.also {
                (mContentView as? IDispatchClipListener)?.disableHandler = true
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        assertView { type, v ->
            if (type == KEYBOARD_TYPE_HELPER) {
                helper = v
            }
        }
        if (autoReferenceId == View.NO_ID) {
            providerInputView()?.also {
                autoReferenceId = it.id
            }
        }
        contentView?.also { view ->
            layoutTransition.addTransition(PaddingTransition().also {
                it.addTarget(view)
            })
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        fixedLayoutParamHeight()
        changeClipRect()
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        if (!clipContent || contentClipRect == null) {
            return super.drawChild(canvas, child, drawingTime)
        }
        if (curState == KeyboardPanelState.KEYBOARD_PANEL_STATE_NONE && lastState == curState) {
            return super.drawChild(canvas, child, drawingTime)
        }
        val clipRect = contentClipRect
        return if (child == contentView && clipRect != null) {
            canvas.save()
            canvas.clipRect(clipRect)
            super.drawChild(canvas, child, drawingTime).also {
                canvas.restore()
            }
        } else {
            super.drawChild(canvas, child, drawingTime)
        }
    }

    override fun canAutoCollapse(): Boolean {
        return autoCollapse
    }

    override fun providerToggleViews(): List<View>? {
        if (toggleIds.isEmpty()) {
            return super.providerToggleViews()
        }
        val result = mutableSetOf<View>()
        toggleIds.forEach {
            findViewById<View>(it)?.also { target ->
                result.add(target)
            }
        }
        super.providerToggleViews()?.also {
            result.addAll(it)
        }
        return result.toList()
    }

    override fun startLayout(provider: IKeyboardStateManger, @KeyboardChangeAction action: Int) {
        super.startLayout(provider, action)
        var duration = 0L
        var interpolator: TimeInterpolator? = null
        var applyConstraintSet: ConstraintSet? = null
        when (action) {
            ACTION_KEYBOARD_TO_NONE, ACTION_PANEL_TO_NONE -> {
                duration = if (action == ACTION_KEYBOARD_TO_NONE) {
                    interpolator = DecelerateInterpolator(2.0f)
                    180
                } else {
                    interpolator = DecelerateInterpolator(1.2f)
                    230
                }
                applyConstraintSet = collapseSet
            }
            ACTION_NONE_TO_PANEL, ACTION_KEYBOARD_TO_PANEL -> {
                duration = if (action == ACTION_KEYBOARD_TO_PANEL) {
                    interpolator = LinearInterpolator()
                    210
                } else {
                    interpolator = DecelerateInterpolator(1.2f)
                    230
                }
                applyConstraintSet = expandToPanelSet
            }
            ACTION_NONE_TO_KEYBOARD, ACTION_PANEL_TO_KEYBOARD -> {
                if (action == ACTION_PANEL_TO_KEYBOARD && provider.getKeyBoardHeight()
                        .plus(panelView.top) == height
                ) {
                    return
                }
                helper?.also {
                    expandToKeyboardSet.setMargin(
                        panelView.id,
                        ConstraintSet.TOP,
                        height.minus(it.bottom).minus(provider.getKeyBoardHeight())
                    )
                }
                duration = if (action == ACTION_NONE_TO_KEYBOARD) {
                    interpolator = AccelerateDecelerateInterpolator()
                    180
                } else {
                    interpolator = LinearInterpolator()
                    210
                }
                applyConstraintSet = expandToKeyboardSet
            }
        }
        val constraintSet = applyConstraintSet
        constraintSet ?: return

        contentView?.takeIf {
            fixedContent && constraintSet.getHeight(it.id) != fixedContentHeight
        }?.also {
            constraintSet.constrainHeight(it.id, fixedContentHeight)
        }

        if (lastState != curState) {
            layoutTransition.duration = duration
            layoutTransition.interpolator = interpolator
            TransitionManager.beginDelayedTransition(this, layoutTransition)
        } else if (
            allEquals(
                KeyboardPanelState.KEYBOARD_PANEL_STATE_KEYBOARD, lastState, curState
            )
        ) {
            layoutTransition.duration = 125
            layoutTransition.interpolator = LinearInterpolator()
            TransitionManager.beginDelayedTransition(this, layoutTransition)
        }


        contentView?.takeIf {
            ((it as? RecyclerView)?.layoutManager as? LinearLayoutManager)?.itemCount ?: 0 < 10
        }?.apply {
            val topPadding = when (curState) {
                KeyboardPanelState.KEYBOARD_PANEL_STATE_NONE -> 0
                KeyboardPanelState.KEYBOARD_PANEL_STATE_KEYBOARD -> provider.getKeyBoardHeight()
                KeyboardPanelState.KEYBOARD_PANEL_STATE_PANEL -> provider.getSwitchPanelHeight()
            }.let {
                paddingTop.minus(resizePadding).plus(it).apply {
                    resizePadding = it
                }
            }
            updatePadding(top = topPadding)
        }

        constraintSet.applyToWithoutCustom(this)
    }

    private fun fixedLayoutParamHeight() {
        contentView.takeIf {
            fixedContent && fixedContentHeight == 0
        }?.apply {
            fixedContentHeight = height
            requestLayout()
        }
    }

    private fun changeClipRect() {
        contentView.takeIf {
            clipContent && contentClipRect == null
        }?.apply {
            contentClipRect = Rect(left, top, right, bottom)
        }
    }

    private fun findId(idString: String): Int {
        return if (idString.isNotEmpty()) {
            val resources = this.resources
            if (resources == null) {
                0
            } else {
                val count = childCount
                for (j in 0 until count) {
                    val child = getChildAt(j)
                    if (child.id != -1) {
                        var res: String? = null
                        try {
                            res = resources.getResourceEntryName(child.id)
                        } catch (var9: Resources.NotFoundException) {
                        }
                        if (idString == res) {
                            return child.id
                        }
                    }
                }
                0
            }
        } else {
            0
        }
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is LayoutParams
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): LayoutParams {
        if (p is ConstraintLayout.LayoutParams) {
            return LayoutParams(p)
        }
        return LayoutParams(p)
    }

    class LayoutParams : ConstraintLayout.LayoutParams, IKeyboardLayoutParams {

        @KeyboardType
        override var keyboardType: Int = KEYBOARD_TYPE_NONE

        constructor(c: Context, attrs: AttributeSet?) : super(c, attrs) {
            val typeArray =
                c.obtainStyledAttributes(attrs, R.styleable.ConstraintKeyboardLayout_Layout)
            keyboardType = typeArray.getInt(
                R.styleable.ConstraintKeyboardLayout_Layout_layout_keyboard_type,
                KEYBOARD_TYPE_NONE
            )
            typeArray.recycle()
        }

        constructor(width: Int, height: Int) : super(width, height)

        constructor(source: ViewGroup.LayoutParams?) : super(source)

        constructor(source: ConstraintLayout.LayoutParams?) : super(source)
    }

}
