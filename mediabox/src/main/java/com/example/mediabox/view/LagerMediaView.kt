package com.example.mediabox.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import com.example.mediabox.data.ImageSource
import com.example.mediabox.ext.IMediaShowListener

class LagerMediaView @JvmOverloads constructor(
    context: Context? = null,
    attrs: AttributeSet? = null
) : SubsamplingScalePlusImageView(context, attrs),
    IMediaShowListener {

    override fun isSupportLargeMedia() = true

    override fun showMedia(uri: Uri) {
        setImage(ImageSource.uri(uri))
    }

    override fun showMedia(drawable: Drawable) {
        throw  IllegalArgumentException("not support set drawable")

    }

    override fun showMedia(bitmap: Bitmap) {
        setImage(ImageSource.bitmap(bitmap))
    }

    override fun showMedia(resId: Int) {
        setImage(ImageSource.resource(resId))
    }
}