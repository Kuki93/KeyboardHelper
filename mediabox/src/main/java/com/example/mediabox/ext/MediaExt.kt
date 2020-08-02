package com.example.mediabox.ext

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.exifinterface.media.ExifInterface
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
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

    fun showMediaCache(bitmap: Bitmap) {
        showMedia(bitmap)
    }

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
            GlideApp.with(getContext())
                .asBitmap()
                .load(data.uri)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onLoadCleared(placeholder: Drawable?) {
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        doEnd?.invoke(false)
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        showMediaCache(resource)
                        doEnd?.invoke(true)
                    }
                })
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
    if (height >= screenSize.y.times(2.2f)) {
        return true
    }
    if (width >= screenSize.x.times(3f) && height >= screenSize.y.times(2f)) {
        return true
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
            setScaleAndCenter(scaleY, PointF(viewSize.x.div(2f), 0f))
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

fun MediaData.checkMediaInfo(mContentResolver: ContentResolver) {
    val data = this
    if (data.width <= 0 || data.height <= 0) {
        mContentResolver.openInputStream(data.uri)?.use { input ->
            ExifInterface(input).apply {
                data.width = getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, -1)
                data.height = getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, -1)
            }
        }
    }

    if (data.width <= 0 || data.height <= 0) {
        mContentResolver.openFileDescriptor(data.uri, "r")?.use {
            it.fileDescriptor?.let { fd ->
                BitmapFactory.decodeFileDescriptor(fd)
            }?.also { bitmap ->
                data.width = bitmap.width
                data.height = bitmap.height
                bitmap.recycle()
            }
        }
    }
}

fun MediaData.isJpg() = mimeType == JPG

fun MediaData.isPng() = mimeType == PNG

fun MediaData.isWebp() = mimeType == WEBP

fun MediaData.isGif() = mimeType == GIF

fun hideSystemUI(activity: Activity) {
    // Enables regular immersive mode.
    // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
    // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    activity.window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            // Set the content to appear under the system bars so that the
            // content doesn't resize when the system bars hide and show.
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            // Hide the nav bar and status bar
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN)
}

// Shows the system bars by removing all the flags
// except for the ones that make the content appear under the system bars.
fun showSystemUI(activity: Activity) {
    activity.window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
}

fun setStatusBarColor(activity: Activity, color: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        activity.window.statusBarColor = color
    }
}

fun setStatusBarTextColor(activity: Activity, isDark: Boolean) {
    //6.0以上
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val decorView = activity.window.decorView
        var vis = decorView.systemUiVisibility
        vis = if (isDark) {
            vis or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            vis and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
        decorView.systemUiVisibility = vis
    }
}

fun getStatusBarHeight(context: Context): Int {
    val resourceId: Int = context.resources.getIdentifier("status_bar_height", "dimen", "android")
    return context.resources.getDimensionPixelSize(resourceId)
}

fun getNavigationBarHeight(context: Context): Int {
    val resourceId: Int =
        context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return context.resources.getDimensionPixelSize(resourceId)
}
