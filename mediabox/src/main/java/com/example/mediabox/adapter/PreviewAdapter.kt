package com.example.mediabox.adapter

import android.graphics.Point
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mediabox.data.MediaData
import com.example.mediabox.ext.*
import com.example.mediabox.view.LagerMediaView
import com.example.mediabox.view.PhotoMediaView

private const val TYPE_NORMAL = 0
private const val TYPE_LAGER = 1

class PreviewAdapter(
    private val screenSize: Point,
    private val list: MutableList<MediaData>
) : RecyclerView.Adapter<PreviewHolder>() {

    override fun getItemViewType(position: Int): Int {
        list[position].takeIf {
            !it.isGif() && it.isLagerImage(screenSize)
        }?.also {
            return TYPE_LAGER
        }
        return TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewHolder {
        return if (viewType == TYPE_NORMAL) {
            PhotoMediaView(parent.context) as IMediaShowListener
        } else {
            LagerMediaView(parent.context) as IMediaShowListener
        }.let {
            PreviewHolder(it.apply {
                getImageView().layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            })
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: PreviewHolder, position: Int) {
        holder.showImage(list[position], getItemViewType(position), screenSize)
    }
}

class PreviewHolder(private val mediaView: IMediaShowListener) :
    RecyclerView.ViewHolder(mediaView.getImageView()) {

    fun showImage(data: MediaData, itemViewType: Int, screenSize: Point) {
        mediaView.showMediaWithListener(data) {
            if (itemViewType == TYPE_LAGER) {
                (mediaView as LagerMediaView).initLagerState(screenSize, data)
            }
        }
    }
}