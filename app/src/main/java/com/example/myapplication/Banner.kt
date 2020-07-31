package com.example.myapplication

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback

private const val INVALID_VALUE = -1

private const val SCROLL_TIME = 1000L

private fun getRealPosition(isIncrease: Boolean, position: Int, realCount: Int): Int {
    if (!isIncrease) {
        return position
    }
    return when (position) {
        0 -> {
            realCount.minus(1)
        }
        realCount.plus(1) -> {
            0
        }
        else -> {
            position.minus(1)
        }
    }
}

interface BannerListener : LifecycleObserver, Runnable {

    fun bindViewPager(lifecycle: Lifecycle, viewPager2: ViewPager2, autoScroll: Boolean = true)

    fun start()

    fun stop()
}

abstract class BannerAdapter<T, VH : BaseRecyclerViewViewHolder> :
    BaseRecyclerViewAdapter<T, VH>() {

    var clickBannerListener: ((position: Int, data: T) -> Unit)? = null

    private val increaseCount = 2

    init {
        itemViewClickListener = object : AdapterItemClickListener {
            override fun onClick(view: View, adapterPosition: Int, extra: Pair<Int, Any?>?) {
                val realPosition = getRealPosition(adapterPosition)
                clickBannerListener?.invoke(realPosition, sources[realPosition])
            }
        }
    }

    final override fun addHeadView(view: View, index: Int, orientation: Int) {
        throw IllegalStateException("func disable")
    }

    final override fun addFooterView(view: View, index: Int, orientation: Int): Unit =
        throw IllegalStateException("func disable")

    final override fun getItemCount(): Int {
        val realCount = getRealCount()
        return if (realCount > 1) realCount.plus(increaseCount) else realCount
    }

    final override fun getSourcePosition(adapterPosition: Int): Int {
        return getRealPosition(adapterPosition)
    }

    @CallSuper
    override fun onBindMainViewHolder(
        holder: VH,
        position: Int,
        viewType: Int,
        payloads: MutableList<Any>
    ): Boolean {
        holder.bindListener(position)
        return super.onBindMainViewHolder(holder, position, viewType, payloads)
    }


    fun getRealCount(): Int {
        return getCount()
    }

    fun getRealPosition(position: Int): Int {
        return getRealPosition(increaseCount == 2, position, getRealCount())
    }
}

/**
 * 轮播图l
 */
class BannerHelper : BannerListener {

    var scrollTime = 500

    private var viewPager2: ViewPager2? = null

    private var lifecycle: Lifecycle? = null

    private val callback = BannerOnPageChangeCallback()

    private val observer = BannerAdapterDataObserver()

    private var startPosition = 1

    private var state = 0

    private var autoScroll: Boolean = true

    private var hasRegister = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun bindViewPager(lifecycle: Lifecycle, viewPager2: ViewPager2, autoScroll: Boolean) {
        if (state == -1) {
            throw IllegalStateException("current state is destroyed")
        }
        if (this.viewPager2 != null) {
            throw IllegalStateException("The viewPager has already been bind.")
        }
        lifecycle.addObserver(BannerLifecycleObserver())

        viewPager2.getChildAt(0)?.setOnTouchListener { _, event ->
            if (!viewPager2.isUserInputEnabled) {
                return@setOnTouchListener false
            }
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    doStop()
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_OUTSIDE -> {
                    doStart()
                }
            }
            return@setOnTouchListener false
        }

        viewPager2.registerOnPageChangeCallback(callback)
        ScrollSpeedManger.reflectLayoutManager(this, viewPager2)

        this.viewPager2 = viewPager2
        this.lifecycle = lifecycle
        this.autoScroll = autoScroll

        requireAdapter()?.also {
            viewPager2.setCurrentItem(startPosition, false)
        }
        doStart()
    }

    override fun start() {
        doStart(false)
    }

    override fun stop() {
        doStop(true)
    }

    override fun run() {
        viewPager2?.apply {
            val count = requireAdapter()?.itemCount ?: 0
            if (count <= 1) {
                return
            }
            val next: Int = currentItem.plus(1) % count
            currentItem = next
            postDelayed(this@BannerHelper, SCROLL_TIME)
        }
    }

    private fun requireAdapter(allowObserver: Boolean = true): BannerAdapter<*, *>? {
        return (viewPager2?.adapter as BannerAdapter<*, *>?)?.also {
            val identity = System.identityHashCode(it)
            if (allowObserver && hasRegister != identity) {
                it.registerAdapterDataObserver(observer)
                hasRegister = identity
            }
        }
    }

    private fun doStart(checkAuto: Boolean = true) {
        if (state == -1) {
            throw IllegalStateException("current state is destroyed")
        }
        if (requireAdapter() == null) {
            throw IllegalStateException("please viewpager2 bind adapter first")
        }
        if (checkAuto && !autoScroll) {
            return
        }
        if (state == 1) {
            return
        }
        if (lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == false) {
            return
        }
        autoScroll = true
        state = 1
        viewPager2?.postDelayed(this, SCROLL_TIME)

    }

    private fun doStop(stopAuto: Boolean = false) {
        if (state == -1) {
            throw IllegalStateException("current state is destroyed")
        }
        if (stopAuto) {
            autoScroll = false
        }
        if (state == 0) {
            return
        }
        state = 0
        viewPager2?.removeCallbacks(this)
    }

    private fun getRealCount(): Int {
        return requireAdapter()?.getRealCount() ?: 0
    }

    private fun getAdapterItemCount(): Int {
        return requireAdapter()?.itemCount ?: 0
    }

    private inner class BannerAdapterDataObserver : AdapterDataObserver() {
        override fun onChanged() {
            if (getAdapterItemCount() <= 1) {
                doStop()
            } else {
                doStart()
            }
        }
    }

    private inner class BannerLifecycleObserver : LifecycleObserver {

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun onStart(owner: LifecycleOwner) {
            doStart()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun onStop(owner: LifecycleOwner) {
            doStop()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy(owner: LifecycleOwner) {
            state = -1
            viewPager2?.unregisterOnPageChangeCallback(callback)
            requireAdapter(false)?.unregisterAdapterDataObserver(observer)
            viewPager2 = null
        }
    }


    private inner class BannerOnPageChangeCallback : OnPageChangeCallback() {

        private var mTempPosition: Int = INVALID_VALUE

        private var isScrolled = false

        override fun onPageSelected(position: Int) {
            if (isScrolled) {
                mTempPosition = position
            }
        }

        override fun onPageScrollStateChanged(state: Int) {
            viewPager2?.apply {
                if (state == ViewPager2.SCROLL_STATE_DRAGGING || state == ViewPager2.SCROLL_STATE_SETTLING) {
                    isScrolled = true
                } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    //滑动闲置或滑动结束
                    isScrolled = false
                    if (mTempPosition != INVALID_VALUE) {
                        if (mTempPosition == 0) {
                            setCurrentItem(getRealCount(), false)
                        } else if (mTempPosition == getAdapterItemCount().minus(1)) {
                            setCurrentItem(1, false)
                        }
                    }
                }
            }
        }
    }
}