package com.example.myapplication.imkeyboard

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

class ClipRecyclerView @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attr, defStyleAttr), IDispatchClipListener {

    override var disableHandler: Boolean = false

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (isDisableTouchEvent()) {
            return false
        }
        return super.dispatchTouchEvent(ev)
    }

}
