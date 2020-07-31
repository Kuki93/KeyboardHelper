package com.example.myapplication.imkeyboard

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout

/**
 * TODO: 未完成
 */
@Deprecated("待完成")
class RelativeKeyboardLayout @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attr, defStyleAttr), IKeyboardLayout {

    override var contentView: View? = null

    override lateinit var panelView: View

    override var lastState: KeyboardPanelState = KeyboardPanelState.KEYBOARD_PANEL_STATE_NONE

    override var curState: KeyboardPanelState = KeyboardPanelState.KEYBOARD_PANEL_STATE_NONE

    override var keyboardHelper: KeyboardHelper? = null

    init {
        isChildrenDrawingOrderEnabled = true
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        getChildAt(1)?.apply {
            layout(left, top.plus(150), right, bottom.plus(150))
        }
    }

    override fun getChildDrawingOrder(childCount: Int, drawingPosition: Int): Int {
        if (drawingPosition == 0) {
            return childCount.minus(1)
        }
        if (drawingPosition == childCount.minus(1)) {
            return 0
        }
        return super.getChildDrawingOrder(childCount, drawingPosition)
    }
}
