package com.example.mediabox.ext

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.mediabox.data.MediaData
import com.example.mediabox.glide.GlideApp
import com.example.mediabox.glide.gif.FrameSequenceDrawable
import com.example.mediabox.view.LagerMediaView
import com.example.mediabox.view.SubsamplingScalePlusImageView

const val JPG = "image/jpeg"
const val PNG = "image/png"
const val WEBP = "image/webp"
const val GIF = "image/gif"

interface IMediaShowListener {

    fun getContext(): Context

    fun isSupportLargeMedia() = false

    fun showMedia(uri: Uri)

    fun showMedia(drawable: Drawable)

    fun showMedia(bitmap: Bitmap)

    fun showMedia(resId: Int)

    fun getImageView() = this as View

    fun getSourceDrawable() = (getImageView() as? ImageView)?.drawable
}

fun RequestBuilder<Drawable>.intoWithListener(view: ImageView, doEnd: () -> Unit) {
    val listener = object : RequestListener<Drawable> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<Drawable>?,
            isFirstResource: Boolean
        ): Boolean {
            doEnd()
            return false
        }

        override fun onResourceReady(
            resource: Drawable?,
            model: Any?,
            target: Target<Drawable>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            doEnd()
            return false
        }
    }
    addListener(listener)
        .into(view)
}

fun IMediaShowListener.showMediaWithListener(data: MediaData, doEnd: ((Boolean) -> Unit)? = null) {
    if (isSupportLargeMedia()) {
        showMedia(data.uri)
        doEnd?.invoke(true)
    } else {
        if (data.isGif()) {
            val listener = doEnd?.let {
                object : RequestListener<FrameSequenceDrawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<FrameSequenceDrawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        it(false)
                        return false
                    }

                    override fun onResourceReady(
                        resource: FrameSequenceDrawable?,
                        model: Any?,
                        target: Target<FrameSequenceDrawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        it(true)
                        return false
                    }
                }
            }
            GlideApp.with(getContext())
                .`as`(FrameSequenceDrawable::class.java)
                .load(data.uri)
                .listener(listener)
                .into(getImageView() as ImageView)
        } else {
            val listener = doEnd?.let {
                object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        it(false)
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        it(true)
                        return false
                    }
                }
            }

            GlideApp.with(getContext())
                .load(data.uri)
                .listener(listener)
                .into(getImageView() as ImageView)
        }
    }
}

fun Context.getScreenSize(): Point {
    return getSystemService(Context.WINDOW_SERVICE)?.let {
        it as? WindowManager
    }?.let {
        val point = Point()
        it.defaultDisplay.getRealSize(point)
        point
    } ?: resources.displayMetrics.let {
        Point(it.widthPixels, it.heightPixels)
    }
}

fun MediaData.isLagerImage(screenSize: Point): Boolean {
    if (width <= 0 || height <= 0) {
        return true
    }
    if (width >= screenSize.x.times(1.2f)) {
        if (width >= screenSize.x.times(2f)) {
            return true
        }
        if (width >= height.times(2.5f)) {
            return true
        }
        if (height >= screenSize.y.times(1.2f)) {
            return true
        }
    }

    if (height >= screenSize.y) {
        if (height >= screenSize.y.times(2f)) {
            return true
        }
        if (height >= width.times(3f)) {
            return true
        }
        if (width >= screenSize.x.times(1.2f)) {
            return true
        }
    }
    return false
}

fun LagerMediaView.initLagerState(viewSize: Point, data: MediaData) {
    if (data.width <= 0 || data.height <= 0) {
        return
    }

    val scaleX = viewSize.x.toFloat().div(data.width)
    val scaleY = viewSize.y.toFloat().div(data.height)

    fun transform(scale: Float): Float {
        return if (scale < 1) {
            1.div(scale)
        } else {
            scale
        }
    }

    val transformScaleX = transform(scaleX)
    val transformScaleY = transform(scaleY)

    val rate = transformScaleX.div(transformScaleY)
    if (rate <= 1f) {
        setScaleAndCenter(scaleX, PointF(viewSize.x.div(2f), 0f))
        setDoubleTapZoomMinScale(scaleX)
    } else {
        if (scaleX > scaleY) {
            setScaleAndCenterWithAnim(scaleY, PointF(viewSize.x.div(2f), 0f))
            setDoubleTapZoomMinScale(scaleY)
        }
    }
}

fun LagerMediaView.setScaleAndCenterWithAnim(scale: Float, viewCenter: PointF) {
    val animationBuilder = animateScaleAndCenter(scale, viewCenter)
    if (animationBuilder != null) {
        animationBuilder.withEasing(SubsamplingScalePlusImageView.EASE_OUT_QUAD).start()
    } else {
        setScaleAndCenter(scale, viewCenter)
    }
}

fun MediaData.isJpg() = mimeType == JPG

fun MediaData.isPng() = mimeType == PNG

fun MediaData.isWebp() = mimeType == WEBP

fun MediaData.isGif() = mimeType == GIF
