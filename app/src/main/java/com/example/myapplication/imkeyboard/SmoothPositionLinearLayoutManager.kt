package com.example.myapplication.imkeyboard

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

class SmoothPositionLinearLayoutManager : LinearLayoutManager {

    constructor(context: Context?) : super(context)

    constructor(
        context: Context?,
        orientation: Int,
        reverseLayout: Boolean
    ) : super(context, orientation, reverseLayout)

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView,
        state: RecyclerView.State,
        position: Int
    ) {
        val topSnappedSmoothScroller = TopSnappedSmoothScroller(recyclerView.context)
        topSnappedSmoothScroller.targetPosition = position
        startSmoothScroll(topSnappedSmoothScroller)
    }

    internal inner class TopSnappedSmoothScroller(context: Context?) :
        LinearSmoothScroller(context) {

        override fun calculateTimeForDeceleration(dx: Int): Int {
            return 250
        }

        override fun getVerticalSnapPreference(): Int {
            return SNAP_TO_END //设置滚动位置
        }
    }
}
