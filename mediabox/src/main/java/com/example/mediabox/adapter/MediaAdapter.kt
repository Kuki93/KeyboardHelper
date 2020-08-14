package com.example.mediabox.adapter

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.example.mediabox.R
import com.example.mediabox.data.MediaData
import com.example.mediabox.ext.isGif
import com.example.mediabox.glide.GlideApp
import kotlinx.android.synthetic.main.recycler_view_item_media_resource.view.*
import java.util.*
import kotlin.collections.ArrayList

class MediaAdapter(private val maxCount: Int) : PagingDataAdapter<MediaData, MediaViewHolder>(
    MediaDiffCallback()
) {
    private var selectLinkedSet: MutableSet<Int> = LinkedHashSet(maxCount)

    private var autoSelectCameras: MutableSet<Uri>? = null

    var itemClickListener: ((Boolean, Int, MediaData) -> Unit)? = null

    var selectCountChangeListener: ((Int, Int) -> Unit)? = null

    override fun getItemViewType(position: Int): Int {
        if (getItem(position) == MediaData.EMPTY) {
            return 0
        }
        return 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        return MediaViewHolder(
            if (viewType == 0) {
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.recycler_view_item_media_camera, parent, false)
            } else {
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.recycler_view_item_media_resource, parent, false)
                    .also {
                        it.image_view.scaleType = ImageView.ScaleType.CENTER_CROP
                    }
            }
        )
    }

    override fun onBindViewHolder(
        holder: MediaViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val data = getItem(position)
        data ?: return
        holder.itemView.setOnClickListener { _ ->
            itemClickListener?.invoke(false, position, data)
        }
        if (data == MediaData.EMPTY) {
            holder.itemView.setOnLongClickListener { _ ->
                itemClickListener?.invoke(true, position, data)
                return@setOnLongClickListener true
            }
            return
        }
        holder.itemView.setOnLongClickListener(null)
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, data)
        } else {
            val selectIndex = selectLinkedSet.indexOf(data.id)
            val isSelected = selectIndex != -1
            val selectStr = if (isSelected) selectIndex.plus(1).toString() else null
            holder.itemView.view_mask?.setBackgroundColor(if (isSelected) 0x6F000000 else 0x0F000000)
            holder.itemView.tv_media_selector?.also {
                it.isSelected = isSelected
                it.text = selectStr
                it.setOnClickListener { _ ->
                    changeSelect(it, holder.itemView.view_mask, data)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
    }

    fun addCameraURI(uri: Uri) {
        autoSelectCameras?.also {
            it.add(uri)
        } ?: kotlin.run {
            autoSelectCameras = mutableSetOf(uri)
        }
    }

    fun getSources(): Pair<IntArray, ArrayList<MediaData>> {
        val count = itemCount
        val sources = ArrayList<MediaData>(count)
        val selects = ArrayList<Pair<Int, Int>>(selectLinkedSet.size)
        repeat(count) { index ->
            getItem(index)?.takeIf {
                it != MediaData.EMPTY
            }?.also {
                sources.add(it)
                val i = selectLinkedSet.indexOf(it.id)
                if (i != -1) {
                    selects.add(it.id to i)
                }
            }
        }
        selects.sortBy {
            it.second
        }
        return selects.map {
            it.first
        }.toIntArray() to sources
    }

    fun getSelectSources(): ArrayList<MediaData> {
        val count = itemCount
        val sources = ArrayList<MediaData>(count)
        repeat(count) { index ->
            getItem(index)?.takeIf {
                val selectIndex = selectLinkedSet.indexOf(it.id)
                it.selectIndex = selectIndex
                selectIndex != -1
            }?.also {
                sources.add(it)
                if (sources.size == selectLinkedSet.size) {
                    return@repeat
                }
            }
        }
        sources.sortBy {
            it.selectIndex
        }
        return sources
    }

    fun clearSelect() {
        try {
            if (selectLinkedSet.isEmpty()) {
                return
            }
            repeat(itemCount) { index ->
                if (selectLinkedSet.isEmpty()) {
                    return
                }
                getItem(index)?.takeIf {
                    selectLinkedSet.remove(it.id)
                }?.also {
                    notifyItemChanged(index, "select")
                }
            }
        } finally {
            selectCountChangeListener?.invoke(selectLinkedSet.size, maxCount)
        }
    }

    fun updateSelect(positionStart: Int, itemCount: Int) {
        if (itemCount <= 0) {
            return
        }
        val ids = LinkedHashSet<Int>(itemCount)
        repeat(itemCount) {
            getItem(positionStart.plus(it))?.also {
                ids.add(it.id)
            }
        }
        val new = LinkedHashSet(selectLinkedSet)
        if (new.removeAll(ids)) {
            diffUpdate(new)
        }
    }

    fun diffUpdate(new: MutableSet<Int>) {
        try {
            val old = selectLinkedSet
            selectLinkedSet = new
            var statistics = 0
            val total = old.size.plus(new.size)
            repeat(itemCount) { index ->
                if (statistics >= total) {
                    return
                }
                getItem(index)?.takeIf {
                    val oldIndex = old.indexOf(it.id)
                    val newIndex = new.indexOf(it.id)
                    if (oldIndex != -1) {
                        statistics++
                    }
                    if (newIndex != -1) {
                        statistics++
                    }
                    oldIndex != newIndex
                }?.also {
                    notifyItemChanged(index, "select")
                }
            }
        } finally {
            selectCountChangeListener?.invoke(selectLinkedSet.size, maxCount)
        }
    }


    private fun onBindViewHolder(holder: MediaViewHolder, data: MediaData) {
        var _selectIndex = selectLinkedSet.indexOf(data.id)
        var _isSelected = _selectIndex != -1
        holder.itemView.tv_media_selector?.apply {
            val cameras = autoSelectCameras
            var removeItem: Uri? = null
            cameras?.forEach {
                if (data.uri.path == it.path) {
                    if (!_isSelected && selectLinkedSet.size < maxCount) {
                        changeSelect(this, holder.itemView.view_mask, data)
                        _selectIndex = selectLinkedSet.size.minus(1)
                        _isSelected = true
                    }
                    removeItem = it
                    return@forEach
                }
            }
            removeItem?.also {
                cameras?.remove(it)
            }
            if (cameras?.isEmpty() == true) {
                autoSelectCameras = null
            }
        }

        val selectIndex = _selectIndex
        val isSelected = _isSelected

        val selectStr = if (isSelected) selectIndex.plus(1).toString() else null
        holder.itemView.view_mask?.setBackgroundColor(if (isSelected) 0x6F000000 else 0x0F000000)
        holder.itemView.tv_media_selector?.also {
            it.isSelected = isSelected
            it.text = selectStr
            it.setOnClickListener { _ ->
                changeSelect(it, holder.itemView.view_mask, data)
            }
        }
        holder.itemView.tv_media_type?.also {
            if (data.isGif()) {
                it.text = "GIF"
                it.visibility = View.VISIBLE
            } else {
                it.text = null
                it.visibility = View.GONE
            }
        }
        holder.itemView.image_view?.also {
            GlideApp.with(it)
                .asBitmap()
                .load(data.uri)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .sizeMultiplier(0.75f)
                .into(object : CustomViewTarget<ImageView, Bitmap>(it) {

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        getView().setImageDrawable(errorDrawable)
                    }

                    override fun onResourceCleared(placeholder: Drawable?) {
                        getView().setImageDrawable(placeholder)
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        getView().setImageBitmap(resource)
                    }
                })
        }
    }

    private fun changeSelect(textView: TextView, maskView: View?, data: MediaData) {
        val selectIndex = selectLinkedSet.indexOf(data.id)
        val isSelected = selectIndex != -1
        if (!isSelected && selectLinkedSet.size >= maxCount) {
            Toast.makeText(textView.context, "你最多只能选择${maxCount}张图片", Toast.LENGTH_SHORT).show()
            return
        }
        textView.isSelected = !isSelected
        maskView?.startBackgroundColorTransform(endColor = if (!isSelected) 0x6F000000 else 0x0F000000)
        if (!isSelected) {
            selectLinkedSet.add(data.id)
            textView.text = selectLinkedSet.size.toString()
        } else {
            textView.text = null
            repeat(itemCount) { index ->
                getItem(index)?.also {
                    if (it.id != data.id && selectLinkedSet.indexOf(it.id) > selectIndex) {
                        notifyItemChanged(index, "select")
                    }
                }
            }
            selectLinkedSet.remove(data.id)
        }
        selectCountChangeListener?.invoke(selectLinkedSet.size, maxCount)
    }

    private fun View.startBackgroundColorTransform(startColor: Int? = null, endColor: Int) {
        if (startColor == null && (background as ColorDrawable).color == endColor) {
            return
        }
        val animator: ValueAnimator = ObjectAnimator.ofInt(
            this,
            "backgroundColor",
            startColor ?: (background as ColorDrawable).color,
            endColor
        ) //对背景色颜色进行改变，操作的属性为"backgroundColor",此处必须这样写，不能全小写,后面的颜色为在对应颜色间进行渐变
        animator.duration = 350
        animator.setEvaluator(ArgbEvaluator()) //如果要颜色渐变必须要ArgbEvaluator，来实现颜色之间的平滑变化，否则会出现颜色不规则跳动
        animator.start()
    }
}

class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

class MediaDiffCallback : DiffUtil.ItemCallback<MediaData>() {

    override fun areItemsTheSame(oldItem: MediaData, newItem: MediaData): Boolean {
        return oldItem.id == newItem.id && oldItem.uri.path == newItem.uri.path
    }

    override fun areContentsTheSame(oldItem: MediaData, newItem: MediaData): Boolean {
        return oldItem == MediaData.EMPTY && newItem == MediaData.EMPTY
    }

    override fun getChangePayload(oldItem: MediaData, newItem: MediaData): Any? {
        if (oldItem.id == newItem.id && oldItem.uri.path == newItem.uri.path) {
            return "select"
        }
        return super.getChangePayload(oldItem, newItem)
    }
}