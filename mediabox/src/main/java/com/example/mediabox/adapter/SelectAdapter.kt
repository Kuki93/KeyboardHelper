package com.example.mediabox.adapter

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.example.mediabox.R
import com.example.mediabox.data.MediaData
import com.example.mediabox.ext.isGif
import com.example.mediabox.glide.GlideApp
import kotlinx.android.synthetic.main.recycler_view_item_media_select.view.*

class SelectAdapter(private var currentSourceIndex: Int, config: AsyncDifferConfig<MediaData>) :
    ListAdapter<MediaData, SelectViewHolder>(config) {

    var itemClickListener: ((Int, MediaData) -> Unit)? = null

    private var oldSelectIndex = -1

    init {
        currentSourceIndex = currentSourceIndex.coerceAtLeast(0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectViewHolder {
        return SelectViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recycler_view_item_media_select, parent, false)
                .also {
                    it.image_view.scaleType = ImageView.ScaleType.CENTER_CROP
                }
        )
    }

    override fun onBindViewHolder(
        holder: SelectViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val data = getItem(position)
            data ?: return
            updateSelect(holder, data)
        }
    }

    override fun onBindViewHolder(holder: SelectViewHolder, position: Int) {
        val data = getItem(position)
        data ?: return
        holder.itemView.tv_media_type?.also {
            if (data.isGif()) {
                it.text = "GIF"
                it.visibility = View.VISIBLE
            } else {
                it.text = null
                it.visibility = View.GONE
            }
        }
        updateSelect(holder, data)
        holder.itemView.image_view?.also {
            GlideApp.with(it)
                .asBitmap()
                .load(data.uri)
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
            it.setOnClickListener { _ ->
                itemClickListener?.invoke(data.sourceIndex, data)
            }
        }
    }

    private fun updateSelect(
        holder: SelectViewHolder,
        data: MediaData
    ) {
        holder.itemView.view_select.apply {
            if (currentSourceIndex == data.sourceIndex) {
                data.curSelected = true
                oldSelectIndex = data.selectIndex
                ContextCompat.getDrawable(context, R.drawable.background_share_stroke).let {
                    it as? GradientDrawable
                }?.also {
                    it.setColor(if (data.selected) 0x0F000000 else 0xBFFFFFFF.toInt())
                    background = it
                } ?: setBackgroundResource(R.drawable.background_share_stroke)
            } else {
                data.curSelected = false
                if (data.selected) {
                    setBackgroundColor(0x0F000000)
                } else {
                    setBackgroundColor(0xBFFFFFFF.toInt())
                }
            }
        }
    }

    fun updateCurrentIndex(index: Int) {
        if (currentSourceIndex != index) {
            currentSourceIndex = index
            if (oldSelectIndex > -1) {
                notifyItemChanged(oldSelectIndex, "select")
            }
            currentList.firstOrNull {
                it.sourceIndex == index
            }?.selectIndex?.takeIf {
                it > -1
            }?.also {
                notifyItemChanged(it, "select")
            }
        }
    }
}

class SelectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)