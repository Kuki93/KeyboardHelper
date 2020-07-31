package com.example.myapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

inline fun ViewGroup.forEachChildViews(action: (index: Int, childView: View) -> Unit) {
    val childCount = this.childCount
    for (index in 0 until childCount) {
        action(index, this.getChildAt(index))
    }
}

/**
 *
 * @param T [sources]类型
 * @param VH  继承[BaseRecyclerViewViewHolder]
 */
abstract class BaseRecyclerViewAdapter<T, VH : BaseRecyclerViewViewHolder> :
    RecyclerView.Adapter<BaseRecyclerViewViewHolder>() {

    companion object {
        const val ITEM_TYPE_HEADER = 8888
        const val ITEM_TYPE_FOOTER = 9999
    }

    open val sources: MutableList<T> = mutableListOf()

    abstract fun onCreateMainViewHolder(parent: ViewGroup, viewType: Int): VH

    open fun onBindMainViewHolder(holder: VH, position: Int) {}

    open fun onBindMainViewHolder(holder: VH, position: Int, viewType: Int) {
        onBindMainViewHolder(holder, position)
    }

    open fun onBindMainViewHolder(holder: VH, position: Int, payloads: MutableList<Any>): Boolean {
        return false
    }

    open fun onBindMainViewHolder(
        holder: VH,
        position: Int,
        viewType: Int,
        payloads: MutableList<Any>
    ): Boolean {
        return onBindMainViewHolder(holder, position, payloads)
    }

    open fun getMainItemViewType(position: Int): Int {
        return super.getItemViewType(position)
    }

    protected var recyclerView: RecyclerView? = null

    var itemViewClickListener: AdapterItemClickListener? = null

    var itemChildViewClickListener: AdapterItemClickListener? = null

    var itemChildViewLongClickListener: AdapterItemLongClickListener? = null

    var itemExpansionViewClickListener: AdapterExpansionItemClickListener? = null

    private var headerViews: LinearLayout? = null

    private var footerViews: LinearLayout? = null

    var itemSpanSize: ((position: Int) -> Int)? = null

    override fun onViewAttachedToWindow(holder: BaseRecyclerViewViewHolder) {
        super.onViewAttachedToWindow(holder)
        (holder.itemView.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.takeIf {
            isHeader(holder.bindingAdapterPosition) || isFooter(holder.bindingAdapterPosition)
        }?.also {
            it.isFullSpan = true
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        (recyclerView.layoutManager as? GridLayoutManager)?.apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (isHeader(position) || isFooter(position)) spanCount else itemSpanSize?.invoke(
                        position
                    ) ?: 1
                }
            }
        }
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    final override fun getItemViewType(position: Int): Int {
        if (headerCount() > 0 && position == 0) {
            return ITEM_TYPE_HEADER
        }
        if (footerCount() > 0 && position == itemCount - 1) {
            return ITEM_TYPE_FOOTER
        }
        return getChildItemViewType(position)
    }


    final override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseRecyclerViewViewHolder {
        footerViews?.takeIf {
            viewType == ITEM_TYPE_FOOTER
        }?.also {
            it.forEachChildViews { _, childView ->
//                childView.onClick { view ->
//                    it.indexOfChild(view).takeIf { index ->
//                        index != -1
//                    }?.also { index ->
//                        itemExpansionViewClickListener?.onFooterItemClick(view, index)
//                    }
//                }
            }
            return ExpansionViewHolder(it)
        }

        headerViews?.takeIf {
            viewType == ITEM_TYPE_HEADER
        }?.also {
            it.forEachChildViews { _, childView ->
//                childView.onClick { view ->
//                    it.indexOfChild(view).takeIf { index ->
//                        index != -1
//                    }?.also { index ->
//                        itemExpansionViewClickListener?.onHeaderItemClick(view, index)
//                    }
//                }
            }
            return ExpansionViewHolder(it)
        }
        return onCreateMainViewHolder(parent, viewType).apply {
            itemViewClickListener = object : AdapterItemClickListener {
                override fun onClick(view: View, adapterPosition: Int, extra: Pair<Int, Any?>?) {
                    this@BaseRecyclerViewAdapter.itemViewClickListener?.onClick(
                        view,
                        adapterPosition,
                        extra
                    )
                }
            }
            itemChildViewClickListener = object : AdapterItemClickListener {
                override fun onClick(view: View, adapterPosition: Int, extra: Pair<Int, Any?>?) {
                    this@BaseRecyclerViewAdapter.itemChildViewClickListener?.onClick(
                        view,
                        adapterPosition,
                        extra
                    )
                }
            }
            itemChildViewLongClickListener = object : AdapterItemLongClickListener {
                override fun onLongClick(
                    view: View,
                    adapterPosition: Int,
                    extra: Pair<Int, Any?>?
                ): Boolean {
                    return this@BaseRecyclerViewAdapter.itemChildViewLongClickListener?.onLongClick(
                        view,
                        adapterPosition,
                        extra
                    ) ?: false
                }
            }
        }
    }

    final override fun onBindViewHolder(holder: BaseRecyclerViewViewHolder, position: Int) {
        val viewType = getItemViewType(position)
        when {
            viewType >= ITEM_TYPE_FOOTER -> {

            }
            viewType >= ITEM_TYPE_HEADER -> {

            }
            else -> {
                onBindMainViewHolder(holder as VH, position, viewType)
            }
        }
    }

    final override fun onBindViewHolder(
        holder: BaseRecyclerViewViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val viewType = getItemViewType(position)
        when {
            viewType >= ITEM_TYPE_FOOTER -> {

            }
            viewType >= ITEM_TYPE_HEADER -> {

            }
            else -> {
                holder.bindListener(position)
                if (onBindMainViewHolder(holder as VH, position, viewType, payloads)) {
                    return
                }
            }
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun getItemCount(): Int {
        return headerCount() + sources.size + footerCount()
    }

    protected fun createView(parent: ViewGroup, layoutId: Int): View {
        return LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
    }

    protected fun createView(context: Context, layoutId: Int, parent: ViewGroup? = null): View {
        return LayoutInflater.from(context).inflate(layoutId, parent, false)
    }

    open fun getNonNullItem(adapterPosition: Int): T {
        return sources[getSourcePosition(adapterPosition)]
    }

    open fun getItem(adapterPosition: Int): T? {
        return sources.getOrNull(getSourcePosition(adapterPosition))
    }

    open fun getSourcePosition(adapterPosition: Int): Int {
        return adapterPosition - headerCount()
    }

    fun isHeader(adapterPosition: Int): Boolean {
        return headerCount() > 0 && adapterPosition == 0
    }

    fun isFooter(adapterPosition: Int): Boolean {
        return footerCount() > 0 && adapterPosition == itemCount - 1
    }

    /**
     * [withExpansion] 是否包括扩展的头尾
     */
    @JvmOverloads
    fun getCount(withExpansion: Boolean = false) = if (withExpansion) itemCount else sources.size

    /**
     * [withExpansion] 是否包括扩展的头尾
     */
    @JvmOverloads
    fun isEmpty(withExpansion: Boolean = false) =
        if (withExpansion) itemCount == 0 else sources.isEmpty()

    fun headerCount(): Int = if ((headerViews?.childCount ?: 0) > 0) 1 else 0

    fun footerCount() = if ((footerViews?.childCount ?: 0) > 0) 1 else 0

    fun getHeaderOrNull(index: Int): View? {
        return headerViews?.getChildAt(index)
    }

    fun getFooterOrNull(index: Int): View? {
        return footerViews?.getChildAt(index)
    }

    fun updateHeader(index: Int, view: View, addIfNotExist: Boolean = false) {
        (headerViews?.let {
            val isAdded = index in 0 until it.childCount
            if (isAdded) {
                it.removeViewAt(index)
                addViewInLayout(it, view, index)
            }
            isAdded
        } ?: false).takeIf {
            !it && addIfNotExist
        }?.also {
            addHeadView(view)
        }
    }

    fun updateFooter(index: Int, view: View, addIfNotExist: Boolean = false) {
        (footerViews?.let {
            val isAdded = index in 0 until it.childCount
            if (isAdded) {
                it.removeViewAt(index)
                addViewInLayout(it, view, index)
            }
            isAdded
        } ?: false).takeIf {
            !it && addIfNotExist
        }?.also {
            addFooterView(view)
        }
    }


    /**
     * 添加headerView
     */
    fun addHeadView(view: View, orientation: Int = LinearLayout.VERTICAL) {
        addHeadView(view, -1, orientation)
    }

    /**
     * 添加headerView
     */
    open fun addHeadView(view: View, index: Int, orientation: Int = LinearLayout.VERTICAL) {
//        recyclerView?.layoutManager?.takeIf {
//            it !is LinearLayoutManager
//        }?.also {
//            throw IllegalStateException("目前只支持LinearLayoutManager添加header")
//        }

        if (headerViews?.indexOfChild(view) ?: -1 != -1) {
            return
        }

        val isAdded = headerCount() > 0
        (headerViews ?: LinearLayout(recyclerView?.context ?: view.context).also {
            headerViews = it
        }).also {
            it.orientation = orientation
            addViewInLayout(it, view, index)
            if (!isAdded) {
                notifyItemInserted(0)
            }
        }
    }


    /**
     * 添加footerView
     */
    fun addFooterView(view: View, orientation: Int = LinearLayout.VERTICAL) {
        addFooterView(view, -1, orientation)
    }


    open fun addFooterView(view: View, index: Int, orientation: Int = LinearLayout.VERTICAL) {
//        recyclerView?.layoutManager?.takeIf {
//            it !is LinearLayoutManager
//        }?.also {
//            throw IllegalStateException("目前只支持LinearLayoutManager添加footer")
//        }


        if (footerViews != null && footerViews?.indexOfChild(view) != -1) {
            return
        }

        val isAdded = footerCount() > 0
        (footerViews ?: LinearLayout(recyclerView?.context ?: view.context).also {
            footerViews = it
        }).also {
            it.orientation = orientation
            addViewInLayout(it, view, index)
            if (!isAdded) {
                notifyItemInserted(itemCount - 1)
            }
        }

    }

    fun removeHeader(index: Int) {
        headerViews?.takeIf {
            index in 0 until it.childCount
        }?.also {
            it.removeViewAt(index)
            if (it.childCount == 0) {
                headerViews = null
                notifyItemRemoved(0)
                notifyItemRangeChanged(0, itemCount)
            }
        }
    }

    fun removeHeader(view: View) {
        headerViews?.takeIf {
            it.indexOfChild(view) != -1
        }?.also {
            it.removeView(view)
            if (it.childCount == 0) {
                headerViews = null
                notifyItemRemoved(0)
                notifyItemRangeChanged(0, itemCount)
            }
        }
    }

    fun removeHeaderAll() {
        if (headerCount() > 0) {
            headerViews = null
            notifyItemRemoved(0)
            notifyItemRangeChanged(0, itemCount)
        }
    }

    fun removeFooter(index: Int) {
        footerViews?.takeIf {
            index in 0 until it.childCount
        }?.also {
            it.removeViewAt(index)
            if (it.childCount == 0) {
                footerViews = null
                notifyItemRemoved(itemCount)
            }
        }
    }

    fun removeFooter(view: View) {
        footerViews?.takeIf {
            it.indexOfChild(view) != -1
        }?.also {
            it.removeView(view)
            if (it.childCount == 0) {
                footerViews = null
                notifyItemRemoved(itemCount)
            }
        }
    }

    fun removeFooterAll() {
        if (footerCount() > 0) {
            footerViews = null
            notifyItemRemoved(itemCount)
        }
    }

    /**
     *
     */
    fun updateSources(data: List<T>, update: Boolean = true) {
        sources.clear()
        sources.addAll(data)
        if (update) {
            notifyDataSetChanged()
        }
    }

    fun clear() {
        sources.clear()
        notifyDataSetChanged()
    }

    fun filterIndex(predicate: (index: Int, T) -> Boolean): Int? {
        sources.forEachIndexed { index, element ->
            if (predicate(index, element)) {
                return index
            }
        }
        return null
    }

    fun filterElement(predicate: (index: Int, T) -> Boolean): T? {
        sources.forEachIndexed { index, element ->
            if (predicate(index, element)) {
                return element
            }
        }
        return null
    }

    fun removeBy(predicate: (index: Int, T) -> Boolean): Boolean {
        sources.forEachIndexed { index, element ->
            if (predicate(index, element)) {
                removeAt(index)
                return true
            }
        }
        return false
    }

    /**
     * [data] 添加的数据源
     * [index] 插入的起始的位置 -1表示从尾部插入
     */
    @JvmOverloads
    fun addSource(data: T, index: Int = -1, update: Boolean = true) {
        addSources(listOf(data), index, update)
    }

    /**
     * [data] 添加的数据源
     * [index] 插入的起始的位置 -1表示从尾部插入
     */
    @JvmOverloads
    open fun addSources(data: List<T>, index: Int = -1, update: Boolean = true) {
        val start: Int
        if (index == -1) {
            start = sources.size
            sources.addAll(data)
        } else {
            start = index
            sources.addAll(index, data)
        }
        if (update) {
            notifyItemRangeInserted(start + headerCount(), data.size)
        }
    }

    @JvmOverloads
    fun updateRangeSources(start: Int, count: Int = 1, payload: Any? = null) {
        if (start < 0 || start > sources.lastIndex) {
            throw IndexOutOfBoundsException("start 取值异常")
        }
        require(count > 0 && start + count <= sources.size) {
            "count 取值异常"
        }
        notifyItemRangeChanged(start + headerCount(), count, payload)
    }

    fun removeSource(data: T, update: Boolean = true) {
        val index = sources.indexOf(data)
        if (index > -1) {
            removeAt(index, update)
        }
    }

    fun removeAt(index: Int, update: Boolean = true) {
        removeAtRange(index, update = update)
    }

    fun removeAtRange(start: Int, count: Int = 1, update: Boolean = true) {
        if (start < 0 && start > sources.lastIndex) {
            throw IndexOutOfBoundsException("start 取值异常")
        }
        require(count > 0 && start + count <= sources.size) {
            "count 取值异常"
        }
        repeat(count) {
            sources.removeAt(start)
        }
        if (update) {
            val positionStart = start + headerCount()
            notifyItemRangeRemoved(positionStart, count)
            notifyItemRangeChanged(positionStart, itemCount.minus(positionStart))
        }
    }


    /**
     * [index] -1表示从最后插入
     */
    private fun addViewInLayout(parent: ViewGroup, child: View, index: Int = -1) {
        parent.addView(child, index)
//        child.onClick { view ->
//            parent.indexOfChild(view).takeIf { index ->
//                index != -1
//            }?.also { index ->
//                if (headerViews == parent) {
//                    itemExpansionViewClickListener?.onHeaderItemClick(view, index)
//                } else {
//                    itemExpansionViewClickListener?.onFooterItemClick(view, index)
//                }
//            }
//        }
    }

    private fun getChildItemViewType(position: Int): Int {
        val type = getMainItemViewType(position)
        require(type != ITEM_TYPE_HEADER && type != ITEM_TYPE_FOOTER) { "该type类型被占用：$type" }
        return type
    }
}

class ExpansionViewHolder(itemView: View) : BaseRecyclerViewViewHolder(itemView)

open class BaseRecyclerViewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    init {
        itemView.parent?.let {
            it as? ViewGroup
        }?.also {
            it.removeView(itemView)
        }
    }

    var itemViewClickListener: AdapterItemClickListener? = null

    var itemChildViewClickListener: AdapterItemClickListener? = null

    var itemChildViewLongClickListener: AdapterItemLongClickListener? = null

    fun bindListener(position: Int, extra: Pair<Int, Any?>? = null, block: (() -> Unit)? = null) {
        itemView.setOnClickListener {
            block?.invoke()
            itemViewClickListener?.onClick(it, position, extra)
        }
    }

    fun bindListener(position: Int, extraFirst: Int, extraSecond: Any? = null) {
        bindListener(position, Pair(extraFirst, extraSecond), null)
    }

    fun addChildViewListener(child: View?, extra: Pair<Int, Any?>? = null) {
        child?.setOnClickListener {
            itemChildViewClickListener?.onClick(it, bindingAdapterPosition, extra)
        }
    }

    fun addChildViewListener(child: View?, extraFirst: Int, extraSecond: Any? = null) {
        addChildViewListener(child, Pair(extraFirst, extraSecond))
    }

    fun clearChildViewListener(child: View?) {
        child?.setOnClickListener(null)
    }

    fun addChildViewLongListener(child: View?, extra: Pair<Int, Any?>? = null) {
        child?.setOnLongClickListener {
            return@setOnLongClickListener itemChildViewLongClickListener?.onLongClick(
                it,
                bindingAdapterPosition,
                extra
            ) ?: false
        }
    }

    fun addChildViewLongListener(child: View?, extraFirst: Int, extraSecond: Any? = null) {
        addChildViewLongListener(child, Pair(extraFirst, extraSecond))
    }

    fun clearChildViewLongListener(child: View?) {
        child?.setOnLongClickListener(null)
    }
}

interface AdapterItemClickListener {
    /**
     * [adapterPosition] 注意如果添加了header，不要使用[BaseRecyclerViewAdapter.sources] 通过 [adapterPosition]取数据
     * @use 推荐使用 [BaseRecyclerViewAdapter.getItem] [adapterPosition] 获取 source
     * [extra] 扩展参数，默认为空
     * [first] Int用于区分type
     * [second] 用于扩展参数
     */
    fun onClick(view: View, adapterPosition: Int, extra: Pair<Int, Any?>?)
}

interface AdapterItemLongClickListener {
    fun onLongClick(view: View, adapterPosition: Int, extra: Pair<Int, Any?>?): Boolean
}


interface AdapterExpansionItemClickListener {
    fun onHeaderItemClick(view: View, index: Int)

    fun onFooterItemClick(view: View, index: Int)
}

open class SimpleExpansionItemClickListener : AdapterExpansionItemClickListener {

    override fun onHeaderItemClick(view: View, index: Int) {

    }

    override fun onFooterItemClick(view: View, index: Int) {

    }
}
