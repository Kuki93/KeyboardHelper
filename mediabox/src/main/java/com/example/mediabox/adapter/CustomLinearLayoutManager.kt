package com.example.mediabox.adapter

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

class CustomLinearLayoutManager(
    context: Context?,
    orientation: Int = RecyclerView.VERTICAL,
    reverseLayout: Boolean = false
) : LinearLayoutManager(context, orientation, reverseLayout) {

    private val scroller: LinearSmoothScroller

    init {
        scroller = CustomSmoothScroller(context)
    }

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView,
        state: RecyclerView.State,
        position: Int
    ) {
        scroller.targetPosition = position
        startSmoothScroll(scroller)
    }

    internal inner class CustomSmoothScroller(context: Context?) :
        LinearSmoothScroller(context) {

        override fun getVerticalSnapPreference(): Int {
            return SNAP_TO_START //设置滚动位置
        }
    }
}
