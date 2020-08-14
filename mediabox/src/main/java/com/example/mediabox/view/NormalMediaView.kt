package com.example.mediabox.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.example.mediabox.ext.IMediaShowListener

class NormalMediaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr), IMediaShowListener {

    override fun showMedia(uri: Uri) {
        setImageURI(uri)
    }

    override fun showMedia(drawable: Drawable) {
        setImageDrawable(drawable)
    }

    override fun showMedia(bitmap: Bitmap) {
        setImageBitmap(bitmap)
    }

    override fun showMedia(resId: Int) {
        setImageResource(resId)
    }
}