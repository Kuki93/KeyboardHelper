package com.example.mediabox.adapter

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.example.mediabox.R
import com.example.mediabox.data.MediaBucket
import com.example.mediabox.glide.GlideApp
import kotlinx.android.synthetic.main.recycler_view_item_media_bucket.view.*

interface OnMediaBucketChangeListener {
    fun onChange(bucket: MediaBucket)
}

class MediaBucketAdapter : RecyclerView.Adapter<MediaBucketHolder>() {

    private val sources = mutableListOf<MediaBucket>()

    private var selectId = 0

    var itemClickListener: ((MediaBucket) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaBucketHolder {
        return MediaBucketHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recycler_view_item_media_bucket, parent, false)
        )
    }

    override fun getItemCount() = sources.size


    override fun onBindViewHolder(holder: MediaBucketHolder, position: Int) {
        holder.itemView.divider.isVisible = position != 0
        sources[position].also {
            holder.itemView.setOnClickListener { _ ->
                itemClickListener?.invoke(it)
            }
            if (it.bucketId == selectId) {
                holder.itemView.iv_check?.setImageResource(R.drawable.ic_round_check_24)
            } else {
                holder.itemView.iv_check?.setImageDrawable(null)
            }
            holder.itemView.tv_display.text = it.displayName
            holder.itemView.tv_count.text = it.size.toString()
            holder.itemView.avatar?.also { view ->
                GlideApp.with(view)
                    .asBitmap()
                    .load(it.uri)
                    .sizeMultiplier(0.75f)
                    .into(object : CustomViewTarget<ImageView, Bitmap>(view) {

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
    }

    fun update(new: MutableList<MediaBucket>, selectId: Int? = null) {
        selectId?.also {
            this.selectId = it
        }
        sources.clear()
        sources.addAll(new)
        notifyDataSetChanged()
    }
}

class MediaBucketHolder(itemView: View) : RecyclerView.ViewHolder(itemView)