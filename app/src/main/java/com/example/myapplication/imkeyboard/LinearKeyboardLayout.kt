package com.example.myapplication.imkeyboard

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.forEachIndexed
import com.example.myapplication.R

const val SCROLL_FLAG_SCROLL = 0
const val SCROLL_FLAG_FIXED = 1

@IntDef(
    SCROLL_FLAG_SCROLL,
    SCROLL_FLAG_FIXED
)
@Retention(AnnotationRetention.SOURCE)
annotation class ScrollFlag

/**
 * TODO: 未完成
 */
@Deprecated("待完成")
class LinearKeyboardLayout @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayoutCompat(context, attr, defStyleAttr), IKeyboardLayout {

    override var contentView: View? = null

    override lateinit var panelView: View

    override var lastState: KeyboardPanelState = KeyboardPanelState.KEYBOARD_PANEL_STATE_NONE

    override var curState: KeyboardPanelState = KeyboardPanelState.KEYBOARD_PANEL_STATE_NONE

    override var keyboardHelper: KeyboardHelper? = null

    private val levelSort = mutableListOf<Level>()

    init {
        isChildrenDrawingOrderEnabled = true
        orientation = VERTICAL
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        assertView()
    }

    override fun setOrientation(orientation: Int) {
        if (orientation == HORIZONTAL) {
            throw IllegalArgumentException("")
        }
        super.setOrientation(orientation)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        val mContentView= contentView ?: return
        val h = contentFixedHeight
        when (curState) {
            KeyboardPanelState.KEYBOARD_PANEL_STATE_NONE -> {

                if (panelView.visibility == View.GONE && h == 0) {
                    updateContentFixedHeight(mContentView.height)
                    panelView.post {
                        panelView.visibility = View.VISIBLE
                    }
                } else if (h == 0) {
                    panelView.apply {
                        post {
                            visibility = View.GONE
                        }
                    }
                } else {
                    val offsetY = mContentView.top.plus(h).minus(mContentView.bottom)
                    mContentView.apply {
                        layout(left, top, right, top.plus(h))
                    }
                    val start = indexOfChild(mContentView).plus(1)
                    if (start != 0) {
                        for (index in start until childCount) {
                            getChildAt(index)?.setOffsetFrame(offsetY)
                        }
                    }
                }
            }
            KeyboardPanelState.KEYBOARD_PANEL_STATE_PANEL -> {
                if (h != 0) {
                    mContentView.apply {
                        layout(left, bottom.minus(h), right, bottom)
                    }
                }
            }
            else -> {
                if (h != 0) {
                    mContentView.apply {
                        layout(left, bottom.minus(h), right, bottom)
                    }
                }
            }
        }
//        if (curState != KeyboardPanelState.KEYBOARD_PANEL_STATE_NONE) {
//
//        }
        getChildAt(1)?.apply {
            layout(left, top.minus(20), right, bottom.minus(20))
        }
    }

    override fun getChildDrawingOrder(childCount: Int, drawingPosition: Int): Int {
//        if (drawingPosition == 0) {
//            return childCount.minus(1)
//        }
//        if (drawingPosition == childCount.minus(1)) {
//            return 0
//        }
        return levelSort.getOrNull(drawingPosition)?.takeIf {
            levelSort.size == childCount
        }?.index ?: super.getChildDrawingOrder(childCount, drawingPosition)
    }

    override fun startLayout(provider: IKeyboardStateManger, action: Int) {
        super.startLayout(provider, action)
        levelSort.clear()
        forEachIndexed { index, view ->
            val lp = view.layoutParams as LayoutParams
            levelSort.add(Level(index, lp.drawLevel))
        }
        levelSort.sortWith(compareBy({ it.level }, { it.index }))
        requestLayout()
    }


    private fun View.setOffsetFrame(offsetY: Int) {
        if (visibility == View.GONE) {
            return
        }
        layout(left, top.plus(offsetY), right, bottom.plus(offsetY))
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
        return LayoutParams(p)
    }

    private data class Level(
        val index: Int,
        val level: Int
    )

    class LayoutParams : LinearLayoutCompat.LayoutParams, IKeyboardLayoutParams {

        @KeyboardType
        override var keyboardType: Int = KEYBOARD_TYPE_NONE

        @ScrollFlag
        var scrollFlag: Int = SCROLL_FLAG_SCROLL

        var drawLevel: Int = 0

        constructor(c: Context, attrs: AttributeSet?) : super(c, attrs) {
            val typeArray =
                c.obtainStyledAttributes(attrs, R.styleable.LinearKeyboardLayout_Layout)
            keyboardType = typeArray.getInt(
                R.styleable.LinearKeyboardLayout_Layout_layout_keyboard_type,
                KEYBOARD_TYPE_NONE
            )
            scrollFlag = typeArray.getInt(
                R.styleable.LinearKeyboardLayout_Layout_layout_scroll_flag,
                SCROLL_FLAG_SCROLL
            )
            drawLevel = typeArray.getInt(
                R.styleable.LinearKeyboardLayout_Layout_layout_draw_level,
                10
            )
            typeArray.recycle()
        }

        constructor(width: Int, height: Int) : super(width, height)

        constructor(source: MarginLayoutParams?) : super(source)

        constructor(source: ViewGroup.LayoutParams?) : super(source)
    }
}
