package com.example.mediabox.adapter

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.example.mediabox.data.MediaData
import com.example.mediabox.glide.GlideApp
import com.example.mediabox.view.SquareImageView

class MediaAdapter : PagingDataAdapter<MediaData, PhotoViewHolder>(
    PhotoDiffCallback()
) {

    var itemClickListener: ((View, MediaData) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        return PhotoViewHolder(
            SquareImageView(parent.context).also {
                it.scaleType = ImageView.ScaleType.CENTER_CROP
            })
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        (holder.itemView as ImageView).also {
            val data = getItem(position)
            data ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                it.transitionName = data.uri.path
            }
            GlideApp.with(it)
                .asBitmap()
                .load(data.uri)
                .sizeMultiplier(0.5f)
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
            it.setOnClickListener { v ->
                itemClickListener?.invoke(v, data)
            }
        }
    }
}

class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

class PhotoDiffCallback : DiffUtil.ItemCallback<MediaData>() {

    override fun areItemsTheSame(oldItem: MediaData, newItem: MediaData): Boolean {
        return oldItem.uri.path == newItem.uri.path
    }

    override fun areContentsTheSame(oldItem: MediaData, newItem: MediaData): Boolean {
        return true
    }
}