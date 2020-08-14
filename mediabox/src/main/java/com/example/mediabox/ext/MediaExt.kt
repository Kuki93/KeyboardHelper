package com.example.mediabox.ext

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.TypedValue
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
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
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

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

    if (width <= 0 || height <= 0) {
        mContentResolver.openInputStream(uri)?.use { input ->
            ExifInterface(input).apply {
                width = getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, -1)
                height = getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, -1)
            }
        }
    }

    if (width <= 0 || height <= 0) {
        mContentResolver.openFileDescriptor(uri, "r")?.use {
            it.fileDescriptor?.let { fd ->
                BitmapFactory.decodeFileDescriptor(fd)
            }?.also { bitmap ->
                width = bitmap.width
                height = bitmap.height
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
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            // Hide the nav bar and status bar
            or View.SYSTEM_UI_FLAG_FULLSCREEN)
}

// Shows the system bars by removing all the flags
// except for the ones that make the content appear under the system bars.
fun showSystemUI(activity: Activity) {
    activity.window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
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

fun isNavBarVisible(activity: Activity): Boolean {
    val viewGroup: ViewGroup? = activity.window.decorView as ViewGroup?
    if (viewGroup != null) {
        for (i in 0 until viewGroup.childCount) {
            val id: Int = viewGroup.getChildAt(i).id
            if (id != View.NO_ID) {
                var resourceEntryName: String? = null
                try {
                    resourceEntryName = activity.resources.getResourceEntryName(id)
                } catch (e: Resources.NotFoundException) {
                }
                if ((("navigationBarBackground" == resourceEntryName) && viewGroup.getChildAt(i).visibility == View.VISIBLE)) {
                    return true
                }
            }
        }
    }
    return false
}

fun View.applyTouchDelegate(
    expandTouchWidth: Int,
    l: View.OnClickListener? = null
) {
    val parentView = parent as View
    parentView.post {
        val rect = Rect()
        getHitRect(rect) // view构建完成后才能获取，所以放在post中执行
        // 4个方向增加矩形区域
        rect.top -= expandTouchWidth.coerceAtMost(rect.top)
        rect.bottom += expandTouchWidth
        rect.left -= expandTouchWidth.coerceAtMost(rect.left)
        rect.right += expandTouchWidth
        parentView.touchDelegate = TouchDelegate(rect, this)
        setOnClickListener(l)
    }
}

fun Context.dip(value: Float): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)
}

fun Context.dip(value: Int): Int {
    return dip(value.toFloat()).toInt()
}

fun Fragment.dip(value: Float): Float {
    return requireContext().dip(value)
}

fun Fragment.dip(value: Int): Int {
    return requireContext().dip(value)
}

fun View.dip(value: Float): Float {
    return context.dip(value)
}

fun View.dip(value: Int): Int {
    return context.dip(value)
}

inline fun <K, V> MutableMap<K, V>.putOrUpdate(
    key: K,
    putValue: () -> V,
    updateValue: (V) -> V
): V {
    val value = get(key)
    val answer = if (value == null) {
        putValue()
    } else {
        updateValue(value)
    }
    put(key, answer)
    return answer
}


inline fun <K, V, R, C : MutableCollection<in R>> Map<out K, V>.flatMapElementTo(
    destination: C,
    transform: (Map.Entry<K, V>) -> R
): C {
    for (element in this) {
        destination.add(transform(element))
    }
    return destination
}

fun copyFile(os: OutputStream, srcFile: File): Boolean {
    val inputStream = FileInputStream(srcFile)
    val bufferSize = 8192
    return try {
        val data = ByteArray(bufferSize)
        var len: Int
        while (inputStream.read(data, 0, bufferSize).also { len = it } != -1) {
            os.write(data, 0, len)
        }
        true
    } catch (e: IOException) {
        e.printStackTrace()
        false
    } finally {
        try {
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        try {
            os.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

@Throws(IOException::class)
fun Context.createMediaUri(
    appTag: String = "MediaBox",
    mimeType: String = JPG
): Pair<Uri, String>? {
    return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
        val uri = if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.INTERNAL_CONTENT_URI
        }
        val cur = System.currentTimeMillis()
        val locale = Locale.getDefault()
        val prefix = "${SimpleDateFormat("yyyyMMdd_HHmmss", locale).format(Date(cur))}_"
        val suffix = ".${MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"}"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.BUCKET_DISPLAY_NAME, appTag)
            put(MediaStore.Images.Media.RELATIVE_PATH, appTag)
            put(MediaStore.Images.Media.DATE_TAKEN, cur.toString())
            put(MediaStore.Images.Media.DISPLAY_NAME, "$prefix$suffix")
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        }
        contentResolver.insert(uri, values)?.let {
            it to it.toString()
        }
    } else {
        createMediaFile()?.let {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                FileProvider.getUriForFile(this, "${packageName}.FileProvider", it)
            } else {
                Uri.fromFile(it)
            } to it.absolutePath
        }
    }
}

@Throws(IOException::class)
fun Context.createMediaFile(
    appTag: String = "MediaBox",
    type: String = Environment.DIRECTORY_PICTURES,
    mimeType: String = JPG
): File? {
    return if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            externalMediaDirs.getOrNull(0)?.let {
                File(it, type)
            }
        } else null) ?: kotlin.run {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                File(Environment.getExternalStoragePublicDirectory(type), appTag)
            } else {
                getExternalFilesDir(type)
            }
        }
    } else {
        File(filesDir, type)
    }?.also {
        if (!it.exists()) {
            it.mkdirs()
        }
    }?.let { dir ->
        val locale = Locale.getDefault()
        val prefix = "${SimpleDateFormat("yyyyMMdd_HHmmss", locale).format(Date())}_"
        val suffix = ".${MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"}"
        File(dir, "$prefix$suffix").takeIf {
            it.createNewFile()
        }
    }
}

fun Context.notifySystemMedia(uri: Uri) {
    sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
}

fun Context.notifySystemMediaV2(
    paths: Array<String>,
    mimeTypes: Array<String>,
    callback: MediaScannerConnection.OnScanCompletedListener? = null
) {
    MediaScannerConnection.scanFile(
        applicationContext,
        paths,
        mimeTypes,
        callback
    )
}

fun Context.notifySystemMediaV2(
    path: String,
    mimeType: String,
    callback: MediaScannerConnection.OnScanCompletedListener? = null
) {
    MediaScannerConnection.scanFile(
        applicationContext,
        arrayOf(path),
        arrayOf(mimeType),
        callback
    )
}

fun Context.deleteFileByUri(uri: Uri): Boolean {
    if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
        contentResolver.delete(uri, null, null)
        return true
    }
    return false
}

fun deleteFileByPath(path: String): Boolean {
    File(path).takeIf {
        it.isFile && it.exists()
    }?.also {
        it.delete()
        return true
    }
    return false
}

fun Context.getRealFilePath(uri: Uri): String? {
    val scheme = uri.scheme
    var data: String? = null
    if (scheme == null) {
        data = uri.path
    } else if (ContentResolver.SCHEME_FILE == scheme) {
        data = uri.path
    } else if (ContentResolver.SCHEME_CONTENT == scheme) {
        val cursor = contentResolver.query(
            uri, arrayOf(MediaStore.Images.ImageColumns.DATA),
            null,
            null,
            null
        )
        if (null != cursor) {
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                if (index > -1) {
                    data = cursor.getString(index)
                }
            }
            cursor.close()
        }
    }
    return data
}
